package com.example.cartelera.ui.quierover

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.cartelera.R
import com.example.cartelera.databinding.FragmentGuardadasBinding
import com.example.cartelera.databinding.ItemQuieroVerBinding
import com.example.cartelera.ui.pelicula.PeliculaActivity
import com.example.cartelera.util.Formato

/** Pestana "Guardadas": la lista "Quiero verla" de este telefono y su proxima funcion (TP6). */
class GuardadasFragment : Fragment() {

    private var _b: FragmentGuardadasBinding? = null
    private val b get() = _b!!
    private val viewModel: QuieroVerViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentGuardadasBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.filas.observe(viewLifecycleOwner) { filas ->
            b.contenedorFilas.removeAllViews()
            b.tvVacia.visibility = if (filas.isEmpty()) View.VISIBLE else View.GONE
            filas.forEach { fila ->
                val vista = ItemQuieroVerBinding.inflate(layoutInflater, b.contenedorFilas, false)
                vista.tvTitulo.text = fila.titulo
                vista.tvEstado.text = when {
                    !fila.verificada -> getString(R.string.quiero_ver_sin_datos)
                    fila.proxima == null -> getString(R.string.quiero_ver_sin_funciones)
                    else -> getString(
                        R.string.quiero_ver_proxima,
                        fila.proxima.fechaHora?.let { Formato.fechaCorta(it) }.orEmpty(),
                        fila.proxima.sala
                    )
                }
                vista.root.setOnClickListener {
                    startActivity(
                        Intent(requireContext(), PeliculaActivity::class.java)
                            .putExtra(PeliculaActivity.EXTRA_CLAVE, fila.clave)
                    )
                }
                vista.btnQuitar.setOnClickListener { viewModel.quitar(fila.clave) }
                b.contenedorFilas.addView(vista.root)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargar()
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}
