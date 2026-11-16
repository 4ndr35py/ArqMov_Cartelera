package com.example.cartelera.ui.comentarios

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cartelera.R
import com.example.cartelera.data.Comentario
import com.example.cartelera.databinding.ActivityComentariosBinding
import com.example.cartelera.databinding.ItemComentarioBinding
import com.example.cartelera.util.Formato
import com.google.android.material.snackbar.Snackbar

/** Comentarios de una pelicula: los aprobados por el cine y un campo para escribir uno. */
class ComentariosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityComentariosBinding
    private val viewModel: ComentariosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityComentariosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        binding.toolbar.setNavigationOnClickListener { finish() }

        viewModel.comentarios.observe(this) { pintar(it) }
        viewModel.mensaje.observe(this) { mensaje ->
            if (mensaje == null) return@observe
            Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG).show()
            if (mensaje == R.string.comentario_enviado) binding.etTexto.setText("")
            viewModel.mensajeMostrado()
        }
        binding.btnEnviar.setOnClickListener {
            viewModel.enviar(binding.etTexto.text?.toString().orEmpty())
        }
        viewModel.cargar(intent.getStringExtra(EXTRA_CLAVE).orEmpty())
    }

    private fun pintar(lista: List<Comentario>) {
        binding.contenedor.removeAllViews()
        binding.tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        lista.forEach { comentario ->
            val fila = ItemComentarioBinding.inflate(layoutInflater, binding.contenedor, false)
            fila.tvTexto.text = comentario.texto
            fila.tvFecha.text = comentario.fecha?.let { Formato.fechaLarga(it) }.orEmpty()
            binding.contenedor.addView(fila.root)
        }
    }

    companion object {
        const val EXTRA_CLAVE = "com.example.cartelera.EXTRA_CLAVE_COMENTARIOS"
    }
}
