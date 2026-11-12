package com.example.cartelera.data

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser

/**
 * Unico punto de acceso a Firebase Authentication. Las Activities y los
 * ViewModels no conocen el SDK: reciben [Usuario] y [AuthException].
 *
 * Desde el TP7 la app arranca con una sesion anonima (la cartelera se ve sin
 * cuenta) y pide email y contrasena recien al marcar una funcion. Al crear la
 * cuenta, la sesion anonima se vincula con el email y el uid no cambia.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val usuarioActual: Usuario?
        get() = auth.currentUser?.aUsuario()

    /** true si hay una cuenta con email; la sesion anonima no alcanza. */
    val tieneCuenta: Boolean
        get() = auth.currentUser?.isAnonymous == false

    /** Devuelve la sesion actual o abre una anonima si no hay ninguna. */
    fun asegurarSesion(alTerminar: (Result<Usuario>) -> Unit) {
        val actual = auth.currentUser
        if (actual != null) {
            alTerminar(Result.success(actual.aUsuario()))
            return
        }
        auth.signInAnonymously().entregar(alTerminar)
    }

    /**
     * Crear cuenta. Si la sesion actual es anonima la vincula con el email
     * (linkWithCredential) y conserva el uid; si no, crea una cuenta nueva.
     */
    fun registrar(email: String, clave: String, alTerminar: (Result<Usuario>) -> Unit) {
        val actual = auth.currentUser
        val tarea = if (actual != null && actual.isAnonymous) {
            actual.linkWithCredential(EmailAuthProvider.getCredential(email, clave))
        } else {
            auth.createUserWithEmailAndPassword(email, clave)
        }
        tarea.entregar(alTerminar)
    }

    /**
     * Ingresar con una cuenta que ya existe. La sesion anonima anterior se
     * abandona: no tiene datos, porque marcar funciones exige cuenta.
     */
    fun ingresar(email: String, clave: String, alTerminar: (Result<Usuario>) -> Unit) {
        auth.signInWithEmailAndPassword(email, clave).entregar(alTerminar)
    }

    fun cerrarSesion() = auth.signOut()

    private fun Task<AuthResult>.entregar(alTerminar: (Result<Usuario>) -> Unit) {
        addOnSuccessListener { r -> alTerminar(Result.success(r.user!!.aUsuario())) }
        addOnFailureListener { e -> alTerminar(Result.failure(traducir(e))) }
    }

    private fun FirebaseUser.aUsuario() = Usuario(uid, isAnonymous, email)

    private fun traducir(e: Exception) = AuthException(
        when (e) {
            is FirebaseAuthWeakPasswordException -> ErrorAuth.CLAVE_DEBIL
            is FirebaseAuthUserCollisionException -> ErrorAuth.EMAIL_EN_USO
            is FirebaseAuthInvalidCredentialsException -> ErrorAuth.CREDENCIALES
            is FirebaseNetworkException -> ErrorAuth.RED
            else -> ErrorAuth.OTRO
        },
        e
    )
}
