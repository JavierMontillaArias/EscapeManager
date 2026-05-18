package com.javiermontillaarias.escapemanager.data.local

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.content.Context
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Channel.CONFLATED: el evento se consume una sola vez y no se repite para
    // nuevos suscriptores, evitando el bucle infinito de reinicios de MainActivity.
    private val _sessionExpiredChannel = Channel<Unit>(Channel.CONFLATED)
    val sessionExpiredFlow = _sessionExpiredChannel.receiveAsFlow()

    companion object {
        private const val PREFS_NAME = "escape_manager_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ID = "user_id"
    }

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_ACCESS_TOKEN, value) }

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_REFRESH_TOKEN, value) }

    var userRole: String?
        get() = prefs.getString(KEY_USER_ROLE, null)
        set(value) = prefs.edit { putString(KEY_USER_ROLE, value) }

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit { putString(KEY_USER_NAME, value) }

    var userId: Int
        get() = prefs.getInt(KEY_USER_ID, -1)
        set(value) = prefs.edit { putInt(KEY_USER_ID, value) }

    val isLoggedIn: Boolean get() = accessToken != null

    val isManager: Boolean get() = userRole == "Manager"

    // CAL-02: escritura atómica de ambos tokens con commit() síncrono — evita la
    // ventana donde accessToken está actualizado en memoria pero refreshToken aún no
    // fue persistido, lo que podría causar tokens inconsistentes si el proceso se mata.
    fun updateTokens(newAccessToken: String, newRefreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, newAccessToken)
            .putString(KEY_REFRESH_TOKEN, newRefreshToken)
            .commit()
    }

    fun clearSession(intentional: Boolean = false) {
        // PROD-01: usar commit() (síncrono) en lugar de apply() (asíncrono) para garantizar
        // que los tokens se eliminan inmediatamente antes de que el hilo de OkHttp
        // pueda leer accessToken en el siguiente request tras la expiración.
        prefs.edit().clear().commit()
        if (!intentional) _sessionExpiredChannel.trySend(Unit)
    }

    private fun SharedPreferences.edit(action: SharedPreferences.Editor.() -> Unit) {
        edit().apply(action).apply()
    }
}
