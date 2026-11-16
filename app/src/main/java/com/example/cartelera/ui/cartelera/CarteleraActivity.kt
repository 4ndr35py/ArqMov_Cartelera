package com.example.cartelera.ui.cartelera

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cartelera.R
import com.example.cartelera.data.avisos.Notificador
import com.example.cartelera.data.avisos.Ruteo
import com.example.cartelera.databinding.ActivityCarteleraBinding
import com.example.cartelera.ui.avisos.AvisosActivity
import com.example.cartelera.ui.donacion.DonacionActivity
import com.example.cartelera.ui.entradas.EntradasActivity
import com.example.cartelera.ui.perfil.PerfilActivity
import com.example.cartelera.ui.pelicula.PeliculaActivity
import com.example.cartelera.ui.login.LoginActivity
import com.example.cartelera.ui.quierover.QuieroVerActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Pantalla de inicio desde el TP7: la cartelera se ve sin crear una cuenta. */
class CarteleraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarteleraBinding
    private val viewModel: CarteleraViewModel by viewModels()

    /** Una tarjeta por pelicula; adentro se elige la funcion. */
    private val adapter = PeliculaAdapter { pelicula ->
        startActivity(
            Intent(this, PeliculaActivity::class.java)
                .putExtra(PeliculaActivity.EXTRA_CLAVE, pelicula.clave)
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
                R.id.accion_avisos -> {
                    startActivity(Intent(this, AvisosActivity::class.java))
                    true
                }
                R.id.accion_quiero_ver -> {
                    startActivity(Intent(this, QuieroVerActivity::class.java))
                    true
                }
                R.id.accion_apoyar -> {
                    startActivity(Intent(this, DonacionActivity::class.java))
                    true
                }
                R.id.accion_acerca -> {
                    mostrarAcercaDe()
                    true
                }
                else -> false
            }
        }

        // Las pestanas que abren otra pantalla no cambian la seleccion: Proximas queda marcada.
        binding.navegacion.selectedItemId = R.id.nav_proximas
        binding.navegacion.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_entradas -> startActivity(Intent(this, EntradasActivity::class.java))
                R.id.nav_guardadas -> startActivity(Intent(this, QuieroVerActivity::class.java))
                R.id.nav_perfil -> startActivity(Intent(this, PerfilActivity::class.java))
            }
            false
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
                    adapter.submitList(estado.peliculas)
                    binding.lista.visibility = View.VISIBLE
                }
                is EstadoCartelera.Vacia -> mostrarMensaje(R.string.cartelera_vacia, reintentar = false)
                is EstadoCartelera.Error -> mostrarMensaje(R.string.cartelera_error, reintentar = true)
                is EstadoCartelera.Cargando -> Unit
            }
        }
        // Un aviso que se abre con la app cerrada llega aca con su payload en los extras.
        if (savedInstanceState == null) abrirDestinoDelAviso(intent)

        viewModel.intereses.observe(this) { adapter.actualizarIntereses(it) }
        viewModel.afiches.observe(this) { adapter.actualizarAfiches(it) }
        viewModel.tieneCuenta.observe(this) { cuenta ->
            binding.toolbar.menu.findItem(R.id.accion_cuenta)?.isVisible = !cuenta
            binding.toolbar.menu.findItem(R.id.accion_salir)?.isVisible = cuenta
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.alVolver()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        abrirDestinoDelAviso(intent)
    }

    /**
     * Si la app se abrio tocando un aviso de FCM (o de "Quiero verla"), el
     * payload llega como extras de este Intent. Se rutea a la pantalla que
     * corresponda; sin claves conocidas, se queda en la cartelera.
     */
    private fun abrirDestinoDelAviso(intent: Intent) {
        val extras = intent.extras ?: return
        val datos = extras.keySet().associateWith { clave -> extras.get(clave)?.toString() }
        val destino = Ruteo.de(datos)
        Notificador.intentDe(this, destino)?.let { startActivity(it) }
    }

    private fun mostrarMensaje(texto: Int, reintentar: Boolean) {
        binding.tvMensaje.setText(texto)
        binding.btnReintentar.visibility = if (reintentar) View.VISIBLE else View.GONE
        binding.grupoMensaje.visibility = View.VISIBLE
    }

    /** Atribucion que exigen los terminos de uso de la API de TMDB. */
    private fun mostrarAcercaDe() {
        val margen = (24 * resources.displayMetrics.density).toInt()
        // TMDB pide que su logo sea menos prominente que el de la app: tamaño natural (110 dp).
        val logo = FrameLayout(this).apply {
            setPadding(margen, margen / 2, margen, 0)
            addView(ImageView(context).apply {
                setImageResource(R.drawable.logo_tmdb)
                contentDescription = getString(R.string.cd_logo_tmdb)
            }, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.acerca_titulo)
            .setMessage(R.string.acerca_texto)
            .setView(logo)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
