package com.example.cartelera.ui.pelicula

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.cartelera.analytics.Metricas
import com.example.cartelera.data.AuthRepository
import com.example.cartelera.data.CarteleraRepository
import com.example.cartelera.data.Ficha
import com.example.cartelera.data.FuncionRepository
import com.example.cartelera.data.Pelicula
import com.example.cartelera.data.PeliculaEnCartel
import com.example.cartelera.data.PeliculaRepository
import com.example.cartelera.data.QuieroVerRepository
import com.example.cartelera.data.Suscripcion
import com.example.cartelera.util.Cartelera
import java.util.Date

/**
 * Una pelicula con sus funciones: la ficha (completada con TMDB), las fotos,
 * el trailer y la lista de funciones para elegir cuando verla.
 */
class PeliculaViewModel(app: Application) : AndroidViewModel(app) {

    private val auth = AuthRepository()
    private val repo = FuncionRepository()
    private val carteleraPublicada = CarteleraRepository(app, repo)
    private val peliculas = PeliculaRepository(app)
    private val quieroVer = QuieroVerRepository(app)

    private val _cartel = MutableLiveData<PeliculaEnCartel?>()
    val cartel: LiveData<PeliculaEnCartel?> = _cartel

    /** true si la pelicula esta en "Quiero verla" de este telefono (TP6). */
    private val _quieroVer = MutableLiveData(false)
    val marcadaQuieroVer: LiveData<Boolean> = _quieroVer

    private val _ficha = MutableLiveData<Ficha>()
    val ficha: LiveData<Ficha> = _ficha

    /** Fotos y trailer de TMDB (vacio si la pelicula no esta en TMDB). */
    private val _tmdb = MutableLiveData<Pelicula?>()
    val tmdb: LiveData<Pelicula?> = _tmdb

    private val _intereses = MutableLiveData<Set<String>>(emptySet())
    val intereses: LiveData<Set<String>> = _intereses

    private var suscripcion: Suscripcion? = null
    private var claveCargada: String? = null

    fun cargar(clave: String) {
        if (clave == claveCargada) return
        claveCargada = clave
        Metricas.verPelicula(clave)
        carteleraPublicada.cargar(Date()) { r ->
            r.onSuccess { lista ->
                val cartel = Cartelera.agrupar(lista).firstOrNull { it.clave == clave }
                _cartel.value = cartel
                if (cartel != null) {
                    _quieroVer.value = quieroVer.estaMarcada(clave)
                    _ficha.value = Ficha.de(cartel.primera, null)
                    cartel.tmdbId?.let { id ->
                        peliculas.cargar(id) { rp ->
                            rp.onSuccess { p ->
                                _tmdb.value = p
                                _ficha.value = Ficha.de(cartel.primera, p)
                            }
                        }
                    }
                }
            }.onFailure { _cartel.value = null }
        }
        escucharIntereses()
    }

    /** Al volver del detalle de una funcion (pudo crear la cuenta o marcar), se reengancha. */
    fun escucharIntereses() {
        suscripcion?.cancelar()
        val uid = auth.usuarioActual?.uid ?: return
        suscripcion = repo.escucharIntereses(uid) { _intereses.value = it }
    }

    /**
     * "Quiero verla" (TP6): marca o desmarca la pelicula. Al marcarla, sus
     * funciones de hoy se dan por avisadas: el aviso es solo para las nuevas.
     */
    fun alternarQuieroVer(): Boolean {
        val cartel = _cartel.value ?: return false
        val marcar = _quieroVer.value != true
        if (marcar) {
            quieroVer.marcar(cartel.clave, cartel.titulo, cartel.funciones.map { it.id })
            Metricas.quieroVerMarcar(cartel.clave)
        } else {
            quieroVer.quitar(cartel.clave)
            Metricas.quieroVerQuitar(cartel.clave)
        }
        Metricas.propiedadQuieroVer(quieroVer.marcadas().isNotEmpty())
        _quieroVer.value = marcar
        return marcar
    }

    /** URL del trailer en YouTube; se abre con la app de YouTube o el navegador. */
    fun urlTrailer(): String? {
        val clave = _tmdb.value?.trailerYoutube ?: return null
        claveCargada?.let { Metricas.verTrailer(it) }
        return "https://www.youtube.com/watch?v=$clave"
    }

    /** La compra se hace en la web de venta; aca solo se registra el click. */
    fun registrarCompra(funcionId: String) = Metricas.clickComprar(funcionId)

    override fun onCleared() {
        suscripcion?.cancelar()
        super.onCleared()
    }
}
