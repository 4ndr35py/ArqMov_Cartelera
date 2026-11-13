package com.example.cartelera.data

/** Datos de una pelicula obtenidos de TMDB, ya en el formato de la app. */
data class Pelicula(
    val tmdbId: Int,
    val titulo: String,
    val sinopsis: String,
    val duracionMin: Int?,
    val generos: List<String>,
    val urlAfiche: String?,
    /** Clave del trailer en YouTube (se abre con youtube.com/watch?v=...). */
    val trailerYoutube: String? = null,
    /** Fotos de la pelicula (backdrops de TMDB). */
    val fotos: List<String> = emptyList()
)

/**
 * Lo que muestra el detalle: combina la funcion que carga el cine con los
 * datos de TMDB. TMDB completa sinopsis, duracion, generos y afiche; si no
 * responde (sin red, sin clave o sin tmdbId) queda lo que cargo el cine.
 */
data class Ficha(
    val titulo: String,
    val director: String,
    val anio: Int,
    val duracionMin: Int,
    val sinopsis: String,
    val generos: List<String>,
    val urlAfiche: String?,
    val deTmdb: Boolean
) {
    companion object {
        fun de(f: Funcion, p: Pelicula?) = Ficha(
            titulo = f.titulo.ifBlank { p?.titulo.orEmpty() },
            director = f.director,
            anio = f.anio,
            duracionMin = p?.duracionMin?.takeIf { it > 0 } ?: f.duracionMin,
            sinopsis = p?.sinopsis?.takeIf { it.isNotBlank() } ?: f.sinopsis,
            generos = p?.generos.orEmpty(),
            urlAfiche = p?.urlAfiche,
            deTmdb = p != null
        )
    }
}
