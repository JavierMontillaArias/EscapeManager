package com.javiermontillaarias.escapemanager.ui.gamemaster.activegame

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javiermontillaarias.escapemanager.data.model.Game
import com.javiermontillaarias.escapemanager.data.model.HintsResponse
import com.javiermontillaarias.escapemanager.data.repository.GameRepository
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

class ActiveGameViewModel(private val repository: GameRepository) : ViewModel() {

    private val _hintsResult = MutableLiveData<Resource<HintsResponse>>()
    val hintsResult: LiveData<Resource<HintsResponse>> = _hintsResult

    private val _closeResult = MutableLiveData<Resource<Game>>()
    val closeResult: LiveData<Resource<Game>> = _closeResult

    private val _elapsedSeconds = MutableLiveData<Long>(0L)
    val elapsedSeconds: LiveData<Long> = _elapsedSeconds

    private var timerJob: Job? = null

    // I-07: offset calculado desde la hora de inicio real del QR para que el timer
    // refleje el tiempo transcurrido real incluso si el Fragment se recrea.
    fun startTimer(startTimeIso: String? = null) {
        val offsetSeconds = parseElapsedSeconds(startTimeIso)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var seconds = offsetSeconds
            while (isActive) {
                _elapsedSeconds.value = seconds
                delay(1000)
                seconds++
            }
        }
    }

    fun stopTimer() = timerJob?.cancel()

    // BUG-01: AtomicBoolean evita doble-tap rápido igual que en QrScannerViewModel
    private val isAddingHint = AtomicBoolean(false)

    fun addHint(gameId: Int) {
        if (!isAddingHint.compareAndSet(false, true)) return
        viewModelScope.launch {
            _hintsResult.value = Resource.Loading
            // B-09: hintsUsed eliminado; la fuente de verdad es el servidor
            _hintsResult.value = repository.addHint(gameId)
            isAddingHint.set(false)
        }
    }

    fun closeGame(gameId: Int, escaped: Boolean, observations: String?) {
        stopTimer()
        viewModelScope.launch {
            _closeResult.value = Resource.Loading
            _closeResult.value = repository.closeGame(gameId, escaped, observations)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    // BUG-01: parsing robusto que maneja datetime con o sin microsegundos y con o sin offset.
    private fun parseElapsedSeconds(startTimeIso: String?): Long {
        if (startTimeIso.isNullOrBlank()) return 0L
        return try {
            // Normalizar: eliminar fracción de segundos y añadir Z si no tiene offset de zona.
            val cleanIso = startTimeIso.substringBefore('.').let {
                if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it
            }
            val startInstant = Instant.parse(cleanIso)
            val elapsed = Instant.now().epochSecond - startInstant.epochSecond
            if (elapsed > 0) elapsed else 0L
        } catch (e: Exception) {
            Log.e("ActiveGame", "parseElapsedSeconds falló con: $startTimeIso", e)
            0L
        }
    }
}
