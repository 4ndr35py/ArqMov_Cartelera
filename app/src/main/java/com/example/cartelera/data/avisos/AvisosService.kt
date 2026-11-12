package com.example.cartelera.data.avisos

import com.example.cartelera.data.MensajeriaRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Servicio de avisos (Figura 2 del TP2). Recibe los mensajes de Firebase Cloud
 * Messaging y guarda el token cuando FCM lo rota.
 *
 * Con la app en segundo plano, los mensajes con bloque "notification" los
 * muestra el sistema en el canal por defecto (ver AndroidManifest). Con la app
 * abierta llegan aca y se muestran con el mismo Notificador que los
 * recordatorios. El dato opcional "funcionId" abre el detalle de esa funcion.
 */
class AvisosService : FirebaseMessagingService() {

    /** Deprecado desde firebase-messaging 25.1.0; ver MensajeriaRepository.registrarToken. */
    @Deprecated("Ver MensajeriaRepository.registrarToken")
    override fun onNewToken(token: String) {
        MensajeriaRepository().guardarToken(token)
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        val titulo = mensaje.notification?.title ?: mensaje.data["titulo"] ?: return
        val texto = mensaje.notification?.body ?: mensaje.data["texto"].orEmpty()
        Notificador.mostrar(applicationContext, titulo, texto, mensaje.data["funcionId"])
    }
}
