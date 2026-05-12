package com.javiermontillaarias.escapemanager.data.repository

import com.javiermontillaarias.escapemanager.data.model.Incident
import com.javiermontillaarias.escapemanager.data.model.IncidentRequest
import com.javiermontillaarias.escapemanager.data.network.ApiService
import com.javiermontillaarias.escapemanager.util.Resource

class IncidentRepository(private val api: ApiService) {

    suspend fun getIncidents(): Resource<List<Incident>> = safeCall { api.getIncidents() }

    suspend fun createIncident(request: IncidentRequest): Resource<Incident> =
        safeCall { api.createIncident(request) }

    suspend fun resolveIncident(id: Int): Resource<Incident> =
        safeCall { api.resolveIncident(id) }

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