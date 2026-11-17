package com.example.cartelera.ui.entradas

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.cartelera.R
import com.example.cartelera.databinding.ItemEntradaQrBinding
import com.example.cartelera.databinding.ItemGrupoEntradasBinding

/**
 * Las entradas de UNA funcion: titulo, fecha, sala y sus QR. [esHoy] marca la funcion de hoy,
 * que es la que se muestra al abrir la pestana.
 */
data class GrupoEntradas(
    val titulo: String,
    val fecha: String,
    val sala: String,
    val esHoy: Boolean,
    val qrs: List<Bitmap>
)

/** Una pagina por funcion. Dentro, las entradas de esa funcion se deslizan de a una. */
class GruposAdapter(
    private val grupos: List<GrupoEntradas>,
    private val enviar: (Bitmap, String) -> Unit
) : RecyclerView.Adapter<GruposAdapter.Holder>() {

    class Holder(val b: ItemGrupoEntradasBinding) : RecyclerView.ViewHolder(b.root) {
        /** Las entradas de la funcion se "pegan" una por una, como las paginas de un carrusel. */
        val snap = PagerSnapHelper().also { it.attachToRecyclerView(b.listaEntradas) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemGrupoEntradasBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = grupos.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val g = grupos[position]
        val ctx = holder.b.root.context
        holder.b.tvTituloFuncion.text = g.titulo
        holder.b.tvFechaFuncion.text = if (g.esHoy) "${ctx.getString(R.string.qr_hoy)} · ${g.fecha}" else g.fecha
        holder.b.tvSala.text = g.sala
        holder.b.tvDeslizar.visibility = if (g.qrs.size > 1) View.VISIBLE else View.GONE
        holder.b.listaEntradas.adapter = EntradasQrAdapter(g.qrs) { bmp, n -> enviar(bmp, "g${position}_$n") }
    }
}

/** Las entradas de una funcion, una pagina por QR. */
class EntradasQrAdapter(
    private val qrs: List<Bitmap>,
    private val enviar: (Bitmap, Int) -> Unit
) : RecyclerView.Adapter<EntradasQrAdapter.Holder>() {

    class Holder(val b: ItemEntradaQrBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemEntradaQrBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = qrs.size

    /** La lista de entradas de la funcion: las flechas la mueven. */
    private var lista: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        lista = recyclerView
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val ctx = holder.b.root.context
        holder.b.ivQr.setImageBitmap(qrs[position])
        holder.b.tvNumero.text = ctx.getString(R.string.qr_entrada, position + 1, qrs.size)
        holder.b.btnEnviar.setOnClickListener { enviar(qrs[position], position + 1) }

        // Flechas: solo del lado hacia donde se puede mover; tocarlas mueve a la entrada vecina.
        holder.b.ivFlechaIzq.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE
        holder.b.ivFlechaDer.visibility = if (position < qrs.size - 1) View.VISIBLE else View.INVISIBLE
        holder.b.ivFlechaIzq.setOnClickListener {
            lista?.smoothScrollToPosition(holder.bindingAdapterPosition - 1)
        }
        holder.b.ivFlechaDer.setOnClickListener {
            lista?.smoothScrollToPosition(holder.bindingAdapterPosition + 1)
        }
    }
}
