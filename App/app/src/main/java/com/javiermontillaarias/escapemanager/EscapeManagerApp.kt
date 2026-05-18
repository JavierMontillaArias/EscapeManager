package com.javiermontillaarias.escapemanager

import android.app.Application
import com.javiermontillaarias.escapemanager.data.local.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class EscapeManagerApp : Application() {
    val sessionManager: SessionManager by lazy { SessionManager(this) }
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
