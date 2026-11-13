package com.example.cartelera.ui.cartelera

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.cartelera.analytics.Metricas
import com.example.cartelera.data.AuthRepository
import com.example.cartelera.data.FuncionRepository
import com.example.cartelera.data.MensajeriaRepository
import com.example.cartelera.data.PeliculaEnCartel
import com.example.cartelera.data.PeliculaRepository
import com.example.cartelera.data.RecordatorioRepository
import com.example.cartelera.data.Suscripcion
import com.example.cartelera.util.Cartelera
import java.util.Date

sealed class EstadoCartelera {
    data object Cargando : EstadoCartelera()
    /** Desde el TP7 la cartelera se organiza por pelicula; las funciones se eligen adentro. */
    data class Lista(val peliculas: List<PeliculaEnCartel>) : EstadoCartelera()
    data object Vacia : EstadoCartelera()
    data object Error : EstadoCartelera()
}

/**
 * La cartelera se ve sin cuenta: al iniciar se abre una sesion anonima
 * (o se reusa la que haya) y recien despues se consulta Firestore, cuyas
 * reglas piden request.auth != null.
 */
class CarteleraViewModel(app: Application) : AndroidViewModel(app) {

    private val auth = AuthRepository()
    private val repo = FuncionRepository()
    private val mensajeria = MensajeriaRepository(auth)
    private val recordatorios = RecordatorioRepository(app)
    private val tmdb = PeliculaRepository(app)

    private val _estado = MutableLiveData<EstadoCartelera>()
    val estado: LiveData<EstadoCartelera> = _estado

    private val _intereses = MutableLiveData<Set<String>>(emptySet())
    val intereses: LiveData<Set<String>> = _intereses

    /** tmdbId -> URL del afiche, para las tarjetas de la cartelera. */
    private val _afiches = MutableLiveData<Map<Int, String>>(emptyMap())
    val afiches: LiveData<Map<Int, String>> = _afiches

    private val _tieneCuenta = MutableLiveData(auth.tieneCuenta)
    val tieneCuenta: LiveData<Boolean> = _tieneCuenta

    private var suscripcion: Suscripcion? = null
    private var uidEscuchado: String? = null

    init {
        iniciar()
    }

    private fun iniciar() {
        _estado.value = EstadoCartelera.Cargando
        auth.asegurarSesion { r ->
            r.onSuccess { usuario ->
                Metricas.identificarUsuario(usuario.uid)
                _tieneCuenta.value = !usuario.anonimo
                cargar()
                escucharIntereses()
                mensajeria.registrarToken()
            }.onFailure {
                _estado.value = EstadoCartelera.Error
            }
        }
    }

    fun cargar() {
        if (auth.usuarioActual == null) {
            iniciar()
            return
        }
        _estado.value = EstadoCartelera.Cargando
        repo.cargarCartelera(Date()) { r ->
            r.onSuccess { lista ->
                Metricas.verCartelera(lista.size)
                val peliculas = Cartelera.agrupar(lista)
                _estado.value = if (peliculas.isEmpty()) EstadoCartelera.Vacia
                else EstadoCartelera.Lista(peliculas)
                cargarAfiches(peliculas)
            }.onFailure {
                _estado.value = EstadoCartelera.Error
            }
        }
    }

    /** Afiche de cada pelicula que esta en TMDB (cacheado: la segunda vez no sale a la red). */
    private fun cargarAfiches(peliculas: List<PeliculaEnCartel>) {
        peliculas.mapNotNull { it.tmdbId }.distinct().forEach { id ->
            tmdb.cargar(id) { r ->
                r.onSuccess { p ->
                    p.urlAfiche?.let { url -> _afiches.value = _afiches.value.orEmpty() + (id to url) }
                }
            }
        }
    }

    /** Al volver a la pantalla: si cambio la cuenta (ingreso en el detalle), se reengancha. */
    fun alVolver() {
        _tieneCuenta.value = auth.tieneCuenta
        val uid = auth.usuarioActual?.uid
        if (uid != null && uid != uidEscuchado) {
            escucharIntereses()
            mensajeria.registrarToken()
        }
    }

    private fun escucharIntereses() {
        suscripcion?.cancelar()
        _intereses.value = emptySet()
        val uid = auth.usuarioActual?.uid ?: return
        uidEscuchado = uid
        suscripcion = repo.escucharIntereses(uid) { _intereses.value = it }
    }

    /** Cierra la cuenta y vuelve a la sesion anonima: la cartelera sigue visible. */
    fun cerrarSesion() {
        Metricas.cerrarSesion()
        Metricas.identificarUsuario(null)
        recordatorios.cancelarTodos()
        suscripcion?.cancelar()
        suscripcion = null
        uidEscuchado = null
        auth.cerrarSesion()
        iniciar()
    }

    /** Los listeners de Firestore se liberan junto con el ViewModel. */
    override fun onCleared() {
        suscripcion?.cancelar()
        super.onCleared()
    }
}
