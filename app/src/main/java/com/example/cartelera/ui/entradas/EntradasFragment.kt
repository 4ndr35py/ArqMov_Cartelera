package com.example.cartelera.ui.entradas

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.example.cartelera.R
import com.example.cartelera.databinding.FragmentEntradasBinding
import com.example.cartelera.util.FechaDia
import com.example.cartelera.util.Formato
import com.example.cartelera.util.QrPrueba
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.util.Date

/**
 * Pestana "Entradas". Muestra las entradas agrupadas por funcion: al abrir, la de hoy; se
 * desliza para ver las otras. Cada QR se puede enviar a otra persona.
 *
 * Los datos de entradas reales todavia no llegan a la app (Eventive solo las entrega con la
 * clave secreta, que va en un servidor). Por eso en versiones de desarrollo se muestran
 * ENTRADAS DE PRUEBA, con QR falsos.
 */
class EntradasFragment : Fragment() {

    private var _b: FragmentEntradasBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentEntradasBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.btnComoLlegar.setOnClickListener { abrirMapa() }
        b.btnHazteSocio.setOnClickListener {
            startActivity(Intent(requireContext(), MembresiasActivity::class.java))
        }

        val grupos = if (esDesarrollo()) gruposDePrueba() else emptyList()
        if (grupos.isEmpty()) {
            b.tvVacias.visibility = View.VISIBLE
            b.listaFunciones.visibility = View.GONE
            return
        }
        b.listaFunciones.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(b.listaFunciones)
        b.listaFunciones.adapter = GruposAdapter(grupos) { bmp, nombre -> enviarQr(bmp, nombre) }
        // Al abrir: la funcion de hoy (o la primera si no hay de hoy).
        val hoy = grupos.indexOfFirst { it.esHoy }.coerceAtLeast(0)
        b.listaFunciones.post { b.listaFunciones.scrollToPosition(hoy) }
    }

    private fun esDesarrollo(): Boolean =
        (requireContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    /** Entradas de prueba: una funcion de hoy con dos QR, y otra de dentro de cuatro dias con uno. */
    private fun gruposDePrueba(): List<GrupoEntradas> {
        val ahora = Date()
        val dentroDe4Dias = Date(ahora.time + 4L * 24 * 60 * 60 * 1000)
        val sala = "Jim Santy Auditorium"
        val titulo = "Girl Winter Film Tour 2026"
        return listOf(
            GrupoEntradas(
                titulo = titulo,
                fecha = Formato.fechaLarga(ahora),
                sala = sala,
                esHoy = true,
                qrs = listOf<Bitmap>(QrPrueba.generar(semilla = 7), QrPrueba.generar(semilla = 11))
            ),
            GrupoEntradas(
                titulo = titulo,
                fecha = Formato.fechaLarga(dentroDe4Dias),
                sala = sala,
                esHoy = false,
                qrs = listOf<Bitmap>(QrPrueba.generar(semilla = 23))
            )
        )
    }

    /**
     * Antes de compartir: el QR es una entrada, asi que solo se comparte si la compraste
     * para la persona que la recibe.
     */
    private fun enviarQr(qr: Bitmap, nombre: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.qr_confirmar_titulo)
            .setMessage(R.string.qr_confirmar_texto)
            .setPositiveButton(R.string.qr_confirmar_si) { _, _ -> compartirQr(qr, nombre) }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    /** Comparte la imagen del QR por la hoja de compartir (WhatsApp, mensajes, correo...). */
    private fun compartirQr(qr: Bitmap, nombre: String) {
        val contexto = requireContext()
        val carpeta = File(contexto.cacheDir, "qr").apply { mkdirs() }
        val archivo = File(carpeta, "entrada_prueba_$nombre.png")
        archivo.outputStream().use { qr.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.qrprueba", archivo)
        val envio = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, getString(R.string.qr_mensaje, getString(R.string.cine_nombre)))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(envio, getString(R.string.qr_enviar)))
    }

    /** Abre el mapa del teatro: la app de mapas del telefono, o el navegador. */
    private fun abrirMapa() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, MAPA.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.sin_navegador, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    private companion object {
        /** Teatro en el mapa; la app de mapas resuelve el nombre. */
        const val MAPA = "geo:0,0?q=Jim+Santy+Auditorium,+Park+City,+UT"
    }
}
