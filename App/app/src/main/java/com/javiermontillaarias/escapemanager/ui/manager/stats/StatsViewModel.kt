package com.javiermontillaarias.escapemanager.ui.manager.stats

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javiermontillaarias.escapemanager.data.model.*
import com.javiermontillaarias.escapemanager.data.repository.StatsRepository
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.launch

class StatsViewModel(private val repository: StatsRepository) : ViewModel() {

    private val _escapeRate = MutableLiveData<Resource<List<EscapeRate>>>()
    val escapeRate: LiveData<Resource<List<EscapeRate>>> = _escapeRate

    private val _hintsAvg = MutableLiveData<Resource<List<HintsAvg>>>()
    val hintsAvg: LiveData<Resource<List<HintsAvg>>> = _hintsAvg

    private val _ranking = MutableLiveData<Resource<List<RankingEntry>>>()
    val ranking: LiveData<Resource<List<RankingEntry>>> = _ranking

    init { loadAll() }

    fun loadAll() {
        viewModelScope.launch {
            _escapeRate.value = Resource.Loading
            _escapeRate.value = repository.getEscapeRate()
        }
        viewModelScope.launch {
            _hintsAvg.value = Resource.Loading
            _hintsAvg.value = repository.getHintsAvg()
        }
        viewModelScope.launch {
            _ranking.value = Resource.Loading
            _ranking.value = repository.getRanking()
        }
    }
}