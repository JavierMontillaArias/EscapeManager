package com.javiermontillaarias.escapemanager.ui.manager.incidents

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javiermontillaarias.escapemanager.data.model.Incident
import com.javiermontillaarias.escapemanager.data.repository.IncidentRepository
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.launch

class IncidentsViewModel(private val repository: IncidentRepository) : ViewModel() {

    private val _incidents = MutableLiveData<Resource<List<Incident>>>()
    val incidents: LiveData<Resource<List<Incident>>> = _incidents

    private val _resolveResult = MutableLiveData<Resource<Incident>>()
    val resolveResult: LiveData<Resource<Incident>> = _resolveResult

    init { loadIncidents() }

    fun loadIncidents() {
        viewModelScope.launch {
            _incidents.value = Resource.Loading
            _incidents.value = repository.getIncidents()
        }
    }

    fun resolve(id: Int) {
        viewModelScope.launch {
            val result = repository.resolveIncident(id)
            _resolveResult.value = result
            if (result is Resource.Success) loadIncidents()
        }
    }
}