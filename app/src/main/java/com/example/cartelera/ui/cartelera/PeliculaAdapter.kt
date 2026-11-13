package com.example.cartelera.ui.cartelera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import com.example.cartelera.R
import com.example.cartelera.data.PeliculaEnCartel
import com.example.cartelera.databinding.ItemPeliculaBinding
import com.example.cartelera.util.Formato

/** Una tarjeta por pelicula: afiche, titulo, fechas, cantidad de funciones y precio. */
class PeliculaAdapter(
    private val alTocar: (PeliculaEnCartel) -> Unit
) : ListAdapter<PeliculaEnCartel, PeliculaAdapter.Holder>(Diferencia) {

    private var intereses: Set<String> = emptySet()
    private var afiches: Map<Int, String> = emptyMap()

    fun actualizarIntereses(nuevos: Set<String>) {
        intereses = nuevos
        notifyItemRangeChanged(0, itemCount)
    }

    fun actualizarAfiches(nuevos: Map<Int, String>) {
        afiches = nuevos
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemPeliculaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.mostrar(getItem(position))

    inner class Holder(private val b: ItemPeliculaBinding) : RecyclerView.ViewHolder(b.root) {
        fun mostrar(p: PeliculaEnCartel) {
            val ctx = b.root.context
            b.tvTitulo.text = p.titulo
            val desde = p.primera.fechaHora
            val hasta = p.ultima.fechaHora
            val rango = if (desde != null && hasta != null) Formato.rangoFechas(desde, hasta) else ""
            val cantidad = ctx.resources.getQuantityString(R.plurals.n_funciones, p.funciones.size, p.funciones.size)
            b.tvFechas.text = ctx.getString(R.string.item_pelicula_fechas, rango, cantidad)
            b.tvDetalle.text = ctx.getString(R.string.item_detalle, p.primera.sala, p.duracionMin, Formato.precio(p.precioDesde))
            b.ivInteres.visibility = if (p.funciones.any { it.id in intereses }) View.VISIBLE else View.GONE
            val afiche = p.tmdbId?.let { afiches[it] }
            if (afiche != null) b.ivAfiche.load(afiche) { crossfade(true) }
            else b.ivAfiche.setImageResource(R.drawable.ic_pelicula)
            b.root.setOnClickListener { alTocar(p) }
        }
    }

    private object Diferencia : DiffUtil.ItemCallback<PeliculaEnCartel>() {
        override fun areItemsTheSame(a: PeliculaEnCartel, b: PeliculaEnCartel) = a.clave == b.clave
        override fun areContentsTheSame(a: PeliculaEnCartel, b: PeliculaEnCartel) = a == b
    }
}
