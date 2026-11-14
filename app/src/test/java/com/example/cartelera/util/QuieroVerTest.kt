package com.example.cartelera.util

import com.example.cartelera.data.Funcion
import com.example.cartelera.data.PeliculaEnCartel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class QuieroVerTest {

    private fun funcion(id: String, clave: String) =
        Funcion(id = id, titulo = "Peli $clave", tmdbId = clave.removePrefix("tmdb:").toIntOrNull(), fechaHora = Date(0))

    private fun pelicula(clave: String, vararg ids: String) = PeliculaEnCartel(
        clave = clave,
        titulo = "Peli $clave",
        tmdbId = clave.removePrefix("tmdb:").toIntOrNull(),
        director = "",
        anio = 2020,
        duracionMin = 90,
        sinopsis = "",
        funciones = ids.map { funcion(it, clave) }
    )

    private val cartelera = listOf(
        pelicula("tmdb:1", "a1", "a2"),
        pelicula("tmdb:2", "b1"),
        pelicula("titulo:la otra", "c1")
    )

    @Test fun sinMarcadasNoHayAvisos() {
        assertTrue(QuieroVer.funcionesNuevas(emptySet(), cartelera, emptySet()).isEmpty())
    }

    @Test fun marcadaConFuncionesNuevasSeAvisa() {
        val nuevas = QuieroVer.funcionesNuevas(setOf("tmdb:1"), cartelera, emptySet())
        assertEquals(listOf("a1", "a2"), nuevas.map { it.id })
    }

    @Test fun peliculasNoMarcadasNoAparecen() {
        val nuevas = QuieroVer.funcionesNuevas(setOf("tmdb:2"), cartelera, emptySet())
        assertEquals(listOf("b1"), nuevas.map { it.id })
    }

    @Test fun funcionYaAvisadaNoSeRepite() {
        val nuevas = QuieroVer.funcionesNuevas(setOf("tmdb:1"), cartelera, setOf("a1"))
        assertEquals(listOf("a2"), nuevas.map { it.id })
    }

    @Test fun peliculaSinTmdbSeReconocePorTitulo() {
        val nuevas = QuieroVer.funcionesNuevas(setOf("titulo:la otra"), cartelera, emptySet())
        assertEquals(listOf("c1"), nuevas.map { it.id })
    }

    @Test fun marcadaQueNoEstaEnLaCarteleraNoAvisaNada() {
        assertTrue(QuieroVer.funcionesNuevas(setOf("tmdb:99"), cartelera, emptySet()).isEmpty())
    }
}
