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
            if (item.itemId == R.id.accion_salir) {
                viewModel.cerrarSesion()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            } else false
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
    }

    private fun mostrarMensaje(texto: Int, reintentar: Boolean) {
        binding.tvMensaje.setText(texto)
        binding.btnReintentar.visibility = if (reintentar) View.VISIBLE else View.GONE
        binding.grupoMensaje.visibility = View.VISIBLE
    }
}
