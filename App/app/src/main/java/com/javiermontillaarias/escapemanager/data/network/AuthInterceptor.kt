package com.javiermontillaarias.escapemanager.data.network

import com.javiermontillaarias.escapemanager.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionManager.accessToken
        val request = chain.request().newBuilder().apply {
            // SEC-03: no sobrescribir si la llamada ya incluye Authorization explícito
            // (p.ej. logout que pasa el token capturado antes de clearSession).
            if (token != null && chain.request().header("Authorization") == null) {
                header("Authorization", "Bearer $token")
            }
        }.build()
        return chain.proceed(request)
    }
}
