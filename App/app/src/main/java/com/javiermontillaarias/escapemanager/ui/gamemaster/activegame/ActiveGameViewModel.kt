package com.javiermontillaarias.escapemanager.ui.gamemaster.activegame

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

class ActiveGameViewModel(private val repository: GameRepository) : ViewModel() {

    private val _hintsResult = MutableLiveData<Resource<HintsResponse>>()
    val hintsResult: LiveData<Resource<HintsResponse>> = _hintsResult

    private val _closeResult = MutableLiveData<Resource<Game>>()
    val closeResult: LiveData<Resource<Game>> = _closeResult

    private val _elapsedSeconds = MutableLiveData<Long>(0L)
    val elapsedSeconds: LiveData<Long> = _elapsedSeconds

    private var timerJob: Job? = null
    private var hintsUsed = 0

    fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _elapsedSeconds.value = (_elapsedSeconds.value ?: 0L) + 1L
            }
        }
    }

    fun stopTimer() = timerJob?.cancel()

    fun addHint(gameId: Int) {
        viewModelScope.launch {
            val result = repository.addHint(gameId)
            _hintsResult.value = result
            if (result is Resource.Success) hintsUsed = result.data.hintsUsed
        }
    }

    fun closeGame(gameId: Int, escaped: Boolean, observations: String) {
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
}