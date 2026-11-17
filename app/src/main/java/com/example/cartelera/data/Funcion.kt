package com.example.cartelera.data

import java.util.Date

/**
 * Una funcion de la cartelera, tal como la ven los ViewModels y las vistas.
 * Es un modelo de dominio: no depende de Firebase (el mapeo desde el
 * documento de Firestore esta en FuncionRepository).
 *
 * [tmdbId] es el id de la pelicula en TMDB. Lo carga el cine junto con la
 * funcion, para pedir afiche, sinopsis, duracion y generos sin buscar por titulo.
 */
data class Funcion(
    val id: String = "",
    val titulo: String = "",
    val director: String = "",
    val anio: Int = 0,
    val duracionMin: Int = 0,
    val sinopsis: String = "",
    val sala: String = "",
    val fechaHora: Date? = null,
    val precio: Double = 0.0,
    val urlEntradas: String = "",
    val tmdbId: Int? = null,
    /** Codigos de accesibilidad: OC, HI, VI (ver util/Accesibilidad). Vacio si no tiene. */
    val accesibilidad: List<String> = emptyList(),
    /** Tipos de entrada a la venta con su precio (General, Student/Senior, Miembro...). Vacio si no hay. */
    val tipos: List<TipoEntrada> = emptyList(),
    /** Afiche de Eventive (URL), para cuando TMDB no tiene la pelicula. Null si no hay. */
    val afiche: String? = null
)

/** Un tipo de entrada de una funcion: nombre tal como lo muestra la venta y precio en dolares. */
data class TipoEntrada(val nombre: String, val precio: Double)
