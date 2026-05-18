package com.javiermontillaarias.escapemanager.ui.manager.stats

import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javiermontillaarias.escapemanager.data.model.*
import com.javiermontillaarias.escapemanager.data.repository.StatsRepository
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class StatsViewModel(private val repository: StatsRepository) : ViewModel() {

    private val _escapeRate = MutableLiveData<Resource<List<EscapeRate>>>()
    val escapeRate: LiveData<Resource<List<EscapeRate>>> = _escapeRate

    private val _hintsAvg = MutableLiveData<Resource<List<HintsAvg>>>()
    val hintsAvg: LiveData<Resource<List<HintsAvg>>> = _hintsAvg

    private val _ranking = MutableLiveData<Resource<List<RankingEntry>>>()
    val ranking: LiveData<Resource<List<RankingEntry>>> = _ranking

    // PR-07: summary y occupancy expuestos para que el Fragment los consuma
    private val _summary = MutableLiveData<Resource<StatsSummary>>()
    val summary: LiveData<Resource<StatsSummary>> = _summary

    private val _occupancy = MutableLiveData<Resource<List<OccupancyData>>>()
    val occupancy: LiveData<Resource<List<OccupancyData>>> = _occupancy

    private var lastLoadMs = 0L

    init { loadAll() }

    fun loadAllIfStale() {
        val now = SystemClock.elapsedRealtime()
        if (_ranking.value is Resource.Success && now - lastLoadMs < 60_000L) return
        loadAll()
    }

    fun loadAll() {
        _escapeRate.value = Resource.Loading
        _hintsAvg.value   = Resource.Loading
        _ranking.value    = Resource.Loading
        _summary.value    = Resource.Loading
        _occupancy.value  = Resource.Loading

        viewModelScope.launch {
            // B-04: supervisorScope garantiza que el fallo de un async no cancela los demás
            supervisorScope {
                // P-03: un único getRanking(); EscapeRate y HintsAvg se derivan de él
                val rankingDeferred   = async { repository.getRanking() }
                val summaryDeferred   = async { repository.getSummary() }
                val occupancyDeferred = async { repository.getOccupancy() }

                val rankingResult = rankingDeferred.await()
                _ranking.value = rankingResult
                when (rankingResult) {
                    is Resource.Success -> {
                        _escapeRate.value = Resource.Success(rankingResult.data.map { e ->
                            EscapeRate(roomId = e.roomId, room = e.room, totalGames = e.totalGames, rate = e.tasaEscape)
                        })
                        _hintsAvg.value = Resource.Success(rankingResult.data.map { e ->
                            HintsAvg(roomId = e.roomId, room = e.room, totalGames = e.totalGames, average = e.promedioPistas)
                        })
                    }
                    is Resource.Error -> {
                        _escapeRate.value = Resource.Error(rankingResult.message)
                        _hintsAvg.value   = Resource.Error(rankingResult.message)
                    }
                    else -> Unit
                }

                _summary.value   = summaryDeferred.await()
                _occupancy.value = occupancyDeferred.await()
                lastLoadMs = SystemClock.elapsedRealtime()
            }
        }
    }
}
