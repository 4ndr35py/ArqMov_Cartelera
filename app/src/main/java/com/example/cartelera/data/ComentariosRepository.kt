package com.example.cartelera.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

/** Un comentario que el cine ya aprobo. */
data class Comentario(val texto: String, val fecha: Date?)

/**
 * Comentarios por pelicula en Cloud Firestore.
 *
 *  comentarios/{id}  clave (pelicula), texto, uid (sesion que escribio), fecha, aprobado
 *
 * Cualquiera con sesion (anonima o cuenta) puede escribir, pero el comentario
 * queda oculto (aprobado = false) hasta que el cine lo aprueba en la consola.
 * Las reglas no permiten que la app lo apruebe ni lo edite.
 */
class ComentariosRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Solo los comentarios aprobados de la pelicula, del mas nuevo al mas viejo. */
    fun cargar(clave: String, alTerminar: (Result<List<Comentario>>) -> Unit) {
        db.collection(COMENTARIOS)
            .whereEqualTo("clave", clave)
            .whereEqualTo("aprobado", true)
            .get()
            .addOnSuccessListener { snap ->
                val lista = snap.documents
                    .mapNotNull { d ->
                        val texto = d.getString("texto") ?: return@mapNotNull null
                        Comentario(texto, d.getTimestamp("fecha")?.toDate())
                    }
                    .sortedByDescending { it.fecha?.time ?: 0L }
                alTerminar(Result.success(lista))
            }
            .addOnFailureListener { alTerminar(Result.failure(it)) }
    }

    fun enviar(clave: String, uid: String, texto: String, alTerminar: (Result<Unit>) -> Unit) {
        db.collection(COMENTARIOS)
            .add(
                mapOf(
                    "clave" to clave,
                    "texto" to texto,
                    "uid" to uid,
                    "fecha" to FieldValue.serverTimestamp(),
                    "aprobado" to false
                )
            )
            .addOnSuccessListener { alTerminar(Result.success(Unit)) }
            .addOnFailureListener { alTerminar(Result.failure(it)) }
    }

    private companion object {
        const val COMENTARIOS = "comentarios"
    }
}
