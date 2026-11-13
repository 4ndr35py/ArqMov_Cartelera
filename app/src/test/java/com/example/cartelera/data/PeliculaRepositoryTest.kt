package com.example.cartelera.data

import com.example.cartelera.data.tmdb.TmdbApi
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Prueba la integracion con TMDB sin salir a internet: MockWebServer hace de
 * api.themoviedb.org y devuelve una respuesta con la forma real de
 * GET /movie/{id} (archivo de prueba en src/test/resources/tmdb).
 */
class PeliculaRepositoryTest {

    @get:Rule val carpeta = TemporaryFolder()

    private lateinit var servidor: MockWebServer

    @Before fun iniciar() {
        servidor = MockWebServer()
        servidor.start()
    }

    @After fun cerrar() = servidor.close()

    private fun repositorio(
        memoria: MutableMap<String, Pelicula> = HashMap(),
        dirCache: java.io.File? = carpeta.newFolder()
    ) = PeliculaRepository(
        PeliculaRepository.crearApi(dirCache, "clave-de-prueba", servidor.url("/3/").toString()),
        hayClave = true,
        memoria = memoria
    )

    private fun respuesta(codigo: Int, cuerpo: String) = MockResponse.Builder()
        .code(codigo)
        .addHeader("Content-Type", "application/json;charset=utf-8")
        .body(cuerpo)
        .build()

    private fun fixture(nombre: String) =
        javaClass.getResource("/tmdb/$nombre")!!.readText()

    /** Llama al repositorio y espera el callback (Retrofit responde en otro hilo). */
    private fun cargar(repo: PeliculaRepository, id: Int, idioma: String = "es"): Result<Pelicula> {
        var resultado: Result<Pelicula>? = null
        val listo = CountDownLatch(1)
        repo.cargar(id, idioma) {
            resultado = it
            listo.countDown()
        }
        assertTrue("TMDB no respondio a tiempo", listo.await(5, TimeUnit.SECONDS))
        return resultado!!
    }

    @Test fun mapeaLaRespuestaDeTmdb() {
        servidor.enqueue(respuesta(200, fixture("movie_nueve_reinas.json")))

        val pelicula = cargar(repositorio(), 12345).getOrThrow()

        assertEquals(12345, pelicula.tmdbId)
        assertEquals("Nueve reinas", pelicula.titulo)
        assertEquals(
            "Dos estafadores se conocen por casualidad y planean vender una plancha de estampillas falsas.",
            pelicula.sinopsis
        )
        assertEquals(114, pelicula.duracionMin)
        assertEquals(listOf("Crimen", "Suspense", "Comedia"), pelicula.generos)
        assertEquals("${TmdbApi.URL_IMAGENES}/afiche_prueba.jpg", pelicula.urlAfiche)
    }

    @Test fun elIdiomaDeTmdbSigueAlDelTelefono() {
        assertEquals("es", TmdbApi.idiomaPara(java.util.Locale.forLanguageTag("es-AR")))
        assertEquals("en-US", TmdbApi.idiomaPara(java.util.Locale.US))
        assertEquals("en-US", TmdbApi.idiomaPara(java.util.Locale.FRANCE))
    }

    @Test fun cadaIdiomaSePideYSeGuardaAparte() {
        servidor.enqueue(respuesta(200, fixture("movie_nueve_reinas.json")))
        servidor.enqueue(respuesta(200, fixture("movie_nueve_reinas.json")))
        val repo = repositorio(dirCache = null)

        cargar(repo, 12345, "es")
        cargar(repo, 12345, "en-US")
        cargar(repo, 12345, "es")   // ya esta en memoria: no sale a la red

        assertEquals(2, servidor.requestCount)
        assertEquals("es", servidor.takeRequest().url.queryParameter("language"))
        assertEquals("en-US", servidor.takeRequest().url.queryParameter("language"))
    }

    @Test fun pideLaPeliculaPorIdEnCastellanoConLaClave() {
        servidor.enqueue(respuesta(200, fixture("movie_nueve_reinas.json")))

        cargar(repositorio(), 12345)

        val pedido = servidor.takeRequest()
        assertEquals("GET", pedido.method)
        assertEquals("/3/movie/12345", pedido.url.encodedPath)
        assertEquals("es", pedido.url.queryParameter("language"))
        assertEquals("clave-de-prueba", pedido.url.queryParameter("api_key"))
    }

    @Test fun sinAficheNiDuracionQuedanNulos() {
        servidor.enqueue(
            respuesta(200, """{"id": 7, "title": "Corto", "poster_path": null, "runtime": 0, "genres": []}""")
        )

        val pelicula = cargar(repositorio(), 7).getOrThrow()

        assertNull(pelicula.urlAfiche)
        assertNull(pelicula.duracionMin)
        assertEquals("", pelicula.sinopsis)
        assertTrue(pelicula.generos.isEmpty())
    }

    @Test fun claveInvalidaDevuelveError401() {
        servidor.enqueue(respuesta(401, """{"status_code": 7, "status_message": "Invalid API key"}"""))

        val error = cargar(repositorio(), 12345).exceptionOrNull()

        assertTrue(error is TmdbException)
        assertEquals(401, (error as TmdbException).codigo)
    }

    @Test fun idInexistenteDevuelveError404() {
        servidor.enqueue(respuesta(404, """{"status_code": 34, "status_message": "Not found"}"""))

        val error = cargar(repositorio(), 999999).exceptionOrNull()

        assertEquals(404, (error as TmdbException).codigo)
    }

    @Test fun laCacheEnMemoriaEvitaElSegundoPedido() {
        servidor.enqueue(respuesta(200, fixture("movie_nueve_reinas.json")))
        val repo = repositorio()

        cargar(repo, 12345)
        val segunda = cargar(repo, 12345).getOrThrow()

        assertEquals("Nueve reinas", segunda.titulo)
        assertEquals(1, servidor.requestCount)
    }

    @Test fun laCacheEnDiscoSirveAunqueSeReinicieLaApp() {
        servidor.enqueue(respuesta(200, fixture("movie_nueve_reinas.json")))
        val dir = carpeta.newFolder()

        // Dos repositorios con memorias distintas simulan dos arranques de la app.
        cargar(repositorio(memoria = HashMap(), dirCache = dir), 12345)
        val segunda = cargar(repositorio(memoria = HashMap(), dirCache = dir), 12345).getOrThrow()

        assertEquals(114, segunda.duracionMin)
        assertEquals(1, servidor.requestCount)
    }
}
