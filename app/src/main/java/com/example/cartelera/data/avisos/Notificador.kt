package com.example.cartelera.data.avisos

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.example.cartelera.R
import com.example.cartelera.ui.cartelera.CarteleraActivity
import com.example.cartelera.ui.detalle.DetalleActivity

/**
 * Muestra las notificaciones de la app, vengan de un recordatorio local
 * (WorkManager) o de un mensaje de FCM. Tocar la notificacion abre el detalle
 * de la funcion, con la cartelera debajo para el boton Atras.
 */
object Notificador {

    const val CANAL_AVISOS = "avisos"

    /** Android 8+ exige un canal. Se llama una vez al iniciar la app. */
    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            CANAL_AVISOS,
            context.getString(R.string.canal_avisos),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.canal_avisos_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /** En Android 13+ hace falta el permiso POST_NOTIFICATIONS. */
    fun puedeNotificar(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun mostrar(context: Context, titulo: String, texto: String, funcionId: String?) {
        if (!puedeNotificar(context)) return
        val notificacion = NotificationCompat.Builder(context, CANAL_AVISOS)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setAutoCancel(true)
            .setContentIntent(alTocar(context, funcionId))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(funcionId.hashCode(), notificacion)
        } catch (_: SecurityException) {
            // El usuario retiro el permiso entre el chequeo y el aviso.
        }
    }

    private fun alTocar(context: Context, funcionId: String?): PendingIntent? {
        val pila = TaskStackBuilder.create(context)
            .addNextIntent(Intent(context, CarteleraActivity::class.java))
        if (!funcionId.isNullOrBlank()) {
            pila.addNextIntent(
                Intent(context, DetalleActivity::class.java)
                    .putExtra(DetalleActivity.EXTRA_FUNCION_ID, funcionId)
                    .putExtra(DetalleActivity.EXTRA_DESDE_AVISO, true)
            )
        }
        return pila.getPendingIntent(
            funcionId.hashCode(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
