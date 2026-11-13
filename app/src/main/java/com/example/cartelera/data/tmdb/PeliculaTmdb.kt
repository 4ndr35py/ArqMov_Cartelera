package com.example.cartelera.data.tmdb

import com.example.cartelera.data.Pelicula
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Respuesta de GET /movie/{id}?append_to_response=videos,images. Solo los campos que usa la app. */
@Serializable
data class PeliculaTmdb(
    val id: Int,
    val title: String = "",
    val overview: String = "",
    val runtime: Int? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    val genres: List<GeneroTmdb> = emptyList(),
    val videos: VideosTmdb = VideosTmdb(),
    val images: ImagenesTmdb = ImagenesTmdb()
)

@Serializable
data class GeneroTmdb(val id: Int, val name: String)

@Serializable
data class VideosTmdb(val results: List<VideoTmdb> = emptyList())

@Serializable
data class VideoTmdb(
    val key: String = "",
    val site: String = "",
    val type: String = "",
    val official: Boolean = false,
    @SerialName("iso_639_1") val idioma: String? = null
)

@Serializable
data class ImagenesTmdb(val backdrops: List<ImagenTmdb> = emptyList())

@Serializable
data class ImagenTmdb(@SerialName("file_path") val ruta: String = "")

/** Cuantas fotos se muestran como maximo (las primeras son las mejor votadas en TMDB). */
const val MAX_FOTOS = 8

/**
 * Mapeo al modelo de la app: arma las URLs del afiche y de las fotos, normaliza
 * vacios y elige el trailer: de YouTube, primero en el idioma pedido, despues el
 * oficial.
 */
fun PeliculaTmdb.aPelicula(
    idioma: String = "en-US",
    urlImagenes: String = TmdbApi.URL_IMAGENES,
    urlFotos: String = TmdbApi.URL_FOTOS
): Pelicula {
    val traileres = videos.results.filter { it.site == "YouTube" && it.type == "Trailer" && it.key.isNotBlank() }
    val trailer = traileres.sortedWith(
        compareByDescending<VideoTmdb> { it.idioma == idioma.take(2) }.thenByDescending { it.official }
    ).firstOrNull()
    return Pelicula(
        tmdbId = id,
        titulo = title,
        sinopsis = overview.trim(),
        duracionMin = runtime?.takeIf { it > 0 },
        generos = genres.map { it.name }.filter { it.isNotBlank() },
        urlAfiche = posterPath?.takeIf { it.isNotBlank() }?.let { urlImagenes + it },
        trailerYoutube = trailer?.key,
        fotos = images.backdrops.map { it.ruta }.filter { it.isNotBlank() }.take(MAX_FOTOS).map { urlFotos + it }
    )
}
