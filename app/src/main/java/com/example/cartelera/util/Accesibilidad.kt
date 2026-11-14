package com.example.cartelera.util

/**
 * Codigos de accesibilidad de una funcion (TP6, avisos de accesibilidad):
 *
 *  OC  subtitulos abiertos (open captions)
 *  HI  para personas sordas o con hipoacusia (subtitulos para sordos)
 *  VI  audiodescripcion para personas con discapacidad visual
 *
 * Los textos que ve el usuario estan en strings.xml (ingles y castellano).
 */
object Accesibilidad {

    val CODIGOS = setOf("OC", "HI", "VI")

    /** Codigos validos, en mayusculas y sin repetidos, en el orden de [CODIGOS]. */
    fun normalizar(codigos: List<String>): List<String> {
        val limpios = codigos.map { it.trim().uppercase() }.toSet()
        return CODIGOS.filter { it in limpios }
    }
}
