package com.javiermontillaarias.escapemanager.data.repository

import com.javiermontillaarias.escapemanager.data.model.Booking
import com.javiermontillaarias.escapemanager.data.model.BookingRequest
import com.javiermontillaarias.escapemanager.data.network.ApiService
import com.javiermontillaarias.escapemanager.util.Resource

class BookingRepository(private val api: ApiService) {

    suspend fun getBookings(date: String? = null): Resource<List<Booking>> =
        safeCall { api.getBookings(date) }

    suspend fun createBooking(request: BookingRequest): Resource<Booking> =
        safeCall { api.createBooking(request) }

    suspend fun updateBooking(id: Int, request: BookingRequest): Resource<Booking> =
        safeCall { api.updateBooking(id, request) }

    suspend fun deleteBooking(id: Int): Resource<Unit> =
        safeCall { api.deleteBooking(id) }

    private suspend fun <T> safeCall(call: suspend () -> retrofit2.Response<T>): Resource<T> {
        return try {
            val response = call()
            if (response.isSuccessful) Resource.Success(response.body()!!)
            else Resource.Error("Error ${response.code()}: ${response.message()}")
        } catch (e: Exception) {
            Resource.Error("Error de conexión: ${e.message}")
        }
    }
}