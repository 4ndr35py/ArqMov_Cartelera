package com.example.cartelera.ui.perfil

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil3.load
import coil3.request.crossfade
import com.example.cartelera.R
import com.example.cartelera.data.AuthRepository
import com.example.cartelera.data.CarteleraRepository
import com.example.cartelera.data.Favorita
import com.example.cartelera.data.FuncionRepository
import com.example.cartelera.data.PeliculaEnCartel
import com.example.cartelera.data.PerfilRepository
import com.example.cartelera.databinding.ActivityEditarPerfilBinding
import com.example.cartelera.util.Cartelera
import com.example.cartelera.util.FechaDia
import com.example.cartelera.util.Formato
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Date

/**
 * Editar perfil: foto, alias, descripcion, fecha de nacimiento y pelicula favorita.
 * Se abre desde el boton "Editar perfil" de la pestana Perfil.
 */
class EditarPerfilActivity : AppCompatActivity() {

    private lateinit var b: ActivityEditarPerfilBinding
    private lateinit var perfil: PerfilRepository
    private var uid: String = ""
    private var fechaNacimiento: String? = null
    private var favorita: Favorita? = null
    private var peliculas: List<PeliculaEnCartel> = emptyList()

    private val elegirFoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@registerForActivityResult
        if (perfil.guardarFoto(uid, uri)) {
            mostrarFoto()
        } else {
            Toast.makeText(this, R.string.perfil_foto_error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        b = ActivityEditarPerfilBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        b.toolbar.setNavigationOnClickListener { finish() }

        val usuario = AuthRepository().usuarioActual
        if (usuario == null) {
            finish()
            return
        }
        uid = usuario.uid
        perfil = PerfilRepository(this)
        b.etAlias.setText(perfil.alias(uid).orEmpty())
        b.etDescripcion.setText(perfil.descripcion(uid).orEmpty())
        fechaNacimiento = perfil.nacimiento(uid)
        favorita = perfil.favorita(uid)
        mostrarFoto()
        pintarNacimiento()
        pintarFavorita()

        b.btnCambiarFoto.setOnClickListener {
            elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        b.btnNacimiento.setOnClickListener { elegirNacimiento() }
        b.btnFavorita.setOnClickListener { elegirFavorita() }
        b.btnGuardar.setOnClickListener { guardar() }

        CarteleraRepository(this, FuncionRepository()).cargar(Date(0)) { r ->
            r.onSuccess { peliculas = Cartelera.agrupar(it) }
        }
    }

    private fun mostrarFoto() {
        val foto = perfil.foto(uid)
        if (foto != null) {
            b.ivFoto.setPadding(0, 0, 0, 0)
            b.ivFoto.load(foto) { crossfade(true) }
        } else {
            val relleno = (24 * resources.displayMetrics.density).toInt()
            b.ivFoto.setPadding(relleno, relleno, relleno, relleno)
            b.ivFoto.setImageResource(R.drawable.ic_nav_perfil)
        }
    }

    private fun pintarNacimiento() {
        val fecha = fechaNacimiento?.let { FechaDia.aDate(it) }
        val edad = fechaNacimiento?.let { FechaDia.anios(it, FechaDia.hoy()) }
        b.btnNacimiento.text = if (fecha != null && edad != null) {
            getString(R.string.perfil_nacimiento_con, Formato.fechaDia(fecha), edad)
        } else {
            getString(R.string.perfil_nacimiento)
        }
    }

    private fun pintarFavorita() {
        b.btnFavorita.text = favorita?.titulo ?: getString(R.string.perfil_elegir_favorita)
    }

    private fun elegirNacimiento() {
        val inicial: Long = fechaNacimiento?.let { FechaDia.millisUtc(it) }
            ?: FechaDia.millisUtc("1990-01-01") ?: 0L
        val selector = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.perfil_nacimiento)
            .setSelection(inicial)
            .setCalendarConstraints(
                CalendarConstraints.Builder().setEnd(MaterialDatePicker.todayInUtcMilliseconds()).build()
            )
            .build()
        selector.addOnPositiveButtonClickListener { millis ->
            fechaNacimiento = FechaDia.desdeMillisUtc(millis)
            pintarNacimiento()
        }
        selector.show(supportFragmentManager, "nacimiento")
    }

    /** Lista de peliculas de la cartelera publicada; "Ninguna" quita la favorita. */
    private fun elegirFavorita() {
        val opciones = listOf(getString(R.string.perfil_ninguna)) + peliculas.map { it.titulo }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.perfil_favorita)
            .setItems(opciones.toTypedArray()) { _, i ->
                favorita = if (i == 0) null else peliculas[i - 1].let { Favorita(it.clave, it.titulo) }
                pintarFavorita()
            }
            .show()
    }

    private fun guardar() {
        perfil.guardarAlias(uid, b.etAlias.text?.toString().orEmpty().trim().take(ALIAS_MAXIMO))
        perfil.guardarDescripcion(uid, b.etDescripcion.text?.toString().orEmpty().trim().take(DESCRIPCION_MAXIMA))
        fechaNacimiento?.let { perfil.guardarNacimiento(uid, it) }
        perfil.guardarFavorita(uid, favorita)
        finish()
    }

    private companion object {
        const val ALIAS_MAXIMO = 20
        const val DESCRIPCION_MAXIMA = 160
    }
}
