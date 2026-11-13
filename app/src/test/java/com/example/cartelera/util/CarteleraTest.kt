package com.example.cartelera.util

import com.example.cartelera.data.Funcion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class CarteleraTest {

    private val zona = TimeZone.getTimeZone("America/Denver")

    private fun fecha(mes: Int, dia: Int, hora: Int) = Calendar.getInstance(zona).apply {
        clear(); set(2026, mes - 1, dia, hora, 0)
    }.time

    private fun funcion(id: String, titulo: String, tmdbId: Int?, mes: Int, dia: Int, hora: Int, precio: Double = 8.0) =
        Funcion(id = id, titulo = titulo, tmdbId = tmdbId, fechaHora = fecha(mes, dia, hora), precio = precio)

    @Test fun agrupaPorPeliculaYOrdenaPorLaProximaFuncion() {
        val lista = listOf(
            funcion("ev-22", "Everest: The Other Side", 1603262, 11, 22, 18),
            funcion("wo-13", "The Way Out", null, 11, 13, 19),
            funcion("ev-20", "Everest: The Other Side", 1603262, 11, 20, 19),
            funcion("wo-14", "The Way Out", null, 11, 14, 19),
        )

        val peliculas = Cartelera.agrupar(lista)

        assertEquals(listOf("The Way Out", "Everest: The Other Side"), peliculas.map { it.titulo })
        assertEquals(listOf("wo-13", "wo-14"), peliculas[0].funciones.map { it.id })
        assertEquals(listOf("ev-20", "ev-22"), peliculas[1].funciones.map { it.id })
        assertEquals("tmdb:1603262", peliculas[1].clave)
        assertEquals("titulo:the way out", peliculas[0].clave)
    }

    @Test fun sinTmdbIdSeReconocePorElTitulo() {
        val peliculas = Cartelera.agrupar(listOf(
            funcion("a", "Girl Winter Film Tour 2026", null, 11, 19, 19),
            funcion("b", " girl winter film tour 2026 ", null, 11, 20, 19),
        ))
        assertEquals(1, peliculas.size)
        assertEquals(2, peliculas[0].funciones.size)
    }

    @Test fun precioDesdeEsElMasBajo() {
        val p = Cartelera.agrupar(listOf(
            funcion("a", "Nocturna", 26700, 11, 14, 16, precio = 0.0),
            funcion("b", "Nocturna", 26700, 11, 15, 16, precio = 8.0),
        )).single()
        assertEquals(0.0, p.precioDesde, 0.0)
    }

    @Test fun lasFuncionesSinFechaSeIgnoran() {
        val peliculas = Cartelera.agrupar(listOf(Funcion(id = "x", titulo = "Sin fecha")))
        assertTrue(peliculas.isEmpty())
    }

    @Test fun rangoDeFechasEnLosDosIdiomas() {
        val es = Locale.forLanguageTag("es-AR")
        assertEquals("13–15 nov", Formato.rangoFechas(fecha(11, 13, 19), fecha(11, 15, 18), zona, es))
        assertEquals("Nov 13–15", Formato.rangoFechas(fecha(11, 13, 19), fecha(11, 15, 18), zona, Locale.US))
        assertEquals("27 nov – 2 dic", Formato.rangoFechas(fecha(11, 27, 19), fecha(12, 2, 18), zona, es))
        assertEquals("Nov 27 – Dec 2", Formato.rangoFechas(fecha(11, 27, 19), fecha(12, 2, 18), zona, Locale.US))
        assertEquals("Sat, Nov 14", Formato.rangoFechas(fecha(11, 14, 16), fecha(11, 14, 16), zona, Locale.US))
        assertEquals("sáb 14 nov", Formato.rangoFechas(fecha(11, 14, 16), fecha(11, 14, 16), zona, es))
    }
}
