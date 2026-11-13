package com.example.cartelera.data

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Prueba la lectura de cartelera.json sin salir a internet: MockWebServer hace
 * de Firebase Hosting. El orden es: red, copia guardada, Firestore.
 */
class CarteleraRepositoryTest {

    @get:Rule val carpeta = TemporaryFolder()

    private lateinit var servidor: MockWebServer
    private lateinit var copia: File
    private var firestoreLlamado = false

    @Before fun iniciar() {
        servidor = MockWebServer()
        servidor.start()
        copia = File(carpeta.root, "cartelera.json")
    }

    @After fun cerrar() = servidor.close()

    /** 13 nov 2026 19:30 en Denver (-07:00) = 14 nov 02:30 UTC. */
    private val a13Nov = 1_794_623_400_000L

    private fun repositorio(firestore: (Date, (Result<List<Funcion>>) -> Unit) -> Unit = { _, alTerminar ->
        firestoreLlamado = true
        alTerminar(Result.success(emptyList()))
    }) = CarteleraRepository(
        cliente = OkHttpClient(),
        url = servidor.url("/cartelera.json").toString(),
        copiaLocal = copia,
        respaldoFirestore = firestore,
        entregar = { accion -> accion() }
    )

    private fun respuesta(codigo: Int, cuerpo: String) = MockResponse.Builder()
        .code(codigo)
        .addHeader("Content-Type", "application/json;charset=utf-8")
        .body(cuerpo)
        .build()

    /** Tres funciones: una pasada (2025), dos de noviembre 2026 en desorden. */
    private val archivo = """
        {
          "version": 1, "cine": "Park City Film", "zona": "America/Denver",
          "generado": "2026-11-12T06:00:00+00:00",
          "funciones": [
            {"id": "b", "titulo": "B", "fechaHora": "2026-11-15T18:00:00-07:00", "precio": 8.0,
             "urlEntradas": "https://ejemplo.org/b", "tmdbId": 533},
            {"id": "a", "titulo": "A", "director": "D", "anio": 2026, "duracionMin": 60,
             "sinopsis": "S", "sala": "Jim Santy Auditorium", "fechaHora": "2026-11-13T19:30:00-07:00",
             "precio": 8.0, "urlEntradas": "https://ejemplo.org/a"},
            {"id": "c", "titulo": "C", "fechaHora": "2025-12-01T18:00:00-07:00", "precio": 0.0,
             "urlEntradas": "https://ejemplo.org/c"}
          ]
        }
    """.trimIndent()

    /** Llama al repositorio y espera el resultado (OkHttp responde en otro hilo). */
    private fun cargar(repo: CarteleraRepository, desde: Date): Result<List<Funcion>> {
        var resultado: Result<List<Funcion>>? = null
        val listo = CountDownLatch(1)
        repo.cargar(desde) {
            resultado = it
            listo.countDown()
        }
        assertTrue("el callback no llego", listo.await(5, TimeUnit.SECONDS))
        return resultado!!
    }

    @Test fun cargaDeLaRed_filtraPorFecha_ordenaYGuardaCopia() {
        servidor.enqueue(respuesta(200, archivo))
        val desde = Date(a13Nov - 30 * 60 * 1000L) // 30 min antes de la funcion A

        val lista = cargar(repositorio(), desde).getOrThrow()

        assertEquals(listOf("a", "b"), lista.map { it.id })
        assertEquals(a13Nov, lista[0].fechaHora!!.time)
        assertEquals(533, lista[1].tmdbId)
        assertEquals(null, lista[0].tmdbId)
        assertTrue(copia.exists())
        assertEquals(archivo, copia.readText())
        assertFalse(firestoreLlamado)
    }

    @Test fun redCaida_usaLaCopiaGuardada() {
        copia.writeText(archivo)
        servidor.enqueue(respuesta(500, "error"))

        val lista = cargar(repositorio(), Date(0)).getOrThrow()

        assertEquals(listOf("c", "a", "b"), lista.map { it.id }) // ordenadas por fecha
        assertFalse(firestoreLlamado)
    }

    @Test fun redCaidaSinCopia_leEncargaAFirestore() {
        servidor.enqueue(respuesta(500, "error"))

        cargar(repositorio(), Date(0))

        assertTrue(firestoreLlamado)
    }

    @Test fun respuestaQueNoEsCartelera_noSeGuarda_yUsaLaCopia() {
        copia.writeText(archivo)
        servidor.enqueue(respuesta(200, "<html>mantenimiento</html>"))

        val lista = cargar(repositorio(), Date(0)).getOrThrow()

        assertEquals(3, lista.size)
        assertEquals(archivo, copia.readText())
        assertFalse(firestoreLlamado)
    }

    @Test fun parseaFechasConDesfasoHorario() {
        val funciones = CarteleraRepository.parsear(archivo)!!
        val a = funciones.first { it.id == "a" }
        assertEquals(a13Nov, a.fechaHora!!.time)
        assertEquals("Jim Santy Auditorium", a.sala)
    }
}
