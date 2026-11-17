package com.example.cartelera.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FechaDiaTest {

    @Test fun diasHastaCuentaDiasCalendario() {
        assertEquals(30L, FechaDia.diasHasta("2026-11-16", "2026-12-16"))
    }

    @Test fun elMismoDiaDaCero() {
        assertEquals(0L, FechaDia.diasHasta("2026-11-16", "2026-11-16"))
    }

    @Test fun unaFechaPasadaDaNegativo() {
        assertEquals(-6L, FechaDia.diasHasta("2026-11-16", "2026-11-10"))
    }

    @Test fun cruzaElFinDeAnio() {
        assertEquals(1L, FechaDia.diasHasta("2026-12-31", "2027-01-01"))
    }

    @Test fun textoInvalidoDaNull() {
        assertNull(FechaDia.diasHasta("hoy", "2026-12-16"))
        assertNull(FechaDia.diasHasta("2026-11-16", "16/12/2026"))
    }

    @Test fun elSelectorDeFechaVuelveElMismoDia() {
        val millis = FechaDia.millisUtc("2026-12-31")
        assertEquals("2026-12-31", millis?.let { FechaDia.desdeMillisUtc(it) })
    }

    @Test fun aniosCumplidos() {
        assertEquals(66, FechaDia.anios("1960-06-15", "2026-11-16"))
    }

    @Test fun aunNoCumpleEnEseAno() {
        assertEquals(63, FechaDia.anios("1962-11-17", "2026-11-16"))
    }

    @Test fun cumpleHoyCuenta() {
        assertEquals(65, FechaDia.anios("1961-11-16", "2026-11-16"))
    }

    @Test fun fechaInvalidaOFutura() {
        assertNull(FechaDia.anios("no", "2026-11-16"))
        assertNull(FechaDia.anios("2030-01-01", "2026-11-16"))
    }
}
