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

    fun verPelicula(clave: String) = evento(VER_PELICULA, "pelicula" to clave.take(100))

    fun verTrailer(clave: String) = evento(VER_TRAILER, "pelicula" to clave.take(100))

    // TP4: monetizacion. H1 del plan: cuanta gente abre "Apoyar al cine".
    fun apoyarAbrir() = evento(APOYAR_ABRIR)

    fun donarIniciar(montoUsd: Int) = evento(DONAR_INICIAR, "monto_usd" to montoUsd.toString())

    fun donacionCompletada(montoUsd: Int) = evento(DONACION_COMPLETADA, "monto_usd" to montoUsd.toString())

    // TP6, estrategia 1: avisos por tema (topics de FCM). La apertura y la
    // recepcion de cada push las registra Analytics solo (notification_open y
    // notification_receive); aca se mide la configuracion del usuario.
    fun avisosPantalla() = evento(AVISOS_PANTALLA)

    fun temaCambiado(tema: String, activo: Boolean) =
        evento(if (activo) TEMA_ACTIVAR else TEMA_DESACTIVAR, "tema" to tema)

    fun permisoAvisos(concedido: Boolean) =
        evento(PERMISO_AVISOS, "resultado" to if (concedido) "concedido" else "rechazado")

    /**
     * Propiedad de usuario: permite segmentar campanas por audiencia en la
     * consola ("avisos_accesibilidad = si"). Los nombres tienen que ser cortos
     * (maximo 24 caracteres) y solo letras, numeros y guion bajo.
     */
    fun propiedadTema(tema: String, activo: Boolean) =
        analytics.setUserProperty("avisos_$tema", if (activo) "si" else "no")

    // TP6, estrategia 2: "Quiero verla".
    fun quieroVerMarcar(clave: String) = evento(QUIERO_VER_MARCAR, "pelicula" to clave.take(100))

    fun quieroVerQuitar(clave: String) = evento(QUIERO_VER_QUITAR, "pelicula" to clave.take(100))

    fun quieroVerAviso(clave: String) = evento(QUIERO_VER_AVISO, "pelicula" to clave.take(100))

    /** Usuario que tiene al menos una pelicula marcada: se cruza con click_comprar. */
    fun propiedadQuieroVer(activo: Boolean) =
        analytics.setUserProperty("usa_quiero_ver", if (activo) "si" else "no")

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
    const val VER_PELICULA = "ver_pelicula"
    const val VER_TRAILER = "ver_trailer"
    const val APOYAR_ABRIR = "apoyar_abrir"
    const val DONAR_INICIAR = "donar_iniciar"
    const val DONACION_COMPLETADA = "donacion_completada"
    const val AVISOS_PANTALLA = "avisos_pantalla"
    const val TEMA_ACTIVAR = "tema_activar"
    const val TEMA_DESACTIVAR = "tema_desactivar"
    const val PERMISO_AVISOS = "permiso_avisos"
    const val QUIERO_VER_MARCAR = "quiero_ver_marcar"
    const val QUIERO_VER_QUITAR = "quiero_ver_quitar"
    const val QUIERO_VER_AVISO = "quiero_ver_aviso"
}
