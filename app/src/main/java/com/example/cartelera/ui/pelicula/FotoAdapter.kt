package com.example.cartelera.ui.pelicula

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import com.example.cartelera.databinding.ItemFotoBinding

/** Galeria horizontal con las fotos de la pelicula (backdrops de TMDB). */
class FotoAdapter : ListAdapter<String, FotoAdapter.Holder>(Diferencia) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemFotoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.b.ivFoto.load(getItem(position)) { crossfade(true) }
    }

    class Holder(val b: ItemFotoBinding) : RecyclerView.ViewHolder(b.root)

    private object Diferencia : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(a: String, b: String) = a == b
        override fun areContentsTheSame(a: String, b: String) = a == b
    }
}
