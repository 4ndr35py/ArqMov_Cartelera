package com.example.cartelera.ui.donacion

import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cartelera.R
import com.example.cartelera.analytics.Metricas
import com.example.cartelera.data.ResultadoDonacion
import com.example.cartelera.databinding.ActivityDonacionBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

/** Pantalla "Apoyar al cine": donaciones de monto fijo por Google Play Billing (TP4). */
class DonacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDonacionBinding
    private val viewModel: DonacionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDonacionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        viewModel.estado.observe(this) { estado ->
            binding.progreso.visibility = if (estado is EstadoDonacion.Cargando) android.view.View.VISIBLE
            else android.view.View.GONE
            binding.grupoOpciones.removeAllViews()
            binding.tvMensaje.visibility = android.view.View.GONE
            when (estado) {
                is EstadoDonacion.Cargando -> Unit
                is EstadoDonacion.Lista -> estado.opciones.forEach { agregarBoton(it) }
                is EstadoDonacion.NoDisponible -> mostrar(getString(R.string.donacion_no_disponible))
            }
        }

        viewModel.resultado.observe(this) { resultado ->
            if (resultado == null) return@observe
            mostrarResultado(resultado)
            viewModel.resultadoMostrado()
        }
    }

    private fun agregarBoton(opcion: OpcionMostrada) {
        val boton = MaterialButton(this).apply {
            text = getString(R.string.donacion_boton, opcion.montoUsd, opcion.precio)
            if (opcion.sugerida) {
                // Efecto de punto de precio: el monto sugerido se destaca.
                setIconResource(R.drawable.ic_estrella_llena)
                contentDescription = getString(R.string.donacion_sugerida_cd, opcion.montoUsd)
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (12 * resources.displayMetrics.density).toInt() }
            setOnClickListener {
                Metricas.donarIniciar(opcion.montoUsd)
                if (!viewModel.donar(this@DonacionActivity, opcion)) {
                    mostrar(getString(R.string.donacion_no_disponible))
                }
            }
        }
        binding.grupoOpciones.addView(boton)
    }

    private fun mostrarResultado(resultado: ResultadoDonacion) {
        when (resultado) {
            is ResultadoDonacion.Gracias -> {
                Metricas.donacionCompletada(resultado.montoUsd)
                Snackbar.make(binding.root, getString(R.string.donacion_gracias, resultado.montoUsd), Snackbar.LENGTH_LONG).show()
            }
            is ResultadoDonacion.Cancelada -> Unit
            is ResultadoDonacion.Pendiente -> mostrar(getString(R.string.donacion_pendiente))
            is ResultadoDonacion.Fallo -> mostrar(getString(R.string.donacion_fallo, resultado.codigo))
        }
    }

    private fun mostrar(texto: String) {
        binding.tvMensaje.text = texto
        binding.tvMensaje.visibility = android.view.View.VISIBLE
    }
}
