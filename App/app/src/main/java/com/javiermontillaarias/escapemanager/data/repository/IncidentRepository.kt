package com.javiermontillaarias.escapemanager.data.repository

import com.javiermontillaarias.escapemanager.data.model.Incident
import com.javiermontillaarias.escapemanager.data.model.IncidentRequest
import com.javiermontillaarias.escapemanager.data.model.IncidentResolveResponse
import com.javiermontillaarias.escapemanager.data.network.ApiService
import com.javiermontillaarias.escapemanager.util.Resource
import com.javiermontillaarias.escapemanager.util.safeApiCall

class IncidentRepository(private val api: ApiService) {

    suspend fun getIncidents(): Resource<List<Incident>> = safeApiCall { api.getIncidents() }

    suspend fun createIncident(request: IncidentRequest): Resource<Incident> =
        safeApiCall { api.createIncident(request) }

    // A2: devuelve IncidentResolveResponse (refleja exactamente lo que devuelve la API)
    suspend fun resolveIncident(id: Int): Resource<IncidentResolveResponse> =
        safeApiCall { api.resolveIncident(id) }
}
