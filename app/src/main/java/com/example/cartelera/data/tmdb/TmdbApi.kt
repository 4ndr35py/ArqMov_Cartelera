package com.example.cartelera.data.tmdb

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Locale

/**
 * API REST de TMDB v3 (https://developer.themoviedb.org/reference/movie-details).
 * La clave se agrega en un interceptor de OkHttp (PeliculaRepository).
 *
 * Un solo pedido trae la ficha, los videos (traileres de YouTube) y las fotos
 * (append_to_response): menos llamadas contra la cuota de TMDB.
 */
interface TmdbApi {

    @GET("movie/{id}")
    fun pelicula(
        @Path("id") id: Int,
        @Query("language") idioma: String,
        @Query("append_to_response") extras: String = "videos,images",
        @Query("include_video_language") idiomasVideo: String = idiomasExtra(idioma),
        @Query("include_image_language") idiomasImagen: String = "null,en,es"
    ): Call<PeliculaTmdb>

    companion object {
        const val URL_BASE = "https://api.themoviedb.org/3/"
        const val URL_IMAGENES = "https://image.tmdb.org/t/p/w500"
        const val URL_FOTOS = "https://image.tmdb.org/t/p/w780"

        /** Idioma de TMDB segun el del telefono: castellano o ingles (por defecto). */
        fun idiomaPara(locale: Locale): String = if (locale.language == "es") "es" else "en-US"

        /** Videos en el idioma pedido y, si no hay, en ingles (el original de casi todos). */
        fun idiomasExtra(idioma: String) = "${idioma.take(2)},en,null"
    }
}
