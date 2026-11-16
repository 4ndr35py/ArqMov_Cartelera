package com.example.cartelera.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class HorarioTest {

    private val denver = Horario.ZONA_CINE

    /** Fecha local de Denver, para que la prueba no dependa de la zona de la PC. */
    private fun enDenver(dia: Int, hora: Int, min: Int = 0): Date =
        Calendar.getInstance(denver).apply {
            clear()
            set(2026, Calendar.NOVEMBER, dia, hora, min)
        }.time

    @Test fun zonaDelCineEsDenver() {
        assertEquals("America/Denver", denver.id)
    }

    @Test fun ochoDeLaManianaEsElPrimerHorarioPermitido() {
        assertFalse(Horario.dentroDeHorarioDeAvisos(enDenver(14, 7, 59), denver))
        assertTrue(Horario.dentroDeHorarioDeAvisos(enDenver(14, 8, 0), denver))
    }

    @Test fun ultimaHoraPermitidaEsLas2159() {
        assertTrue(Horario.dentroDeHorarioDeAvisos(enDenver(14, 21, 59), denver))
        assertFalse(Horario.dentroDeHorarioDeAvisos(enDenver(14, 22, 0), denver))
    }

    @Test fun usaLaHoraDelCineNoLaDelTelefono() {
        val noche = enDenver(14, 23, 30)
        assertEquals(23, Horario.horaEnCine(noche, denver))
        assertFalse(Horario.dentroDeHorarioDeAvisos(noche, denver))
    }

    @Test fun laFechaDelCineSigueAlDiaDeDenver() {
        // 02:00 UTC del 16 es las 19:00 del 15 en Denver (MST, UTC-7).
        val utc = java.time.Instant.parse("2026-11-16T02:00:00Z").toEpochMilli()
        assertEquals("2026-11-15", Horario.fechaCine(Date(utc), denver))
    }

    @Test fun revisaUnaVezPorDiaDentroDeLaVentana() {
        val diez = enDenver(14, 10)
        assertTrue(Horario.debeRevisar(diez, null, denver))
        assertTrue(Horario.debeRevisar(diez, "2026-11-13", denver))
        assertFalse(Horario.debeRevisar(diez, Horario.fechaCine(diez, denver), denver))
    }

    @Test fun noRevisaDeNocheAunqueNoSeHayaRevisadoHoy() {
        assertFalse(Horario.debeRevisar(enDenver(14, 23, 30), null, denver))
        assertFalse(Horario.debeRevisar(enDenver(14, 7, 0), null, denver))
    }
}
