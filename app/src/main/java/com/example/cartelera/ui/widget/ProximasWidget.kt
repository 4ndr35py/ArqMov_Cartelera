package com.example.cartelera.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.cartelera.R
import com.example.cartelera.data.CarteleraRepository
import com.example.cartelera.data.Funcion
import com.example.cartelera.ui.cartelera.CarteleraActivity
import com.example.cartelera.util.Formato
import java.io.File
import java.io.IOException
import java.util.Date

/**
 * Widget de la pantalla de inicio: las proximas tres funciones. Lee la copia de la
 * cartelera que la app guarda en el telefono (no hace red). Sin copia, dice que abra la app.
 */
class ProximasWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val proximas = proximasDeLaCopia(context)
        for (id in ids) {
            val vistas = RemoteViews(context.packageName, R.layout.widget_proximas)
            val lineas = listOf(R.id.tvF1, R.id.tvF2, R.id.tvF3)
            lineas.forEachIndexed { i, viewId ->
                val f = proximas.getOrNull(i)
                val texto = when {
                    f != null -> "${f.titulo} · ${f.fechaHora?.let { Formato.fechaCorta(it) }.orEmpty()}"
                    i == 0 -> context.getString(R.string.widget_vacio)
                    else -> ""
                }
                vistas.setTextViewText(viewId, texto)
            }
            val abrir = PendingIntent.getActivity(
                context, 0, Intent(context, CarteleraActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            vistas.setOnClickPendingIntent(R.id.widgetRaiz, abrir)
            manager.updateAppWidget(id, vistas)
        }
    }

    private fun proximasDeLaCopia(context: Context): List<Funcion> {
        val copia = File(context.filesDir, "cartelera.json")
        val texto = try {
            if (copia.exists()) copia.readText(Charsets.UTF_8) else null
        } catch (e: IOException) {
            null
        } ?: return emptyList()
        val ahora = Date()
        return CarteleraRepository.parsear(texto).orEmpty()
            .filter { it.fechaHora?.after(ahora) == true }
            .sortedBy { it.fechaHora }
            .take(3)
    }
}
