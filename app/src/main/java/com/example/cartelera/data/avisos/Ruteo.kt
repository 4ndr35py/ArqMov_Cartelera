package com.example.cartelera.data.avisos

/**
 * A donde lleva un aviso cuando el usuario lo toca. Lo decide el payload de
 * datos del mensaje (o del recordatorio local); la pantalla la arma Notificador.
 */
sealed class Destino {
    /** Sin destino especifico: la cartelera (pantalla de inicio). */
    data object Cartelera : Destino()
    data class Funcion(val id: String) : Destino()
    data class Pelicula(val clave: String) : Destino()
    data object Avisos : Destino()
}

/**
 * Ruteo del payload de datos de FCM a una pantalla. Claves del payload (las
 * que se cargan como "Custom data" en la consola de Firebase):
 *
 *  funcionId      abre el detalle de esa funcion (prioridad mas alta)
 *  peliculaClave  abre la ficha de la pelicula, p. ej. "tmdb:1603262"
 *  destino        "avisos" abre la pantalla de Avisos
 *  tipo           tema del aviso: cartelera_semanal, accesibilidad, festival, eventos
 *
 * Si no hay ninguna clave conocida, el aviso lleva a la cartelera.
 * Logica pura: los tests pasan mapas de texto, como los que llegan del sistema.
 */
object Ruteo {

    const val FUNCION_ID = "funcionId"
    const val PELICULA_CLAVE = "peliculaClave"
    const val DESTINO = "destino"
    const val TIPO = "tipo"
    const val DESTINO_AVISOS = "avisos"

    fun de(datos: Map<String, String?>): Destino {
        datos[FUNCION_ID]?.takeIf { it.isNotBlank() }?.let { return Destino.Funcion(it) }
        datos[PELICULA_CLAVE]?.takeIf { it.isNotBlank() }?.let { return Destino.Pelicula(it) }
        if (datos[DESTINO] == DESTINO_AVISOS) return Destino.Avisos
        return Destino.Cartelera
    }
}
