package com.javiermontillaarias.escapemanager.data.repository

import com.javiermontillaarias.escapemanager.data.model.Booking
import com.javiermontillaarias.escapemanager.data.model.BookingRequest
import com.javiermontillaarias.escapemanager.data.model.BookingUpdateRequest
import com.javiermontillaarias.escapemanager.data.model.MessageResponse
import com.javiermontillaarias.escapemanager.data.network.ApiService
import com.javiermontillaarias.escapemanager.util.Resource
import com.javiermontillaarias.escapemanager.util.safeApiCall

class BookingRepository(private val api: ApiService) {

    suspend fun getBookings(
        fecha: String?  = null,
        salaId: Int?    = null,
        estado: String? = null,
        skip: Int       = 0,
        limit: Int      = 100
    ): Resource<List<Booking>> = safeApiCall { api.getBookings(fecha, salaId, estado, skip, limit) }

    suspend fun getBooking(id: Int): Resource<Booking> =
        safeApiCall { api.getBooking(id) }

    suspend fun createBooking(request: BookingRequest): Resource<Booking> =
        safeApiCall { api.createBooking(request) }

    // C2: usa BookingUpdateRequest con campos opcionales
    suspend fun updateBooking(id: Int, request: BookingUpdateRequest): Resource<Booking> =
        safeApiCall { api.updateBooking(id, request) }

    suspend fun deleteBooking(id: Int): Resource<Unit> =
        safeApiCall { api.deleteBooking(id) }

    suspend fun resendQr(id: Int): Resource<MessageResponse> =
        safeApiCall { api.resendQr(id) }
}
