package com.example.cartelera.data.tmdb

import com.example.cartelera.data.Pelicula
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Respuesta de GET /movie/{id}. Solo los campos que usa la app. */
@Serializable
data class PeliculaTmdb(
    val id: Int,
    val title: String = "",
    val overview: String = "",
    val runtime: Int? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    val genres: List<GeneroTmdb> = emptyList()
)

@Serializable
data class GeneroTmdb(val id: Int, val name: String)

/** Mapeo al modelo de la app: arma la URL del afiche y normaliza vacios. */
fun PeliculaTmdb.aPelicula(urlImagenes: String = TmdbApi.URL_IMAGENES) = Pelicula(
    tmdbId = id,
    titulo = title,
    sinopsis = overview.trim(),
    duracionMin = runtime?.takeIf { it > 0 },
    generos = genres.map { it.name }.filter { it.isNotBlank() },
    urlAfiche = posterPath?.takeIf { it.isNotBlank() }?.let { urlImagenes + it }
)
