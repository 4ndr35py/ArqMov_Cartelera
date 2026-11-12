package com.example.cartelera.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Unico punto de acceso a Firebase Cloud Messaging.
 *
 * Guarda el token FCM del dispositivo en usuarios/{uid}.tokenFcm, para que el
 * cine (o una Cloud Function en una version futura) pueda mandarle avisos.
 */
class MensajeriaRepository(
    private val auth: AuthRepository = AuthRepository(),
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Pide el token actual y lo guarda para el usuario de la sesion.
     *
     * firebase-messaging 25.1.0 deprecó getToken/onNewToken en favor de un
     * registro por Firebase Installation ID (register/onRegistered). Se mantiene
     * la API clásica, que sigue funcionando y es la que usa el "mensaje de
     * prueba" de la consola; migrar queda pendiente.
     */
    @Suppress("DEPRECATION")
    fun registrarToken() {
        messaging.token.addOnSuccessListener { token -> guardarToken(token) }
    }

    /** Lo llama el servicio de avisos cuando FCM rota el token. */
    fun guardarToken(token: String) {
        val uid = auth.usuarioActual?.uid ?: return
        db.collection("usuarios").document(uid).set(
            mapOf("tokenFcm" to token, "actualizado" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
    }
}
