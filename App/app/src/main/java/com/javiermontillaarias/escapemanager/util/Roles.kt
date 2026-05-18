package com.javiermontillaarias.escapemanager.util

object Roles {
    const val MANAGER    = "Manager"
    const val GAME_MASTER = "Game Master"
}

// CAL-05: constantes de estado de reserva para evitar strings literales dispersos.
// Si la API cambia un valor, basta con actualizar aquí.
object EstadoReserva {
    const val PENDIENTE  = "pendiente"
    const val CONFIRMADA = "confirmada"
    const val EN_CURSO   = "en_curso"
    const val COMPLETADA = "completada"
    const val CANCELADA  = "cancelada"
}
