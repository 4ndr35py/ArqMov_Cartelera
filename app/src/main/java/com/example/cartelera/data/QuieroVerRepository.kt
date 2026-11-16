package com.example.cartelera.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cartelera.data.avisos.QuieroVerWorker
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

/**
 * Lista "Quiero verla" (TP6, estrategia 2), guardada en el telefono.
 *
 * Es local a proposito: no pide cuenta, no escribe en Firestore (cuota de
 * Spark) y el trabajo periodico que avisa de funciones nuevas puede leerla sin
 * sesion. La contrapartida: la lista no se sincroniza entre dispositivos.
 *
 * Guarda clave de pelicula -> titulo, y las funciones ya avisadas, para no
 * repetir un aviso.
 */
class QuieroVerRepository(context: Context) {

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val workManager get() = WorkManager.getInstance(app)

    /** Claves de pelicula marcadas ("tmdb:..." o "titulo:..."). */
    fun marcadas(): Set<String> = titulos().keys

    /** Clave -> titulo, para mostrar la lista sin volver a buscarlos. */
    fun titulos(): Map<String, String> {
        val guardado = prefs.getString(CLAVE_PELICULAS, null) ?: return emptyMap()
        return try {
            json.decodeFromString(MAPA, guardado)
        } catch (e: SerializationException) {
            emptyMap()
        }
    }

    fun estaMarcada(clave: String): Boolean = clave in marcadas()

    fun yaAvisadas(): Set<String> = prefs.getStringSet(CLAVE_AVISADAS, emptySet()).orEmpty()

    /**
     * Marca una pelicula. [funcionesActuales] son las funciones que ya estaban
     * programadas: se dan por avisadas, asi solo se avisa de las que aparezcan
     * despues. Por eso se registran antes de programar el trabajo periodico.
     */
    fun marcar(clave: String, titulo: String, funcionesActuales: Collection<String>) {
        val nuevo = titulos() + (clave to titulo)
        prefs.edit()
            .putString(CLAVE_PELICULAS, json.encodeToString(MAPA, nuevo))
            .putStringSet(CLAVE_AVISADAS, yaAvisadas() + funcionesActuales)
            .apply()
        programarVigilancia()
    }

    fun quitar(clave: String) {
        val restantes = titulos() - clave
        val editor = prefs.edit().putString(CLAVE_PELICULAS, json.encodeToString(MAPA, restantes))
        // Sin peliculas marcadas no hay nada que vigilar: se detiene el trabajo.
        if (restantes.isEmpty()) {
            editor.remove(CLAVE_AVISADAS)
            detenerVigilancia()
        }
        editor.apply()
    }

    /** El trabajo registra aca las funciones que ya aviso. */
    fun registrarAvisadas(ids: Collection<String>) {
        prefs.edit().putStringSet(CLAVE_AVISADAS, yaAvisadas() + ids).apply()
    }

    /** Dia (hora de Denver) de la ultima revision de la cartelera. */
    fun ultimaRevision(): String? = prefs.getString(CLAVE_ULTIMA_REVISION, null)

    fun registrarRevision(fecha: String) {
        prefs.edit().putString(CLAVE_ULTIMA_REVISION, fecha).apply()
    }

    /**
     * Revisa la cartelera (con red) para ver si aparecio una funcion nueva de
     * alguna pelicula marcada. Corre cada 12 h: el trabajo solo descarga una vez
     * por dia y solo dentro de la ventana de avisos (ver [Horario.debeRevisar]).
     *
     * Por que no cada 24 h: un trabajo de 24 h conserva la hora en que se marco
     * la primera pelicula. Si esa hora cae de noche, o despues del cambio de
     * horario de verano, cada ejecucion queda fuera de la ventana y nunca avisa.
     * Con 12 h siempre hay una ejecucion dentro de la ventana.
     */
    private fun programarVigilancia() {
        val pedido = PeriodicWorkRequestBuilder<QuieroVerWorker>(REVISION_CADA_HORAS, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.UPDATE, pedido)
    }

    private fun detenerVigilancia() {
        workManager.cancelUniqueWork(TRABAJO)
    }

    private companion object {
        const val PREFS = "quiero_ver"
        const val CLAVE_PELICULAS = "peliculas"
        const val CLAVE_AVISADAS = "funciones_avisadas"
        const val CLAVE_ULTIMA_REVISION = "ultima_revision"
        const val TRABAJO = "quiero_ver_vigilancia"
        const val REVISION_CADA_HORAS = 12L

        val json = Json
        val MAPA = MapSerializer(String.serializer(), String.serializer())
    }
}
