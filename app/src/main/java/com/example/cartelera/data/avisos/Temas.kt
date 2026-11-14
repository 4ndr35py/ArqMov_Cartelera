package com.example.cartelera.data.avisos

import com.example.cartelera.util.Formato
import java.util.Locale

/**
 * Temas de avisos (TP6, estrategia 1). Cada tema es un topic de Firebase Cloud
 * Messaging y existe en dos idiomas: el topic se arma como "<tema>_<idioma>",
 * por ejemplo `cartelera_semanal_en` o `cartelera_semanal_es`. Asi la consola
 * puede mandar la misma campaña en ingles o en castellano segun la audiencia.
 *
 * Logica pura (sin Android ni Firebase): se prueba con JUnit en la JVM.
 */
enum class TemaAviso(val id: String) {
    CARTELERA_SEMANAL("cartelera_semanal"),
    ACCESIBILIDAD("accesibilidad"),
    FESTIVAL("festival"),
    EVENTOS("eventos")
}

object Temas {

    /** Canal de los avisos sin tema (recordatorios de funciones y mensajes generales). */
    const val CANAL_GENERAL = "avisos"

    /** "es" si el telefono esta en castellano; "en" en cualquier otro caso. */
    fun idioma(idioma: Locale = Locale.getDefault()): String =
        if (Formato.esCastellano(idioma)) "es" else "en"

    /** Topic de FCM para [tema] en el idioma del telefono, p. ej. `accesibilidad_es`. */
    fun topic(tema: TemaAviso, idioma: Locale = Locale.getDefault()): String =
        "${tema.id}_${idioma(idioma)}"

    /**
     * Topic del otro idioma. Al cambiar el idioma del telefono se deja de
     * recibir el topic viejo, para no duplicar los avisos.
     */
    fun topicOtroIdioma(tema: TemaAviso, idioma: Locale = Locale.getDefault()): String =
        "${tema.id}_${if (idioma(idioma) == "es") "en" else "es"}"

    /**
     * Canal de Android para un aviso segun su campo "tipo" (el id del tema).
     * Cada tema tiene su propio canal, asi el usuario puede silenciar uno sin
     * apagar los demas desde los ajustes del telefono.
     */
    fun canal(tipo: String?): String =
        TemaAviso.entries.firstOrNull { it.id == tipo }?.id ?: CANAL_GENERAL
}
