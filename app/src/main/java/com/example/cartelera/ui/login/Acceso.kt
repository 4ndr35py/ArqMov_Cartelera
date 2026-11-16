package com.example.cartelera.ui.login

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.example.cartelera.R
import com.example.cartelera.data.AuthRepository

/**
 * Compuerta de cuenta. Sin cuenta solo se ve la cartelera y la ficha de cada pelicula;
 * todo lo demas (listas, avisos, comentarios, compra) pide una cuenta real. Una sesion
 * anonima no alcanza: si no hay cuenta, se avisa y se abre el ingreso.
 */
object Acceso {
    /** true si hay cuenta. Si no, avisa, abre el ingreso y devuelve false. */
    fun pedirCuenta(actividad: Activity): Boolean {
        if (AuthRepository().tieneCuenta) return true
        Toast.makeText(actividad, R.string.requiere_cuenta, Toast.LENGTH_SHORT).show()
        actividad.startActivity(Intent(actividad, LoginActivity::class.java))
        return false
    }

    fun requiereCuenta(actividad: Activity, accion: () -> Unit) {
        if (pedirCuenta(actividad)) accion()
    }
}
