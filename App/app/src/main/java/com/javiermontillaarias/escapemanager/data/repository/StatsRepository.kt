package com.javiermontillaarias.escapemanager.data.repository

import com.javiermontillaarias.escapemanager.data.model.*
import com.javiermontillaarias.escapemanager.data.network.ApiService
import com.javiermontillaarias.escapemanager.util.Resource
import com.javiermontillaarias.escapemanager.util.safeApiCall

class StatsRepository(private val api: ApiService) {

    suspend fun getSummary(): Resource<StatsSummary> = safeApiCall { api.getStatsSummary() }
    suspend fun getOccupancy(): Resource<List<OccupancyData>> = safeApiCall { api.getOccupancy() }
    suspend fun getRanking(): Resource<List<RankingEntry>> = safeApiCall { api.getRanking() }
}
