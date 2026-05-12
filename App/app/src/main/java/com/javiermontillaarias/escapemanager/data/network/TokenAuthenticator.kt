package com.javiermontillaarias.escapemanager.data.network

import com.javiermontillaarias.escapemanager.data.local.SessionManager
import com.javiermontillaarias.escapemanager.data.model.RefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val sessionManager: SessionManager,
    private val api: ApiService
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Evitar bucle infinito
        if (response.request.header("Authorization") == null) return null

        val newToken = runBlocking {
            try {
                val refreshToken = sessionManager.refreshToken ?: return@runBlocking null
                val res = api.refreshToken(RefreshRequest(refreshToken))
                if (res.isSuccessful) {
                    val body = res.body()!!
                    sessionManager.accessToken = body.accessToken
                    body.accessToken
                } else {
                    sessionManager.clearSession()
                    null
                }
            } catch (e: Exception) {
                null
            }
        } ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }
}