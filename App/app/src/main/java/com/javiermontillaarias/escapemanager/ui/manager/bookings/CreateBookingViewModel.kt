package com.javiermontillaarias.escapemanager.ui.manager.bookings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javiermontillaarias.escapemanager.data.model.Booking
import com.javiermontillaarias.escapemanager.data.model.BookingRequest
import com.javiermontillaarias.escapemanager.data.model.Room
import com.javiermontillaarias.escapemanager.data.repository.BookingRepository
import com.javiermontillaarias.escapemanager.data.repository.RoomRepository
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.launch

class CreateBookingViewModel(
    private val bookingRepo: BookingRepository,
    private val roomRepo: RoomRepository
) : ViewModel() {

    private val _rooms = MutableLiveData<Resource<List<Room>>>()
    val rooms: LiveData<Resource<List<Room>>> = _rooms

    private val _createResult = MutableLiveData<Resource<Booking>>(Resource.Idle)
    val createResult: LiveData<Resource<Booking>> = _createResult

    init {
        loadRooms()
    }

    // B-7: expuesto para que el Fragment pueda ofrecer un botón "Reintentar" si la carga falla
    fun loadRooms() {
        // INC-02: usar getActiveRooms() (GET /rooms/active) en lugar de getRooms(soloActivas=true)
        // para alinearse semánticamente con el endpoint público de salas activas.
        viewModelScope.launch { _rooms.value = roomRepo.getActiveRooms() }
    }

    fun createBooking(request: BookingRequest) {
        viewModelScope.launch {
            _createResult.value = Resource.Loading
            _createResult.value = bookingRepo.createBooking(request)
        }
    }
}
