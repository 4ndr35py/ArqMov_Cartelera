package com.example.cartelera.ui.cartelera

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.textfield.TextInputEditText
import com.example.cartelera.R
import com.example.cartelera.data.avisos.Notificador
import com.example.cartelera.data.avisos.Ruteo
import com.example.cartelera.databinding.ActivityCarteleraBinding
import com.example.cartelera.ui.avisos.AvisosActivity
import com.example.cartelera.ui.donacion.DonacionActivity
import com.example.cartelera.ui.entradas.EntradasFragment
import com.example.cartelera.ui.login.Acceso
import com.example.cartelera.ui.login.LoginActivity
import com.example.cartelera.ui.perfil.PerfilFragment
import com.example.cartelera.ui.quierover.GuardadasFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Pantalla de inicio. Las pestanas (Proximas, Archivo, Entradas, Guardadas, Perfil)
 * son fragmentos dentro de esta pantalla: la barra inferior flotante no desaparece al
 * cambiar de pestana, y "atras" vuelve a Proximas antes de cerrar la app.
 */
class CarteleraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarteleraBinding
    private val viewModel: CarteleraViewModel by viewModels()

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
        // La barra flotante no suma su propio inset de navegacion: el de la ventana ya lo pone la pantalla.
        ViewCompat.setOnApplyWindowInsetsListener(binding.navegacion) { v, insets ->
            v.setPadding(v.paddingLeft, 0, v.paddingRight, 0)
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
                    Acceso.requiereCuenta(this) { startActivity(Intent(this, AvisosActivity::class.java)) }
                    true
                }
                R.id.accion_apoyar -> {
                    startActivity(Intent(this, DonacionActivity::class.java))
                    true
                }
                R.id.accion_idioma -> {
                    mostrarIdioma()
                    true
                }
                R.id.accion_buscar -> {
                    mostrarBuscar()
                    true
                }
                R.id.accion_acerca -> {
                    mostrarAcercaDe()
                    true
                }
                else -> false
            }
        }

        // Entradas, Guardadas y Perfil piden cuenta: sin cuenta no se cambia de pestana.
        binding.navegacion.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_proximas, R.id.nav_archivo -> {
                    mostrar(item.itemId)
                    true
                }
                R.id.nav_entradas, R.id.nav_guardadas, R.id.nav_perfil -> {
                    if (Acceso.pedirCuenta(this)) {
                        mostrar(item.itemId)
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
        if (savedInstanceState == null) {
            binding.navegacion.selectedItemId = R.id.nav_proximas
        }

        // Tras girar o cambiar el idioma la actividad se recrea: se repone el titulo y el menu.
        if (savedInstanceState != null) {
            binding.navegacion.post {
                val id = binding.navegacion.selectedItemId
                val titulos = mapOf(
                    R.id.nav_archivo to R.string.archivo_titulo,
                    R.id.nav_entradas to R.string.entradas_titulo,
                    R.id.nav_guardadas to R.string.quiero_ver_titulo,
                    R.id.nav_perfil to R.string.perfil_titulo
                )
                binding.toolbar.setTitle(titulos[id] ?: R.string.cartelera_titulo)
                actualizarMenu(id)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.navegacion.selectedItemId != R.id.nav_proximas) {
                    binding.navegacion.selectedItemId = R.id.nav_proximas
                } else {
                    finish()
                }
            }
        })

        viewModel.tieneCuenta.observe(this) { cuenta ->
            binding.toolbar.menu.findItem(R.id.accion_cuenta)?.isVisible = !cuenta
            binding.toolbar.menu.findItem(R.id.accion_salir)?.isVisible = cuenta
        }
        // Un aviso que se abre con la app cerrada llega aca con su payload en los extras.
        if (savedInstanceState == null) abrirDestinoDelAviso(intent)
    }

    /** Cambia el contenido de la pestana; la barra inferior queda igual. */
    private fun mostrar(id: Int) {
        val (titulo, fragmento) = when (id) {
            R.id.nav_archivo -> R.string.archivo_titulo to PeliculasFragment.archivo()
            R.id.nav_entradas -> R.string.entradas_titulo to EntradasFragment()
            R.id.nav_guardadas -> R.string.quiero_ver_titulo to GuardadasFragment()
            R.id.nav_perfil -> R.string.perfil_titulo to PerfilFragment()
            else -> R.string.cartelera_titulo to PeliculasFragment.proximas()
        }
        binding.toolbar.setTitle(titulo)
        // El menu de tres puntos (cuenta, avisos, apoyo, acerca de) vive solo en Perfil.
        actualizarMenu(id)
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenido, fragmento)
            .commit()
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

    /**
     * Menu de la barra: los tres puntos solo en Perfil, buscar solo en las listas, y
     * "ingresar" o "cerrar sesion" segun la cuenta (setGroupVisible pisaba ese estado).
     */
    private fun actualizarMenu(id: Int) {
        val menu = binding.toolbar.menu
        menu.setGroupVisible(R.id.grupo_config, id == R.id.nav_perfil)
        menu.findItem(R.id.accion_buscar)?.isVisible = id == R.id.nav_proximas || id == R.id.nav_archivo
        val conCuenta = viewModel.tieneCuenta.value == true
        menu.findItem(R.id.accion_cuenta)?.isVisible = !conCuenta
        menu.findItem(R.id.accion_salir)?.isVisible = conCuenta
    }

    /** Idioma de la app: el del telefono, o espanol / ingles. La app se redibuja sola. */
    private fun mostrarIdioma() {
        val opciones = arrayOf(getString(R.string.idioma_sistema), "Español", "English")
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.accion_idioma)
            .setItems(opciones) { _, i ->
                val idioma = when (i) {
                    1 -> LocaleListCompat.forLanguageTags("es")
                    2 -> LocaleListCompat.forLanguageTags("en")
                    else -> LocaleListCompat.getEmptyLocaleList()
                }
                AppCompatDelegate.setApplicationLocales(idioma)
            }
            .show()
    }

    /** Busca por titulo o director dentro de Proximas o Archivo. */
    private fun mostrarBuscar() {
        val entrada = TextInputEditText(this).apply {
            setText(viewModel.filtros.value?.texto.orEmpty())
            hint = getString(R.string.buscar_hint)
            setSingleLine(true)
            val relleno = (20 * resources.displayMetrics.density).toInt()
            setPadding(relleno, relleno / 2, relleno, relleno / 2)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.buscar_titulo)
            .setView(entrada)
            .setPositiveButton(R.string.buscar_aplicar) { _, _ ->
                viewModel.buscar(entrada.text?.toString().orEmpty())
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
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
