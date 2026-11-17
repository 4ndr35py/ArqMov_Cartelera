package com.example.cartelera.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.cartelera.util.Accesibilidad
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Cartelera desde el archivo estatico que publica el cine en Firebase Hosting
 * (TP3, seed/publicar_cartelera.py -> hosting/public/cartelera.json).
 *
 * Orden de lectura:
 * 1. la red: cartelera.json, servido por la CDN de Hosting (cache de 5 min);
 * 2. si la red falla: la ultima copia guardada en el telefono;
 * 3. si no hay copia (primera apertura sin red, o Hosting caido): Firestore,
 *    como antes del TP3.
 *
 * En el plan Spark cada lectura de Firestore consume cupo diario, por eso
 * Firestore queda como ultimo recurso y no como camino normal.
 *
 * Los callbacks llegan siempre en el hilo principal, como los de Firestore.
 */
class CarteleraRepository internal constructor(
    private val cliente: OkHttpClient,
    private val url: String,
    private val copiaLocal: File?,
    private val respaldoFirestore: (Date, (Result<List<Funcion>>) -> Unit) -> Unit,
    private val entregar: (() -> Unit) -> Unit
) {

    constructor(context: Context, firestore: FuncionRepository = FuncionRepository()) : this(
        cliente = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build(),
        url = URL_CARTELERA,
        copiaLocal = File(context.filesDir, "cartelera.json"),
        respaldoFirestore = { desde, alTerminar -> firestore.cargarCartelera(desde, alTerminar) },
        entregar = { accion -> Handler(Looper.getMainLooper()).post(accion) }
    )

    /** Membresias y pases a la venta, publicados junto con la cartelera. */
    fun cargarPases(alTerminar: (Result<List<Pase>>) -> Unit) {
        val pedido = Request.Builder().url(url).build()
        cliente.newCall(pedido).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val cuerpo = response.use { if (it.isSuccessful) it.body.string() else null }
                val pases = cuerpo?.let { parsearPases(it) }
                entregar {
                    alTerminar(
                        if (pases != null) Result.success(pases)
                        else Result.failure(IOException("La cartelera no trae pases"))
                    )
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                entregar { alTerminar(Result.failure(e)) }
            }
        })
    }

    /** Funciones desde [desde] en adelante, ordenadas por fecha. */
    fun cargar(desde: Date, alTerminar: (Result<List<Funcion>>) -> Unit) {
        val pedido = Request.Builder().url(url).build()
        cliente.newCall(pedido).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val cuerpo = response.use { if (it.isSuccessful) it.body.string() else null }
                val funciones = cuerpo?.let { parsear(it) }
                if (cuerpo != null && funciones != null) {
                    guardarCopia(cuerpo)
                    entregar { alTerminar(Result.success(filtrar(funciones, desde))) }
                } else {
                    usarCopiaOFirestore(desde, alTerminar)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                usarCopiaOFirestore(desde, alTerminar)
            }
        })
    }

    private fun usarCopiaOFirestore(desde: Date, alTerminar: (Result<List<Funcion>>) -> Unit) {
        val copia = leerCopia()?.let { parsear(it) }
        entregar {
            if (copia != null) alTerminar(Result.success(filtrar(copia, desde)))
            else respaldoFirestore(desde, alTerminar)
        }
    }

    private fun filtrar(funciones: List<Funcion>, desde: Date): List<Funcion> =
        funciones
            .filter { f -> f.fechaHora?.let { !it.before(desde) } ?: false }
            .sortedBy { it.fechaHora }

    private fun guardarCopia(cuerpo: String) {
        val archivo = copiaLocal ?: return
        try {
            val temporal = File(archivo.parentFile, archivo.name + ".tmp")
            temporal.writeText(cuerpo, Charsets.UTF_8)
            if (!temporal.renameTo(archivo)) {
                archivo.writeText(cuerpo, Charsets.UTF_8)
                temporal.delete()
            }
        } catch (e: IOException) {
            // Sin copia local la app sigue funcionando; solo se pierde el respaldo sin red.
        }
    }

    private fun leerCopia(): String? =
        copiaLocal?.takeIf { it.exists() }?.let {
            try { it.readText(Charsets.UTF_8) } catch (e: IOException) { null }
        }

    companion object {
        /** Archivo publicado por el cine (Firebase Hosting, sitio cartelera-tp2). */
        const val URL_CARTELERA = "https://cartelera-tp2.web.app/cartelera.json"

        private val json = Json { ignoreUnknownKeys = true }

        /** Pases y membresias del JSON publicado; null si el archivo no tiene esa forma. */
        internal fun parsearPases(texto: String): List<Pase>? = try {
            json.decodeFromString(CarteleraPublicada.serializer(), texto)
                .pases.map { Pase(it.nombre, it.precio, it.membresia, it.entradasPorFuncion, it.cancelable, it.renovacion) }
        } catch (e: Exception) {
            null
        }

        /**
         * Convierte el JSON publicado en funciones. Devuelve null si el archivo
         * no tiene la forma esperada (por ejemplo, una pagina de error HTML).
         */
        internal fun parsear(texto: String): List<Funcion>? = try {
            json.decodeFromString(CarteleraPublicada.serializer(), texto)
                .funciones.map { it.aFuncion() }
        } catch (e: Exception) {
            null
        }
    }
}

/** Forma del archivo cartelera.json (ver seed/publicar_cartelera.py). */
@Serializable
internal data class CarteleraPublicada(
    val version: Int = 0,
    val cine: String = "",
    val zona: String = "",
    val generado: String = "",
    val funciones: List<FuncionPublicada> = emptyList(),
    val pases: List<PasePublicado> = emptyList()
)

@Serializable
internal data class PasePublicado(
    val nombre: String = "",
    val precio: Double = 0.0,
    val membresia: Boolean = false,
    val entradasPorFuncion: Int? = null,
    val cancelable: Boolean = false,
    val renovacion: Boolean = false
)

@Serializable
internal data class TipoPublicado(
    val nombre: String = "",
    val precio: Double = 0.0
)

@Serializable
internal data class FuncionPublicada(
    val id: String = "",
    val titulo: String = "",
    val director: String = "",
    val anio: Int = 0,
    val duracionMin: Int = 0,
    val sinopsis: String = "",
    val sala: String = "",
    /** ISO 8601 con desfase, por ejemplo 2026-11-13T19:30:00-07:00. */
    val fechaHora: String,
    val precio: Double = 0.0,
    val urlEntradas: String = "",
    val tmdbId: Int? = null,
    /** TP6: OC, HI, VI. Ausente en las cargas anteriores: lista vacia. */
    val accesibilidad: List<String> = emptyList(),
    /** Tipos de entrada con precio (publicados desde Eventive). Ausente en cargas viejas: lista vacia. */
    val tipos: List<TipoPublicado> = emptyList(),
    /** Afiche de Eventive (URL https). Ausente en cargas viejas: null. */
    val afiche: String? = null
) {
    fun aFuncion() = Funcion(
        id = id,
        titulo = titulo,
        director = director,
        anio = anio,
        duracionMin = duracionMin,
        sinopsis = sinopsis,
        sala = sala,
        fechaHora = parsearFecha(fechaHora),
        precio = precio,
        urlEntradas = urlEntradas,
        tmdbId = tmdbId,
        accesibilidad = Accesibilidad.normalizar(accesibilidad),
        tipos = tipos.map { TipoEntrada(it.nombre, it.precio) },
        afiche = afiche?.takeIf { it.startsWith("https://") }
    )

    private fun parsearFecha(texto: String): Date =
        // XXX acepta el desfase "-07:00" y funciona desde API 24 (java.time no).
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(texto)
            ?: throw IllegalArgumentException("Fecha invalida: $texto")
}
