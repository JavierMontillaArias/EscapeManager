package com.javiermontillaarias.escapemanager.ui.manager.bookings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javiermontillaarias.escapemanager.data.model.Booking
import com.javiermontillaarias.escapemanager.data.model.MessageResponse
import com.javiermontillaarias.escapemanager.data.repository.BookingRepository
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.launch

class BookingDetailViewModel(private val repo: BookingRepository) : ViewModel() {

    private val _booking = MutableLiveData<Resource<Booking>>(Resource.Idle)
    val booking: LiveData<Resource<Booking>> = _booking

    private val _resendResult = MutableLiveData<Resource<MessageResponse>>(Resource.Idle)
    val resendResult: LiveData<Resource<MessageResponse>> = _resendResult

    // INC-04: resultado independiente para la cancelación vía DELETE /bookings/{id}
    private val _cancelResult = MutableLiveData<Resource<Unit>>(Resource.Idle)
    val cancelResult: LiveData<Resource<Unit>> = _cancelResult

    fun load(id: Int) {
        viewModelScope.launch {
            _booking.value = Resource.Loading
            _booking.value = repo.getBooking(id)
        }
    }

    fun resendQr(id: Int) {
        viewModelScope.launch {
            _resendResult.value = Resource.Loading
            _resendResult.value = repo.resendQr(id)
        }
    }

    // INC-04: usar el endpoint DELETE /bookings/{id} para cancelar en lugar de PATCH /status
    fun cancelBooking(id: Int) {
        viewModelScope.launch {
            _cancelResult.value = Resource.Loading
            _cancelResult.value = repo.deleteBooking(id)
        }
    }
}
