package com.example.cartelera.ui.entradas

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cartelera.R
import com.example.cartelera.data.CarteleraRepository
import com.example.cartelera.data.Pase
import com.example.cartelera.databinding.ActivityMembresiasBinding
import com.example.cartelera.databinding.ItemMembresiaBinding
import com.example.cartelera.util.Formato

/**
 * "Hazte socio": las membresias y los pases del cine, con lo que da cada uno (entradas por
 * funcion, si se puede cancelar, si se renueva sola). Se compran en la web de la venta.
 */
class MembresiasActivity : AppCompatActivity() {

    private lateinit var b: ActivityMembresiasBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        b = ActivityMembresiasBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        b.toolbar.setNavigationOnClickListener { finish() }
        b.btnComprarWeb.setOnClickListener { abrirVenta() }

        CarteleraRepository(this).cargarPases { r ->
            r.onSuccess { pintar(it) }.onFailure { b.tvError.visibility = android.view.View.VISIBLE }
        }
    }

    private fun pintar(pases: List<Pase>) {
        b.contenedor.removeAllViews()
        pases.forEach { p ->
            val fila = ItemMembresiaBinding.inflate(LayoutInflater.from(this), b.contenedor, false)
            fila.tvNombre.text = p.nombre
            fila.tvPrecio.text = Formato.precio(p.precio)
            fila.tvTipo.setText(if (p.membresia) R.string.pase_membresia else R.string.pase_pase)
            val lineas = mutableListOf<String>()
            p.entradasPorFuncion?.let { lineas += resources.getQuantityString(R.plurals.n_entradas_funcion, it, it) }
            lineas += getString(if (p.cancelable) R.string.membresia_cancelable else R.string.membresia_no_cancelable)
            lineas += getString(if (p.renovacion) R.string.membresia_renueva else R.string.membresia_unico)
            fila.tvDetalle.text = lineas.joinToString("\n")
            b.contenedor.addView(fila.root)
        }
    }

    private fun abrirVenta() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, VENTA.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.sin_navegador, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val VENTA = "https://parkcityfilm.eventive.org/"
    }
}
