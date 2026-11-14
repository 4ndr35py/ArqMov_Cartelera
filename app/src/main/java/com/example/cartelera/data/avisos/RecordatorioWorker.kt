package com.example.cartelera.data.avisos

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Trabajo que programa RecordatorioRepository. WorkManager lo ejecuta cuando
 * vence la demora (sobrevive a reinicios del telefono) y solo muestra la
 * notificacion: el texto ya viene armado.
 */
class RecordatorioWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val id = inputData.getString(CLAVE_ID) ?: return Result.failure()
        val titulo = inputData.getString(CLAVE_TITULO).orEmpty()
        val texto = inputData.getString(CLAVE_TEXTO).orEmpty()
        Notificador.mostrar(applicationContext, titulo, texto, mapOf(Ruteo.FUNCION_ID to id))
        return Result.success()
    }

    companion object {
        const val CLAVE_ID = "funcion_id"
        const val CLAVE_TITULO = "titulo"
        const val CLAVE_TEXTO = "texto"
    }
}
