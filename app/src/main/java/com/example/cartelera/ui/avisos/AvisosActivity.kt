package com.example.cartelera.ui.avisos

import android.Manifest
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cartelera.R
import com.example.cartelera.analytics.Metricas
import com.example.cartelera.data.avisos.Notificador
import com.example.cartelera.data.avisos.TemaAviso
import com.example.cartelera.databinding.ActivityAvisosBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar

/**
 * Pantalla "Avisos" (TP6): un interruptor por tema. Activar un tema pide el
 * permiso de notificaciones (Android 13+) con una explicacion previa; nunca se
 * pide al abrir la app.
 */
class AvisosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAvisosBinding
    private val viewModel: AvisosViewModel by viewModels()

    /** Tema que el usuario quiso activar mientras se pedia el permiso. */
    private var temaPendiente: TemaAviso? = null

    private val pedirPermiso = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        Metricas.permisoAvisos(concedido)
        val tema = temaPendiente
        temaPendiente = null
        if (tema == null) return@registerForActivityResult
        if (concedido) {
            viewModel.cambiar(tema, activar = true)
        } else {
            render(viewModel.temas.value.orEmpty())
            avisar(R.string.avisos_sin_permiso)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAvisosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        binding.toolbar.setNavigationOnClickListener { finish() }

        TemaAviso.entries.forEach { tema ->
            switchPara(tema).setOnClickListener { alPulsar(tema, it as MaterialSwitch) }
        }

        viewModel.temas.observe(this) { render(it) }
        viewModel.error.observe(this) { error ->
            if (error) {
                avisar(R.string.avisos_error)
                // Vuelve a mostrar lo que hay guardado, no lo que se pulso.
                render(viewModel.temas.value.orEmpty())
                viewModel.errorMostrado()
            }
        }
    }

    /** Activar pide permiso si hace falta; desactivar no necesita nada. */
    private fun alPulsar(tema: TemaAviso, interruptor: MaterialSwitch) {
        if (!interruptor.isChecked) {
            viewModel.cambiar(tema, activar = false)
            return
        }
        if (Notificador.puedeNotificar(this)) {
            viewModel.cambiar(tema, activar = true)
        } else {
            // Se revierte hasta que el usuario acepte la explicacion y el permiso.
            interruptor.isChecked = false
            explicarPermiso(tema)
        }
    }

    private fun explicarPermiso(tema: TemaAviso) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.avisos_permiso_titulo)
            .setMessage(R.string.avisos_permiso_texto)
            .setPositiveButton(R.string.btn_continuar) { _, _ ->
                // Solo se llega aca en Android 13+ (antes Notificador.puedeNotificar es true).
                temaPendiente = tema
                pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton(R.string.btn_ahora_no, null)
            .show()
    }

    private fun render(temas: Map<TemaAviso, Boolean>) {
        TemaAviso.entries.forEach { tema -> switchPara(tema).isChecked = temas[tema] == true }
    }

    private fun switchPara(tema: TemaAviso): MaterialSwitch = when (tema) {
        TemaAviso.CARTELERA_SEMANAL -> binding.swCarteleraSemanal
        TemaAviso.ACCESIBILIDAD -> binding.swAccesibilidad
        TemaAviso.FESTIVAL -> binding.swFestival
        TemaAviso.EVENTOS -> binding.swEventos
    }

    private fun avisar(texto: Int) =
        Snackbar.make(binding.root, texto, Snackbar.LENGTH_LONG).show()
}
