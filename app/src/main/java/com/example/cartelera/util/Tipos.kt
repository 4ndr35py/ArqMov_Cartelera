package com.example.cartelera.util

import com.example.cartelera.data.TipoEntrada

/**
 * Que tipo de entrada se preselecciona: miembro (si tiene membresia vigente), senior o
 * estudiante (segun la edad), o la general. La persona puede cambiarlo antes de comprar.
 *
 * La venta del cine (Eventive) decide los precios; esto solo elige cual mostrar primero.
 */
object Tipos {

    /** Desde esta edad (cumplida) se preselecciona el precio senior. */
    const val EDAD_SENIOR = 65

    /** Menores de esta edad se preselecciona el precio estudiante (la persona puede cambiarlo). */
    const val EDAD_ESTUDIANTE = 18

    /**
     * Devuelve el tipo preseleccionado, o null si la funcion no tiene tipos a la venta.
     * [edad] es null si la persona no cargo su fecha de nacimiento.
     */
    fun elegir(tipos: List<TipoEntrada>, edad: Int?, esMiembro: Boolean): TipoEntrada? {
        if (tipos.isEmpty()) return null
        val miembro = buscar(tipos, "member", "miembro").filterNot { it.nombre.contains("non", ignoreCase = true) }
        val senior = buscar(tipos, "senior")
        val estudiante = buscar(tipos, "student", "estudiante")
        val general = buscar(tipos, "general")
        return when {
            esMiembro && miembro.isNotEmpty() -> miembro.minBy { it.precio }
            edad != null && edad >= EDAD_SENIOR && senior.isNotEmpty() -> senior.minBy { it.precio }
            edad != null && edad < EDAD_ESTUDIANTE && estudiante.isNotEmpty() -> estudiante.minBy { it.precio }
            general.isNotEmpty() -> general.minBy { it.precio }
            else -> tipos.minBy { it.precio }
        }
    }

    private fun buscar(tipos: List<TipoEntrada>, vararg claves: String): List<TipoEntrada> =
        tipos.filter { t -> claves.any { t.nombre.contains(it, ignoreCase = true) } }
}
