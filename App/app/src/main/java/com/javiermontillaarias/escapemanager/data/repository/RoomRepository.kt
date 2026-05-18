package com.javiermontillaarias.escapemanager.data.repository

import com.javiermontillaarias.escapemanager.data.model.Room
import com.javiermontillaarias.escapemanager.data.model.RoomRequest
import com.javiermontillaarias.escapemanager.data.network.ApiService
import com.javiermontillaarias.escapemanager.util.Resource
import com.javiermontillaarias.escapemanager.util.safeApiCall

class RoomRepository(private val api: ApiService) {

    suspend fun getRooms(soloActivas: Boolean? = null): Resource<List<Room>> =
        safeApiCall { api.getRooms(soloActivas) }

    suspend fun getActiveRooms(): Resource<List<Room>> =
        safeApiCall { api.getActiveRooms() }

    suspend fun createRoom(request: RoomRequest): Resource<Room> =
        safeApiCall { api.createRoom(request) }

    suspend fun updateRoom(id: Int, request: RoomRequest): Resource<Room> =
        safeApiCall { api.updateRoom(id, request) }

    // I-06: Resource<Room> para que la UI pueda actualizar el item borrado
    suspend fun deleteRoom(id: Int): Resource<Room> =
        safeApiCall { api.deleteRoom(id) }
}
