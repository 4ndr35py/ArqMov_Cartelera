package com.example.cartelera.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Instrumentacion de la fase Measure. Cada evento corresponde a una metrica
 * definida en el plan de medicion del informe (seccion 3). Centralizar los
 * nombres aca evita que un error de tipeo parta una metrica en dos.
 */
object Metricas {

    private lateinit var analytics: FirebaseAnalytics

    fun inicializar(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context)
    }

    fun identificarUsuario(uid: String?) = analytics.setUserId(uid)

    // M1: tasa de registro completo
    fun registro() = analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, Bundle().apply {
        putString(FirebaseAnalytics.Param.METHOD, "email")
    })

    fun ingreso() = analytics.logEvent(FirebaseAnalytics.Event.LOGIN, Bundle().apply {
        putString(FirebaseAnalytics.Param.METHOD, "email")
    })

    fun errorAuth(tipo: String) = evento(ERROR_AUTH, "tipo" to tipo)

    // M2: la cartelera se carga y se recorre
    fun verCartelera(cantidad: Int) =
        analytics.logEvent(VER_CARTELERA, Bundle().apply { putLong("cantidad", cantidad.toLong()) })

    fun verFuncion(id: String, titulo: String) =
        evento(VER_FUNCION, "funcion_id" to id, "titulo" to titulo.take(100))

    // M3: interes declarado
    fun marcarInteres(id: String, interesa: Boolean) =
        evento(MARCAR_INTERES, "funcion_id" to id, "valor" to if (interesa) "si" else "no")

    // M4: intencion de compra (smoke test sobre la venta existente)
    fun clickComprar(id: String) = evento(CLICK_COMPRAR, "funcion_id" to id)

    fun cerrarSesion() = evento(CERRAR_SESION)

    // TP7: lo que el TP2 dejo para medir en el proximo ciclo
    fun compartirFuncion(id: String) = evento(COMPARTIR_FUNCION, "funcion_id" to id)

    fun abrirAviso(id: String) = evento(ABRIR_AVISO, "funcion_id" to id)

    private fun evento(nombre: String, vararg params: Pair<String, String>) {
        val bundle = Bundle()
        params.forEach { (k, v) -> bundle.putString(k, v) }
        analytics.logEvent(nombre, bundle)
    }

    const val VER_CARTELERA = "ver_cartelera"
    const val VER_FUNCION = "ver_funcion"
    const val MARCAR_INTERES = "marcar_interes"
    const val CLICK_COMPRAR = "click_comprar"
    const val ERROR_AUTH = "error_auth"
    const val CERRAR_SESION = "cerrar_sesion"
    const val COMPARTIR_FUNCION = "compartir_funcion"
    const val ABRIR_AVISO = "abrir_aviso"
}
