package com.javiermontillaarias.escapemanager.data.network

import com.javiermontillaarias.escapemanager.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<RefreshResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<UserResponse>

    // ── Rooms ─────────────────────────────────────────────────────────────────

    @GET("rooms")
    suspend fun getRooms(): Response<List<Room>>

    @POST("rooms")
    suspend fun createRoom(@Body request: RoomRequest): Response<Room>

    @PUT("rooms/{id}")
    suspend fun updateRoom(@Path("id") id: Int, @Body request: RoomRequest): Response<Room>

    @DELETE("rooms/{id}")
    suspend fun deleteRoom(@Path("id") id: Int): Response<Unit>

    // ── Bookings ──────────────────────────────────────────────────────────────

    @GET("bookings")
    suspend fun getBookings(@Query("date") date: String? = null): Response<List<Booking>>

    @GET("bookings/{id}")
    suspend fun getBooking(@Path("id") id: Int): Response<Booking>

    @POST("bookings")
    suspend fun createBooking(@Body request: BookingRequest): Response<Booking>

    @PUT("bookings/{id}")
    suspend fun updateBooking(@Path("id") id: Int, @Body request: BookingRequest): Response<Booking>

    @DELETE("bookings/{id}")
    suspend fun deleteBooking(@Path("id") id: Int): Response<Unit>

    // ── QR ────────────────────────────────────────────────────────────────────

    @POST("qr/validate")
    suspend fun validateQr(@Body request: QrValidateRequest): Response<QrValidateResponse>

    // ── Games ─────────────────────────────────────────────────────────────────

    @GET("games")
    suspend fun getGames(): Response<List<Game>>

    @PATCH("games/{id}/hints")
    suspend fun addHint(@Path("id") id: Int): Response<HintsResponse>

    @POST("games/{id}/close")
    suspend fun closeGame(@Path("id") id: Int, @Body request: CloseGameRequest): Response<Game>

    // ── Incidents ─────────────────────────────────────────────────────────────

    @GET("incidents")
    suspend fun getIncidents(): Response<List<Incident>>

    @POST("incidents")
    suspend fun createIncident(@Body request: IncidentRequest): Response<Incident>

    @PATCH("incidents/{id}/resolve")
    suspend fun resolveIncident(@Path("id") id: Int): Response<Incident>

    // ── Stats ─────────────────────────────────────────────────────────────────

    @GET("stats/summary")
    suspend fun getStatsSummary(): Response<StatsSummary>

    @GET("stats/escape-rate")
    suspend fun getEscapeRate(): Response<List<EscapeRate>>

    @GET("stats/hints-avg")
    suspend fun getHintsAvg(): Response<List<HintsAvg>>

    @GET("stats/occupancy")
    suspend fun getOccupancy(): Response<List<OccupancyData>>

    @GET("stats/ranking")
    suspend fun getRanking(): Response<List<RankingEntry>>
}