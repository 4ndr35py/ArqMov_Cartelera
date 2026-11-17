package com.example.cartelera.data

import android.content.Context

/**
 * Una pelicula que la persona marco como vista: clave, titulo, dia ("yyyy-MM-dd") y
 * si le gusto: true = me gusta, false = no me gusta, null = sin opinion.
 */
data class Vista(val clave: String, val titulo: String, val fecha: String, val gusto: Boolean? = null)

/**
 * "Ya la vi": las peliculas vistas de cada cuenta, guardadas en el telefono.
 * Una entrada por pelicula; marcarla de nuevo cambia el dia, no la opinion.
 */
class VistasRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Las vistas de la cuenta, de la mas reciente a la mas vieja. */
    fun vistas(uid: String): List<Vista> =
        leer(uid).mapNotNull { aVista(it) }.sortedByDescending { it.fecha }

    fun vista(uid: String, clave: String): Vista? =
        vistas(uid).firstOrNull { it.clave == clave }

    fun estaVista(uid: String, clave: String): Boolean =
        vista(uid, clave) != null

    fun marcar(uid: String, vista: Vista) {
        val nuevas = leer(uid)
            .filterNot { entrada -> aVista(entrada)?.clave == vista.clave }
            .toMutableSet()
        nuevas += codificar(vista)
        guardar(uid, nuevas)
    }

    /** Cambia la opinion de una pelicula ya vista. No hace nada si no esta vista. */
    fun opinar(uid: String, clave: String, gusto: Boolean?) {
        val actual = vista(uid, clave) ?: return
        marcar(uid, actual.copy(gusto = gusto))
    }

    fun quitar(uid: String, clave: String) {
        guardar(uid, leer(uid).filterNot { aVista(it)?.clave == clave }.toSet())
    }

    private fun leer(uid: String): Set<String> =
        prefs.getStringSet(CLAVE + uid, emptySet()).orEmpty()

    /** SharedPreferences exige un Set nuevo: no se puede modificar el que se leyo. */
    private fun guardar(uid: String, entradas: Set<String>) {
        prefs.edit().putStringSet(CLAVE + uid, HashSet(entradas)).apply()
    }

    private fun codificar(v: Vista): String {
        val opinion = when (v.gusto) {
            true -> "1"
            false -> "0"
            null -> ""
        }
        return listOf(v.clave, v.fecha, v.titulo, opinion).joinToString(SEPARADOR)
    }

    private fun aVista(entrada: String): Vista? {
        val partes = entrada.split(SEPARADOR, limit = 4)
        if (partes.size < 3) return null
        val gusto = when (partes.getOrNull(3)) {
            "1" -> true
            "0" -> false
            else -> null
        }
        return Vista(clave = partes[0], fecha = partes[1], titulo = partes[2], gusto = gusto)
    }

    private companion object {
        const val PREFS = "vistas"
        const val CLAVE = "vistas_"
        /** Separador que no aparece en titulos ni claves. */
        const val SEPARADOR = "\u001F"
    }
}
