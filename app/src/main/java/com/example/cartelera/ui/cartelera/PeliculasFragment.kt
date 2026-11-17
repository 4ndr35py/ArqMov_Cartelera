package com.example.cartelera.ui.cartelera

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.cartelera.R
import com.example.cartelera.data.PeliculaEnCartel
import com.example.cartelera.databinding.FragmentPeliculasBinding
import com.example.cartelera.ui.pelicula.PeliculaActivity

/**
 * Lista de peliculas de una pestana: "Proximas" (la cartelera) o "Archivo" (funciones que ya
 * pasaron, para leer comentarios). Los filtros (texto y accesibilidad) vienen del ViewModel
 * de la pantalla principal, asi que la barra inferior no vuelve a cargar nada.
 */
class PeliculasFragment : Fragment() {

    private var _b: FragmentPeliculasBinding? = null
    private val b get() = _b!!
    private val viewModel: CarteleraViewModel by activityViewModels()
    private val esArchivo get() = requireArguments().getBoolean(ARG_ARCHIVO)

    /** Lo que llego de la cartelera (o del archivo), sin filtrar. */
    private var base: List<PeliculaEnCartel> = emptyList()
    private var filtros = CarteleraViewModel.Filtros()

    private val adapter = PeliculaAdapter { pelicula ->
        startActivity(
            Intent(requireContext(), PeliculaActivity::class.java)
                .putExtra(PeliculaActivity.EXTRA_CLAVE, pelicula.clave)
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentPeliculasBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.lista.adapter = adapter
        viewModel.intereses.observe(viewLifecycleOwner) { adapter.actualizarIntereses(it) }
        viewModel.afiches.observe(viewLifecycleOwner) { adapter.actualizarAfiches(it) }

        // Chips: el estado de cada chip viene del ViewModel (por eso se usa click, no checked-change).
        b.chipOc.setOnClickListener { viewModel.alternarAccesibilidad("OC") }
        b.chipHi.setOnClickListener { viewModel.alternarAccesibilidad("HI") }
        b.chipVi.setOnClickListener { viewModel.alternarAccesibilidad("VI") }
        viewModel.filtros.observe(viewLifecycleOwner) { f ->
            filtros = f
            b.chipOc.isChecked = "OC" in f.accesibilidad
            b.chipHi.isChecked = "HI" in f.accesibilidad
            b.chipVi.isChecked = "VI" in f.accesibilidad
            pintar()
        }

        if (esArchivo) {
            b.swipe.setOnRefreshListener { viewModel.cargarArchivo() }
            b.btnReintentar.setOnClickListener { viewModel.cargarArchivo() }
            viewModel.archivo.observe(viewLifecycleOwner) { peliculas ->
                b.swipe.isRefreshing = false
                base = peliculas
                if (peliculas.isEmpty()) mostrarMensaje(R.string.archivo_vacia, reintentar = false)
                pintar()
            }
            viewModel.cargarArchivo()
        } else {
            b.swipe.setOnRefreshListener { viewModel.cargar() }
            b.btnReintentar.setOnClickListener { viewModel.cargar() }
            viewModel.estado.observe(viewLifecycleOwner) { estado ->
                b.swipe.isRefreshing = estado is EstadoCartelera.Cargando
                b.grupoMensaje.visibility = View.GONE
                when (estado) {
                    is EstadoCartelera.Lista -> {
                        base = estado.peliculas
                        pintar()
                    }
                    is EstadoCartelera.Vacia -> {
                        base = emptyList()
                        pintar()
                        mostrarMensaje(R.string.cartelera_vacia, reintentar = false)
                    }
                    is EstadoCartelera.Error -> {
                        base = emptyList()
                        pintar()
                        mostrarMensaje(R.string.cartelera_error, reintentar = true)
                    }
                    is EstadoCartelera.Cargando -> b.lista.visibility = View.GONE
                }
            }
        }
    }

    /** Aplica el texto y los chips a la lista, y avisa si el filtro no deja nada. */
    private fun pintar() {
        if (_b == null) return
        val visibles = base.filter { coincide(it) }
        adapter.submitList(visibles)
        b.lista.visibility = if (visibles.isEmpty()) View.GONE else View.VISIBLE
        when {
            base.isEmpty() -> Unit // el mensaje lo pone el estado (vacia, error o archivo vacio)
            visibles.isEmpty() -> mostrarMensaje(
                if (filtros.accesibilidad.isNotEmpty() && filtros.texto.isEmpty()) R.string.accesibilidad_vacia
                else R.string.sin_resultados,
                reintentar = false
            )
            else -> b.grupoMensaje.visibility = View.GONE
        }
    }

    /** Titulo o director contienen el texto; y tiene todos los subtitulos/audiodescripciones elegidos. */
    private fun coincide(p: PeliculaEnCartel): Boolean {
        val texto = filtros.texto
        val porTexto = texto.isEmpty() ||
            p.titulo.contains(texto, ignoreCase = true) ||
            p.funciones.any { it.director.contains(texto, ignoreCase = true) }
        val porAccesibilidad = filtros.accesibilidad.all { codigo ->
            p.funciones.any { codigo in it.accesibilidad }
        }
        return porTexto && porAccesibilidad
    }

    private fun mostrarMensaje(texto: Int, reintentar: Boolean) {
        b.tvMensaje.setText(texto)
        b.btnReintentar.visibility = if (reintentar) View.VISIBLE else View.GONE
        b.grupoMensaje.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ARCHIVO = "archivo"

        fun proximas() = PeliculasFragment().apply { arguments = Bundle().apply { putBoolean(ARG_ARCHIVO, false) } }
        fun archivo() = PeliculasFragment().apply { arguments = Bundle().apply { putBoolean(ARG_ARCHIVO, true) } }
    }
}
