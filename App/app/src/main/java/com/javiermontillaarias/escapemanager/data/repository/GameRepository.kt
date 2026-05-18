package com.javiermontillaarias.escapemanager.data.repository

import com.javiermontillaarias.escapemanager.data.model.*
import com.javiermontillaarias.escapemanager.data.network.ApiService
import com.javiermontillaarias.escapemanager.util.Resource
import com.javiermontillaarias.escapemanager.util.safeApiCall

class GameRepository(private val api: ApiService) {

    suspend fun validateQr(token: String): Resource<QrValidateResponse> =
        safeApiCall { api.validateQr(QrValidateRequest(token)) }

    suspend fun addHint(gameId: Int): Resource<HintsResponse> =
        safeApiCall { api.addHint(gameId) }

    suspend fun closeGame(gameId: Int, escaped: Boolean, observations: String?): Resource<Game> =
        safeApiCall { api.closeGame(gameId, CloseGameRequest(escaped, observations)) }

    suspend fun getGames(skip: Int = 0, limit: Int = 100): Resource<List<Game>> =
        safeApiCall { api.getGames(skip, limit) }
}
