package com.example.cartelera.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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

    /** Dia calendario en la hora del cine, p. ej. "2026-11-15". */
    fun fechaCine(ahora: Date, zona: TimeZone = ZONA_CINE): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zona }.format(ahora)

    /**
     * Si el trabajo de "Quiero verla" debe revisar la cartelera ahora: dentro de
     * la ventana de avisos y todavia no se reviso ese dia (hora de Denver).
     * Con un trabajo cada 12 h, siempre cae una ejecucion dentro de la ventana,
     * y la marca del dia evita descargar la cartelera mas de una vez por dia.
     */
    fun debeRevisar(ahora: Date, ultimaRevision: String?, zona: TimeZone = ZONA_CINE): Boolean =
        dentroDeHorarioDeAvisos(ahora, zona) && fechaCine(ahora, zona) != ultimaRevision
}
