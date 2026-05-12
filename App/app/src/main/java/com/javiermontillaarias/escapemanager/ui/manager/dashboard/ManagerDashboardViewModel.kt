package com.javiermontillaarias.escapemanager.ui.manager.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javiermontillaarias.escapemanager.data.model.StatsSummary
import com.javiermontillaarias.escapemanager.data.repository.StatsRepository
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.launch

class ManagerDashboardViewModel(private val statsRepo: StatsRepository) : ViewModel() {

    private val _summary = MutableLiveData<Resource<StatsSummary>>()
    val summary: LiveData<Resource<StatsSummary>> = _summary

    init { loadSummary() }

    fun loadSummary() {
        viewModelScope.launch {
            _summary.value = Resource.Loading
            _summary.value = statsRepo.getSummary()
        }
    }
}