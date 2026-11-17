package com.example.cartelera.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.random.Random

/**
 * QR de PRUEBA para ver como se veria la entrada en pantalla. No codifica ningun ticket
 * real: no sirve para entrar al cine. Solo se muestra en versiones de desarrollo.
 */
object QrPrueba {

    fun generar(celdas: Int = 25, tamano: Int = 600, semilla: Int = 7): Bitmap {
        val bmp = Bitmap.createBitmap(tamano, tamano, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        val pincel = Paint().apply { color = Color.BLACK }
        val azar = Random(semilla)
        val matriz = Array(celdas) { BooleanArray(celdas) { azar.nextBoolean() } }
        // Tres marcas de posicion, como en un QR real.
        for ((fila, col) in listOf(0 to 0, 0 to celdas - 7, celdas - 7 to 0)) {
            for (i in 0 until 7) for (j in 0 until 7) {
                val borde = i == 0 || i == 6 || j == 0 || j == 6
                val centro = i in 2..4 && j in 2..4
                matriz[fila + i][col + j] = borde || centro
            }
        }
        val lado = tamano.toFloat() / celdas
        for (fila in 0 until celdas) for (col in 0 until celdas) {
            if (matriz[fila][col]) {
                canvas.drawRect(col * lado, fila * lado, (col + 1) * lado, (fila + 1) * lado, pincel)
            }
        }
        return bmp
    }
}
