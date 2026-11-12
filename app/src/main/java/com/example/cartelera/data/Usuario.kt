package com.example.cartelera.data

/**
 * Sesion actual, sin tipos de Firebase. [anonimo] es true mientras el usuario
 * mira la cartelera sin haber creado una cuenta.
 */
data class Usuario(
    val uid: String,
    val anonimo: Boolean,
    val email: String?
)
