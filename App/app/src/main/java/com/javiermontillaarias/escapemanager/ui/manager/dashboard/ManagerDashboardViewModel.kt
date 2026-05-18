package com.javiermontillaarias.escapemanager.ui.manager.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javiermontillaarias.escapemanager.data.model.StatsSummary
import com.javiermontillaarias.escapemanager.data.repository.StatsRepository
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.launch
import android.os.SystemClock

class ManagerDashboardViewModel(private val statsRepo: StatsRepository) : ViewModel() {

    private val _summary = MutableLiveData<Resource<StatsSummary>>()
    val summary: LiveData<Resource<StatsSummary>> = _summary

    private var lastLoadMs: Long = 0L

    init { loadSummary() }

    fun loadSummary(force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && _summary.value is Resource.Success && now - lastLoadMs < 30_000L) return
        viewModelScope.launch {
            _summary.value = Resource.Loading
            val result = statsRepo.getSummary()
            if (result is Resource.Success) lastLoadMs = SystemClock.elapsedRealtime()
            _summary.value = result
        }
    }
}