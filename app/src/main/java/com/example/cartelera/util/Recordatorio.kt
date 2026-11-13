package com.example.cartelera.util

import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Cuando avisar de una funcion marcada. Es logica pura (sin Android), por eso
 * se prueba con JUnit en la JVM.
 */
object Recordatorio {

    /** Anticipacion por defecto: dos horas antes de la funcion. */
    const val ANTICIPACION_MIN = 120L

    /**
     * Momento del aviso:
     * - [funcion] menos la anticipacion, si todavia no paso;
     * - [ahora], si ya paso ese momento pero la funcion no empezo;
     * - null, si la funcion ya empezo (no tiene sentido avisar).
     */
    fun momento(funcion: Date, ahora: Date, anticipacionMin: Long = ANTICIPACION_MIN): Date? {
        if (!funcion.after(ahora)) return null
        val ideal = Date(funcion.time - anticipacionMin * 60_000)
        return if (ideal.after(ahora)) ideal else ahora
    }

    /** Milisegundos que faltan para el aviso (0 si es inmediato). */
    fun demoraMs(momento: Date, ahora: Date): Long = (momento.time - ahora.time).coerceAtLeast(0)

    /**
     * Texto de la notificacion, en el idioma del telefono:
     * "Empieza el sábado 14 de noviembre, 19:00 h en Jim Santy Auditorium."
     * "Starts Saturday, November 14, 7:00 PM at Jim Santy Auditorium."
     */
    fun texto(fecha: Date, sala: String, zona: TimeZone = TimeZone.getDefault(), idioma: Locale = Locale.getDefault()): String {
        val cuando = Formato.fechaLarga(fecha, zona, idioma)
        return if (Formato.esCastellano(idioma)) {
            if (sala.isBlank()) "Empieza el $cuando." else "Empieza el $cuando en $sala."
        } else {
            if (sala.isBlank()) "Starts $cuando." else "Starts $cuando at $sala."
        }
    }
}
