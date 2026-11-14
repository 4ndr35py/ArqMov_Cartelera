package com.example.cartelera.util

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * Cuando es razonable mandar un aviso. Regla del TP6: nunca de noche, en la
 * hora del cine (Park City, Utah: America/Denver), aunque el telefono este en
 * otra zona. Logica pura, probada en la JVM.
 */
object Horario {

    val ZONA_CINE: TimeZone = TimeZone.getTimeZone("America/Denver")

    /** Avisos entre las 8:00 y las 21:59 (la hora de fin es exclusiva). */
    const val HORA_INICIO = 8
    const val HORA_FIN = 22

    fun horaEnCine(ahora: Date, zona: TimeZone = ZONA_CINE): Int =
        Calendar.getInstance(zona).apply { time = ahora }.get(Calendar.HOUR_OF_DAY)

    fun dentroDeHorarioDeAvisos(ahora: Date, zona: TimeZone = ZONA_CINE): Boolean =
        horaEnCine(ahora, zona) in HORA_INICIO until HORA_FIN
}
