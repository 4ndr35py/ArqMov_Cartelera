package com.example.cartelera.util

import com.example.cartelera.data.Funcion
import com.example.cartelera.data.PeliculaEnCartel

/**
 * Logica de "Quiero verla" (TP6, estrategia 2). Compara las peliculas que el
 * usuario marco con la cartelera publicada y devuelve las funciones que todavia
 * no se avisaron. Logica pura: se prueba en la JVM.
 */
object QuieroVer {

    /**
     * Funciones de las peliculas marcadas que no estan en [yaAvisadas].
     *
     * [marcadas] son claves de pelicula ("tmdb:..." o "titulo:..."), como las
     * arma [Cartelera.clave]. Al marcar una pelicula, la app registra como
     * "ya avisadas" las funciones que ya estaban programadas: solo se avisa
     * de las que aparecen despues.
     */
    fun funcionesNuevas(
        marcadas: Set<String>,
        cartelera: List<PeliculaEnCartel>,
        yaAvisadas: Set<String>
    ): List<Funcion> =
        cartelera
            .filter { it.clave in marcadas }
            .flatMap { it.funciones }
            .filter { it.id !in yaAvisadas }
}
