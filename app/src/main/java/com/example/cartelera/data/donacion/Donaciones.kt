package com.example.cartelera.data.donacion

/** Una donacion de monto fijo. El id es el mismo que se carga en Play Console. */
data class OpcionDonacion(val productId: String, val montoUsd: Int)

/**
 * Catalogo de donaciones (TP4). Son productos consumibles de Google Play
 * Billing: se compran, se consumen y el usuario puede volver a donar.
 * Sin Billing ni Android, este objeto es logica pura y se testea en la JVM.
 */
object Donaciones {

    val opciones: List<OpcionDonacion> = listOf(
        OpcionDonacion("donacion_2usd", 2),
        OpcionDonacion("donacion_5usd", 5),
        OpcionDonacion("donacion_10usd", 10),
    )

    /** Monto sugerido (se resalta en la pantalla: efecto de punto de precio). */
    const val MONTO_SUGERIDO_USD = 5

    val idsProducto: List<String> get() = opciones.map { it.productId }

    /** Monto en USD del producto, o null si el id no pertenece al catalogo. */
    fun montoDe(productId: String): Int? =
        opciones.firstOrNull { it.productId == productId }?.montoUsd
}
