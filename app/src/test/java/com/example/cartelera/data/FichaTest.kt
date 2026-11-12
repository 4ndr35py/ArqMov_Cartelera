package com.example.cartelera.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** TMDB completa la ficha; si no responde, queda lo que cargo el cine. */
class FichaTest {

    private val funcion = Funcion(
        id = "f02", titulo = "Nueve reinas", director = "Fabián Bielinsky", anio = 2000,
        duracionMin = 110, sinopsis = "Sinopsis cargada por el cine."
    )

    private val pelicula = Pelicula(
        tmdbId = 12345, titulo = "Nine Queens", sinopsis = "Sinopsis de TMDB.",
        duracionMin = 114, generos = listOf("Crimen"), urlAfiche = "https://img/afiche.jpg"
    )

    @Test fun sinTmdbQuedanLosDatosDelCine() {
        val ficha = Ficha.de(funcion, null)
        assertEquals("Sinopsis cargada por el cine.", ficha.sinopsis)
        assertEquals(110, ficha.duracionMin)
        assertNull(ficha.urlAfiche)
        assertFalse(ficha.deTmdb)
    }

    @Test fun conTmdbSeCompletaPeroElTituloEsElDelCine() {
        val ficha = Ficha.de(funcion, pelicula)
        assertEquals("Nueve reinas", ficha.titulo)
        assertEquals("Sinopsis de TMDB.", ficha.sinopsis)
        assertEquals(114, ficha.duracionMin)
        assertEquals(listOf("Crimen"), ficha.generos)
        assertEquals("https://img/afiche.jpg", ficha.urlAfiche)
        assertTrue(ficha.deTmdb)
    }

    @Test fun camposVaciosDeTmdbNoPisanLosDelCine() {
        val ficha = Ficha.de(funcion, pelicula.copy(sinopsis = "", duracionMin = null))
        assertEquals("Sinopsis cargada por el cine.", ficha.sinopsis)
        assertEquals(110, ficha.duracionMin)
    }
}
