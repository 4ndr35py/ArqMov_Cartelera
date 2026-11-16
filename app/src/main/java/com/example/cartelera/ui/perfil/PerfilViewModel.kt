package com.example.cartelera.ui.perfil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.cartelera.data.QuieroVerRepository

/**
 * Datos del perfil. Hoy solo hay lo que la app guarda en el telefono: la lista
 * "Quiero verla". Membresia y pelicula favorita dependen de datos del cine que
 * todavia no estan conectados.
 */
class PerfilViewModel(app: Application) : AndroidViewModel(app) {

    /** Peliculas en la lista "Quiero verla" de este telefono. */
    val guardadas: Int = QuieroVerRepository(app).titulos().size
}
