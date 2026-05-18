package com.javiermontillaarias.escapemanager.data.network

import com.javiermontillaarias.escapemanager.data.local.SessionManager
import com.javiermontillaarias.escapemanager.data.model.RefreshRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val sessionManager: SessionManager,
    private val api: ApiService
) : Authenticator {

    // Variables de instancia en lugar de companion object para evitar estado global
    // compartido entre instancias en tests o escenarios de múltiples usuarios (BUG-04)
    private var lastRefreshTime = 0L
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null

        synchronized(refreshLock) {
            val now = System.currentTimeMillis()
            if (now - lastRefreshTime < 5_000L) {
                val currentToken = sessionManager.accessToken ?: return null
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val newToken = runBlocking(Dispatchers.IO) {
                try {
                    val refreshToken = sessionManager.refreshToken ?: return@runBlocking null
                    val res = api.refreshToken(RefreshRequest(refreshToken))
                    if (res.isSuccessful) {
                        val body = res.body() ?: return@runBlocking null
                        // CAL-02: escritura atómica — evita tokens inconsistentes si el proceso
                        // se interrumpe entre las dos escrituras individuales anteriores
                        sessionManager.updateTokens(body.accessToken, body.refreshToken)
                        body.accessToken
                    } else {
                        sessionManager.clearSession()
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            lastRefreshTime = System.currentTimeMillis()
            if (newToken == null) return null
            return response.request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
        }
    }
}
