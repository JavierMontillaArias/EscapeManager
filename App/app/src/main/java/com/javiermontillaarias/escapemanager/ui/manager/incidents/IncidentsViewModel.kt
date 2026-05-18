package com.javiermontillaarias.escapemanager.ui.manager.incidents

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javiermontillaarias.escapemanager.data.model.Incident
import com.javiermontillaarias.escapemanager.data.model.IncidentResolveResponse
import com.javiermontillaarias.escapemanager.data.repository.IncidentRepository
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.launch

class IncidentsViewModel(private val repository: IncidentRepository) : ViewModel() {

    private val _incidents = MutableLiveData<Resource<List<Incident>>>()
    val incidents: LiveData<Resource<List<Incident>>> = _incidents

    private val _resolveResult = MutableLiveData<Resource<IncidentResolveResponse>>()
    val resolveResult: LiveData<Resource<IncidentResolveResponse>> = _resolveResult

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
            if (result is Resource.Success) {
                // Q-04: actualizar la lista localmente en lugar de recargar desde el servidor
                val current = (_incidents.value as? Resource.Success)?.data ?: return@launch
                val resolucion = result.data.fechaResolucion
                _incidents.value = Resource.Success(current.map { incident ->
                    if (incident.id == id) incident.copy(resuelta = true, fechaResolucion = resolucion)
                    else incident
                })
            } else if (result is Resource.Error) {
                // BUG-03: en error, recargar para sincronizar con el estado real del servidor
                // (p.ej. si otro usuario ya resolvió la incidencia)
                loadIncidents()
            }
        }
    }
}
