package com.javiermontillaarias.escapemanager.data.repository

import com.javiermontillaarias.escapemanager.data.model.*
import com.javiermontillaarias.escapemanager.data.network.ApiService
import com.javiermontillaarias.escapemanager.util.Resource

class StatsRepository(private val api: ApiService) {

    suspend fun getSummary(): Resource<StatsSummary> = safeCall { api.getStatsSummary() }
    suspend fun getEscapeRate(): Resource<List<EscapeRate>> = safeCall { api.getEscapeRate() }
    suspend fun getHintsAvg(): Resource<List<HintsAvg>> = safeCall { api.getHintsAvg() }
    suspend fun getOccupancy(): Resource<List<OccupancyData>> = safeCall { api.getOccupancy() }
    suspend fun getRanking(): Resource<List<RankingEntry>> = safeCall { api.getRanking() }

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