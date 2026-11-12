package com.example.cartelera.util

/**
 * Validacion local de credenciales, previa a llamar a Firebase.
 * Es logica pura (sin Android), por eso se prueba con JUnit en la JVM.
 */
object Validador {

    /** Firebase rechaza contrasenas de menos de 6 caracteres. */
    const val LARGO_MINIMO_CLAVE = 6

    private val PATRON_EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    enum class Error { EMAIL_VACIO, EMAIL_INVALIDO, CLAVE_VACIA, CLAVE_CORTA }

    fun validar(email: String, clave: String): Error? = when {
        email.isBlank() -> Error.EMAIL_VACIO
        !PATRON_EMAIL.matches(email.trim()) -> Error.EMAIL_INVALIDO
        clave.isEmpty() -> Error.CLAVE_VACIA
        clave.length < LARGO_MINIMO_CLAVE -> Error.CLAVE_CORTA
        else -> null
    }
}
