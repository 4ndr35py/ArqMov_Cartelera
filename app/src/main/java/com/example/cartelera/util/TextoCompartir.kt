package com.example.cartelera.util

import com.example.cartelera.data.Funcion
import java.util.TimeZone

/**
 * Texto que se manda por el Sharesheet de Android (WhatsApp u otra app).
 * Logica pura, probada con JUnit.
 *
 *   «Nueve reinas» (Fabián Bielinsky, 2000)
 *   sábado 14 de noviembre, 21:00 h · Sala 1 · $ 3.500
 *   Entradas: https://...
 */
object TextoCompartir {

    fun armar(f: Funcion, zona: TimeZone = TimeZone.getDefault()): String {
        val lineas = mutableListOf<String>()

        val ficha = listOf(f.director, f.anio.takeIf { it > 0 }?.toString())
            .filterNot { it.isNullOrBlank() }
            .joinToString(", ")
        lineas += if (ficha.isEmpty()) "«${f.titulo}»" else "«${f.titulo}» ($ficha)"

        val cuando = f.fechaHora?.let { Formato.fechaLarga(it, zona) }
        lineas += listOfNotNull(cuando, f.sala.ifBlank { null }, Formato.precio(f.precio))
            .joinToString(" · ")

        if (f.urlEntradas.isNotBlank()) {
            lineas += if (f.precio > 0.0) "Entradas: ${f.urlEntradas}" else "Más info: ${f.urlEntradas}"
        }
        return lineas.joinToString("\n")
    }
}
