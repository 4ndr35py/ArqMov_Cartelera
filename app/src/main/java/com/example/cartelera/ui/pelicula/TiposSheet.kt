package com.example.cartelera.ui.pelicula

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import com.example.cartelera.R
import com.example.cartelera.data.TipoEntrada
import com.example.cartelera.databinding.SheetTiposBinding
import com.example.cartelera.util.Formato
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Hoja inferior para elegir el tipo de entrada antes de comprar. [inicial] viene
 * preseleccionado segun la edad o la membresia; la persona puede cambiarlo.
 */
object TiposSheet {

    fun mostrar(context: Context, tipos: List<TipoEntrada>, inicial: TipoEntrada?, alComprar: (TipoEntrada) -> Unit) {
        val dialogo = BottomSheetDialog(context)
        val b = SheetTiposBinding.inflate(LayoutInflater.from(context))
        var elegido = inicial ?: tipos.first()

        fun pintarBoton() {
            b.btnComprarTipo.text = context.getString(R.string.btn_comprar_tipo, Formato.precio(elegido.precio))
        }

        tipos.forEach { t ->
            val radio = RadioButton(context).apply {
                id = View.generateViewId()
                text = "${t.nombre} · ${Formato.precio(t.precio)}"
                isChecked = t == elegido
                setOnCheckedChangeListener { _, marcado ->
                    if (marcado) {
                        elegido = t
                        pintarBoton()
                    }
                }
            }
            b.grupoTipos.addView(radio)
        }
        pintarBoton()
        b.btnComprarTipo.setOnClickListener {
            dialogo.dismiss()
            alComprar(elegido)
        }
        dialogo.setContentView(b.root)
        dialogo.show()
    }
}
