package com.example.cartelera.ui.detalle

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil3.load
import coil3.request.crossfade
import com.example.cartelera.R
import com.example.cartelera.data.Ficha
import com.example.cartelera.data.Funcion
import com.example.cartelera.databinding.ActivityDetalleBinding
import com.example.cartelera.ui.login.LoginActivity
import com.example.cartelera.util.Formato
import com.google.android.material.snackbar.Snackbar

class DetalleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalleBinding
    private val viewModel: DetalleViewModel by viewModels()

    /** Sin cuenta, «Me interesa» abre el login; si vuelve con cuenta, se marca. */
    private val pedirCuenta = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        viewModel.escucharIntereses()
        if (r.resultCode == Activity.RESULT_OK) alternarInteres()
    }

    /** POST_NOTIFICATIONS se pide recien al marcar la primera funcion, no al abrir la app. */
    private val pedirPermisoAvisos = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (!concedido) avisar(R.string.interes_sin_permiso)
    }

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
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.accion_compartir) {
                compartir()
                true
            } else false
        }

        // Lectura defensiva del extra.
        val id = intent.getStringExtra(EXTRA_FUNCION_ID)
        if (id.isNullOrBlank()) {
            finish()
            return
        }
        viewModel.cargar(id, desdeAviso = intent.getBooleanExtra(EXTRA_DESDE_AVISO, false))

        viewModel.funcion.observe(this) { f ->
            if (f == null) {
                Toast.makeText(this, R.string.detalle_error, Toast.LENGTH_SHORT).show()
                finish()
            } else mostrarFuncion(f)
        }
        viewModel.ficha.observe(this) { mostrarFicha(it) }
        viewModel.interesa.observe(this) { interesa ->
            binding.btnInteres.setText(if (interesa) R.string.btn_interesa_si else R.string.btn_interesa_no)
            binding.btnInteres.setIconResource(
                if (interesa) R.drawable.ic_estrella_llena else R.drawable.ic_estrella_vacia
            )
        }

        binding.btnInteres.setOnClickListener { alternarInteres() }
        binding.btnComprar.setOnClickListener { abrirVenta() }
    }

    private fun mostrarFuncion(f: Funcion) {
        binding.contenido.visibility = View.VISIBLE
        binding.tvFecha.text = f.fechaHora?.let { Formato.fechaLarga(it) } ?: ""
        binding.tvSala.text = getString(R.string.detalle_sala, f.sala, Formato.precio(f.precio))
        binding.btnComprar.isEnabled = f.urlEntradas.isNotBlank()
    }

    private fun mostrarFicha(ficha: Ficha) {
        binding.tvTitulo.text = ficha.titulo
        binding.tvFicha.text = getString(R.string.detalle_ficha, ficha.director, ficha.anio, ficha.duracionMin)
        binding.tvGeneros.text = ficha.generos.joinToString(" · ")
        binding.tvGeneros.visibility = if (ficha.generos.isEmpty()) View.GONE else View.VISIBLE
        binding.tvSinopsis.text = ficha.sinopsis
        if (ficha.urlAfiche != null) {
            binding.ivAfiche.visibility = View.VISIBLE
            binding.ivAfiche.load(ficha.urlAfiche) { crossfade(true) }
        } else {
            binding.ivAfiche.visibility = View.GONE
        }
        binding.grupoTmdb.visibility = if (ficha.deTmdb) View.VISIBLE else View.GONE
    }

    private fun alternarInteres() {
        when (viewModel.alternarInteres()) {
            ResultadoInteres.PEDIR_CUENTA ->
                pedirCuenta.launch(Intent(this, LoginActivity::class.java))
            ResultadoInteres.MARCADA -> {
                avisar(R.string.interes_marcado)
                pedirPermisoSiHaceFalta()
            }
            ResultadoInteres.DESMARCADA -> avisar(R.string.interes_desmarcado)
            ResultadoInteres.SIN_DATOS -> Unit
        }
    }

    private fun pedirPermisoSiHaceFalta() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permiso = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permiso) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            pedirPermisoAvisos.launch(permiso)
        }
    }

    /** Sharesheet de Android: el usuario elige con qué app compartir. */
    private fun compartir() {
        val texto = viewModel.textoParaCompartir() ?: return
        val enviar = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
            putExtra(Intent.EXTRA_TITLE, viewModel.funcion.value?.titulo)
        }
        startActivity(Intent.createChooser(enviar, null))
    }

    /**
     * Smoke test de la hipotesis de compra: la app no vende entradas, abre la
     * pagina de venta que el cine ya usa. Medimos el click, no la venta.
     */
    private fun abrirVenta() {
        val url = viewModel.funcion.value?.urlEntradas ?: return
        viewModel.registrarClickComprar()
        try {
            // Directo: con la visibilidad de paquetes de Android 11+, resolveActivity()
            // puede no encontrar la app que de verdad abre el link.
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(this, R.string.sin_navegador, Toast.LENGTH_SHORT).show()
        }
    }

    private fun avisar(texto: Int) =
        Snackbar.make(binding.root, texto, Snackbar.LENGTH_LONG).setAnchorView(binding.barraAcciones).show()

    companion object {
        const val EXTRA_FUNCION_ID = "com.example.cartelera.EXTRA_FUNCION_ID"
        const val EXTRA_DESDE_AVISO = "com.example.cartelera.EXTRA_DESDE_AVISO"
    }
}
