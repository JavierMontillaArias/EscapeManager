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
    val name: String,
    val email: String,
    val role: String  // "Manager" | "Game Master"
)

// ── Room ──────────────────────────────────────────────────────────────────────

data class Room(
    val id: Int = 0,
    val name: String,
    val theme: String,
    val capacity: Int,
    val difficulty: String  // "baja" | "media" | "alta" | "extrema"
)

data class RoomRequest(
    val name: String,
    val theme: String,
    val capacity: Int,
    val difficulty: String
)

// ── Booking ───────────────────────────────────────────────────────────────────

data class Booking(
    val id: Int = 0,
    @SerializedName("room_id") val roomId: Int,
    @SerializedName("group_name") val groupName: String,
    @SerializedName("num_people") val numPeople: Int,
    val email: String,
    val date: String,   // "YYYY-MM-DD"
    val time: String,   // "HH:MM"
    val status: String, // "pendiente"|"confirmada"|"en_curso"|"completada"|"cancelada"
    @SerializedName("qr_token") val qrToken: String? = null,
    val room: Room? = null
)

data class BookingRequest(
    @SerializedName("room_id") val roomId: Int,
    @SerializedName("group_name") val groupName: String,
    @SerializedName("num_people") val numPeople: Int,
    val email: String,
    val date: String,
    val time: String
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
    @SerializedName("booking_id") val bookingId: Int,
    @SerializedName("gamemaster_id") val gamemasterId: Int,
    @SerializedName("hints_used") val hintsUsed: Int,
    val escaped: Boolean?,
    val observations: String?,
    @SerializedName("start_time") val startTime: String?,
    @SerializedName("end_time") val endTime: String?,
    val booking: Booking? = null
)

data class CloseGameRequest(
    val escaped: Boolean,
    val observations: String
)

data class HintsResponse(
    val message: String,
    @SerializedName("hints_used") val hintsUsed: Int
)

// ── Incident ──────────────────────────────────────────────────────────────────

data class Incident(
    val id: Int = 0,
    @SerializedName("room_id") val roomId: Int,
    val description: String,
    val resolved: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null,
    val room: Room? = null
)

data class IncidentRequest(
    @SerializedName("room_id") val roomId: Int,
    val description: String
)

// ── Stats ─────────────────────────────────────────────────────────────────────

data class StatsSummary(
    @SerializedName("total_bookings") val totalBookings: Int,
    @SerializedName("active_games") val activeGames: Int,
    @SerializedName("total_rooms") val totalRooms: Int,
    @SerializedName("pending_incidents") val pendingIncidents: Int,
    @SerializedName("monthly_revenue") val monthlyRevenue: Double?
)

data class EscapeRate(
    val room: String,
    val rate: Double
)

data class HintsAvg(
    val room: String,
    val average: Double
)

data class OccupancyData(
    val month: String,
    val occupancy: Double
)

data class RankingEntry(
    val rank: Int,
    val room: String,
    @SerializedName("total_games") val totalGames: Int
)