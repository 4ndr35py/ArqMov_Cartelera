package com.example.cartelera.ui.pelicula

import android.content.ActivityNotFoundException
import android.content.Intent
import com.example.cartelera.ui.login.Acceso
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil3.load
import coil3.request.crossfade
import com.example.cartelera.R
import com.example.cartelera.data.Ficha
import com.example.cartelera.data.PeliculaEnCartel
import com.example.cartelera.data.AuthRepository
import android.net.Uri
import android.provider.CalendarContract
import com.example.cartelera.data.Comentario
import com.example.cartelera.data.Funcion
import com.example.cartelera.data.PerfilRepository
import com.example.cartelera.util.FechaDia
import com.example.cartelera.util.Tipos
import com.example.cartelera.databinding.ActivityPeliculaBinding
import com.example.cartelera.databinding.ItemComentarioBinding
import com.example.cartelera.databinding.ItemFuncionPeliculaBinding
import com.example.cartelera.ui.comentarios.ComentariosViewModel
import com.example.cartelera.ui.detalle.DetalleActivity
import com.example.cartelera.util.Formato
import com.google.android.material.snackbar.Snackbar
import java.util.Date

/**
 * Pantalla de una pelicula: afiche, ficha, trailer, fotos y la lista de
 * funciones. Al elegir una funcion se abre su detalle (marcar, comprar, compartir).
 */
class PeliculaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPeliculaBinding
    private val viewModel: PeliculaViewModel by viewModels()
    private val comentariosVm: ComentariosViewModel by viewModels()
    private var claveActual: String = ""
    private val fotos = FotoAdapter()
    private var cartel: PeliculaEnCartel? = null
    private var intereses: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPeliculaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.rvFotos.adapter = fotos

        // Desde la app o desde un link compartido (https://cartelera-tp2.web.app/pelicula/?c=...).
        val clave = intent.getStringExtra(EXTRA_CLAVE) ?: intent.data?.getQueryParameter("c")
        if (clave.isNullOrBlank()) {
            finish()
            return
        }
        claveActual = clave
        viewModel.cargar(clave)
        cargarComentarios()

        viewModel.cartel.observe(this) { c ->
            if (c == null) {
                Toast.makeText(this, R.string.detalle_error, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                cartel = c
                mostrarFunciones()
            }
        }
        viewModel.ficha.observe(this) { mostrarFicha(it) }
        viewModel.tmdb.observe(this) { p ->
            val listaFotos = p?.fotos.orEmpty()
            fotos.submitList(listaFotos)
            binding.grupoFotos.visibility = if (listaFotos.isEmpty()) View.GONE else View.VISIBLE
            binding.btnTrailer.visibility = if (p?.trailerYoutube != null) View.VISIBLE else View.GONE
        }
        viewModel.intereses.observe(this) {
            intereses = it
            mostrarFunciones()
        }
        viewModel.cartel.observe(this) {
            binding.btnQuieroVer.isEnabled = it != null
            binding.btnYaLaVi.isEnabled = it != null
        }
        viewModel.vista.observe(this) { vista ->
            binding.btnYaLaVi.setText(if (vista) R.string.btn_ya_la_vi_si else R.string.btn_ya_la_vi_no)
            binding.btnYaLaVi.setIconResource(if (vista) R.drawable.ic_estrella_llena else 0)
            binding.grupoGusto.visibility = if (vista) View.VISIBLE else View.GONE
        }
        viewModel.gusto.observe(this) { gusto ->
            binding.btnMeGusta.isChecked = gusto == true
            binding.btnNoMeGusta.isChecked = gusto == false
        }
        binding.btnMeGusta.setOnClickListener { viewModel.opinar(true) }
        binding.btnNoMeGusta.setOnClickListener { viewModel.opinar(false) }
        binding.btnYaLaVi.setOnClickListener {
            if (!Acceso.pedirCuenta(this)) return@setOnClickListener
            val vista = viewModel.alternarVista()
            Snackbar.make(
                binding.root,
                if (vista) R.string.vista_marcada else R.string.vista_quitada,
                Snackbar.LENGTH_LONG
            ).setAnchorView(binding.btnYaLaVi).show()
        }
        viewModel.marcadaQuieroVer.observe(this) { marcada ->
            binding.btnQuieroVer.setText(if (marcada) R.string.btn_quiero_si else R.string.btn_quiero_no)
            binding.btnQuieroVer.setIconResource(if (marcada) R.drawable.ic_estrella_llena else R.drawable.ic_estrella_vacia)
        }
        binding.btnTrailer.setOnClickListener { verTrailer() }
        binding.btnRecomendar.setOnClickListener { recomendar() }
        comentariosVm.comentarios.observe(this) { pintarComentarios(it) }
        comentariosVm.mensaje.observe(this) { mensaje ->
            if (mensaje == null) return@observe
            Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG).setAnchorView(binding.btnEnviarComentario).show()
            if (mensaje == R.string.comentario_enviado) binding.etComentario.setText("")
            comentariosVm.mensajeMostrado()
        }
        binding.btnEnviarComentario.setOnClickListener {
            comentariosVm.enviar(binding.etComentario.text?.toString().orEmpty())
        }
        binding.btnIngresarComentarios.setOnClickListener { Acceso.pedirCuenta(this) }
        binding.btnQuieroVer.setOnClickListener {
            if (!Acceso.pedirCuenta(this)) return@setOnClickListener
            val marcada = viewModel.alternarQuieroVer()
            Snackbar.make(
                binding.root,
                if (marcada) R.string.quiero_ver_marcada else R.string.quiero_ver_quitada,
                Snackbar.LENGTH_LONG
            ).setAnchorView(binding.btnQuieroVer).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.escucharIntereses()
        cargarComentarios()
    }

    /**
     * Comentarios en esta misma pagina. Sin cuenta solo se muestra el aviso de ingreso;
     * con cuenta se leen los publicados y se puede escribir uno.
     */
    private fun cargarComentarios() {
        val conCuenta = AuthRepository().tieneCuenta
        binding.grupoSinCuentaComentarios.visibility = if (conCuenta) View.GONE else View.VISIBLE
        binding.grupoComentarios.visibility = if (conCuenta) View.VISIBLE else View.GONE
        if (conCuenta && claveActual.isNotBlank()) comentariosVm.cargar(claveActual)
    }

    private fun pintarComentarios(lista: List<Comentario>) {
        binding.contenedorComentarios.removeAllViews()
        binding.tvComentariosVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        lista.forEach { comentario ->
            val fila = ItemComentarioBinding.inflate(layoutInflater, binding.contenedorComentarios, false)
            fila.tvTexto.text = comentario.texto
            fila.tvFecha.text = comentario.fecha?.let { Formato.fechaLarga(it) }.orEmpty()
            binding.contenedorComentarios.addView(fila.root)
        }
    }

    private fun mostrarFicha(ficha: Ficha) {
        binding.contenido.visibility = View.VISIBLE
        binding.tvTitulo.text = ficha.titulo
        binding.tvFicha.text = getString(R.string.detalle_ficha, ficha.director, ficha.anio, ficha.duracionMin)
        binding.tvGeneros.text = ficha.generos.joinToString(" · ")
        binding.tvGeneros.visibility = if (ficha.generos.isEmpty()) View.GONE else View.VISIBLE
        binding.tvSinopsis.text = ficha.sinopsis
        // Afiche de TMDB; si no hay, el que publica Eventive.
        val afiche = ficha.urlAfiche ?: cartel?.funciones?.firstNotNullOfOrNull { it.afiche }
        if (afiche != null) {
            binding.ivAfiche.visibility = View.VISIBLE
            binding.ivAfiche.load(afiche) { crossfade(true) }
        } else {
            binding.ivAfiche.visibility = View.GONE
        }
        binding.grupoTmdb.visibility = if (ficha.deTmdb) View.VISIBLE else View.GONE
    }

    /**
     * Una fila por funcion: fecha y hora, sala y precio, y la estrella si esta marcada.
     * Las funciones que ya pasaron se ven, pero no se pueden comprar.
     */
    private fun mostrarFunciones() {
        val c = cartel ?: return
        val contenedor = binding.contenedorFunciones
        contenedor.removeAllViews()
        val ahora = Date()
        c.funciones.forEach { f ->
            val pasada = f.fechaHora?.before(ahora) == true
            val fila = ItemFuncionPeliculaBinding.inflate(LayoutInflater.from(this), contenedor, false)
            fila.tvFecha.text = f.fechaHora?.let { Formato.fechaLarga(it) } ?: ""
            val detalle = getString(R.string.detalle_sala, f.sala, Formato.precio(f.precio))
            fila.tvDetalle.text = if (pasada) "${getString(R.string.funcion_pasada)} · $detalle" else detalle
            fila.ivInteres.visibility = if (f.id in intereses) View.VISIBLE else View.GONE
            fila.root.alpha = if (pasada) 0.5f else 1f
            fila.root.isClickable = !pasada
            fila.root.setOnClickListener { if (Acceso.pedirCuenta(this)) elegirTipo(f) }
            fila.ivCalendario.visibility = if (pasada) View.GONE else View.VISIBLE
            fila.ivCalendario.setOnClickListener { agendar(f) }
            contenedor.addView(fila.root)
        }
    }

    /** Agrega la funcion al calendario del telefono; la app de calendario termina el trabajo. */
    private fun agendar(f: Funcion) {
        val inicio = f.fechaHora?.time ?: return
        val duracion = (cartel?.duracionMin ?: 120).coerceAtLeast(30) * 60_000L
        val evento = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, inicio)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, inicio + duracion)
            .putExtra(CalendarContract.Events.TITLE, f.titulo)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, f.sala)
            .putExtra(CalendarContract.Events.DESCRIPTION, getString(R.string.cine_nombre))
        try {
            startActivity(evento)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.sin_navegador, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Recomendar a alguien: comparte el titulo y el enlace de compra de la proxima funcion.
     * No comparte el QR: la entrada es de quien la compro.
     */
    private fun recomendar() {
        val c = cartel ?: return
        // El link abre la pagina de la pelicula: en la app si esta instalada, o en la web.
        val enlace = Uri.Builder()
            .scheme("https")
            .authority(HOST_PAGINA)
            .path("/pelicula/")
            .appendQueryParameter("c", c.clave)
            .build()
            .toString()
        val envio = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, getString(R.string.recomendar_texto, c.titulo, enlace))
        startActivity(Intent.createChooser(envio, getString(R.string.recomendar_titulo)))
    }

    /**
     * Con tipos de entrada (General, Student/Senior, Miembro...) abre la hoja para elegir;
     * el preseleccionado sale de la edad y de la membresia de la cuenta.
     */
    private fun elegirTipo(f: Funcion) {
        if (f.tipos.isEmpty()) {
            comprarFuncion(f.id, f.urlEntradas)
            return
        }
        val uid = AuthRepository().usuarioActual?.uid
        val perfil = PerfilRepository(this)
        val hoy = FechaDia.hoy()
        val edad = uid?.let { perfil.nacimiento(it) }?.let { FechaDia.anios(it, hoy) }
        // Socio: el tipo y el vencimiento vendran de Eventive; hasta entonces no se preselecciona.
        val esMiembro = false
        val inicial = Tipos.elegir(f.tipos, edad, esMiembro)
        TiposSheet.mostrar(this, f.tipos, inicial) { _ -> comprarFuncion(f.id, f.urlEntradas) }
    }

    /**
     * Tocar una funcion abre directo su venta (la URL de esa funcion), sin pasar
     * por el detalle. Si la funcion no tiene URL de venta, cae al detalle.
     */
    private fun comprarFuncion(funcionId: String, urlEntradas: String) {
        if (urlEntradas.isBlank()) {
            startActivity(
                Intent(this, DetalleActivity::class.java)
                    .putExtra(DetalleActivity.EXTRA_FUNCION_ID, funcionId)
            )
            return
        }
        viewModel.registrarCompra(funcionId)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, urlEntradas.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.sin_navegador, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * El trailer se abre en la app de YouTube (o en el navegador): Intent implicito.
     * Se lanza directo: desde Android 11 resolveActivity() no "ve" la app de YouTube
     * (visibilidad de paquetes) aunque este instalada.
     */
    private fun verTrailer() {
        val url = viewModel.urlTrailer() ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.sin_navegador, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_CLAVE = "com.example.cartelera.EXTRA_CLAVE_PELICULA"
        /** Dominio de Hosting donde viven las paginas de cada pelicula (y el link de la app). */
        const val HOST_PAGINA = "cartelera-tp2.web.app"
    }
}
