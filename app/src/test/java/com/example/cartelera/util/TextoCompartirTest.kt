package com.example.cartelera.util

import com.example.cartelera.data.Funcion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class TextoCompartirTest {

    private val zona = TimeZone.getTimeZone("America/Denver")
    private val es = Locale.forLanguageTag("es-AR")

    private val fecha = Calendar.getInstance(zona).apply {
        clear()
        set(2026, Calendar.NOVEMBER, 20, 19, 0)
    }.time

    private val everest = Funcion(
        id = "everest-1120",
        titulo = "Everest: The Other Side",
        director = "Jimmy Chin",
        anio = 2025,
        sala = "Jim Santy Auditorium",
        fechaHora = fecha,
        precio = 12.0,
        urlEntradas = "https://cine.example/entradas"
    )

    @Test fun incluyeTituloDiaHoraSalaPrecioYLink() {
        val lineas = TextoCompartir.armar(everest, zona, es).lines()
        assertEquals(3, lineas.size)
        assertEquals("«Everest: The Other Side» (Jimmy Chin, 2025)", lineas[0])
        assertTrue(lineas[1], lineas[1].startsWith("viernes 20 de noviembre, 19:00 h · Jim Santy Auditorium · "))
        assertTrue(lineas[1], lineas[1].contains("US$"))
        assertEquals("Entradas: https://cine.example/entradas", lineas[2])
    }

    @Test fun funcionGratuitaNoDiceEntradas() {
        val texto = TextoCompartir.armar(everest.copy(precio = 0.0), zona, es)
        assertTrue(texto, texto.contains(" · Gratis"))
        assertFalse(texto, texto.contains("Entradas:"))
        assertTrue(texto, texto.endsWith("Más info: https://cine.example/entradas"))
    }

    @Test fun sinLinkNiDirectorNoDejaHuecos() {
        val f = everest.copy(director = "", anio = 0, urlEntradas = "")
        val lineas = TextoCompartir.armar(f, zona, es).lines()
        assertEquals(2, lineas.size)
        assertEquals("«Everest: The Other Side»", lineas[0])
    }

    @Test fun enInglesSiElTelefonoEstaEnIngles() {
        val lineas = TextoCompartir.armar(everest, zona, Locale.US).lines()
        assertEquals("“Everest: The Other Side” (Jimmy Chin, 2025)", lineas[0])
        assertEquals("Friday, November 20, 7:00 PM · Jim Santy Auditorium · $12", lineas[1])
        assertEquals("Tickets: https://cine.example/entradas", lineas[2])
    }
}
