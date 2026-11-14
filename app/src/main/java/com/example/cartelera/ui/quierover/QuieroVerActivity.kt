package com.example.cartelera.ui.quierover

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cartelera.R
import com.example.cartelera.databinding.ActivityQuieroVerBinding
import com.example.cartelera.databinding.ItemQuieroVerBinding
import com.example.cartelera.ui.pelicula.PeliculaActivity
import com.example.cartelera.util.Formato

/** Lista "Quiero verla" (TP6): las peliculas marcadas y su proxima funcion. */
class QuieroVerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuieroVerBinding
    private val viewModel: QuieroVerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityQuieroVerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        binding.toolbar.setNavigationOnClickListener { finish() }

        viewModel.filas.observe(this) { filas ->
            binding.contenedorFilas.removeAllViews()
            binding.tvVacia.visibility = if (filas.isEmpty()) View.VISIBLE else View.GONE
            filas.forEach { fila ->
                val vista = ItemQuieroVerBinding.inflate(LayoutInflater.from(this), binding.contenedorFilas, false)
                vista.tvTitulo.text = fila.titulo
                vista.tvEstado.text = when {
                    !fila.verificada -> getString(R.string.quiero_ver_sin_datos)
                    fila.proxima == null -> getString(R.string.quiero_ver_sin_funciones)
                    else -> getString(
                        R.string.quiero_ver_proxima,
                        fila.proxima.fechaHora?.let { Formato.fechaCorta(it) }.orEmpty(),
                        fila.proxima.sala
                    )
                }
                vista.root.setOnClickListener {
                    startActivity(Intent(this, PeliculaActivity::class.java).putExtra(PeliculaActivity.EXTRA_CLAVE, fila.clave))
                }
                vista.btnQuitar.setOnClickListener { viewModel.quitar(fila.clave) }
                binding.contenedorFilas.addView(vista.root)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargar()
    }
}
