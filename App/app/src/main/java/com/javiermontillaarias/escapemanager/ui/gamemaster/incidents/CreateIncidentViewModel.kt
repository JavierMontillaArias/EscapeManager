package com.javiermontillaarias.escapemanager.ui.gamemaster.incidents

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javiermontillaarias.escapemanager.data.model.Incident
import com.javiermontillaarias.escapemanager.data.model.IncidentRequest
import com.javiermontillaarias.escapemanager.data.model.Room
import com.javiermontillaarias.escapemanager.data.repository.IncidentRepository
import com.javiermontillaarias.escapemanager.data.repository.RoomRepository
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.launch

class CreateIncidentViewModel(
    private val incidentRepo: IncidentRepository,
    private val roomRepo: RoomRepository
) : ViewModel() {

    private val _rooms = MutableLiveData<Resource<List<Room>>>()
    val rooms: LiveData<Resource<List<Room>>> = _rooms

    private val _createResult = MutableLiveData<Resource<Incident>>()
    val createResult: LiveData<Resource<Incident>> = _createResult

    init {
        loadRooms()
    }

    // BUG-03: método público para que el Fragment pueda reintentar la carga de salas
    fun loadRooms() {
        viewModelScope.launch {
            _rooms.value = Resource.Loading
            _rooms.value = roomRepo.getActiveRooms()
        }
    }

    fun createIncident(request: IncidentRequest) {
        viewModelScope.launch {
            _createResult.value = Resource.Loading
            _createResult.value = incidentRepo.createIncident(request)
        }
    }
}
