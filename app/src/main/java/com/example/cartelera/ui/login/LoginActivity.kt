package com.example.cartelera.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cartelera.databinding.ActivityLoginBinding
import com.example.cartelera.ui.cartelera.CarteleraActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.haySesion()) {
            irACartelera()
            return
        }

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        binding.btnIngresar.setOnClickListener {
            viewModel.ingresar(email(), clave())
        }
        binding.btnRegistrar.setOnClickListener {
            viewModel.registrar(email(), clave())
        }

        // La Activity solo observa y dibuja. El estado vive en el ViewModel,
        // por eso sobrevive a una rotacion (ver escenario 4 del TP1).
        viewModel.estado.observe(this) { estado ->
            val cargando = estado is EstadoLogin.Cargando
            binding.progreso.visibility = if (cargando) View.VISIBLE else View.GONE
            binding.btnIngresar.isEnabled = !cargando
            binding.btnRegistrar.isEnabled = !cargando
            binding.tvError.visibility = View.GONE
            when (estado) {
                is EstadoLogin.Error -> {
                    binding.tvError.setText(estado.mensaje)
                    binding.tvError.visibility = View.VISIBLE
                }
                is EstadoLogin.Exito -> irACartelera()
                else -> Unit
            }
        }
    }

    private fun email() = binding.etEmail.text?.toString().orEmpty()
    private fun clave() = binding.etClave.text?.toString().orEmpty()

    private fun irACartelera() {
        startActivity(Intent(this, CarteleraActivity::class.java))
        finish()
    }
}
