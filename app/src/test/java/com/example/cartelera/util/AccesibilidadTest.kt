package com.example.cartelera.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AccesibilidadTest {

    @Test fun normalizaMayusculasYDescartaCodigosDesconocidos() {
        assertEquals(listOf("OC", "VI"), Accesibilidad.normalizar(listOf(" oc ", "vi", "xx")))
    }

    @Test fun noRepiteYRespetaElOrden() {
        assertEquals(listOf("OC", "HI", "VI"), Accesibilidad.normalizar(listOf("VI", "hi", "OC", "OC")))
    }

    @Test fun listaVaciaQuedaVacia() {
        assertEquals(emptyList<String>(), Accesibilidad.normalizar(emptyList()))
    }
}
