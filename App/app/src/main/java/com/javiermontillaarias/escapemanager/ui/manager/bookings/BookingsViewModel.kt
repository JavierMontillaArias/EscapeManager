package com.javiermontillaarias.escapemanager.ui.manager.bookings

import android.os.SystemClock
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

    // BUG-07: público para que BookingsFragment pueda sincronizar el campo de texto
    var currentFilter: String? = null
        private set

    // P-1: timestamp de la última carga para stale check de 30s
    private var lastLoadMs: Long = 0

    init { loadBookings() }

    fun loadBookings(date: String? = currentFilter, force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        // P-1: evitar recarga si los datos son frescos (< 30s) y el filtro no cambió
        if (!force && _bookings.value is Resource.Success && now - lastLoadMs < 30_000L && date == currentFilter) return
        currentFilter = date
        viewModelScope.launch {
            _bookings.value = Resource.Loading
            _bookings.value = repository.getBookings(fecha = date)
            lastLoadMs = SystemClock.elapsedRealtime()
        }
    }

    fun deleteBooking(id: Int) {
        viewModelScope.launch {
            val result = repository.deleteBooking(id)
            _deleteResult.value = when (result) {
                // P-04: eliminar localmente en lugar de recargar toda la lista
                is Resource.Success -> {
                    val current = (_bookings.value as? Resource.Success)?.data
                    if (current != null) {
                        _bookings.value = Resource.Success(current.filter { it.id != id })
                    }
                    Resource.Success(Unit)
                }
                is Resource.Error   -> Resource.Error(result.message)
                is Resource.Loading -> Resource.Loading
                is Resource.Idle    -> Resource.Idle
            }
        }
    }
}
