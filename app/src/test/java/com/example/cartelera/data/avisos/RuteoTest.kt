package com.example.cartelera.data.avisos

import org.junit.Assert.assertEquals
import org.junit.Test

class RuteoTest {

    @Test fun funcionIdAbreElDetalleDeLaFuncion() {
        assertEquals(Destino.Funcion("f-42"), Ruteo.de(mapOf("funcionId" to "f-42")))
    }

    @Test fun peliculaClaveAbreLaFicha() {
        assertEquals(Destino.Pelicula("tmdb:1603262"), Ruteo.de(mapOf("peliculaClave" to "tmdb:1603262")))
    }

    @Test fun destinoAvisosAbreLaPantallaDeAvisos() {
        assertEquals(Destino.Avisos, Ruteo.de(mapOf("destino" to "avisos", "tipo" to "eventos")))
    }

    @Test fun sinClavesConocidasVaALaCartelera() {
        assertEquals(Destino.Cartelera, Ruteo.de(mapOf("tipo" to "festival", "otra" to "x")))
        assertEquals(Destino.Cartelera, Ruteo.de(emptyMap()))
    }

    @Test fun funcionIdTienePrioridadSobrePelicula() {
        val datos = mapOf("funcionId" to "f1", "peliculaClave" to "tmdb:9", "destino" to "avisos")
        assertEquals(Destino.Funcion("f1"), Ruteo.de(datos))
    }

    @Test fun valoresEnBlancoSeIgnoran() {
        assertEquals(Destino.Pelicula("tmdb:9"), Ruteo.de(mapOf("funcionId" to "  ", "peliculaClave" to "tmdb:9")))
        assertEquals(Destino.Cartelera, Ruteo.de(mapOf("funcionId" to null)))
    }
}
