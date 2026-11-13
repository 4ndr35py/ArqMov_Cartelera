package com.example.cartelera.data.donacion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DonacionesTest {

    @Test
    fun `hay tres montos fijos y estan ordenados de menor a mayor`() {
        val montos = Donaciones.opciones.map { it.montoUsd }
        assertEquals(listOf(2, 5, 10), montos)
    }

    @Test
    fun `los ids de producto coinciden con los que se cargan en Play Console`() {
        assertEquals(listOf("donacion_2usd", "donacion_5usd", "donacion_10usd"), Donaciones.idsProducto)
    }

    @Test
    fun `montoDe devuelve el monto del producto y null para ids desconocidos`() {
        assertEquals(5, Donaciones.montoDe("donacion_5usd"))
        assertNull(Donaciones.montoDe("premium_sin_anuncios"))
    }

    @Test
    fun `el monto sugerido existe en el catalogo`() {
        assertTrue(Donaciones.opciones.any { it.montoUsd == Donaciones.MONTO_SUGERIDO_USD })
    }

    @Test
    fun `cada id de producto es valido para Play (minusculas, digitos y guion bajo)`() {
        val patron = Regex("^[a-z0-9_]+$")
        Donaciones.idsProducto.forEach { id -> assertTrue(id, patron.matches(id)) }
    }
}
