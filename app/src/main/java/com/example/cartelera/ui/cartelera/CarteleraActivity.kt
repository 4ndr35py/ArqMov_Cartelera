package com.example.cartelera.ui.cartelera

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cartelera.R
import com.example.cartelera.databinding.ActivityCarteleraBinding
import com.example.cartelera.ui.detalle.DetalleActivity
import com.example.cartelera.ui.login.LoginActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Pantalla de inicio desde el TP7: la cartelera se ve sin crear una cuenta. */
class CarteleraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarteleraBinding
    private val viewModel: CarteleraViewModel by viewModels()

    private val adapter = FuncionAdapter { funcion ->
        startActivity(
            Intent(this, DetalleActivity::class.java)
                .putExtra(DetalleActivity.EXTRA_FUNCION_ID, funcion.id)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCarteleraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.accion_cuenta -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                    true
                }
                R.id.accion_salir -> {
                    viewModel.cerrarSesion()
                    true
                }
                R.id.accion_acerca -> {
                    mostrarAcercaDe()
                    true
                }
                else -> false
            }
        }

        binding.lista.adapter = adapter
        binding.swipe.setOnRefreshListener { viewModel.cargar() }
        binding.btnReintentar.setOnClickListener { viewModel.cargar() }

        viewModel.estado.observe(this) { estado ->
            binding.swipe.isRefreshing = estado is EstadoCartelera.Cargando
            binding.lista.visibility = View.GONE
            binding.grupoMensaje.visibility = View.GONE
            when (estado) {
                is EstadoCartelera.Lista -> {
                    adapter.submitList(estado.funciones)
                    binding.lista.visibility = View.VISIBLE
                }
                is EstadoCartelera.Vacia -> mostrarMensaje(R.string.cartelera_vacia, reintentar = false)
                is EstadoCartelera.Error -> mostrarMensaje(R.string.cartelera_error, reintentar = true)
                is EstadoCartelera.Cargando -> Unit
            }
        }
        viewModel.intereses.observe(this) { adapter.actualizarIntereses(it) }
        viewModel.tieneCuenta.observe(this) { cuenta ->
            binding.toolbar.menu.findItem(R.id.accion_cuenta)?.isVisible = !cuenta
            binding.toolbar.menu.findItem(R.id.accion_salir)?.isVisible = cuenta
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.alVolver()
    }

    private fun mostrarMensaje(texto: Int, reintentar: Boolean) {
        binding.tvMensaje.setText(texto)
        binding.btnReintentar.visibility = if (reintentar) View.VISIBLE else View.GONE
        binding.grupoMensaje.visibility = View.VISIBLE
    }

    /** Atribucion que exigen los terminos de uso de la API de TMDB. */
    private fun mostrarAcercaDe() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.acerca_titulo)
            .setMessage(R.string.acerca_texto)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
