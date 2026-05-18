package com.javiermontillaarias.escapemanager

import android.app.Application
import com.javiermontillaarias.escapemanager.data.local.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class EscapeManagerApp : Application() {
    val sessionManager: SessionManager by lazy { SessionManager(this) }

    // B-6: scope de Application — no se cancela con los Fragments; necesario para
    // operaciones que deben completarse aunque el Fragment origen se destruya (ej: logoutApi).
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
