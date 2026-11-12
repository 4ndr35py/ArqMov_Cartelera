package com.example.cartelera.ui.cartelera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cartelera.R
import com.example.cartelera.data.Funcion
import com.example.cartelera.databinding.ItemFuncionBinding
import com.example.cartelera.util.Formato

class FuncionAdapter(
    private val alTocar: (Funcion) -> Unit
) : ListAdapter<Funcion, FuncionAdapter.Holder>(Diferencia) {

    private var intereses: Set<String> = emptySet()

    fun actualizarIntereses(nuevos: Set<String>) {
        intereses = nuevos
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemFuncionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) =
        holder.mostrar(getItem(position))

    inner class Holder(private val b: ItemFuncionBinding) : RecyclerView.ViewHolder(b.root) {
        fun mostrar(f: Funcion) {
            val contexto = b.root.context
            b.tvTitulo.text = f.titulo
            b.tvFecha.text = f.fechaHora?.let { Formato.fechaCorta(it) } ?: ""
            b.tvDetalle.text = contexto.getString(
                R.string.item_detalle, f.sala, f.duracionMin, Formato.precio(f.precio)
            )
            b.ivInteres.visibility = if (f.id in intereses) View.VISIBLE else View.GONE
            b.root.setOnClickListener { alTocar(f) }
        }
    }

    private object Diferencia : DiffUtil.ItemCallback<Funcion>() {
        override fun areItemsTheSame(a: Funcion, b: Funcion) = a.id == b.id
        override fun areContentsTheSame(a: Funcion, b: Funcion) = a == b
    }
}
