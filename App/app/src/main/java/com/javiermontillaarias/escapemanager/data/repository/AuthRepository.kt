package com.javiermontillaarias.escapemanager.data.repository

import com.javiermontillaarias.escapemanager.data.local.SessionManager
import com.javiermontillaarias.escapemanager.data.model.LoginRequest
import com.javiermontillaarias.escapemanager.data.model.RefreshRequest
import com.javiermontillaarias.escapemanager.data.network.ApiService
import com.javiermontillaarias.escapemanager.util.Resource

class AuthRepository(private val api: ApiService, private val sessionManager: SessionManager) {
    suspend fun login(email: String, password: String): Resource<Unit> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                sessionManager.accessToken = body.accessToken
                sessionManager.refreshToken = body.refreshToken
                // Fetch user info
                val meResponse = api.getMe()
                if (meResponse.isSuccessful) {
                    val user = meResponse.body()!!
                    sessionManager.userRole = user.role
                    sessionManager.userName = user.name
                    sessionManager.userId = user.id
                }
                Resource.Success(Unit)
            } else {
                Resource.Error("Credenciales incorrectas")
            }
        } catch (e: Exception) {
            Resource.Error("Error de conexión: ${e.message}")
        }
    }

    suspend fun refreshToken(): Boolean {
        return try {
            val refreshToken = sessionManager.refreshToken ?: return false
            val response = api.refreshToken(RefreshRequest(refreshToken))
            if (response.isSuccessful) {
                sessionManager.accessToken = response.body()!!.accessToken
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    fun logout() = sessionManager.clearSession()
}