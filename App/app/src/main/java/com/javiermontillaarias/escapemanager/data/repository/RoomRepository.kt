package com.javiermontillaarias.escapemanager.data.repository

import com.javiermontillaarias.escapemanager.data.model.Room
import com.javiermontillaarias.escapemanager.data.model.RoomRequest
import com.javiermontillaarias.escapemanager.data.network.ApiService
import com.javiermontillaarias.escapemanager.util.Resource

class RoomRepository(private val api: ApiService) {

    suspend fun getRooms(): Resource<List<Room>> = safeCall { api.getRooms() }

    suspend fun createRoom(request: RoomRequest): Resource<Room> = safeCall { api.createRoom(request) }

    suspend fun updateRoom(id: Int, request: RoomRequest): Resource<Room> =
        safeCall { api.updateRoom(id, request) }

    suspend fun deleteRoom(id: Int): Resource<Unit> = safeCall { api.deleteRoom(id) }

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