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

    // SEC-03: logout requiere access token en Authorization para que el servidor
    // valide que el refresh token pertenece al usuario autenticado.
    // Se pasa explícitamente para que funcione incluso tras clearSession().
    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") authorization: String,
        @Body request: RefreshRequest
    ): Response<Unit>

    // ── Rooms ─────────────────────────────────────────────────────────────────

    @GET("rooms")
    suspend fun getRooms(@Query("activa") activa: Boolean? = null): Response<List<Room>>

    @GET("rooms/active")
    suspend fun getActiveRooms(): Response<List<Room>>

    @POST("rooms")
    suspend fun createRoom(@Body request: RoomRequest): Response<Room>

    @PUT("rooms/{id}")
    suspend fun updateRoom(@Path("id") id: Int, @Body request: RoomRequest): Response<Room>

    @DELETE("rooms/{id}")
    suspend fun deleteRoom(@Path("id") id: Int): Response<Room>

    // ── Bookings ──────────────────────────────────────────────────────────────

    @GET("bookings")
    suspend fun getBookings(
        @Query("date")    fecha: String?  = null,
        @Query("sala_id") salaId: Int?    = null,
        @Query("estado")  estado: String? = null,
        @Query("skip")    skip: Int       = 0,
        @Query("limit")   limit: Int      = 100
    ): Response<List<Booking>>

    @GET("bookings/{id}")
    suspend fun getBooking(@Path("id") id: Int): Response<Booking>

    @POST("bookings")
    suspend fun createBooking(@Body request: BookingRequest): Response<Booking>

    @PUT("bookings/{id}")
    suspend fun updateBooking(@Path("id") id: Int, @Body request: BookingUpdateRequest): Response<Booking>

    @DELETE("bookings/{id}")
    suspend fun deleteBooking(@Path("id") id: Int): Response<Unit>

    @POST("bookings/{id}/resend-qr")
    suspend fun resendQr(@Path("id") id: Int): Response<MessageResponse>

    // ── QR ────────────────────────────────────────────────────────────────────

    @POST("qr/validate")
    suspend fun validateQr(@Body request: QrValidateRequest): Response<QrValidateResponse>

    // ── Games ─────────────────────────────────────────────────────────────────

    @GET("games")
    suspend fun getGames(
        @Query("skip")  skip: Int  = 0,
        @Query("limit") limit: Int = 100
    ): Response<List<Game>>

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
    suspend fun resolveIncident(@Path("id") id: Int): Response<IncidentResolveResponse>

    // ── Stats ─────────────────────────────────────────────────────────────────

    @GET("stats/summary")
    suspend fun getStatsSummary(): Response<StatsSummary>

    @GET("stats/occupancy")
    suspend fun getOccupancy(): Response<List<OccupancyData>>

    @GET("stats/ranking")
    suspend fun getRanking(): Response<List<RankingEntry>>
}
