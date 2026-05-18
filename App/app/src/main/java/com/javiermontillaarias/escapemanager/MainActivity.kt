package com.javiermontillaarias.escapemanager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.javiermontillaarias.escapemanager.BuildConfig
import com.javiermontillaarias.escapemanager.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // S-03: detectar URL de producción sin configurar
        if (BuildConfig.BASE_URL.contains("tudominio")) {
            if (BuildConfig.DEBUG) Log.w("Config", "BASE_URL es placeholder — configura la URL real para producción")
            else throw IllegalStateException("BASE_URL de producción no configurada. Actualiza buildConfigField en build.gradle.kts")
        }

        // PR-07: observar expiración de sesión y reiniciar la App al login
        val sessionManager = (application as EscapeManagerApp).sessionManager
        lifecycleScope.launch {
            sessionManager.sessionExpiredFlow.collect {
                val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }
}
