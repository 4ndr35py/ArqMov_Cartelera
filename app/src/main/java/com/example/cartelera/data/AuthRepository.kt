package com.example.cartelera.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * Unico punto de acceso a Firebase Authentication. Las Activities y los
 * ViewModels no conocen el SDK de Firebase: dependen de esta clase.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val usuarioActual: FirebaseUser?
        get() = auth.currentUser

    fun registrar(email: String, clave: String, alTerminar: (Result<FirebaseUser>) -> Unit) {
        auth.createUserWithEmailAndPassword(email, clave)
            .addOnSuccessListener { r -> alTerminar(Result.success(r.user!!)) }
            .addOnFailureListener { e -> alTerminar(Result.failure(e)) }
    }

    fun ingresar(email: String, clave: String, alTerminar: (Result<FirebaseUser>) -> Unit) {
        auth.signInWithEmailAndPassword(email, clave)
            .addOnSuccessListener { r -> alTerminar(Result.success(r.user!!)) }
            .addOnFailureListener { e -> alTerminar(Result.failure(e)) }
    }

    fun cerrarSesion() = auth.signOut()
}
