package com.example.cartelera.data

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.cartelera.R
import com.example.cartelera.data.avisos.RecordatorioWorker
import com.example.cartelera.util.Recordatorio
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Unico punto de acceso a WorkManager. Programa en el telefono el aviso previo
 * a una funcion marcada y lo cancela al desmarcarla.
 *
 * Reemplaza a la Cloud Function de la Figura 2 del TP2: el proyecto queda en el
 * plan gratuito (Spark) de Firebase, que no permite desplegar Functions.
 */
class RecordatorioRepository(context: Context) {

    private val app = context.applicationContext
    private val workManager get() = WorkManager.getInstance(app)

    /** Devuelve el momento programado, o null si la funcion ya empezo. */
    fun programar(funcion: Funcion, ahora: Date = Date()): Date? {
        val fecha = funcion.fechaHora ?: return null
        val momento = Recordatorio.momento(fecha, ahora) ?: return null
        val pedido = OneTimeWorkRequestBuilder<RecordatorioWorker>()
            .setInitialDelay(Recordatorio.demoraMs(momento, ahora), TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    RecordatorioWorker.CLAVE_ID to funcion.id,
                    RecordatorioWorker.CLAVE_TITULO to app.getString(R.string.aviso_titulo, funcion.titulo),
                    RecordatorioWorker.CLAVE_TEXTO to Recordatorio.texto(fecha, funcion.sala)
                )
            )
            .addTag(ETIQUETA)
            .build()
        // Un trabajo por funcion: volver a marcar reemplaza el anterior.
        workManager.enqueueUniqueWork(nombre(funcion.id), ExistingWorkPolicy.REPLACE, pedido)
        return momento
    }

    fun cancelar(funcionId: String) {
        workManager.cancelUniqueWork(nombre(funcionId))
    }

    /** Al cerrar sesion, los avisos de esa cuenta dejan de tener sentido. */
    fun cancelarTodos() {
        workManager.cancelAllWorkByTag(ETIQUETA)
    }

    private fun nombre(funcionId: String) = "recordatorio_$funcionId"

    private companion object {
        const val ETIQUETA = "recordatorio"
    }
}
