package com.example.cartelera.data.avisos

import com.example.cartelera.data.MensajeriaRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Servicio de avisos (Figura 2 del TP2). Recibe los mensajes de Firebase Cloud
 * Messaging y guarda el token cuando FCM lo rota.
 *
 * Con la app en segundo plano, los mensajes con bloque "notification" los
 * muestra el sistema, y al tocarlos la app se abre con el payload como extras
 * (CarteleraActivity lo rutea). Con la app abierta llegan aca: el campo "tipo"
 * elige el canal del tema y [Ruteo] decide a donde lleva el toque.
 */
class AvisosService : FirebaseMessagingService() {

    /** Deprecado desde firebase-messaging 25.1.0; ver MensajeriaRepository.registrarToken. */
    @Deprecated("Ver MensajeriaRepository.registrarToken")
    override fun onNewToken(token: String) {
        MensajeriaRepository().guardarToken(token)
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        val datos: Map<String, String?> = mensaje.data
        val titulo = mensaje.notification?.title ?: datos["titulo"] ?: return
        val texto = mensaje.notification?.body ?: datos["texto"].orEmpty()
        Notificador.mostrar(
            applicationContext,
            titulo,
            texto,
            datos,
            canal = Temas.canal(datos[Ruteo.TIPO])
        )
    }
}
