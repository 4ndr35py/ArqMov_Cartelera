package com.example.cartelera.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Formato {

    private val LOCALE: Locale = Locale.forLanguageTag("es-AR")

    /** "sáb 31/10 · 21:00" */
    fun fechaCorta(fecha: Date, zona: TimeZone = TimeZone.getDefault()): String {
        val f = SimpleDateFormat("EEE dd/MM · HH:mm", LOCALE)
        f.timeZone = zona
        return f.format(fecha).replace(".", "")
    }

    /** "sábado 31 de octubre, 21:00 h" */
    fun fechaLarga(fecha: Date, zona: TimeZone = TimeZone.getDefault()): String {
        val f = SimpleDateFormat("EEEE d 'de' MMMM, HH:mm 'h'", LOCALE)
        f.timeZone = zona
        return f.format(fecha)
    }

    fun precio(valor: Double): String =
        if (valor <= 0.0) "Gratis"
        else NumberFormat.getCurrencyInstance(LOCALE).apply { maximumFractionDigits = 0 }.format(valor)
}
