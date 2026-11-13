package com.example.cartelera.data

/**
 * Una pelicula de la cartelera con todas sus funciones, ordenadas por fecha.
 * La pantalla de inicio muestra una por pelicula y, adentro, se elige la funcion.
 */
data class PeliculaEnCartel(
    /** "tmdb:1603262" o, si la pelicula no esta en TMDB, "titulo:the way out". */
    val clave: String,
    val titulo: String,
    val tmdbId: Int?,
    val director: String,
    val anio: Int,
    val duracionMin: Int,
    val sinopsis: String,
    val funciones: List<Funcion>
) {
    val primera: Funcion get() = funciones.first()
    val ultima: Funcion get() = funciones.last()
    /** Precio mas bajo entre sus funciones (0 = alguna es gratuita). */
    val precioDesde: Double get() = funciones.minOf { it.precio }
}
