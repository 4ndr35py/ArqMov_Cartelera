package com.example.cartelera.ui.avisos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.cartelera.analytics.Metricas
import com.example.cartelera.data.avisos.AvisosRepository
import com.example.cartelera.data.avisos.TemaAviso

/**
 * Preferencias de avisos por tema (TP6, estrategia 1). La vista no conoce
 * Firebase: pide cambios por el repositorio y muestra el estado que llega.
 */
class AvisosViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AvisosRepository()

    private val _temas = MutableLiveData<Map<TemaAviso, Boolean>>()
    val temas: LiveData<Map<TemaAviso, Boolean>> = _temas

    /** true si el ultimo cambio no se pudo guardar; la vista lo muestra una vez. */
    private val _error = MutableLiveData(false)
    val error: LiveData<Boolean> = _error

    init {
        Metricas.avisosPantalla()
        repo.leerTemas { _temas.value = it }
    }

    /** Solo se llama con el permiso de notificaciones ya concedido (ver AvisosActivity). */
    fun cambiar(tema: TemaAviso, activar: Boolean) {
        repo.guardarTema(tema, activar) { resultado ->
            resultado.onSuccess {
                Metricas.temaCambiado(tema.id, activar)
                Metricas.propiedadTema(tema.id, activar)
                _temas.value = _temas.value.orEmpty() + (tema to activar)
            }.onFailure {
                _error.value = true
            }
        }
    }

    fun errorMostrado() {
        _error.value = false
    }
}
