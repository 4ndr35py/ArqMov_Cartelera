package com.example.cartelera.data

/**
 * Escucha en tiempo real que se puede cancelar. Envuelve el
 * ListenerRegistration de Firestore para que los ViewModels no lo conozcan.
 */
fun interface Suscripcion {
    fun cancelar()
}
