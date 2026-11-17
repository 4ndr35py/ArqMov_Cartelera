package com.example.cartelera.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Fechas de solo dia ("yyyy-MM-dd"): fin de membresia, dia en que se vio una pelicula.
 * Se guardan como texto para no depender de la zona horaria del telefono.
 * Logica pura: los tests no necesitan Android.
 */
object FechaDia {

    private const val PATRON = "yyyy-MM-dd"
    private val UTC = TimeZone.getTimeZone("UTC")
    private const val DIA_MS = 86_400_000L

    /** Hoy segun el reloj del telefono, como "yyyy-MM-dd". */
    fun hoy(ahora: Date = Date()): String =
        SimpleDateFormat(PATRON, Locale.US).format(ahora)

    /** Fecha de solo dia a medianoche UTC, o null si el texto no es valido. */
    fun aDate(iso: String): Date? = try {
        SimpleDateFormat(PATRON, Locale.US).apply { timeZone = UTC }.parse(iso)
    } catch (e: Exception) {
        null
    }

    /** "yyyy-MM-dd" de los milisegundos que entrega el selector de fecha (siempre UTC). */
    fun desdeMillisUtc(millis: Long): String =
        SimpleDateFormat(PATRON, Locale.US).apply { timeZone = UTC }.format(Date(millis))

    /** Milisegundos UTC para preseleccionar una fecha en el selector. */
    fun millisUtc(iso: String): Long? = aDate(iso)?.time

    /**
     * Dias desde [hoy] hasta [vence] (ambos "yyyy-MM-dd"). Cero si es hoy,
     * negativo si ya paso. Null si alguna fecha no es valida.
     */
    fun diasHasta(hoy: String, vence: String): Long? {
        val a = dias(hoy) ?: return null
        val b = dias(vence) ?: return null
        return b - a
    }

    /**
     * Anos cumplidos a la fecha [hoy], dado el dia de nacimiento (ambos "yyyy-MM-dd").
     * Null si alguna fecha no es valida o si nacio despues de [hoy].
     */
    fun anios(nacimiento: String, hoy: String): Int? {
        val n = partes(nacimiento) ?: return null
        val h = partes(hoy) ?: return null
        var edad = h[0] - n[0]
        if (h[1] < n[1] || (h[1] == n[1] && h[2] < n[2])) edad--
        return if (edad < 0) null else edad
    }

    private fun partes(iso: String): IntArray? {
        val p = iso.split("-")
        if (p.size != 3) return null
        val y = p[0].toIntOrNull() ?: return null
        val m = p[1].toIntOrNull() ?: return null
        val d = p[2].toIntOrNull() ?: return null
        return intArrayOf(y, m, d)
    }

    private fun dias(iso: String): Long? {
        val partes = iso.split("-")
        if (partes.size != 3) return null
        val y = partes[0].toIntOrNull() ?: return null
        val m = partes[1].toIntOrNull() ?: return null
        val d = partes[2].toIntOrNull() ?: return null
        val cal = Calendar.getInstance(UTC).apply {
            clear()
            set(y, m - 1, d)
        }
        return cal.timeInMillis / DIA_MS
    }
}
