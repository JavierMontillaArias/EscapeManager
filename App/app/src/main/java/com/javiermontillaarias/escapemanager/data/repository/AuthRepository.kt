package com.javiermontillaarias.escapemanager.data.repository

import com.javiermontillaarias.escapemanager.data.local.SessionManager
import com.javiermontillaarias.escapemanager.data.model.LoginRequest
import com.javiermontillaarias.escapemanager.data.model.RefreshRequest
import com.javiermontillaarias.escapemanager.data.network.ApiService
import com.javiermontillaarias.escapemanager.util.Resource
import com.javiermontillaarias.escapemanager.util.safeApiCall

class AuthRepository(private val api: ApiService, private val sessionManager: SessionManager) {

    // B-3: usar safeApiCall para mapear correctamente 429 → "Demasiados intentos",
    //      422 → mensaje de validación, 500 → "Error en el servidor", etc.
    suspend fun login(email: String, password: String): Resource<Unit> {
        val result = safeApiCall { api.login(LoginRequest(email, password)) }
        return when (result) {
            is Resource.Success -> {
                val body = result.data
                // INC-01: user es nullable en LoginResponse; guardar de forma segura
                val user = body.user ?: return Resource.Error("Respuesta de login incompleta")
                sessionManager.accessToken  = body.accessToken
                sessionManager.refreshToken = body.refreshToken
                // I-05: usar rol_nombre directamente en lugar de IDs hardcodeados
                sessionManager.userRole = user.rolNombre
                sessionManager.userName = user.name
                sessionManager.userId   = user.id
                Resource.Success(Unit)
            }
            is Resource.Error -> result
            else              -> Resource.Error("Error inesperado")
        }
    }

    // SEC-03: acepta el access token capturado antes de clearSession() para que
    // el servidor pueda validar que el refresh token pertenece al usuario autenticado.
    suspend fun logoutApi(refreshToken: String, authHeader: String) {
        try {
            api.logout(authHeader, RefreshRequest(refreshToken))
        } catch (_: Exception) {
            // silenciar errores de red; la sesión local se limpia igualmente
        }
    }

    fun logout() = sessionManager.clearSession()
}
