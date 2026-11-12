package com.example.cartelera.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidadorTest {

    @Test fun emailVacio() =
        assertEquals(Validador.Error.EMAIL_VACIO, Validador.validar("  ", "secreta1"))

    @Test fun emailSinArroba() =
        assertEquals(Validador.Error.EMAIL_INVALIDO, Validador.validar("andres.gmail.com", "secreta1"))

    @Test fun emailSinDominio() =
        assertEquals(Validador.Error.EMAIL_INVALIDO, Validador.validar("andres@gmail", "secreta1"))

    @Test fun claveVacia() =
        assertEquals(Validador.Error.CLAVE_VACIA, Validador.validar("a@b.com", ""))

    @Test fun claveCorta() =
        assertEquals(Validador.Error.CLAVE_CORTA, Validador.validar("a@b.com", "12345"))

    @Test fun claveEnElLimite() =
        assertNull(Validador.validar("a@b.com", "123456"))

    @Test fun emailConEspaciosAlrededor() =
        assertNull(Validador.validar("  usuario@frsfco.utn.edu.ar ", "secreta1"))
}
