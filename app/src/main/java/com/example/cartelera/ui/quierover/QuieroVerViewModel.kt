package com.example.cartelera.ui.quierover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.cartelera.analytics.Metricas
import com.example.cartelera.data.CarteleraRepository
import com.example.cartelera.data.Funcion
import com.example.cartelera.data.FuncionRepository
import com.example.cartelera.data.QuieroVerRepository
import com.example.cartelera.util.Cartelera
import java.util.Date

/** Una pelicula de la lista "Quiero verla" y su proxima funcion, si hay cartelera. */
data class FilaQuieroVer(
    val clave: String,
    val titulo: String,
    /** Proxima funcion publicada; null si no hay ninguna programada. */
    val proxima: Funcion?,
    /** false si no se pudo leer la cartelera: no se sabe si hay funciones. */
    val verificada: Boolean
)

class QuieroVerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = QuieroVerRepository(app)
    private val carteleraPublicada = CarteleraRepository(app, FuncionRepository())

    private val _filas = MutableLiveData<List<FilaQuieroVer>>(emptyList())
    val filas: LiveData<List<FilaQuieroVer>> = _filas

    /** Lee la cartelera publicada y arma la lista con la proxima funcion de cada pelicula. */
    fun cargar() {
        val marcadas = repo.titulos()
        carteleraPublicada.cargar(Date()) { resultado ->
            val porClave = resultado.getOrNull()
                ?.let { Cartelera.agrupar(it) }
                ?.associateBy { it.clave }
            _filas.value = marcadas
                .map { (clave, titulo) ->
                    FilaQuieroVer(
                        clave = clave,
                        titulo = titulo,
                        proxima = porClave?.get(clave)?.primera,
                        verificada = porClave != null
                    )
                }
                .sortedBy { it.titulo }
        }
    }

    fun quitar(clave: String) {
        repo.quitar(clave)
        Metricas.quieroVerQuitar(clave)
        Metricas.propiedadQuieroVer(repo.marcadas().isNotEmpty())
        cargar()
    }
}
