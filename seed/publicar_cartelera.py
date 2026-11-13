"""
Genera el JSON estatico de la cartelera (TP3): hosting/public/cartelera.json.

La cartelera cambia pocas veces por semana, asi que el cine la publica como
un archivo estatico en Firebase Hosting, que lo sirve desde su CDN. Asi, mil
aperturas de la app no consumen mil lecturas de Firestore (cuota gratuita).

Este script NO escribe en Firestore y NO necesita clave de servicio: lee
seed/cartelera_parkcity.json y genera el archivo. La publicacion es un paso
aparte (firebase deploy --only hosting), ver TP3_Escalabilidad/.

Uso:
    python seed/publicar_cartelera.py
    python seed/publicar_cartelera.py --datos seed/cartelera_parkcity.json --desde 2026-11-13
"""
import argparse
import json
import os
from datetime import date, datetime, time
from pathlib import Path
from zoneinfo import ZoneInfo

VERSION = 1
RAIZ = Path(__file__).resolve().parent.parent
DATOS_POR_DEFECTO = RAIZ / "seed" / "cartelera_parkcity.json"
SALIDA_POR_DEFECTO = RAIZ / "hosting" / "public" / "cartelera.json"


def construir(datos: dict, desde: datetime) -> tuple[dict, list[str]]:
    """Arma el documento de cartelera. Devuelve (documento, advertencias)."""
    zona = ZoneInfo(datos["zona"])
    filas = []          # (cuando, funcion) para ordenar por fecha real
    ids: set[str] = set()
    advertencias: list[str] = []

    for p in datos["peliculas"]:
        url = p.get("urlEntradas", "")  # vacio = pelicula gratis, sin link de compra
        if url and not url.startswith("https://"):
            raise ValueError(f"urlEntradas sin https en '{p['slug']}': {url}")

        for dia, hora in p["funciones"]:
            cuando = datetime.combine(date.fromisoformat(dia), time.fromisoformat(hora), zona)
            if cuando < desde:
                continue
            fid = f"{p['slug']}-{dia}"
            if fid in ids:
                raise ValueError(f"id de funcion repetido: {fid}")
            ids.add(fid)
            funcion = {
                "id": fid,
                "titulo": p["titulo"],
                "director": p["director"],
                "anio": p["anio"],
                "duracionMin": p["duracionMin"],
                "sinopsis": p["sinopsis"],
                "sala": p.get("sala", datos["sala"]),
                "fechaHora": cuando.isoformat(),   # con desfase horario: sin ambiguedad
                "precio": float(p["precio"]),
                "urlEntradas": url,
            }
            if p.get("tmdbId"):
                funcion["tmdbId"] = p["tmdbId"]
            filas.append((cuando, funcion))

    filas.sort(key=lambda fila: fila[0])
    documento = {
        "version": VERSION,
        "cine": datos["cine"],
        "zona": datos["zona"],
        "generado": datetime.now(ZoneInfo("UTC")).isoformat(timespec="seconds"),
        "funciones": [funcion for _, funcion in filas],
    }
    return documento, advertencias


def escribir(salida: Path, documento: dict) -> None:
    """Escribe en un archivo temporal y lo reemplaza: nunca queda un JSON a medias."""
    salida.parent.mkdir(parents=True, exist_ok=True)
    temporal = salida.with_name(salida.name + ".tmp")
    temporal.write_text(json.dumps(documento, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    os.replace(temporal, salida)


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--datos", type=Path, default=DATOS_POR_DEFECTO)
    p.add_argument("--salida", type=Path, default=SALIDA_POR_DEFECTO)
    p.add_argument("--desde", type=date.fromisoformat,
                   help="fecha desde la que incluir funciones (AAAA-MM-DD). Por defecto: hoy en la zona del cine")
    args = p.parse_args()

    datos = json.loads(args.datos.read_text(encoding="utf-8"))
    zona = ZoneInfo(datos["zona"])
    if args.desde:
        desde = datetime.combine(args.desde, time(0, 0), zona)
    else:
        hoy = datetime.now(zona).date()
        desde = datetime.combine(hoy, time(0, 0), zona)

    documento, advertencias = construir(datos, desde)
    if not documento["funciones"]:
        raise SystemExit("No quedan funciones desde esa fecha: no se escribe el archivo.")

    escribir(args.salida, documento)
    for aviso in advertencias:
        print(f"ADVERTENCIA: {aviso}")
    print(f"Listo: {len(documento['funciones'])} funciones desde {desde:%d/%m/%Y} -> {args.salida}")


if __name__ == "__main__":
    main()
