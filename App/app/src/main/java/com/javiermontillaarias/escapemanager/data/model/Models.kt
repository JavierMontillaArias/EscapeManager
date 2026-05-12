package com.javiermontillaarias.escapemanager.data.model

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String
)

data class RefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class RefreshResponse(
    @SerializedName("access_token") val accessToken: String
)

data class UserResponse(
    val id: Int,
    @SerializedName("nombre") val name: String,
    val email: String,
    @SerializedName("rol_id") val rolId: Int,
    val activo: Boolean
) {
    val role: String get() = if (rolId == 3) "Manager" else "Game Master"
    val isManager: Boolean get() = rolId == 3
}

// ── Room ──────────────────────────────────────────────────────────────────────

data class Room(
    val id: Int = 0,
    @SerializedName("nombre") val name: String,
    @SerializedName("tematica") val theme: String,
    @SerializedName("capacidad_max") val capacity: Int,
    val dificultad: String,
    val activa: Boolean = true
)

data class RoomRequest(
    @SerializedName("nombre") val name: String,
    @SerializedName("tematica") val theme: String,
    @SerializedName("capacidad_max") val capacity: Int,
    val dificultad: String
)

// ── Booking ───────────────────────────────────────────────────────────────────

data class Booking(
    val id: Int = 0,
    @SerializedName("sala_id") val roomId: Int,
    @SerializedName("nombre_grupo") val groupName: String,
    @SerializedName("num_personas") val numPeople: Int,
    val email: String,
    val fecha: String,
    val hora: String,
    val estado: String,
    @SerializedName("qr_token") val qrToken: String? = null,
    val sala: Room? = null
)

data class BookingRequest(
    @SerializedName("sala_id") val roomId: Int,
    @SerializedName("nombre_grupo") val groupName: String,
    @SerializedName("num_personas") val numPeople: Int,
    val email: String,
    val fecha: String,
    val hora: String
)

// ── QR ────────────────────────────────────────────────────────────────────────

data class QrValidateRequest(
    @SerializedName("qr_token") val qrToken: String
)

data class QrValidateResponse(
    val message: String,
    @SerializedName("game_id") val gameId: Int,
    val booking: Booking
)

// ── Game ──────────────────────────────────────────────────────────────────────

data class Game(
    val id: Int,
    @SerializedName("reserva_id") val bookingId: Int,
    @SerializedName("gamemaster_id") val gamemasterId: Int,
    @SerializedName("pistas_usadas") val hintsUsed: Int,
    val escaparon: Boolean?,
    val observaciones: String?,
    @SerializedName("hora_inicio_real") val startTime: String?,
    @SerializedName("hora_fin_real") val endTime: String?,
    val gamemaster: GameMaster? = null
)

data class GameMaster(
    val id: Int,
    @SerializedName("nombre") val name: String
)

data class CloseGameRequest(
    val escaparon: Boolean,
    val observaciones: String
)

data class HintsResponse(
    val message: String,
    @SerializedName("pistas_usadas") val hintsUsed: Int
)

// ── Incident ──────────────────────────────────────────────────────────────────

data class Incident(
    val id: Int = 0,
    @SerializedName("sala_id") val roomId: Int,
    val descripcion: String,
    val resuelta: Boolean = false,
    @SerializedName("fecha_reporte") val createdAt: String? = null,
    val sala: Room? = null
)

data class IncidentRequest(
    @SerializedName("sala_id") val roomId: Int,
    val descripcion: String
)

// ── Stats ─────────────────────────────────────────────────────────────────────

data class StatsSummary(
    @SerializedName("total_reservas") val totalBookings: Int,
    @SerializedName("total_partidas") val activeGames: Int,
    @SerializedName("total_salas") val totalRooms: Int,
    @SerializedName("incidencias_pendientes") val pendingIncidents: Int,
    @SerializedName("ingresos_mes") val monthlyRevenue: Double?
)

data class EscapeRate(
    @SerializedName("sala_nombre") val room: String,
    @SerializedName("tasa_escape") val rate: Double
)

data class HintsAvg(
    @SerializedName("sala_nombre") val room: String,
    @SerializedName("promedio_pistas") val average: Double
)

data class OccupancyData(
    val month: String,
    val occupancy: Double
)

data class RankingEntry(
    @SerializedName("posicion") val rank: Int,
    @SerializedName("sala_nombre") val room: String,
    @SerializedName("total_partidas") val totalGames: Int
)