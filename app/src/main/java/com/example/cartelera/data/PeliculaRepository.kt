package com.example.cartelera.data

import android.content.Context
import com.example.cartelera.BuildConfig
import com.example.cartelera.data.tmdb.PeliculaTmdb
import com.example.cartelera.data.tmdb.TmdbApi
import com.example.cartelera.data.tmdb.aPelicula
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Falla de TMDB con el codigo HTTP (401 clave invalida, 404 id inexistente...). */
class TmdbException(val codigo: Int, mensaje: String) : Exception(mensaje)

/**
 * Unico punto de acceso a la API REST de TMDB (Retrofit + OkHttp).
 *
 * Cache en dos niveles, porque la ficha de una pelicula casi no cambia:
 * - en memoria, por id, mientras viva el proceso;
 * - en disco, con la cache HTTP de OkHttp, forzada a 24 h.
 * Asi se respeta la cuota de TMDB y el detalle abre sin red si ya se vio.
 */
class PeliculaRepository internal constructor(
    private val api: TmdbApi,
    private val hayClave: Boolean,
    private val memoria: MutableMap<String, Pelicula> = MEMORIA
) {
    constructor(context: Context) : this(
        crearApi(File(context.cacheDir, "tmdb"), BuildConfig.TMDB_API_KEY, TmdbApi.URL_BASE),
        BuildConfig.TMDB_API_KEY.isNotBlank()
    )

    /** [idioma] de TMDB: "es" o "en-US" segun el telefono (TmdbApi.idiomaPara). */
    fun cargar(
        tmdbId: Int,
        idioma: String = TmdbApi.idiomaPara(Locale.getDefault()),
        alTerminar: (Result<Pelicula>) -> Unit
    ) {
        val clave = "$tmdbId|$idioma"
        memoria[clave]?.let {
            alTerminar(Result.success(it))
            return
        }
        if (!hayClave) {
            alTerminar(Result.failure(TmdbException(0, "Falta TMDB_API_KEY en local.properties")))
            return
        }
        api.pelicula(tmdbId, idioma).enqueue(object : Callback<PeliculaTmdb> {
            override fun onResponse(call: Call<PeliculaTmdb>, response: Response<PeliculaTmdb>) {
                val cuerpo = response.body()
                if (response.isSuccessful && cuerpo != null) {
                    val pelicula = cuerpo.aPelicula(idioma)
                    memoria[clave] = pelicula
                    alTerminar(Result.success(pelicula))
                } else {
                    alTerminar(Result.failure(TmdbException(response.code(), response.message())))
                }
            }

            override fun onFailure(call: Call<PeliculaTmdb>, t: Throwable) {
                alTerminar(Result.failure(t))
            }
        })
    }

    companion object {
        private val MEMORIA = ConcurrentHashMap<String, Pelicula>()
        private const val TAMANIO_CACHE = 10L * 1024 * 1024
        private const val VIGENCIA_S = 24 * 60 * 60

        private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

        /** Arma Retrofit; los tests lo llaman con la URL de MockWebServer. */
        internal fun crearApi(dirCache: File?, clave: String, urlBase: String): TmdbApi {
            val cliente = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .apply { if (dirCache != null) cache(Cache(dirCache, TAMANIO_CACHE)) }
                // La clave viaja como parametro api_key en cada pedido.
                .addInterceptor { chain ->
                    val url = chain.request().url.newBuilder()
                        .addQueryParameter("api_key", clave)
                        .build()
                    chain.proceed(chain.request().newBuilder().url(url).build())
                }
                // Las fichas casi no cambian: se guardan 24 h en la cache de disco.
                .addNetworkInterceptor { chain ->
                    val respuesta = chain.proceed(chain.request())
                    if (!respuesta.isSuccessful) respuesta
                    else respuesta.newBuilder()
                        .header("Cache-Control", "public, max-age=$VIGENCIA_S")
                        .removeHeader("Pragma")
                        .build()
                }
                .build()
            return Retrofit.Builder()
                .baseUrl(urlBase)
                .client(cliente)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(TmdbApi::class.java)
        }
    }
}
