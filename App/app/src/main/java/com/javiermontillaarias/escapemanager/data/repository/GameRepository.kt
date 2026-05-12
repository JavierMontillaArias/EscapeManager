package com.javiermontillaarias.escapemanager.data.repository

import com.javiermontillaarias.escapemanager.data.model.*
import com.javiermontillaarias.escapemanager.data.network.ApiService
import com.javiermontillaarias.escapemanager.util.Resource

class GameRepository(private val api: ApiService) {

    suspend fun validateQr(token: String): Resource<QrValidateResponse> =
        safeCall { api.validateQr(QrValidateRequest(token)) }

    suspend fun addHint(gameId: Int): Resource<HintsResponse> =
        safeCall { api.addHint(gameId) }

    suspend fun closeGame(gameId: Int, escaped: Boolean, observations: String): Resource<Game> =
        safeCall { api.closeGame(gameId, CloseGameRequest(escaped, observations)) }

    suspend fun getGames(): Resource<List<Game>> = safeCall { api.getGames() }

    private suspend fun <T> safeCall(call: suspend () -> retrofit2.Response<T>): Resource<T> {
        return try {
            val response = call()
            if (response.isSuccessful) Resource.Success(response.body()!!)
            else {
                val errorBody = response.errorBody()?.string() ?: response.message()
                Resource.Error("Error ${response.code()}: $errorBody")
            }
        } catch (e: Exception) {
            Resource.Error("Error de conexión: ${e.message}")
        }
    }
}