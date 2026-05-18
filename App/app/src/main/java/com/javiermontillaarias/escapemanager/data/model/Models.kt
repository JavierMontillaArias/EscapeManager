package com.javiermontillaarias.escapemanager.data.model

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("access_token")  val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type")    val tokenType: String,
    val user: UserResponse?  // INC-01: nullable — solo presente en /auth/login, null en /auth/refresh
)

data class RefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class RefreshResponse(
    @SerializedName("access_token")  val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String
)

data class UserResponse(
    val id: Int,
    @SerializedName("nombre")     val name: String,
    val email: String,
    @SerializedName("rol_id")     val rolId: Int,
    @SerializedName("rol_nombre") val rolNombre: String,
    val activo: Boolean
) {
    val role: String     get() = rolNombre
    val isManager: Boolean get() = rolNombre == "Manager"
}

// ── Room ──────────────────────────────────────────────────────────────────────

data class Room(
    val id: Int = 0,
    @SerializedName("nombre")       val name: String,
    @SerializedName("tematica")     val theme: String,
    @SerializedName("capacidad_max") val capacity: Int,
    val dificultad: String,
    val activa: Boolean = true
)

// INC-06: activa nullable para que el update pueda togglear sin afectar el create
data class RoomRequest(
    @SerializedName("nombre")        val name: String,
    @SerializedName("tematica")      val theme: String,
    @SerializedName("capacidad_max") val capacity: Int,
    val dificultad: String,
    val activa: Boolean? = null
)

// ── Booking ───────────────────────────────────────────────────────────────────

// INC-04: horaFin no-nullable — la API siempre lo devuelve
data class Booking(
    val id: Int = 0,
    @SerializedName("sala_id")       val roomId: Int,
    @SerializedName("nombre_grupo")  val groupName: String,
    @SerializedName("num_personas")  val numPeople: Int,
    @SerializedName("email_cliente") val email: String,
    val fecha: String,
    @SerializedName("hora_inicio")   val horaInicio: String,
    @SerializedName("hora_fin")      val horaFin: String,
    val estado: String,
    @SerializedName("qr_token")      val qrToken: String? = null,
    @SerializedName("qr_usado")      val qrUsado: Boolean = false,
    @SerializedName("qr_email_enviado") val qrEmailEnviado: Boolean = false,
    val sala: Room? = null,
    @SerializedName("created_at")    val createdAt: String
)

data class BookingRequest(
    @SerializedName("sala_id")       val roomId: Int,
    @SerializedName("nombre_grupo")  val groupName: String,
    @SerializedName("num_personas")  val numPeople: Int,
    @SerializedName("email_cliente") val email: String,
    val fecha: String,
    @SerializedName("hora_inicio")   val horaInicio: String,
    @SerializedName("hora_fin")      val horaFin: String
)

data class BookingUpdateRequest(
    @SerializedName("sala_id")       val roomId: Int? = null,
    @SerializedName("nombre_grupo")  val groupName: String? = null,
    @SerializedName("num_personas")  val numPeople: Int? = null,
    @SerializedName("email_cliente") val email: String? = null,
    val fecha: String? = null,
    @SerializedName("hora_inicio")   val horaInicio: String? = null,
    @SerializedName("hora_fin")      val horaFin: String? = null,
    val estado: String? = null
)

// ── QR ────────────────────────────────────────────────────────────────────────

data class QrValidateRequest(
    @SerializedName("qr_token") val qrToken: String
)

data class QrValidateResponse(
    val message: String,
    @SerializedName("game_id")          val gameId: Int,
    @SerializedName("reserva_id")       val bookingId: Int,
    @SerializedName("nombre_grupo")     val nombreGrupo: String,
    val sala: String,
    @SerializedName("hora_inicio_real") val startTime: String
)

// ── Game ──────────────────────────────────────────────────────────────────────

data class Game(
    val id: Int,
    @SerializedName("reserva_id")    val bookingId: Int,
    @SerializedName("gamemaster_id") val gamemasterId: Int,
    @SerializedName("pistas_usadas") val hintsUsed: Int,
    val escaparon: Boolean?,
    val observaciones: String?,
    @SerializedName("hora_inicio_real") val startTime: String?,
    @SerializedName("hora_fin_real")    val endTime: String?,
    val gamemaster: GameMaster
)

data class GameMaster(
    val id: Int,
    @SerializedName("nombre") val name: String
)

// CAL-04: observaciones nullable — string vacío → null
data class CloseGameRequest(
    val escaparon: Boolean,
    val observaciones: String?
)

data class HintsResponse(
    val id: Int,
    val message: String,
    @SerializedName("pistas_usadas") val hintsUsed: Int
)

// ── Incident ──────────────────────────────────────────────────────────────────

// CAL-08: info de sala anidada para mostrar nombre en lugar de solo ID
// I-3: campo id añadido para permitir navegación a sala desde incidencia
data class IncidentRoom(
    val id: Int,
    @SerializedName("nombre") val nombre: String
)

data class Incident(
    val id: Int = 0,
    @SerializedName("sala_id")          val roomId: Int,
    @SerializedName("usuario_id")       val usuarioId: Int = 0,
    val descripcion: String,
    val resuelta: Boolean = false,
    @SerializedName("fecha")            val createdAt: String? = null,
    @SerializedName("fecha_resolucion") val fechaResolucion: String? = null,
    @SerializedName("sala")             val sala: IncidentRoom? = null
)

data class IncidentRequest(
    @SerializedName("sala_id") val roomId: Int,
    val descripcion: String
)

data class IncidentResolveResponse(
    val id: Int,
    val resuelta: Boolean,
    @SerializedName("fecha_resolucion") val fechaResolucion: String,
    val message: String
)

// ── Stats ─────────────────────────────────────────────────────────────────────

data class StatsSummary(
    @SerializedName("total_reservas")           val totalBookings: Int,
    @SerializedName("total_partidas")           val totalGames: Int,
    @SerializedName("porcentaje_escape_global") val escapeRate: Double,
    @SerializedName("sala_mas_popular")         val topRoom: String?,
    @SerializedName("reservas_hoy")             val bookingsToday: Int,
    @SerializedName("partidas_en_curso")        val activeGames: Int,
    @SerializedName("total_salas")              val totalRooms: Int,
    @SerializedName("incidencias_pendientes")   val pendingIncidents: Int
)

data class EscapeRate(
    @SerializedName("sala_id")        val roomId: Int = 0,
    @SerializedName("sala_nombre")    val room: String?,
    @SerializedName("total_partidas") val totalGames: Int = 0,
    @SerializedName("tasa_escape")    val rate: Double
)

data class HintsAvg(
    @SerializedName("sala_id")         val roomId: Int = 0,
    @SerializedName("sala_nombre")     val room: String?,
    @SerializedName("total_partidas")  val totalGames: Int = 0,
    @SerializedName("promedio_pistas") val average: Double
)

data class OccupancyData(
    @SerializedName("franja")         val franja: String,
    @SerializedName("total_reservas") val totalReservas: Int,
    @SerializedName("porcentaje")     val occupancy: Double
)

data class MessageResponse(val message: String)

data class RankingEntry(
    @SerializedName("posicion")        val rank: Int,
    @SerializedName("sala_id")         val roomId: Int,
    @SerializedName("sala_nombre")     val room: String,
    @SerializedName("total_partidas")  val totalGames: Int,
    @SerializedName("tasa_escape")     val tasaEscape: Double,
    @SerializedName("promedio_pistas") val promedioPistas: Double,
    val score: Double
)
