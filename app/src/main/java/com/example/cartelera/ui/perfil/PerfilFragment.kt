package com.example.cartelera.ui.perfil

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import coil3.load
import coil3.request.crossfade
import com.example.cartelera.R
import com.example.cartelera.databinding.FragmentPerfilBinding
import com.example.cartelera.databinding.ItemFilaPerfilBinding
import com.example.cartelera.ui.pelicula.PeliculaActivity
import com.example.cartelera.util.FechaDia
import com.example.cartelera.util.Formato

/**
 * Pestana "Perfil": foto, nombre, descripcion, pelicula favorita, y lo que la persona marco
 * como vista o comento. Los cambios se editan en "Editar perfil" (un solo boton).
 */
class PerfilFragment : Fragment() {

    private var _b: FragmentPerfilBinding? = null
    private val b get() = _b!!
    private val viewModel: PerfilViewModel by viewModels()

    private var datos: PerfilUi? = null
    private var titulos: Map<String, String> = emptyMap()
    private var seccionVistas = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentPerfilBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.btnEditar.setOnClickListener {
            startActivity(Intent(requireContext(), EditarPerfilActivity::class.java))
        }
        b.grupoSeccion.addOnButtonCheckedListener { _, id, checked ->
            if (checked) {
                seccionVistas = id == R.id.btnSeccionVistas
                pintarSeccion()
            }
        }
        b.grupoSeccion.check(R.id.btnSeccionVistas)

        viewModel.datos.observe(viewLifecycleOwner) { d ->
            datos = d
            pintar()
        }
        viewModel.titulos.observe(viewLifecycleOwner) { t ->
            titulos = t
            pintarSeccion()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargar()
    }

    private fun pintar() {
        val d = datos ?: return
        b.tvNombre.text = d.alias?.takeIf { it.isNotBlank() } ?: getString(R.string.perfil_sin_alias)
        b.tvCorreo.text = d.correo
        b.tvNVistas.text = d.vistas.size.toString()
        b.tvNComentarios.text = d.comentarios?.size?.toString() ?: "…"
        b.tvNLista.text = d.quieroVer.toString()

        val descripcion = d.descripcion
        b.tvDescripcion.text = descripcion
        b.tvDescripcion.visibility = if (descripcion.isNullOrBlank()) View.GONE else View.VISIBLE

        // El titulo de la favorita va en mayusculas; el texto de "todavia no elegiste" no grita.
        b.tvFavorita.isAllCaps = d.favorita != null
        b.tvFavorita.text = d.favorita?.titulo ?: getString(R.string.perfil_sin_favorita)

        val foto = d.foto
        if (foto != null) {
            b.ivFoto.setPadding(0, 0, 0, 0)
            b.ivFoto.load(foto) { crossfade(true) }
        } else {
            // Sin foto: el icono de persona, con aire alrededor.
            val relleno = (22 * resources.displayMetrics.density).toInt()
            b.ivFoto.setPadding(relleno, relleno, relleno, relleno)
            b.ivFoto.setImageResource(R.drawable.ic_nav_perfil)
        }
        pintarSeccion()
    }

    /** Vistas o comentarios, segun la pestana elegida. */
    private fun pintarSeccion() {
        val d = datos ?: return
        b.contenedorFilas.removeAllViews()
        b.tvVacia.visibility = View.GONE
        if (seccionVistas) {
            b.tvSeccion.text = resources.getQuantityString(R.plurals.n_vistas, d.vistas.size, d.vistas.size)
            if (d.vistas.isEmpty()) mostrarVacia(R.string.perfil_vacia_vistas)
            d.vistas.forEach { v ->
                val cuando = FechaDia.aDate(v.fecha)?.let { Formato.fechaDia(it) } ?: v.fecha
                val opinion = when (v.gusto) {
                    true -> " · " + getString(R.string.btn_me_gusta)
                    false -> " · " + getString(R.string.btn_no_me_gusta)
                    null -> ""
                }
                agregarFila(v.titulo, getString(R.string.perfil_vista_el, cuando) + opinion, null, v.clave)
            }
        } else {
            b.tvSeccion.setText(R.string.perfil_seccion_comentarios)
            when {
                d.errorComentarios -> mostrarVacia(R.string.comentarios_error)
                d.comentarios == null -> Unit
                d.comentarios.isEmpty() -> mostrarVacia(R.string.perfil_vacia_comentarios)
                else -> d.comentarios.forEach { c ->
                    val pelicula = titulos[c.clave] ?: c.clave.substringAfter(':')
                    val cuando = c.fecha?.let { Formato.fechaLarga(it) }.orEmpty()
                    agregarFila(pelicula, cuando, c.texto, c.clave)
                }
            }
        }
    }

    private fun agregarFila(titulo: String, subtitulo: String, texto: String?, clave: String) {
        val fila = ItemFilaPerfilBinding.inflate(layoutInflater, b.contenedorFilas, false)
        fila.tvTitulo.text = titulo
        fila.tvSub.text = subtitulo
        if (texto != null) {
            fila.tvTexto.text = texto
            fila.tvTexto.visibility = View.VISIBLE
        }
        fila.root.setOnClickListener {
            startActivity(
                Intent(requireContext(), PeliculaActivity::class.java)
                    .putExtra(PeliculaActivity.EXTRA_CLAVE, clave)
            )
        }
        b.contenedorFilas.addView(fila.root)
    }

    private fun mostrarVacia(texto: Int) {
        b.tvVacia.setText(texto)
        b.tvVacia.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}
