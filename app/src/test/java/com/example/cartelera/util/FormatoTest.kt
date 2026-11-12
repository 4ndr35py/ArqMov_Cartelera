package com.example.cartelera.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class FormatoTest {

    private val zona = TimeZone.getTimeZone("America/Argentina/Cordoba")

    private fun fecha(anio: Int, mes: Int, dia: Int, hora: Int, min: Int) =
        Calendar.getInstance(zona).apply {
            clear()
            set(anio, mes - 1, dia, hora, min)
        }.time

    @Test fun fechaCortaIncluyeDiaYHora() {
        val texto = Formato.fechaCorta(fecha(2026, 10, 31, 21, 0), zona)
        assertTrue(texto, texto.endsWith("31/10 · 21:00"))
    }

    @Test fun fechaLargaEnCastellano() {
        val texto = Formato.fechaLarga(fecha(2026, 10, 31, 21, 0), zona)
        assertEquals("sábado 31 de octubre, 21:00 h", texto)
    }

    @Test fun precioCeroEsGratis() = assertEquals("Gratis", Formato.precio(0.0))
}
