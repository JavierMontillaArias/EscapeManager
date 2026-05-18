package com.javiermontillaarias.escapemanager.ui.gamemaster.dashboard

import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javiermontillaarias.escapemanager.data.model.Game
import com.javiermontillaarias.escapemanager.data.repository.GameRepository
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.launch

class GmDashboardViewModel(private val repository: GameRepository) : ViewModel() {

    private val _games = MutableLiveData<Resource<List<Game>>>()
    val games: LiveData<Resource<List<Game>>> = _games

    private var lastLoadMs = 0L

    init { loadGames() }

    fun loadGames(skip: Int = 0, limit: Int = 100) {
        lastLoadMs = SystemClock.elapsedRealtime()
        viewModelScope.launch {
            _games.value = Resource.Loading
            _games.value = repository.getGames(skip, limit)
        }
    }

    // BUG-02: evitar peticiones innecesarias en onResume — recargar solo si los datos
    // tienen más de 30 segundos o aún no se han cargado con éxito.
    fun loadGamesIfStale() {
        val now = SystemClock.elapsedRealtime()
        if (_games.value is Resource.Success && now - lastLoadMs < 30_000L) return
        loadGames()
    }
}
