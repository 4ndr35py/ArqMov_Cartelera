package com.example.cartelera.ui.detalle

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cartelera.R
import com.example.cartelera.data.Funcion
import com.example.cartelera.databinding.ActivityDetalleBinding
import com.example.cartelera.util.Formato

class DetalleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalleBinding
    private val viewModel: DetalleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Lectura defensiva del extra.
        val id = intent.getStringExtra(EXTRA_FUNCION_ID)
        if (id.isNullOrBlank()) {
            finish()
            return
        }
        viewModel.cargar(id)

        viewModel.funcion.observe(this) { f ->
            if (f == null) {
                Toast.makeText(this, R.string.detalle_error, Toast.LENGTH_SHORT).show()
                finish()
            } else mostrar(f)
        }
        viewModel.interesa.observe(this) { interesa ->
            binding.btnInteres.setText(if (interesa) R.string.btn_interesa_si else R.string.btn_interesa_no)
            binding.btnInteres.setIconResource(
                if (interesa) R.drawable.ic_estrella_llena else R.drawable.ic_estrella_vacia
            )
        }

        binding.btnInteres.setOnClickListener { viewModel.alternarInteres() }
        binding.btnComprar.setOnClickListener { abrirVenta() }
    }

    private fun mostrar(f: Funcion) {
        binding.contenido.visibility = View.VISIBLE
        binding.tvTitulo.text = f.titulo
        binding.tvFicha.text = getString(R.string.detalle_ficha, f.director, f.anio, f.duracionMin)
        binding.tvFecha.text = f.fechaHora?.toDate()?.let { Formato.fechaLarga(it) } ?: ""
        binding.tvSala.text = getString(R.string.detalle_sala, f.sala, Formato.precio(f.precio))
        binding.tvSinopsis.text = f.sinopsis
        binding.btnComprar.isEnabled = f.urlEntradas.isNotBlank()
    }

    /**
     * Smoke test de la hipotesis de compra: el MVP no vende entradas, abre la
     * pagina de venta que el cine ya usa. Medimos el click, no la venta.
     */
    private fun abrirVenta() {
        val url = viewModel.funcion.value?.urlEntradas ?: return
        viewModel.registrarClickComprar()
        val ver = Intent(Intent.ACTION_VIEW, url.toUri())
        if (ver.resolveActivity(packageManager) != null) startActivity(ver)
        else Toast.makeText(this, R.string.sin_navegador, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_FUNCION_ID = "com.example.cartelera.EXTRA_FUNCION_ID"
    }
}
