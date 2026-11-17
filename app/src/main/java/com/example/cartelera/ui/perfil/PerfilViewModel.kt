package com.example.cartelera.ui.perfil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.cartelera.data.AuthRepository
import com.example.cartelera.data.CarteleraRepository
import com.example.cartelera.data.ComentarioPropio
import com.example.cartelera.data.ComentariosRepository
import com.example.cartelera.data.Favorita
import com.example.cartelera.data.FuncionRepository
import com.example.cartelera.data.PerfilRepository
import com.example.cartelera.data.QuieroVerRepository
import com.example.cartelera.data.Vista
import com.example.cartelera.data.VistasRepository
import com.example.cartelera.util.Cartelera
import java.io.File
import java.util.Date

/**
 * Lo que muestra el perfil. [comentarios] es null mientras se leen; [errorComentarios]
 * si la lectura fallo (por ejemplo, reglas de Firestore sin publicar).
 */
data class PerfilUi(
    val correo: String,
    val alias: String?,
    val descripcion: String?,
    val nacimiento: String?,
    val favorita: Favorita?,
    val foto: File?,
    val vistas: List<Vista>,
    val quieroVer: Int,
    val comentarios: List<ComentarioPropio>?,
    val errorComentarios: Boolean
)

/** Datos del perfil de la cuenta actual. Los cambios los guarda la pantalla Editar perfil. */
class PerfilViewModel(app: Application) : AndroidViewModel(app) {

    private val auth = AuthRepository()
    private val perfil = PerfilRepository(app)
    private val vistas = VistasRepository(app)
    private val quieroVer = QuieroVerRepository(app)
    private val comentarios = ComentariosRepository()
    private val cartelera = CarteleraRepository(app, FuncionRepository())

    private val _datos = MutableLiveData<PerfilUi>()
    val datos: LiveData<PerfilUi> = _datos

    /** Clave de pelicula -> titulo, para mostrar los comentarios con el nombre de la pelicula. */
    private val _titulos = MutableLiveData<Map<String, String>>(emptyMap())
    val titulos: LiveData<Map<String, String>> = _titulos

    fun cargar() {
        val usuario = auth.usuarioActual ?: return
        val uid = usuario.uid
        val previos = _datos.value
        _datos.value = PerfilUi(
            correo = usuario.email.orEmpty(),
            alias = perfil.alias(uid),
            descripcion = perfil.descripcion(uid),
            nacimiento = perfil.nacimiento(uid),
            favorita = perfil.favorita(uid),
            foto = perfil.foto(uid),
            vistas = vistas.vistas(uid),
            quieroVer = quieroVer.marcadas().size,
            comentarios = previos?.comentarios,
            errorComentarios = false
        )
        comentarios.misComentarios(uid) { r ->
            r.onSuccess { lista ->
                _datos.value = _datos.value?.copy(comentarios = lista, errorComentarios = false)
            }.onFailure {
                _datos.value = _datos.value?.copy(comentarios = null, errorComentarios = true)
            }
        }
        cartelera.cargar(Date(0)) { r ->
            r.onSuccess { lista -> _titulos.value = Cartelera.agrupar(lista).associate { it.clave to it.titulo } }
        }
    }
}
