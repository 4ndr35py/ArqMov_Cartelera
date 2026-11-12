package com.example.cartelera.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Una funcion de la cartelera. Corresponde a un documento de la coleccion
 * "funciones" en Firestore. Todos los campos tienen valor por defecto porque
 * Firestore necesita un constructor sin argumentos para deserializar.
 */
data class Funcion(
    @DocumentId val id: String = "",
    val titulo: String = "",
    val director: String = "",
    val anio: Int = 0,
    val duracionMin: Int = 0,
    val sinopsis: String = "",
    val sala: String = "",
    val fechaHora: Timestamp? = null,
    val precio: Double = 0.0,
    val urlEntradas: String = ""
)
