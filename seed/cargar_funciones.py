"""
Carga la cartelera en Cloud Firestore (coleccion "funciones").

Acepta dos formatos de datos:

* cartelera_parkcity.json (TP7): la cartelera real de Park City Film, con la
  zona horaria y la sala del cine, y cada pelicula con sus funciones en fechas
  absolutas ("2026-11-20", "19:00") y su propio link de compra.
* funciones.json (TP2): seis funciones de prueba con fechas relativas a hoy
  ("dia": 1 es manana), para que la cartelera nunca quede vacia.

Uso (ver README.md para el paso a paso completo):
    python cargar_funciones.py --clave ruta/a/clave-servicio.json --datos cartelera_parkcity.json --limpiar
    python cargar_funciones.py --emulador --datos cartelera_parkcity.json --limpiar [--funcion-prueba 122]
"""
import argparse
import json
import urllib.request
from datetime import date, datetime, time, timedelta, timezone
from pathlib import Path
from zoneinfo import ZoneInfo

COLECCION = "funciones"


# ------------------------------- destinos -------------------------------

class DestinoFirebase:
    """Proyecto real, con el Admin SDK y la clave de una cuenta de servicio."""

    def __init__(self, clave: str):
        import firebase_admin
        from firebase_admin import credentials, firestore
        firebase_admin.initialize_app(credentials.Certificate(clave))
        self.db = firestore.client()

    def guardar(self, doc_id, datos):
        self.db.collection(COLECCION).document(doc_id).set(datos)

    def limpiar(self):
        n = 0
        for doc in self.db.collection(COLECCION).stream():
            doc.reference.delete()
            n += 1
        return n


def _valor_firestore(v):
    """Convierte un valor de Python al formato tipado de la API REST de Firestore."""
    if isinstance(v, bool):
        return {"booleanValue": v}
    if isinstance(v, int):
        return {"integerValue": str(v)}
    if isinstance(v, float):
        return {"doubleValue": v}
    if isinstance(v, datetime):
        return {"timestampValue": v.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")}
    return {"stringValue": str(v)}


class DestinoEmulador:
    """Firebase Emulator Suite, por REST. 'Bearer owner' saltea las reglas."""

    def __init__(self, host: str, proyecto: str):
        self.base = f"http://{host}/v1/projects/{proyecto}/databases/(default)/documents/{COLECCION}"
        self.cabeceras = {"Content-Type": "application/json", "Authorization": "Bearer owner"}

    def _pedir(self, url, metodo, cuerpo=None):
        pedido = urllib.request.Request(url, data=cuerpo, method=metodo, headers=self.cabeceras)
        with urllib.request.urlopen(pedido) as r:
            return json.loads(r.read() or b"{}")

    def guardar(self, doc_id, datos):
        cuerpo = json.dumps({"fields": {k: _valor_firestore(v) for k, v in datos.items()}}).encode()
        self._pedir(f"{self.base}/{doc_id}", "PATCH", cuerpo)

    def limpiar(self):
        docs = self._pedir(f"{self.base}?pageSize=500", "GET").get("documents", [])
        for d in docs:
            self._pedir(f"{self.base}/{d['name'].rsplit('/', 1)[1]}", "DELETE")
        return len(docs)


# ------------------------------- datos -------------------------------

def funciones_parkcity(datos: dict):
    """Formato del TP7: una entrada por pelicula, con sus funciones en fechas absolutas."""
    zona = ZoneInfo(datos["zona"])
    for p in datos["peliculas"]:
        for dia, hora in p["funciones"]:
            cuando = datetime.combine(date.fromisoformat(dia), time.fromisoformat(hora), zona)
            doc = {
                "titulo": p["titulo"], "director": p["director"], "anio": p["anio"],
                "duracionMin": p["duracionMin"], "sinopsis": p["sinopsis"],
                "sala": p.get("sala", datos["sala"]), "fechaHora": cuando,
                "precio": float(p["precio"]), "urlEntradas": p["urlEntradas"],
            }
            if p.get("tmdbId"):
                doc["tmdbId"] = p["tmdbId"]   # la app pide /movie/{id} a TMDB
            yield f"{p['slug']}-{dia}", doc


def funciones_tp2(lista: list, zona: ZoneInfo, url: str):
    """Formato del TP2: fechas relativas a hoy ("dia": 1 es manana)."""
    hoy = datetime.now(zona).replace(hour=0, minute=0, second=0, microsecond=0)
    for f in lista:
        f = dict(f)
        hh, mm = (int(x) for x in f.pop("hora").split(":"))
        f["fechaHora"] = hoy + timedelta(days=f.pop("dia"), hours=hh, minutes=mm)
        f["urlEntradas"] = url
        yield f.pop("id"), f


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--clave", help="JSON de la cuenta de servicio (proyecto real)")
    p.add_argument("--emulador", action="store_true",
                   help="cargar en el Firebase Emulator Suite en lugar del proyecto real")
    p.add_argument("--host-emulador", default="127.0.0.1:8080")
    p.add_argument("--proyecto", default="cartelera-tp2")
    p.add_argument("--datos", default=str(Path(__file__).with_name("cartelera_parkcity.json")))
    p.add_argument("--url", default="https://parkcityfilm.org/",
                   help="solo formato TP2: pagina de venta de entradas")
    p.add_argument("--zona", default="America/Denver", help="solo formato TP2")
    p.add_argument("--limpiar", action="store_true",
                   help="borra las funciones que ya haya antes de cargar")
    p.add_argument("--funcion-prueba", type=int, metavar="MINUTOS",
                   help="agrega una copia de la primera funcion que empieza dentro de MINUTOS, "
                        "para probar el recordatorio (se avisa 120 min antes)")
    args = p.parse_args()
    if not args.emulador and not args.clave:
        p.error("falta --clave (o usa --emulador)")

    destino = (DestinoEmulador(args.host_emulador, args.proyecto) if args.emulador
               else DestinoFirebase(args.clave))
    datos = json.loads(Path(args.datos).read_text(encoding="utf-8"))
    if isinstance(datos, dict):
        zona = ZoneInfo(datos["zona"])
        funciones = list(funciones_parkcity(datos))
    else:
        zona = ZoneInfo(args.zona)
        funciones = list(funciones_tp2(datos, zona, args.url))

    if args.limpiar:
        print(f"Borradas {destino.limpiar()} funciones anteriores.")

    for doc_id, doc in funciones:
        destino.guardar(doc_id, doc)
        print(f"  {doc_id:<48} {doc['fechaHora'].astimezone(zona):%a %d/%m %H:%M}  {doc['titulo']}")

    if args.funcion_prueba is not None:
        _, base = funciones[0]
        base = dict(base, titulo=f"{base['titulo']} (prueba)",
                    fechaHora=datetime.now(zona).replace(second=0, microsecond=0)
                    + timedelta(minutes=args.funcion_prueba))
        destino.guardar("prueba-recordatorio", base)
        print(f"  {'prueba-recordatorio':<48} {base['fechaHora']:%a %d/%m %H:%M}  {base['titulo']}")

    print(f"Listo: {len(funciones)} funciones cargadas.")


if __name__ == "__main__":
    main()
