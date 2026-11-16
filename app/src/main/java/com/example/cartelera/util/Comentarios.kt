package com.example.cartelera.util

/**
 * Reglas de los comentarios por pelicula (TP6). Logica pura: se prueba en la JVM.
 * Las mismas reglas estan en firebase/firestore.rules (largo maximo y texto).
 */
object Comentarios {

    const val LARGO_MAXIMO = 500

    enum class Validacion { OK, VACIO, DEMASIADO_LARGO }

    /** Valida el texto ya recortado de espacios de los extremos. */
    fun validar(texto: String): Validacion {
        val limpio = normalizar(texto)
        return when {
            limpio.isEmpty() -> Validacion.VACIO
            limpio.length > LARGO_MAXIMO -> Validacion.DEMASIADO_LARGO
            else -> Validacion.OK
        }
    }

    fun normalizar(texto: String): String = texto.trim()
}
