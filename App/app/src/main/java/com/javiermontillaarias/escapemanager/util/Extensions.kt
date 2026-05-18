package com.javiermontillaarias.escapemanager.util

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.javiermontillaarias.escapemanager.EscapeManagerApp
import com.javiermontillaarias.escapemanager.R
import com.javiermontillaarias.escapemanager.data.local.SessionManager
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val Fragment.appSessionManager: SessionManager
    get() = (requireActivity().application as EscapeManagerApp).sessionManager

fun Fragment.performLogout() {
    val app = requireActivity().application as EscapeManagerApp
    val sm = app.sessionManager
    val refreshToken = sm.refreshToken
    // SEC-03: capturar el access token ANTES de limpiar la sesión para poder
    // enviarlo como Authorization en la llamada de logout al servidor.
    val accessToken = sm.accessToken

    sm.clearSession(intentional = true)
    try {
        findNavController().navigate(R.id.loginFragment)
    } catch (_: Exception) {}

    // B-6: usar applicationScope en lugar de lifecycleScope para que la revocación
    // del token no se cancele cuando el Fragment origen se destruye tras la navegación.
    if (refreshToken != null) {
        val authHeader = if (accessToken != null) "Bearer $accessToken" else ""
        val api = RetrofitClient.getApiService(sm)
        val repo = AuthRepository(api, sm)
        app.applicationScope.launch(Dispatchers.IO) {
            repo.logoutApi(refreshToken, authHeader)
        }
    }
}
