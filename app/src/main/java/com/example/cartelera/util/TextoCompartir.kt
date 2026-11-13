package com.example.cartelera.util

import com.example.cartelera.data.Funcion
import java.util.Locale
import java.util.TimeZone

/**
 * Texto que se manda por el Sharesheet de Android (a la app que elija el usuario), en el
 * idioma del telefono. Logica pura, probada con JUnit.
 *
 *   «Everest: The Other Side» (Jimmy Chin, Elizabeth Chai Vasarhelyi, 2025)
 *   viernes 20 de noviembre, 19:00 h · Jim Santy Auditorium · US$ 12
 *   Entradas: https://...
 */
object TextoCompartir {

    fun armar(f: Funcion, zona: TimeZone = TimeZone.getDefault(), idioma: Locale = Locale.getDefault()): String {
        val es = Formato.esCastellano(idioma)
        val lineas = mutableListOf<String>()

        val ficha = listOf(f.director, f.anio.takeIf { it > 0 }?.toString())
            .filterNot { it.isNullOrBlank() }
            .joinToString(", ")
        val titulo = if (es) "«${f.titulo}»" else "“${f.titulo}”"
        lineas += if (ficha.isEmpty()) titulo else "$titulo ($ficha)"

        val cuando = f.fechaHora?.let { Formato.fechaLarga(it, zona, idioma) }
        lineas += listOfNotNull(cuando, f.sala.ifBlank { null }, Formato.precio(f.precio, idioma))
            .joinToString(" · ")

        if (f.urlEntradas.isNotBlank()) {
            val etiqueta = when {
                f.precio > 0.0 -> if (es) "Entradas" else "Tickets"
                else -> if (es) "Más info" else "More info"
            }
            lineas += "$etiqueta: ${f.urlEntradas}"
        }
        return lineas.joinToString("\n")
    }
}
