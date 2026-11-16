package com.example.cartelera.data.avisos

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.cartelera.R
import com.example.cartelera.analytics.Metricas
import com.example.cartelera.data.CarteleraRepository
import com.example.cartelera.data.Funcion
import com.example.cartelera.data.QuieroVerRepository
import com.example.cartelera.util.Cartelera
import com.example.cartelera.util.Formato
import com.example.cartelera.util.Horario
import com.example.cartelera.util.QuieroVer
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Trabajo diario de "Quiero verla" (TP6). Descarga la cartelera publicada en
 * Hosting (sin servidor propio) y avisa en el telefono de las funciones nuevas
 * de las peliculas marcadas. La comparacion esta en [QuieroVer]; aca solo se
 * descarga, se avisa y se registra lo avisado.
 *
 * Fuera del horario de avisos (ver [Horario]) no hace nada: el dia siguiente
 * lo vuelve a intentar con las mismas funciones.
 */
class QuieroVerWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val repo = QuieroVerRepository(applicationContext)
        val marcadas = repo.marcadas()
        if (marcadas.isEmpty()) return Result.success()
        val ahora = Date()
        // Fuera de la ventana, o ya revisado hoy: no hace nada (ver Horario.debeRevisar).
        if (!Horario.debeRevisar(ahora, repo.ultimaRevision())) return Result.success()

        val funciones = try {
            descargarCartelera()
        } catch (e: IOException) {
            return Result.retry()
        }
        if (funciones == null) return Result.success()

        val nuevas = QuieroVer.funcionesNuevas(marcadas, Cartelera.agrupar(funciones), repo.yaAvisadas())
        // Un aviso por pelicula, aunque tenga varias funciones nuevas.
        nuevas.groupBy { Cartelera.clave(it) }.forEach { (clave, lista) ->
            avisar(clave, lista)
            Metricas.quieroVerAviso(clave)
        }
        if (nuevas.isNotEmpty()) repo.registrarAvisadas(nuevas.map { it.id })
        repo.registrarRevision(Horario.fechaCine(ahora))
        return Result.success()
    }

    private fun avisar(clave: String, funciones: List<Funcion>) {
        val primera = funciones.first()
        val cuando = primera.fechaHora?.let { Formato.fechaCorta(it) }.orEmpty()
        val cuerpo = applicationContext.getString(R.string.quiero_ver_aviso_texto, cuando, primera.sala)
        Notificador.mostrar(
            applicationContext,
            applicationContext.getString(R.string.quiero_ver_aviso_titulo, primera.titulo),
            cuerpo,
            mapOf(Ruteo.PELICULA_CLAVE to clave)
        )
    }

    /** null si la respuesta no es una cartelera valida (pagina de error, por ejemplo). */
    private fun descargarCartelera(): List<Funcion>? {
        val cliente = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val pedido = Request.Builder().url(CarteleraRepository.URL_CARTELERA).build()
        return cliente.newCall(pedido).execute().use { respuesta ->
            if (!respuesta.isSuccessful) null
            else CarteleraRepository.parsear(respuesta.body.string())
        }
    }
}
