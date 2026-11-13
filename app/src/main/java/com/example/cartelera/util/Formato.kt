package com.example.cartelera.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Fechas y precios en el idioma del telefono (castellano o ingles).
 * Logica pura: los tests pasan el idioma y la zona horaria explicitamente.
 */
object Formato {

    /** El cine (Park City Film, Utah) cobra en dolares. */
    private val MONEDA: Currency = Currency.getInstance("USD")

    fun esCastellano(idioma: Locale = Locale.getDefault()) = idioma.language == "es"

    /** "sáb 14/11 · 19:00"  /  "Sat 11/14 · 7:00 PM" */
    fun fechaCorta(fecha: Date, zona: TimeZone = TimeZone.getDefault(), idioma: Locale = Locale.getDefault()): String {
        val patron = if (esCastellano(idioma)) "EEE dd/MM · HH:mm" else "EEE MM/dd · h:mm a"
        val f = SimpleDateFormat(patron, idioma)
        f.timeZone = zona
        return f.format(fecha).replace(".", "")
    }

    /** "sábado 14 de noviembre, 19:00 h"  /  "Saturday, November 14, 7:00 PM" */
    fun fechaLarga(fecha: Date, zona: TimeZone = TimeZone.getDefault(), idioma: Locale = Locale.getDefault()): String {
        val patron = if (esCastellano(idioma)) "EEEE d 'de' MMMM, HH:mm 'h'" else "EEEE, MMMM d, h:mm a"
        val f = SimpleDateFormat(patron, idioma)
        f.timeZone = zona
        return f.format(fecha)
    }

    /**
     * Rango de fechas de una pelicula para la tarjeta de la cartelera:
     * un dia "sáb 14 nov" / "Sat, Nov 14"; mismo mes "13–15 nov" / "Nov 13–15";
     * distinto mes "28 nov – 2 dic" / "Nov 28 – Dec 2".
     */
    fun rangoFechas(desde: Date, hasta: Date, zona: TimeZone = TimeZone.getDefault(), idioma: Locale = Locale.getDefault()): String {
        val es = esCastellano(idioma)
        fun f(patron: String, fecha: Date) =
            SimpleDateFormat(patron, idioma).apply { timeZone = zona }.format(fecha).replace(".", "")
        val dia = SimpleDateFormat("yyyyMMdd", Locale.ROOT).apply { timeZone = zona }
        val mes = SimpleDateFormat("yyyyMM", Locale.ROOT).apply { timeZone = zona }
        return when {
            dia.format(desde) == dia.format(hasta) ->
                if (es) f("EEE d MMM", desde) else f("EEE, MMM d", desde)
            mes.format(desde) == mes.format(hasta) ->
                if (es) "${f("d", desde)}–${f("d MMM", hasta)}" else "${f("MMM d", desde)}–${f("d", hasta)}"
            else ->
                if (es) "${f("d MMM", desde)} – ${f("d MMM", hasta)}" else "${f("MMM d", desde)} – ${f("MMM d", hasta)}"
        }
    }

    /** "Gratis" / "Free"; si no, el precio en dolares ("US$ 12" / "$12"). */
    fun precio(valor: Double, idioma: Locale = Locale.getDefault()): String {
        if (valor <= 0.0) return if (esCastellano(idioma)) "Gratis" else "Free"
        val f = NumberFormat.getCurrencyInstance(idioma).apply {
            currency = MONEDA
            maximumFractionDigits = if (valor % 1.0 == 0.0) 0 else 2
        }
        return f.format(valor).replace(' ', ' ')
    }
}
