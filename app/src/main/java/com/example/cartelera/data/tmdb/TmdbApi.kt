package com.example.cartelera.data.tmdb

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Locale

/**
 * API REST de TMDB v3 (https://developer.themoviedb.org/reference/movie-details).
 * La clave se agrega en un interceptor de OkHttp (PeliculaRepository).
 */
interface TmdbApi {

    @GET("movie/{id}")
    fun pelicula(
        @Path("id") id: Int,
        @Query("language") idioma: String
    ): Call<PeliculaTmdb>

    companion object {
        const val URL_BASE = "https://api.themoviedb.org/3/"
        const val URL_IMAGENES = "https://image.tmdb.org/t/p/w500"

        /** Idioma de TMDB segun el del telefono: castellano o ingles (por defecto). */
        fun idiomaPara(locale: Locale): String = if (locale.language == "es") "es" else "en-US"
    }
}
