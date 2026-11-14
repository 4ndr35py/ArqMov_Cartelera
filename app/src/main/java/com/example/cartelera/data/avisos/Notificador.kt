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
import com.example.cartelera.ui.avisos.AvisosActivity
import com.example.cartelera.ui.cartelera.CarteleraActivity
import com.example.cartelera.ui.detalle.DetalleActivity
import com.example.cartelera.ui.pelicula.PeliculaActivity

/**
 * Muestra las notificaciones de la app, vengan de un recordatorio local
 * (WorkManager), de un aviso "Quiero verla" o de un mensaje de FCM.
 *
 * Canales (Android 8+): "avisos" para los recordatorios y un canal por tema
 * (cartelera_semanal, accesibilidad, festival, eventos). Asi el usuario puede
 * silenciar un tema desde los ajustes del telefono sin perder los demas.
 *
 * Tocar la notificacion abre el destino que dice el payload (ver [Ruteo]),
 * con la cartelera debajo para el boton Atras.
 */
object Notificador {

    const val CANAL_AVISOS = Temas.CANAL_GENERAL

    /** Android 8+ exige un canal. Se llama una vez al iniciar la app. */
    fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CANAL_AVISOS,
                context.getString(R.string.canal_avisos),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.canal_avisos_desc) }
        )
        val textos = mapOf(
            TemaAviso.CARTELERA_SEMANAL to (R.string.canal_cartelera_semanal to R.string.canal_cartelera_semanal_desc),
            TemaAviso.ACCESIBILIDAD to (R.string.canal_accesibilidad to R.string.canal_accesibilidad_desc),
            TemaAviso.FESTIVAL to (R.string.canal_festival to R.string.canal_festival_desc),
            TemaAviso.EVENTOS to (R.string.canal_eventos to R.string.canal_eventos_desc)
        )
        for ((tema, recursos) in textos) {
            manager.createNotificationChannel(
                NotificationChannel(
                    tema.id,
                    context.getString(recursos.first),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = context.getString(recursos.second) }
            )
        }
    }

    /** En Android 13+ hace falta el permiso POST_NOTIFICATIONS. */
    fun puedeNotificar(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun mostrar(
        context: Context,
        titulo: String,
        texto: String,
        datos: Map<String, String?>,
        canal: String = CANAL_AVISOS
    ) {
        if (!puedeNotificar(context)) return
        val destino = Ruteo.de(datos)
        val clave = datos[Ruteo.FUNCION_ID] ?: datos[Ruteo.PELICULA_CLAVE] ?: titulo
        val notificacion = NotificationCompat.Builder(context, canal)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setAutoCancel(true)
            .setContentIntent(alTocar(context, destino, clave.hashCode()))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(clave.hashCode(), notificacion)
        } catch (_: SecurityException) {
            // El usuario retiro el permiso entre el chequeo y el aviso.
        }
    }

    /** Pantalla a la que lleva un destino. La cartelera es la base de la pila de Atras. */
    fun intentDe(context: Context, destino: Destino): Intent? = when (destino) {
        Destino.Cartelera -> null
        is Destino.Funcion -> Intent(context, DetalleActivity::class.java)
            .putExtra(DetalleActivity.EXTRA_FUNCION_ID, destino.id)
            .putExtra(DetalleActivity.EXTRA_DESDE_AVISO, true)
        is Destino.Pelicula -> Intent(context, PeliculaActivity::class.java)
            .putExtra(PeliculaActivity.EXTRA_CLAVE, destino.clave)
        Destino.Avisos -> Intent(context, AvisosActivity::class.java)
    }

    private fun alTocar(context: Context, destino: Destino, codigo: Int): PendingIntent? {
        val pila = TaskStackBuilder.create(context)
            .addNextIntent(Intent(context, CarteleraActivity::class.java))
        intentDe(context, destino)?.let { pila.addNextIntent(it) }
        return pila.getPendingIntent(
            codigo,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
