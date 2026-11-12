# Cartelera (MVP) · TP2 Arquitecturas Móviles

MVP de una cartelera para un cine independiente. Android nativo en Kotlin,
arquitectura MVVM (ViewModel + LiveData + repositorios) y Firebase como
backend: Authentication (email y contraseña), Cloud Firestore y Analytics.

Compilado y probado con AGP 9.4.1, Gradle 9.8.2, Kotlin 2.4.21,
compileSdk/targetSdk 37, minSdk 24 y Firebase BoM 35.1.0.

> **TP7 (rama `tp7`): integración de APIs y SDKs.** El estado entregado del
> TP2 quedó en el tag `tp2-entregado`. Ver la sección [TP7](#tp7-integraciones)
> al final.

## 1. Abrir el proyecto

1. Descomprimí la carpeta y abrila desde Android Studio (File → Open).
2. Esperá el Gradle Sync. Si Android Studio ofrece actualizar AGP o Gradle,
   podés aceptar.

El proyecto **no compila** hasta que agregues `app/google-services.json`
(paso 2). Ese archivo identifica tu proyecto de Firebase y no se versiona.

## 2. Configurar Firebase (una sola vez)

1. Entrá a https://console.firebase.google.com y creá un proyecto
   (por ejemplo `cartelera-tp2`). Dejá Google Analytics **activado** y elegí
   o creá una cuenta de Analytics.
2. En la portada del proyecto, tocá el ícono de Android para registrar la app:
   - Nombre del paquete: `com.example.cartelera`
   - El SHA-1 no hace falta para email y contraseña.
3. Descargá `google-services.json` y copialo en la carpeta `app/`.
4. **Authentication** → Comenzar → pestaña *Sign-in method* → habilitá
   *Correo electrónico/contraseña*.
5. **Firestore Database** → Crear base de datos → modo producción →
   ubicación `southamerica-east1` (São Paulo).
6. Firestore → pestaña *Reglas* → pegá el contenido de
   `firebase/firestore.rules` → Publicar.
7. Firestore te va a pedir un índice la primera vez que corra la consulta de
   cartelera si hiciera falta: el error en Logcat trae el link para crearlo
   con un clic. (Con un solo campo de filtro y orden normalmente no lo pide.)

## 3. Cargar la cartelera de prueba

El script calcula las fechas desde mañana en adelante, así la cartelera
nunca aparece vacía. Necesitás Python 3.10 o superior.

1. Consola de Firebase → ⚙ Configuración del proyecto → *Cuentas de servicio*
   → *Generar nueva clave privada*. Guardá el JSON **fuera** del proyecto,
   por ejemplo en `~/claves/cartelera-admin.json`. No lo subas a ningún repo.
2. En una terminal, desde la carpeta del proyecto:

```bash
cd seed
python3 -m venv .venv
source .venv/bin/activate          # en Windows: .venv\Scripts\activate
pip install firebase-admin
python cargar_funciones.py \
  --clave ~/claves/cartelera-admin.json \
  --url "https://www.ejemplo-cine.com.ar/entradas"
deactivate
```

`--url` es la página de venta de entradas que abre el botón *Comprar entrada*.
Usá la del cine real si tenés una; si no, cualquier URL https sirve para medir
el clic. Para editar las películas, modificá `seed/funciones.json` y volvé a
correr el script (sobrescribe por id).

## 4. Correr la app

Ejecutá `app` sobre un emulador **con Google Play** (imagen "Google Play" o
"Google APIs") o sobre un teléfono. Creá una cuenta con *Crear cuenta*.

Tests unitarios (validación y formato de fechas):

```bash
./gradlew testDebugUnitTest
```

## 5. Ver los eventos de Analytics en tiempo real (DebugView)

Los eventos tardan hasta 24 h en aparecer en los reportes normales. Para verlos
en el momento durante las pruebas:

```bash
adb shell setprop debug.firebase.analytics.app com.example.cartelera
```

Después: consola de Firebase → Analytics → **DebugView**. Para desactivarlo:

```bash
adb shell setprop debug.firebase.analytics.app .none.
```

Eventos que registra la app (ver `analytics/Metricas.kt`):

| Evento            | Cuándo                                   | Métrica |
|-------------------|------------------------------------------|---------|
| `sign_up`         | cuenta creada                            | M1      |
| `login`           | ingreso con cuenta existente             | M1      |
| `error_auth`      | falla de registro o ingreso (`tipo`)     | M1      |
| `ver_cartelera`   | carga de la lista (`cantidad`)           | M2      |
| `ver_funcion`     | apertura del detalle (`funcion_id`)      | M2, M4  |
| `marcar_interes`  | toca *Me interesa* (`valor` si/no)       | M3      |
| `click_comprar`   | toca *Comprar entrada*                   | M4      |
| `cerrar_sesion`   | cierra sesión                            | —       |

## Estructura

```
app/src/main/java/com/example/cartelera/
├── CarteleraApp.kt              Application: inicializa Analytics
├── analytics/Metricas.kt        nombres de eventos y parámetros
├── data/                        modelo y repositorios (Firebase)
│   ├── Funcion.kt
│   ├── AuthRepository.kt
│   └── FuncionRepository.kt
├── ui/login/                    LoginActivity + LoginViewModel
├── ui/cartelera/                CarteleraActivity + ViewModel + Adapter
├── ui/detalle/                  DetalleActivity + DetalleViewModel
└── util/                        Validador y Formato (lógica pura, testeada)
firebase/firestore.rules         reglas de seguridad
seed/                            carga de la cartelera de prueba
```

## TP7: integraciones

| Integración | Tipo | Entra por | Para qué |
|---|---|---|---|
| Firebase Cloud Messaging + WorkManager | SDK | `MensajeriaRepository`, `RecordatorioRepository`, `data/avisos/` | Recordatorio 2 h antes de una función marcada |
| Sharesheet de Android (`ACTION_SEND`) | API de la plataforma | `DetalleActivity` + `util/TextoCompartir` | Compartir una función por WhatsApp u otra app |
| TMDB (API REST) con Retrofit | API REST | `PeliculaRepository`, `data/tmdb/` | Afiche, sinopsis, duración y géneros en es-AR |
| Firebase Auth anónimo + vinculación | (mismo SDK de Auth) | `AuthRepository` | Ver la cartelera sin cuenta; el email se pide al marcar |

Ninguna Activity ni ViewModel importa clases de Firebase, Retrofit ni WorkManager.

**Recordatorio local en lugar de Cloud Functions.** El proyecto queda en el plan
gratuito de Firebase (Spark), que no permite desplegar Functions. El aviso se
programa en el teléfono con WorkManager al marcar la función y se cancela al
desmarcarla. FCM sigue integrado: el token se guarda en `usuarios/{uid}.tokenFcm`
y los mensajes que se envían desde la consola llegan a `AvisosService`.

### Configuración extra

1. **API key de TMDB.** Creá una cuenta en themoviedb.org → Configuración → API
   y pedí una clave. Pegala en `local.properties` (git lo ignora):
   `TMDB_API_KEY=tu_clave`. Sin clave, la app muestra los datos que cargó el cine.
2. **Ingreso anónimo.** Consola de Firebase → Authentication → Sign-in method →
   habilitá *Anónimo*.
3. **Reglas.** Volvé a publicar `firebase/firestore.rules` (agrega `usuarios/{uid}`
   para el token FCM).
4. **Cartelera.** Agregá `"tmdbId"` a cada función de `seed/funciones.json` y volvé
   a correr el script. Para probar el recordatorio:
   `python cargar_funciones.py --clave ... --url ... --funcion-prueba 125`
   crea la función `f99`, que empieza en 125 minutos (el aviso llega a los 5).

### Tests

`./gradlew testDebugUnitTest` corre 31 tests: los 10 del TP2 y los del TP7
(`PeliculaRepositoryTest` con MockWebServer, `FichaTest`, `TextoCompartirTest`
y `RecordatorioTest`).

Datos de películas e imágenes: [TMDB](https://www.themoviedb.org). This product
uses the TMDB API but is not endorsed or certified by TMDB.
