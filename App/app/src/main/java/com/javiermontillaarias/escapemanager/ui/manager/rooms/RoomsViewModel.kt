package com.javiermontillaarias.escapemanager.ui.manager.rooms

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javiermontillaarias.escapemanager.data.model.Room
import com.javiermontillaarias.escapemanager.data.model.RoomRequest
import com.javiermontillaarias.escapemanager.data.repository.RoomRepository
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.launch

class RoomsViewModel(private val repository: RoomRepository) : ViewModel() {

    private val _rooms = MutableLiveData<Resource<List<Room>>>()
    val rooms: LiveData<Resource<List<Room>>> = _rooms

    private val _operationResult = MutableLiveData<Resource<Unit>>(Resource.Idle)
    val operationResult: LiveData<Resource<Unit>> = _operationResult

    init { loadRooms() }

    fun loadRooms() {
        viewModelScope.launch {
            _rooms.value = Resource.Loading
            _rooms.value = repository.getRooms()
        }
    }

    fun createRoom(name: String, theme: String, capacity: Int, difficulty: String) {
        viewModelScope.launch {
            _operationResult.value = Resource.Loading
            val result = repository.createRoom(RoomRequest(name, theme, capacity, difficulty))
            if (result is Resource.Success) loadRooms()
            _operationResult.value = when (result) {
                is Resource.Success -> Resource.Success(Unit)
                is Resource.Error -> Resource.Error(result.message)
                else -> Resource.Idle
            }
        }
    }

    fun updateRoom(id: Int, name: String, theme: String, capacity: Int, difficulty: String) {
        viewModelScope.launch {
            _operationResult.value = Resource.Loading
            val result = repository.updateRoom(id, RoomRequest(name, theme, capacity, difficulty))
            if (result is Resource.Success) loadRooms()
            _operationResult.value = when (result) {
                is Resource.Success -> Resource.Success(Unit)
                is Resource.Error -> Resource.Error(result.message)
                else -> Resource.Idle
            }
        }
    }

    fun deleteRoom(id: Int) {
        viewModelScope.launch {
            _operationResult.value = Resource.Loading
            val result = repository.deleteRoom(id)
            if (result is Resource.Success) loadRooms()
            _operationResult.value = when (result) {
                is Resource.Success -> Resource.Success(Unit)
                is Resource.Error -> Resource.Error(result.message)
                else -> Resource.Idle
            }
        }
    }
}