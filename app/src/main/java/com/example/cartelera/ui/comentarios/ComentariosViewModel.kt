package com.example.cartelera.ui.comentarios

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.cartelera.R
import com.example.cartelera.data.AuthRepository
import com.example.cartelera.data.Comentario
import com.example.cartelera.data.ComentariosRepository
import com.example.cartelera.util.Comentarios

/** Comentarios de una pelicula: carga los aprobados y envia uno nuevo. */
class ComentariosViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ComentariosRepository()
    private val sesion = AuthRepository()

    private val _comentarios = MutableLiveData<List<Comentario>>(emptyList())
    val comentarios: LiveData<List<Comentario>> = _comentarios

    /** Texto de un mensaje para mostrar (un string de recursos), o null si no hay. */
    private val _mensaje = MutableLiveData<Int?>(null)
    val mensaje: LiveData<Int?> = _mensaje

    private var clave: String = ""

    fun cargar(clave: String) {
        this.clave = clave
        repo.cargar(clave) { resultado ->
            resultado
                .onSuccess { _comentarios.value = it }
                .onFailure { _mensaje.value = R.string.comentarios_error }
        }
    }

    fun enviar(textoEscrito: String) {
        when (Comentarios.validar(textoEscrito)) {
            Comentarios.Validacion.VACIO -> _mensaje.value = R.string.comentario_vacio
            Comentarios.Validacion.DEMASIADO_LARGO -> _mensaje.value = R.string.comentario_largo
            Comentarios.Validacion.OK -> {
                val texto = Comentarios.normalizar(textoEscrito)
                sesion.asegurarSesion { cuenta ->
                    cuenta
                        .onSuccess { usuario ->
                            repo.enviar(clave, usuario.uid, texto) { envio ->
                                _mensaje.value = if (envio.isSuccess) R.string.comentario_enviado
                                else R.string.comentario_error
                            }
                        }
                        .onFailure { _mensaje.value = R.string.comentario_error }
                }
            }
        }
    }

    fun mensajeMostrado() {
        _mensaje.value = null
    }
}
