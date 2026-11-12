package com.example.cartelera.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date

/**
 * Acceso a Cloud Firestore.
 *
 *  funciones/{funcionId}                      cartelera, la carga el cine
 *  usuarios/{uid}/intereses/{funcionId}       funciones marcadas por el usuario
 */
class FuncionRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Funciones desde [desde] en adelante, ordenadas por fecha. */
    fun cargarCartelera(desde: Date, alTerminar: (Result<List<Funcion>>) -> Unit) {
        db.collection(FUNCIONES)
            .whereGreaterThanOrEqualTo("fechaHora", Timestamp(desde))
            .orderBy("fechaHora", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snap ->
                val lista = snap.toObjects(FuncionDoc::class.java).map { it.aFuncion() }
                alTerminar(Result.success(lista))
            }
            .addOnFailureListener { e -> alTerminar(Result.failure(e)) }
    }

    fun cargarFuncion(id: String, alTerminar: (Result<Funcion>) -> Unit) {
        db.collection(FUNCIONES).document(id).get()
            .addOnSuccessListener { doc ->
                val funcion = doc.toObject(FuncionDoc::class.java)?.aFuncion()
                if (funcion != null) alTerminar(Result.success(funcion))
                else alTerminar(Result.failure(NoSuchElementException("La funcion no existe")))
            }
            .addOnFailureListener { e -> alTerminar(Result.failure(e)) }
    }

    /** Escucha en tiempo real los ids de las funciones marcadas por el usuario. */
    fun escucharIntereses(uid: String, alCambiar: (Set<String>) -> Unit): Suscripcion {
        val registro = intereses(uid).addSnapshotListener { snap, _ ->
            alCambiar(snap?.documents?.map { it.id }?.toSet() ?: emptySet())
        }
        return Suscripcion { registro.remove() }
    }

    fun marcarInteres(uid: String, funcionId: String, interesa: Boolean) {
        val ref = intereses(uid).document(funcionId)
        if (interesa) ref.set(mapOf("creado" to FieldValue.serverTimestamp()))
        else ref.delete()
    }

    private fun intereses(uid: String) =
        db.collection(USUARIOS).document(uid).collection(INTERESES)

    companion object {
        private const val FUNCIONES = "funciones"
        private const val USUARIOS = "usuarios"
        private const val INTERESES = "intereses"
    }
}

/**
 * Forma del documento en Firestore. Es interna al paquete de datos: hacia
 * afuera solo sale [Funcion], sin Timestamp ni anotaciones de Firebase.
 * (Firestore necesita una clase publica con constructor sin argumentos.)
 */
internal data class FuncionDoc(
    @DocumentId val id: String = "",
    val titulo: String = "",
    val director: String = "",
    val anio: Int = 0,
    val duracionMin: Int = 0,
    val sinopsis: String = "",
    val sala: String = "",
    val fechaHora: Timestamp? = null,
    val precio: Double = 0.0,
    val urlEntradas: String = "",
    val tmdbId: Long? = null
) {
    fun aFuncion() = Funcion(
        id = id,
        titulo = titulo,
        director = director,
        anio = anio,
        duracionMin = duracionMin,
        sinopsis = sinopsis,
        sala = sala,
        fechaHora = fechaHora?.toDate(),
        precio = precio,
        urlEntradas = urlEntradas,
        tmdbId = tmdbId?.toInt()
    )
}
