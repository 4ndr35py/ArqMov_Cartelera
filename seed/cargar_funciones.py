"""
Carga la cartelera de prueba en Cloud Firestore.

Las fechas se calculan a partir de hoy ("dia": 1 es manana), para que la
cartelera nunca quede vacia por tener funciones en el pasado.

Uso (ver README.md para el paso a paso completo):
    python cargar_funciones.py --clave ruta/a/clave-servicio.json --url https://...

TP7, contra el Firebase Emulator Suite (no hace falta clave ni firebase-admin):
    python cargar_funciones.py --emulador --url https://... [--funcion-prueba 121]
"""
import argparse
import json
import urllib.request
from datetime import datetime, timedelta, timezone
from pathlib import Path
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError


def escritor_firebase(clave: str):
    """Escribe en el proyecto real con el Admin SDK (cuenta de servicio)."""
    import firebase_admin
    from firebase_admin import credentials, firestore

    firebase_admin.initialize_app(credentials.Certificate(clave))
    db = firestore.client()
    return lambda doc_id, datos: db.collection("funciones").document(doc_id).set(datos)


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


def escritor_emulador(host: str, proyecto: str):
    """Escribe en el emulador de Firestore por REST. 'Bearer owner' saltea las reglas."""
    base = f"http://{host}/v1/projects/{proyecto}/databases/(default)/documents/funciones"

    def guardar(doc_id, datos):
        cuerpo = json.dumps({"fields": {k: _valor_firestore(v) for k, v in datos.items()}}).encode()
        pedido = urllib.request.Request(
            f"{base}/{doc_id}", data=cuerpo, method="PATCH",
            headers={"Content-Type": "application/json", "Authorization": "Bearer owner"},
        )
        urllib.request.urlopen(pedido).close()

    return guardar


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--clave", help="JSON de la cuenta de servicio (proyecto real)")
    p.add_argument("--emulador", action="store_true",
                   help="cargar en el Firebase Emulator Suite en lugar del proyecto real")
    p.add_argument("--host-emulador", default="127.0.0.1:8080")
    p.add_argument("--proyecto", default="cartelera-tp2")
    p.add_argument("--url", required=True, help="URL de la pagina de venta de entradas")
    p.add_argument("--zona", default="America/Argentina/Cordoba")
    p.add_argument("--datos", default=str(Path(__file__).with_name("funciones.json")))
    p.add_argument(
        "--funcion-prueba", type=int, metavar="MINUTOS",
        help="TP7: agrega la funcion 'f99' (copia de la primera) que empieza dentro de "
             "MINUTOS, para probar el recordatorio (se avisa 120 min antes)",
    )
    args = p.parse_args()
    if not args.emulador and not args.clave:
        p.error("falta --clave (o usa --emulador)")

    guardar = (escritor_emulador(args.host_emulador, args.proyecto) if args.emulador
               else escritor_firebase(args.clave))
    try:
        zona = ZoneInfo(args.zona)
    except ZoneInfoNotFoundError:
        # Python en Windows no trae la base de zonas (paquete tzdata). Argentina
        # usa UTC-3 todo el año, sin horario de verano, asi que alcanza con eso.
        print(f"Aviso: no encontre la zona {args.zona}; uso UTC-3.")
        zona = timezone(timedelta(hours=-3))
    hoy = datetime.now(zona).replace(hour=0, minute=0, second=0, microsecond=0)

    funciones = json.loads(Path(args.datos).read_text(encoding="utf-8"))
    for f in funciones:
        # "tmdbId" (opcional) viaja tal cual: la app lo usa para pedir /movie/{id} a TMDB.
        hh, mm = (int(x) for x in f.pop("hora").split(":"))
        fecha = hoy + timedelta(days=f.pop("dia"), hours=hh, minutes=mm)
        doc_id = f.pop("id")
        f["fechaHora"] = fecha
        f["urlEntradas"] = args.url
        guardar(doc_id, f)
        print(f"  {doc_id}  {fecha:%a %d/%m %H:%M}  {f['titulo']}")

    if args.funcion_prueba is not None:
        base = json.loads(Path(args.datos).read_text(encoding="utf-8"))[0]
        for clave in ("id", "dia", "hora"):
            base.pop(clave)
        fecha = datetime.now(zona).replace(second=0, microsecond=0) + timedelta(minutes=args.funcion_prueba)
        base.update(titulo=f"{base['titulo']} (prueba)", fechaHora=fecha, urlEntradas=args.url)
        guardar("f99", base)
        print(f"  f99  {fecha:%a %d/%m %H:%M}  {base['titulo']}")

    print(f"Listo: {len(funciones)} funciones cargadas.")


if __name__ == "__main__":
    main()
