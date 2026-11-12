package com.example.cartelera.util

import com.example.cartelera.data.Funcion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TextoCompartirTest {

    private val zona = TimeZone.getTimeZone("America/Argentina/Cordoba")

    private val fecha = Calendar.getInstance(zona).apply {
        clear()
        set(2026, Calendar.NOVEMBER, 14, 21, 0)
    }.time

    private val nueveReinas = Funcion(
        id = "f02",
        titulo = "Nueve reinas",
        director = "Fabián Bielinsky",
        anio = 2000,
        sala = "Sala 1",
        fechaHora = fecha,
        precio = 3500.0,
        urlEntradas = "https://cine.example/entradas"
    )

    @Test fun incluyeTituloDiaHoraSalaPrecioYLink() {
        val lineas = TextoCompartir.armar(nueveReinas, zona).lines()
        assertEquals(3, lineas.size)
        assertEquals("«Nueve reinas» (Fabián Bielinsky, 2000)", lineas[0])
        assertTrue(lineas[1], lineas[1].startsWith("sábado 14 de noviembre, 21:00 h · Sala 1 · "))
        assertTrue(lineas[1], lineas[1].contains("3.500"))
        assertEquals("Entradas: https://cine.example/entradas", lineas[2])
    }

    @Test fun funcionGratuitaNoDiceEntradas() {
        val texto = TextoCompartir.armar(nueveReinas.copy(precio = 0.0), zona)
        assertTrue(texto, texto.contains(" · Gratis"))
        assertFalse(texto, texto.contains("Entradas:"))
        assertTrue(texto, texto.endsWith("Más info: https://cine.example/entradas"))
    }

    @Test fun sinLinkNiDirectorNoDejaHuecos() {
        val f = nueveReinas.copy(director = "", anio = 0, urlEntradas = "")
        val lineas = TextoCompartir.armar(f, zona).lines()
        assertEquals(2, lineas.size)
        assertEquals("«Nueve reinas»", lineas[0])
    }
}
