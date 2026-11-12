package com.example.cartelera.ui.detalle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.cartelera.analytics.Metricas
import com.example.cartelera.data.AuthRepository
import com.example.cartelera.data.Ficha
import com.example.cartelera.data.Funcion
import com.example.cartelera.data.FuncionRepository
import com.example.cartelera.data.PeliculaRepository
import com.example.cartelera.data.RecordatorioRepository
import com.example.cartelera.data.Suscripcion
import com.example.cartelera.util.TextoCompartir

/** Que paso al tocar «Me interesa»; la vista decide que mostrar. */
enum class ResultadoInteres { PEDIR_CUENTA, MARCADA, DESMARCADA, SIN_DATOS }

class DetalleViewModel(app: Application) : AndroidViewModel(app) {

    private val auth = AuthRepository()
    private val repo = FuncionRepository()
    private val peliculas = PeliculaRepository(app)
    private val recordatorios = RecordatorioRepository(app)

    private val _funcion = MutableLiveData<Funcion?>()
    val funcion: LiveData<Funcion?> = _funcion

    private val _ficha = MutableLiveData<Ficha>()
    val ficha: LiveData<Ficha> = _ficha

    private val _interesa = MutableLiveData(false)
    val interesa: LiveData<Boolean> = _interesa

    private var suscripcion: Suscripcion? = null
    private var idCargado: String? = null

    /** Idempotente: tras una rotacion no se vuelve a consultar nada. */
    fun cargar(id: String, desdeAviso: Boolean = false) {
        if (id == idCargado) return
        idCargado = id
        if (desdeAviso) Metricas.abrirAviso(id)
        repo.cargarFuncion(id) { r ->
            r.onSuccess { f ->
                Metricas.verFuncion(f.id, f.titulo)
                _funcion.value = f
                _ficha.value = Ficha.de(f, null)
                completarConTmdb(f)
            }.onFailure { _funcion.value = null }
        }
        escucharIntereses()
    }

    /** Si la funcion tiene tmdbId, TMDB completa afiche, sinopsis, duracion y generos. */
    private fun completarConTmdb(f: Funcion) {
        val tmdbId = f.tmdbId ?: return
        peliculas.cargar(tmdbId) { r ->
            r.onSuccess { p -> _ficha.value = Ficha.de(f, p) }
            // Si TMDB falla, queda la ficha que cargo el cine.
        }
    }

    /** Se llama al iniciar y despues de crear la cuenta o ingresar (el uid puede cambiar). */
    fun escucharIntereses() {
        val id = idCargado ?: return
        suscripcion?.cancelar()
        _interesa.value = false
        val uid = auth.usuarioActual?.uid ?: return
        suscripcion = repo.escucharIntereses(uid) { ids -> _interesa.value = id in ids }
    }

    /**
     * Marca o desmarca la funcion. Sin cuenta devuelve PEDIR_CUENTA: la vista
     * abre el login y, si el usuario crea la cuenta, vuelve a llamar.
     * Al marcar se programa el recordatorio; al desmarcar se cancela.
     */
    fun alternarInteres(): ResultadoInteres {
        val f = _funcion.value ?: return ResultadoInteres.SIN_DATOS
        if (!auth.tieneCuenta) return ResultadoInteres.PEDIR_CUENTA
        val uid = auth.usuarioActual?.uid ?: return ResultadoInteres.PEDIR_CUENTA
        val nuevo = !(_interesa.value ?: false)
        repo.marcarInteres(uid, f.id, nuevo)
        Metricas.marcarInteres(f.id, nuevo)
        return if (nuevo) {
            recordatorios.programar(f)
            ResultadoInteres.MARCADA
        } else {
            recordatorios.cancelar(f.id)
            ResultadoInteres.DESMARCADA
        }
    }

    /** Texto para el Sharesheet; registra el evento de medicion. */
    fun textoParaCompartir(): String? {
        val f = _funcion.value ?: return null
        Metricas.compartirFuncion(f.id)
        return TextoCompartir.armar(f)
    }

    fun registrarClickComprar() {
        idCargado?.let { Metricas.clickComprar(it) }
    }

    override fun onCleared() {
        suscripcion?.cancelar()
        super.onCleared()
    }
}
