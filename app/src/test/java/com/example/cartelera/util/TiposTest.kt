package com.example.cartelera.util

import com.example.cartelera.data.TipoEntrada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TiposTest {

    private val venta = listOf(
        TipoEntrada("Park City Film Member", 6.0),
        TipoEntrada("Student/Senior", 7.0),
        TipoEntrada("General Admission", 8.0),
        TipoEntrada("Non-Members", 50.0)
    )

    @Test fun sinTiposNoHayNada() {
        assertNull(Tipos.elegir(emptyList(), 30, true))
    }

    @Test fun miembroRecibeElPrecioDeMiembro() {
        assertEquals("Park City Film Member", Tipos.elegir(venta, 40, true)?.nombre)
    }

    @Test fun sinMembresiaNoSeElijeMiembroNiNoMiembros() {
        assertEquals("General Admission", Tipos.elegir(venta, 40, false)?.nombre)
    }

    @Test fun seniorDesdeLaEdadSenior() {
        assertEquals("Student/Senior", Tipos.elegir(venta, Tipos.EDAD_SENIOR, false)?.nombre)
    }

    @Test fun menorDe18RecibeElPrecioEstudiante() {
        assertEquals("Student/Senior", Tipos.elegir(venta, 17, false)?.nombre)
    }

    @Test fun desde18SinMembresiaQuedaLaGeneral() {
        assertEquals("General Admission", Tipos.elegir(venta, 18, false)?.nombre)
    }

    @Test fun sinFechaDeNacimientoQuedaLaGeneral() {
        assertEquals("General Admission", Tipos.elegir(venta, null, false)?.nombre)
    }

    @Test fun sinTipoGeneralEligeElMasBarato() {
        val sinGeneral = listOf(TipoEntrada("VIP", 42.5), TipoEntrada("Otro", 9.0))
        assertEquals("Otro", Tipos.elegir(sinGeneral, null, false)?.nombre)
    }
}
