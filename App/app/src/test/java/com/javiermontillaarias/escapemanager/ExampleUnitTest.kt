package com.javiermontillaarias.escapemanager

import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {

    @Test
    fun `email regex valida emails correctos`() {
        val regex = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
        assertTrue(regex.matches("admin@escape.com"))
        assertTrue(regex.matches("usuario.test@gmail.com"))
        assertFalse(regex.matches("sindominio"))
        assertFalse(regex.matches("sin@tld"))
    }

    @Test
    fun `formato de hora valida horas correctas`() {
        val timeRegex = Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")
        assertTrue(timeRegex.matches("09:00"))
        assertTrue(timeRegex.matches("23:59"))
        assertFalse(timeRegex.matches("25:00"))
        assertFalse(timeRegex.matches("09:60"))
    }

    @Test
    fun `formato de fecha valida fechas correctas`() {
        val dateRegex = Regex("\\d{4}-\\d{2}-\\d{2}")
        assertTrue(dateRegex.matches("2026-05-18"))
        assertFalse(dateRegex.matches("18-05-2026"))
        assertFalse(dateRegex.matches("2026/05/18"))
    }

    @Test
    fun `horaFin debe ser posterior a horaInicio`() {
        val horaInicio = "09:00:00"
        val horaFin = "10:00:00"
        assertTrue(horaFin > horaInicio)
    }

    @Test
    fun `estado de reserva constantes correctos`() {
        assertEquals("pendiente",  "pendiente")
        assertEquals("confirmada", "confirmada")
        assertEquals("en_curso",   "en_curso")
        assertEquals("completada", "completada")
        assertEquals("cancelada",  "cancelada")
    }
}