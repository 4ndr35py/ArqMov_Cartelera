package com.example.cartelera.ui.entradas

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cartelera.R
import com.example.cartelera.databinding.ActivityEntradasBinding

/**
 * Pestana "Entradas" de la barra inferior. Por ahora la app no guarda entradas:
 * la compra se hace en la pagina de venta del cine y el QR lo entrega esa pagina.
 */
class EntradasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntradasBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEntradasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnComprar.setOnClickListener { abrirVenta() }
    }

    private fun abrirVenta() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, VENTA.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.sin_navegador, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        /** Pagina de venta del cine (Eventive); las funciones tienen su propio enlace. */
        const val VENTA = "https://parkcityfilm.eventive.org/"
    }
}
