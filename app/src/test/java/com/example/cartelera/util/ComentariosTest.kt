package com.example.cartelera.util

import com.example.cartelera.util.Comentarios.Validacion
import org.junit.Assert.assertEquals
import org.junit.Test

class ComentariosTest {

    @Test fun soloEspaciosNoSeAceptan() {
        assertEquals(Validacion.VACIO, Comentarios.validar("   \n  "))
    }

    @Test fun textoNormalSeAcepta() {
        assertEquals(Validacion.OK, Comentarios.validar("Qué buena película"))
    }

    @Test fun elLargoMaximoEs500() {
        assertEquals(Validacion.OK, Comentarios.validar("a".repeat(500)))
        assertEquals(Validacion.DEMASIADO_LARGO, Comentarios.validar("a".repeat(501)))
    }

    @Test fun losEspaciosDeLosExtremosNoCuentan() {
        assertEquals(Validacion.OK, Comentarios.validar("  " + "a".repeat(500) + "  "))
    }

    @Test fun normalizarRecortaLosExtremos() {
        assertEquals("hola", Comentarios.normalizar("  hola \n"))
    }
}
