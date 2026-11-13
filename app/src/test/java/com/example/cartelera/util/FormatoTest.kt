package com.example.cartelera.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class FormatoTest {

    private val zona = TimeZone.getTimeZone("America/Denver")
    private val es = Locale.forLanguageTag("es-AR")
    private val en = Locale.US

    private fun fecha(anio: Int, mes: Int, dia: Int, hora: Int, min: Int) =
        Calendar.getInstance(zona).apply {
            clear()
            set(anio, mes - 1, dia, hora, min)
        }.time

    @Test fun fechaCortaIncluyeDiaYHora() {
        val texto = Formato.fechaCorta(fecha(2026, 10, 31, 21, 0), zona, es)
        assertTrue(texto, texto.endsWith("31/10 · 21:00"))
    }

    @Test fun fechaLargaEnCastellano() {
        val texto = Formato.fechaLarga(fecha(2026, 10, 31, 21, 0), zona, es)
        assertEquals("sábado 31 de octubre, 21:00 h", texto)
    }

    @Test fun precioCeroEsGratis() = assertEquals("Gratis", Formato.precio(0.0, es))

    // TP7: la app tambien esta en ingles

    @Test fun fechaCortaEnIngles() {
        val texto = Formato.fechaCorta(fecha(2026, 11, 20, 19, 0), zona, en)
        assertEquals("Fri 11/20 · 7:00 PM", texto)
    }

    @Test fun fechaLargaEnIngles() {
        val texto = Formato.fechaLarga(fecha(2026, 11, 20, 19, 0), zona, en)
        assertEquals("Friday, November 20, 7:00 PM", texto)
    }

    @Test fun precioEnDolaresSegunElIdioma() {
        assertEquals("$12", Formato.precio(12.0, en))
        assertTrue(Formato.precio(12.0, es), Formato.precio(12.0, es).contains("US$"))
        assertEquals("Free", Formato.precio(0.0, en))
    }
}
