package com.example.cartelera.ui.perfil

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cartelera.R
import com.example.cartelera.databinding.ActivityPerfilBinding

/** Pestana "Perfil" de la barra inferior. Muestra lo que la app sabe de la persona. */
class PerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilBinding
    private val viewModel: PerfilViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvGuardadas.text = getString(R.string.perfil_guardadas, viewModel.guardadas)
    }
}
