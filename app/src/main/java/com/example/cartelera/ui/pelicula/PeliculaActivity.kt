package com.example.cartelera.ui.pelicula

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil3.load
import coil3.request.crossfade
import com.example.cartelera.R
import com.example.cartelera.data.Ficha
import com.example.cartelera.data.PeliculaEnCartel
import com.example.cartelera.databinding.ActivityPeliculaBinding
import com.example.cartelera.databinding.ItemFuncionPeliculaBinding
import com.example.cartelera.ui.detalle.DetalleActivity
import com.example.cartelera.util.Formato

/**
 * Pantalla de una pelicula: afiche, ficha, trailer, fotos y la lista de
 * funciones. Al elegir una funcion se abre su detalle (marcar, comprar, compartir).
 */
class PeliculaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPeliculaBinding
    private val viewModel: PeliculaViewModel by viewModels()
    private val fotos = FotoAdapter()
    private var cartel: PeliculaEnCartel? = null
    private var intereses: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPeliculaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.rvFotos.adapter = fotos

        val clave = intent.getStringExtra(EXTRA_CLAVE)
        if (clave.isNullOrBlank()) {
            finish()
            return
        }
        viewModel.cargar(clave)

        viewModel.cartel.observe(this) { c ->
            if (c == null) {
                Toast.makeText(this, R.string.detalle_error, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                cartel = c
                mostrarFunciones()
            }
        }
        viewModel.ficha.observe(this) { mostrarFicha(it) }
        viewModel.tmdb.observe(this) { p ->
            val listaFotos = p?.fotos.orEmpty()
            fotos.submitList(listaFotos)
            binding.grupoFotos.visibility = if (listaFotos.isEmpty()) View.GONE else View.VISIBLE
            binding.btnTrailer.visibility = if (p?.trailerYoutube != null) View.VISIBLE else View.GONE
        }
        viewModel.intereses.observe(this) {
            intereses = it
            mostrarFunciones()
        }
        binding.btnTrailer.setOnClickListener { verTrailer() }
    }

    override fun onResume() {
        super.onResume()
        viewModel.escucharIntereses()
    }

    private fun mostrarFicha(ficha: Ficha) {
        binding.contenido.visibility = View.VISIBLE
        binding.tvTitulo.text = ficha.titulo
        binding.tvFicha.text = getString(R.string.detalle_ficha, ficha.director, ficha.anio, ficha.duracionMin)
        binding.tvGeneros.text = ficha.generos.joinToString(" · ")
        binding.tvGeneros.visibility = if (ficha.generos.isEmpty()) View.GONE else View.VISIBLE
        binding.tvSinopsis.text = ficha.sinopsis
        if (ficha.urlAfiche != null) {
            binding.ivAfiche.visibility = View.VISIBLE
            binding.ivAfiche.load(ficha.urlAfiche) { crossfade(true) }
        } else {
            binding.ivAfiche.visibility = View.GONE
        }
        binding.grupoTmdb.visibility = if (ficha.deTmdb) View.VISIBLE else View.GONE
    }

    /** Una fila por funcion: fecha y hora, sala y precio, y la estrella si esta marcada. */
    private fun mostrarFunciones() {
        val c = cartel ?: return
        val contenedor = binding.contenedorFunciones
        contenedor.removeAllViews()
        c.funciones.forEach { f ->
            val fila = ItemFuncionPeliculaBinding.inflate(LayoutInflater.from(this), contenedor, false)
            fila.tvFecha.text = f.fechaHora?.let { Formato.fechaLarga(it) } ?: ""
            fila.tvDetalle.text = getString(R.string.detalle_sala, f.sala, Formato.precio(f.precio))
            fila.ivInteres.visibility = if (f.id in intereses) View.VISIBLE else View.GONE
            fila.root.setOnClickListener {
                startActivity(
                    Intent(this, DetalleActivity::class.java)
                        .putExtra(DetalleActivity.EXTRA_FUNCION_ID, f.id)
                )
            }
            contenedor.addView(fila.root)
        }
    }

    /**
     * El trailer se abre en la app de YouTube (o en el navegador): Intent implicito.
     * Se lanza directo: desde Android 11 resolveActivity() no "ve" la app de YouTube
     * (visibilidad de paquetes) aunque este instalada.
     */
    private fun verTrailer() {
        val url = viewModel.urlTrailer() ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.sin_navegador, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_CLAVE = "com.example.cartelera.EXTRA_CLAVE_PELICULA"
    }
}
