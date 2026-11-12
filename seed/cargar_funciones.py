"""
Carga la cartelera de prueba en Cloud Firestore.

Las fechas se calculan a partir de hoy ("dia": 1 es manana), para que la
cartelera nunca quede vacia por tener funciones en el pasado.

Uso (ver README.md para el paso a paso completo):
    python cargar_funciones.py --clave ruta/a/clave-servicio.json --url https://...
"""
import argparse
import json
from datetime import datetime, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

import firebase_admin
from firebase_admin import credentials, firestore


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--clave", required=True, help="JSON de la cuenta de servicio")
    p.add_argument("--url", required=True, help="URL de la pagina de venta de entradas")
    p.add_argument("--zona", default="America/Argentina/Cordoba")
    p.add_argument("--datos", default=str(Path(__file__).with_name("funciones.json")))
    p.add_argument(
        "--funcion-prueba", type=int, metavar="MINUTOS",
        help="TP7: agrega la funcion 'f99' (copia de la primera) que empieza dentro de "
             "MINUTOS, para probar el recordatorio (se avisa 120 min antes)",
    )
    args = p.parse_args()

    firebase_admin.initialize_app(credentials.Certificate(args.clave))
    db = firestore.client()
    zona = ZoneInfo(args.zona)
    hoy = datetime.now(zona).replace(hour=0, minute=0, second=0, microsecond=0)

    funciones = json.loads(Path(args.datos).read_text(encoding="utf-8"))
    for f in funciones:
        # "tmdbId" (opcional) viaja tal cual: la app lo usa para pedir /movie/{id} a TMDB.
        hh, mm = (int(x) for x in f.pop("hora").split(":"))
        fecha = hoy + timedelta(days=f.pop("dia"), hours=hh, minutes=mm)
        doc_id = f.pop("id")
        f["fechaHora"] = fecha
        f["urlEntradas"] = args.url
        db.collection("funciones").document(doc_id).set(f)
        print(f"  {doc_id}  {fecha:%a %d/%m %H:%M}  {f['titulo']}")

    if args.funcion_prueba is not None:
        base = json.loads(Path(args.datos).read_text(encoding="utf-8"))[0]
        for clave in ("id", "dia", "hora"):
            base.pop(clave)
        fecha = datetime.now(zona).replace(second=0, microsecond=0) + timedelta(minutes=args.funcion_prueba)
        base.update(titulo=f"{base['titulo']} (prueba)", fechaHora=fecha, urlEntradas=args.url)
        db.collection("funciones").document("f99").set(base)
        print(f"  f99  {fecha:%a %d/%m %H:%M}  {base['titulo']}")

    print(f"Listo: {len(funciones)} funciones cargadas.")


if __name__ == "__main__":
    main()
