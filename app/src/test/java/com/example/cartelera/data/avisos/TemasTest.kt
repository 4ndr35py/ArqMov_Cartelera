package com.example.cartelera.data.avisos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TemasTest {

    private val castellano = Locale("es", "AR")
    private val ingles = Locale.US

    @Test fun topicEnCastellanoTerminaEnEs() {
        assertEquals("cartelera_semanal_es", Temas.topic(TemaAviso.CARTELERA_SEMANAL, castellano))
    }

    @Test fun topicEnInglesTerminaEnEn() {
        assertEquals("cartelera_semanal_en", Temas.topic(TemaAviso.CARTELERA_SEMANAL, ingles))
    }

    @Test fun otroIdiomaNoEsElDelTelefono() {
        assertEquals("accesibilidad_en", Temas.topicOtroIdioma(TemaAviso.ACCESIBILIDAD, castellano))
        assertEquals("accesibilidad_es", Temas.topicOtroIdioma(TemaAviso.ACCESIBILIDAD, ingles))
    }

    @Test fun cualquierIdiomaQueNoSeaCastellanoUsaIngles() {
        assertEquals("eventos_en", Temas.topic(TemaAviso.EVENTOS, Locale.FRANCE))
    }

    @Test fun todosLosTopicsSonValidosParaFcm() {
        // FCM acepta letras, numeros, guion, guion bajo, punto, tilde y %.
        val valido = Regex("[a-zA-Z0-9-_.~%]{1,900}")
        for (tema in TemaAviso.entries) for (idioma in listOf(castellano, ingles)) {
            assertTrue(valido.matches(Temas.topic(tema, idioma)))
        }
    }

    @Test fun cadaTemaYIdiomaTieneSuTopicPropio() {
        val todos = TemaAviso.entries.flatMap { t -> listOf(castellano, ingles).map { Temas.topic(t, it) } }
        assertEquals(todos.size, todos.toSet().size)
        assertNotEquals(Temas.topic(TemaAviso.FESTIVAL, ingles), Temas.topic(TemaAviso.EVENTOS, ingles))
    }

    @Test fun canalDelTemaEsSuId() {
        assertEquals("accesibilidad", Temas.canal("accesibilidad"))
        assertEquals("festival", Temas.canal("festival"))
    }

    @Test fun tipoDesconocidoOSinTipoVaAlCanalGeneral() {
        assertEquals(Temas.CANAL_GENERAL, Temas.canal("otro"))
        assertEquals(Temas.CANAL_GENERAL, Temas.canal(null))
    }
}
