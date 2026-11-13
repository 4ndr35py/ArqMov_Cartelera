package com.example.cartelera.util

import com.example.cartelera.data.Funcion
import com.example.cartelera.data.PeliculaEnCartel

/**
 * Agrupa las funciones (un documento de Firestore por funcion) por pelicula.
 * Logica pura, probada con JUnit.
 */
object Cartelera {

    /** La misma pelicula se reconoce por su tmdbId; si no lo tiene, por el titulo. */
    fun clave(f: Funcion): String =
        f.tmdbId?.let { "tmdb:$it" } ?: "titulo:${f.titulo.trim().lowercase()}"

    /**
     * Una entrada por pelicula, ordenadas por su proxima funcion; adentro, las
     * funciones de esa pelicula en orden cronologico. Las funciones sin fecha se ignoran.
     */
    fun agrupar(funciones: List<Funcion>): List<PeliculaEnCartel> =
        funciones
            .filter { it.fechaHora != null }
            .sortedBy { it.fechaHora }
            .groupBy { clave(it) }          // groupBy respeta el orden: la primera funcion manda
            .map { (clave, lista) ->
                val f = lista.first()
                PeliculaEnCartel(
                    clave = clave,
                    titulo = f.titulo,
                    tmdbId = f.tmdbId,
                    director = f.director,
                    anio = f.anio,
                    duracionMin = f.duracionMin,
                    sinopsis = f.sinopsis,
                    funciones = lista
                )
            }
}
