package com.example.cartelera.ui.cartelera

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cartelera.analytics.Metricas
import com.example.cartelera.data.AuthRepository
import com.example.cartelera.data.Funcion
import com.example.cartelera.data.FuncionRepository
import com.google.firebase.firestore.ListenerRegistration
import java.util.Date

sealed class EstadoCartelera {
    data object Cargando : EstadoCartelera()
    data class Lista(val funciones: List<Funcion>) : EstadoCartelera()
    data object Vacia : EstadoCartelera()
    data object Error : EstadoCartelera()
}

class CarteleraViewModel(
    private val auth: AuthRepository = AuthRepository(),
    private val repo: FuncionRepository = FuncionRepository()
) : ViewModel() {

    private val _estado = MutableLiveData<EstadoCartelera>()
    val estado: LiveData<EstadoCartelera> = _estado

    private val _intereses = MutableLiveData<Set<String>>(emptySet())
    val intereses: LiveData<Set<String>> = _intereses

    private var registroIntereses: ListenerRegistration? = null

    init {
        cargar()
        auth.usuarioActual?.let { usuario ->
            registroIntereses = repo.escucharIntereses(usuario.uid) { _intereses.value = it }
        }
    }

    fun cargar() {
        _estado.value = EstadoCartelera.Cargando
        repo.cargarCartelera(Date()) { r ->
            r.onSuccess { lista ->
                Metricas.verCartelera(lista.size)
                _estado.value = if (lista.isEmpty()) EstadoCartelera.Vacia
                else EstadoCartelera.Lista(lista)
            }.onFailure {
                _estado.value = EstadoCartelera.Error
            }
        }
    }

    fun cerrarSesion() {
        Metricas.cerrarSesion()
        Metricas.identificarUsuario(null)
        auth.cerrarSesion()
    }

    /** El listener de Firestore se libera junto con el ViewModel. */
    override fun onCleared() {
        registroIntereses?.remove()
        super.onCleared()
    }
}
