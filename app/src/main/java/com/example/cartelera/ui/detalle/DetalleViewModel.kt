package com.example.cartelera.ui.detalle

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cartelera.analytics.Metricas
import com.example.cartelera.data.AuthRepository
import com.example.cartelera.data.Funcion
import com.example.cartelera.data.FuncionRepository
import com.google.firebase.firestore.ListenerRegistration

class DetalleViewModel(
    private val auth: AuthRepository = AuthRepository(),
    private val repo: FuncionRepository = FuncionRepository()
) : ViewModel() {

    private val _funcion = MutableLiveData<Funcion?>()
    val funcion: LiveData<Funcion?> = _funcion

    private val _interesa = MutableLiveData(false)
    val interesa: LiveData<Boolean> = _interesa

    private var registro: ListenerRegistration? = null
    private var idCargado: String? = null

    /** Idempotente: tras una rotacion no se vuelve a consultar Firestore. */
    fun cargar(id: String) {
        if (id == idCargado) return
        idCargado = id
        repo.cargarFuncion(id) { r ->
            r.onSuccess { f ->
                Metricas.verFuncion(f.id, f.titulo)
                _funcion.value = f
            }.onFailure { _funcion.value = null }
        }
        auth.usuarioActual?.let { usuario ->
            registro = repo.escucharIntereses(usuario.uid) { ids -> _interesa.value = id in ids }
        }
    }

    fun alternarInteres() {
        val id = idCargado ?: return
        val usuario = auth.usuarioActual ?: return
        val nuevo = !(_interesa.value ?: false)
        repo.marcarInteres(usuario.uid, id, nuevo)
        Metricas.marcarInteres(id, nuevo)
    }

    fun registrarClickComprar() {
        idCargado?.let { Metricas.clickComprar(it) }
    }

    override fun onCleared() {
        registro?.remove()
        super.onCleared()
    }
}
