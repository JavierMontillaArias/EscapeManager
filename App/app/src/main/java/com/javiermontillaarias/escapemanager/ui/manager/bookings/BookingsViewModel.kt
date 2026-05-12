package com.javiermontillaarias.escapemanager.ui.manager.bookings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javiermontillaarias.escapemanager.data.model.Booking
import com.javiermontillaarias.escapemanager.data.repository.BookingRepository
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.launch

class BookingsViewModel(private val repository: BookingRepository) : ViewModel() {

    private val _bookings = MutableLiveData<Resource<List<Booking>>>()
    val bookings: LiveData<Resource<List<Booking>>> = _bookings

    private val _deleteResult = MutableLiveData<Resource<Unit>>()
    val deleteResult: LiveData<Resource<Unit>> = _deleteResult

    private var currentFilter: String? = null

    init { loadBookings() }

    fun loadBookings(date: String? = currentFilter) {
        currentFilter = date
        viewModelScope.launch {
            _bookings.value = Resource.Loading
            _bookings.value = repository.getBookings(date)
        }
    }

    fun deleteBooking(id: Int) {
        viewModelScope.launch {
            val result = repository.deleteBooking(id)
            _deleteResult.value = when (result) {
                is Resource.Success -> { loadBookings(); Resource.Success(Unit) }
                is Resource.Error -> Resource.Error(result.message)
                is Resource.Loading -> Resource.Loading
            }
        }
    }
}