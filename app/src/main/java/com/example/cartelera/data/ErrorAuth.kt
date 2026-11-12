package com.example.cartelera.data

/** Errores de autenticacion, ya traducidos por AuthRepository. */
enum class ErrorAuth { CLAVE_DEBIL, EMAIL_EN_USO, CREDENCIALES, RED, OTRO }

class AuthException(val tipo: ErrorAuth, causa: Throwable? = null) :
    Exception(tipo.name, causa)
