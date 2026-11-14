package com.example.cartelera.data.avisos

import com.example.cartelera.data.AuthRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Locale

/**
 * Unico punto de acceso a los topics de FCM y a las preferencias de avisos
 * (TP6, estrategia 1).
 *
 * Las preferencias viven en usuarios/{uid}.temas, un mapa tema -> activo.
 * Los topics son por instalacion: el dispositivo se suscribe a `<tema>_<idioma>`
 * segun lo que el usuario eligio. [sincronizar] deja los topics del telefono
 * de acuerdo con esas preferencias; se llama al iniciar la app.
 */
class AvisosRepository(
    private val auth: AuthRepository = AuthRepository(),
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Preferencias del usuario actual. Sin preferencias guardadas, todo esta apagado. */
    fun leerTemas(alTerminar: (Map<TemaAviso, Boolean>) -> Unit) {
        val uid = auth.usuarioActual?.uid
        if (uid == null) {
            alTerminar(apagados())
            return
        }
        db.collection(USUARIOS).document(uid).get()
            .addOnSuccessListener { doc ->
                val mapa = doc.get(CAMPO_TEMAS) as? Map<*, *>
                alTerminar(
                    TemaAviso.entries.associateWith { tema ->
                        mapa?.get(tema.id) == true
                    }
                )
            }
            .addOnFailureListener { alTerminar(apagados()) }
    }

    /**
     * Activa o desactiva un tema: primero el topic del telefono, despues la
     * preferencia en Firestore. Si el topic falla, no se guarda nada.
     */
    fun guardarTema(
        tema: TemaAviso,
        activo: Boolean,
        idioma: Locale = Locale.getDefault(),
        alTerminar: (Result<Unit>) -> Unit
    ) {
        val uid = auth.usuarioActual?.uid
        if (uid == null) {
            alTerminar(Result.failure(IllegalStateException("Sin sesion")))
            return
        }
        val topic = Temas.topic(tema, idioma)
        val topicViejo = Temas.topicOtroIdioma(tema, idioma)
        val cambioTopic = if (activo) messaging.subscribeToTopic(topic) else messaging.unsubscribeFromTopic(topic)
        cambioTopic
            .addOnSuccessListener {
                // Un topic del otro idioma puede haber quedado de antes: se borra.
                messaging.unsubscribeFromTopic(topicViejo)
                guardarPreferencia(uid, tema, activo, alTerminar)
            }
            .addOnFailureListener { alTerminar(Result.failure(it)) }
    }

    private fun guardarPreferencia(uid: String, tema: TemaAviso, activo: Boolean, alTerminar: (Result<Unit>) -> Unit) {
        db.collection(USUARIOS).document(uid).set(
            mapOf(
                CAMPO_TEMAS to mapOf(tema.id to activo),
                "actualizado" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
            .addOnSuccessListener { alTerminar(Result.success(Unit)) }
            .addOnFailureListener { alTerminar(Result.failure(it)) }
    }

    /**
     * Deja los topics del telefono de acuerdo con [temas]: suscribe los activos
     * en el idioma actual y se da de baja del resto. Es idempotente.
     */
    fun sincronizar(temas: Map<TemaAviso, Boolean>, idioma: Locale = Locale.getDefault()) {
        for ((tema, activo) in temas) {
            if (activo) {
                messaging.subscribeToTopic(Temas.topic(tema, idioma))
                messaging.unsubscribeFromTopic(Temas.topicOtroIdioma(tema, idioma))
            } else {
                messaging.unsubscribeFromTopic(Temas.topic(tema, idioma))
                messaging.unsubscribeFromTopic(Temas.topicOtroIdioma(tema, idioma))
            }
        }
    }

    private fun apagados(): Map<TemaAviso, Boolean> = TemaAviso.entries.associateWith { false }

    private companion object {
        const val USUARIOS = "usuarios"
        const val CAMPO_TEMAS = "temas"
    }
}
