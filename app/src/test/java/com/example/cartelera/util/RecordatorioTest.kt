package com.example.cartelera.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class RecordatorioTest {

    private val zona = TimeZone.getTimeZone("America/Argentina/Cordoba")

    private fun fecha(dia: Int, hora: Int, min: Int): Date =
        Calendar.getInstance(zona).apply {
            clear()
            set(2026, Calendar.NOVEMBER, dia, hora, min)
        }.time

    @Test fun avisaDosHorasAntes() {
        val funcion = fecha(14, 21, 0)
        val ahora = fecha(13, 10, 0)
        assertEquals(fecha(14, 19, 0), Recordatorio.momento(funcion, ahora))
    }

    @Test fun siFaltanMenosDeDosHorasAvisaEnElMomento() {
        val funcion = fecha(14, 21, 0)
        val ahora = fecha(14, 20, 15)
        assertEquals(ahora, Recordatorio.momento(funcion, ahora))
    }

    @Test fun justoEnElLimiteAvisaEnElMomento() {
        val funcion = fecha(14, 21, 0)
        val ahora = fecha(14, 19, 0)
        assertEquals(ahora, Recordatorio.momento(funcion, ahora))
    }

    @Test fun siLaFuncionYaEmpezoNoAvisa() {
        val funcion = fecha(14, 21, 0)
        assertNull(Recordatorio.momento(funcion, fecha(14, 21, 0)))
        assertNull(Recordatorio.momento(funcion, fecha(14, 22, 30)))
    }

    @Test fun anticipacionConfigurable() {
        val funcion = fecha(14, 21, 0)
        assertEquals(fecha(14, 20, 30), Recordatorio.momento(funcion, fecha(13, 9, 0), anticipacionMin = 30))
    }

    @Test fun cruzaLaMedianoche() {
        val funcion = fecha(15, 0, 30)
        assertEquals(fecha(14, 22, 30), Recordatorio.momento(funcion, fecha(14, 12, 0)))
    }

    @Test fun demoraEnMilisegundos() {
        val ahora = fecha(14, 18, 0)
        assertEquals(60 * 60_000L, Recordatorio.demoraMs(fecha(14, 19, 0), ahora))
        assertEquals(0L, Recordatorio.demoraMs(ahora, ahora))
    }

    @Test fun textoDeLaNotificacion() {
        assertEquals(
            "Empieza el sábado 14 de noviembre, 21:00 h en Sala 1.",
            Recordatorio.texto(fecha(14, 21, 0), "Sala 1", zona)
        )
    }
}
