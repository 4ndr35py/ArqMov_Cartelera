"""
Arma la cartelera desde Eventive (solo lectura) con la misma forma que cartelera.json.

Lee event_buckets, films y events con la clave secreta de local.properties
(EVENTIVE_SECRET_KEY). La clave no se imprime ni se guarda. No pide tickets
ni orders: esos datos tienen compradores y no hacen falta para la cartelera.

Uso:
    python seed/desde_eventive.py --salida RUTA.json
    python seed/desde_eventive.py --salida RUTA.json --desde 2026-11-01 --bucket "Park City Film"
"""
import argparse
import html
import json
import re
import urllib.error
import urllib.request
from datetime import datetime, date, time
from pathlib import Path
from zoneinfo import ZoneInfo

RAIZ = Path(__file__).resolve().parent.parent
API = "https://api.eventive.org"
ZONA = "America/Denver"
CINE = "Park City Film"


def clave_secreta() -> str:
    for linea in (RAIZ / "local.properties").read_text(encoding="utf-8").splitlines():
        if linea.startswith("EVENTIVE_SECRET_KEY="):
            return linea.split("=", 1)[1].strip()
    raise SystemExit("Falta EVENTIVE_SECRET_KEY en local.properties")


def pedir(ruta: str, clave: str) -> dict:
    pedido = urllib.request.Request(API + ruta, headers={"x-api-key": clave, "Accept-Version": "~1", "User-Agent": "cartelera-sync/1.0"})
    try:
        with urllib.request.urlopen(pedido, timeout=60) as respuesta:
            return json.loads(respuesta.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raise SystemExit(f"Eventive {ruta}: HTTP {e.code}")


def texto_plano(s: str) -> str:
    sin_etiquetas = re.sub(r"<[^>]+>", " ", s or "")
    return re.sub(r"\s+", " ", html.unescape(sin_etiquetas)).strip()


def numero(s, por_defecto: int = 0) -> int:
    m = re.search(r"\d+", str(s or ""))
    return int(m.group()) if m else por_defecto


def datos_del_seed(ruta: Path) -> dict:
    """titulo en minusculas -> tmdbId y accesibilidad del seed manual (para afiches y subtitulos)."""
    if not ruta.exists():
        return {}
    seed = json.loads(ruta.read_text(encoding="utf-8"))
    return {
        p["titulo"].strip().lower(): {"tmdbId": p.get("tmdbId"), "accesibilidad": p.get("accesibilidad")}
        for p in seed.get("peliculas", [])
    }


SIN_TIPO = re.compile(r"donat|support|suggested|waitlist|lovers club|film club|pay at|comp|punch|coupon|dinner", re.I)


def tipos_de(ev: dict) -> list:
    """Tipos de entrada a la venta (con precio): General, Student/Senior, Member, VIP, etc. Sin donaciones ni cortesias."""
    vistos = {}
    for tb in ev.get("ticket_buckets") or []:
        nombre = (tb.get("name") or "").strip()
        precio = tb.get("price", 0)
        if precio <= 0 or SIN_TIPO.search(nombre):
            continue
        vistos[(nombre, precio)] = True
    return [{"nombre": n, "precio": p / 100} for (n, p) in sorted(vistos, key=lambda k: (k[1], k[0]))]


def max_por_funcion(pb: dict):
    """Cuantas entradas por funcion da el pase (regla de Eventive), o None si no hay limite."""
    for regla in pb.get("rules") or []:
        uso = ((regla.get("restrictions") or {}).get("uses") or {})
        if uso.get("max_per_event"):
            return int(uso["max_per_event"])
    return None


def pases_de(pases: list, ids_bucket: set) -> list:
    """Membresias y pases a la venta, con precio en dolares (los de precio vacio o de prueba no se muestran)."""
    filas = []
    for pb in pases:
        nombre = (pb.get("name") or "").strip()
        if pb.get("event_bucket") not in ids_bucket or pb.get("visibility") != "visible":
            continue
        if "test" in nombre.lower():
            continue
        precio = pb.get("price")
        if not precio or precio <= 0:
            continue
        filas.append({
            "nombre": nombre,
            "precio": precio / 100,
            "membresia": bool(pb.get("is_membership")),
            "entradasPorFuncion": max_por_funcion(pb),
            "cancelable": bool(pb.get("cancellation_allowed")),
            "renovacion": bool(pb.get("recurrence_enabled")),
        })
    return sorted(filas, key=lambda x: (not x["membresia"], x["precio"]))


def construir(eventos: list, films: dict, ids_bucket: set, desde: datetime, seed: dict) -> list:
    zona = ZoneInfo(ZONA)
    funciones = []
    for ev in eventos:
        if ev.get("event_bucket") not in ids_bucket or not ev.get("is_dated"):
            continue
        if ev.get("visibility") != "visible" or not ev.get("start_time"):
            continue
        inicio = datetime.fromisoformat(ev["start_time"].replace("Z", "+00:00")).astimezone(zona)
        if inicio < desde:
            continue
        # Las entradas de cortesia (precio 0) no cuentan: el precio es el de la entrada mas barata a la venta.
        tipos = tipos_de(ev)
        generales = [t["precio"] for t in tipos if "general" in t["nombre"].lower()]
        precio = generales[0] if generales else (min(t["precio"] for t in tipos) if tipos else 0.0)
        url = ev.get("external_tickets_url") or ""
        if url and not url.startswith("https://"):
            url = ""
        sala = (ev.get("venue") or {}).get("name", "")
        for ref in ev.get("films") or []:
            f = films.get(ref.get("id"), ref)
            credito = f.get("credits") or {}
            detalle = f.get("details") or {}
            titulo = f.get("name", "")
            extra = seed.get(titulo.strip().lower(), {})
            fila = {
                "id": f"{ev['id']}-{ref.get('id', '')}",
                "titulo": titulo,
                "director": credito.get("director", "") if isinstance(credito, dict) else "",
                "anio": numero(detalle.get("year") if isinstance(detalle, dict) else None),
                "duracionMin": numero(detalle.get("runtime") if isinstance(detalle, dict) else None),
                "sinopsis": texto_plano(f.get("short_description") or f.get("description") or ""),
                "sala": sala,
                "fechaHora": inicio.isoformat(),
                "precio": float(precio),
                "urlEntradas": url,
            }
            fila["tipos"] = tipos
            afiche = f.get("poster_image") or f.get("cover_image")
            if afiche and afiche.startswith("https://"):
                fila["afiche"] = afiche
            if extra.get("tmdbId"):
                fila["tmdbId"] = extra["tmdbId"]
            if extra.get("accesibilidad"):
                fila["accesibilidad"] = extra["accesibilidad"]
            funciones.append(fila)
    funciones.sort(key=lambda x: x["fechaHora"])
    return funciones


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--salida", type=Path, required=True)
    p.add_argument("--bucket", default=CINE, help="nombre exacto del event bucket")
    p.add_argument("--desde", type=date.fromisoformat, default=None,
                   help="desde esta fecha (AAAA-MM-DD). Por defecto: hoy en la zona del cine")
    args = p.parse_args()

    clave = clave_secreta()
    buckets = pedir("/event_buckets", clave)["event_buckets"]
    ids = {b["id"] for b in buckets if b.get("name") == args.bucket}
    if not ids:
        raise SystemExit(f"No hay event bucket llamado {args.bucket!r}")
    films = {f["id"]: f for f in pedir("/films", clave)["films"]}
    eventos = pedir("/events", clave)["events"]
    pases = pases_de(pedir("/pass_buckets", clave)["pass_buckets"], ids)

    zona = ZoneInfo(ZONA)
    desde = datetime.combine(args.desde or datetime.now(zona).date(), time(0, 0), zona)
    funciones = construir(eventos, films, ids, desde, datos_del_seed(RAIZ / "seed" / "cartelera_parkcity.json"))
    if not funciones:
        raise SystemExit("Eventive no devolvio funciones desde esa fecha: no se escribe el archivo.")

    documento = {
        "version": 1,
        "cine": CINE,
        "zona": ZONA,
        "generado": datetime.now(ZoneInfo("UTC")).isoformat(timespec="seconds"),
        "funciones": funciones,
        "pases": pases,
    }
    args.salida.parent.mkdir(parents=True, exist_ok=True)
    temporal = args.salida.with_name(args.salida.name + ".tmp")
    temporal.write_text(json.dumps(documento, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    temporal.replace(args.salida)

    peliculas = {f["titulo"] for f in funciones}
    print(f"Listo: {len(funciones)} funciones de {len(peliculas)} peliculas -> {args.salida}")


if __name__ == "__main__":
    main()
