package com.example.cartelera.data

/**
 * Una membresia o pase a la venta del cine (desde Eventive). [entradasPorFuncion] es cuantas
 * entradas da por funcion (null si no hay limite); [renovacion] si se renueva sola.
 */
data class Pase(
    val nombre: String,
    val precio: Double,
    val membresia: Boolean,
    val entradasPorFuncion: Int? = null,
    val cancelable: Boolean = false,
    val renovacion: Boolean = false
)
