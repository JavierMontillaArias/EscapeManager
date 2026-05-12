package com.javiermontillaarias.escapemanager.ui.manager.bookings

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javiermontillaarias.escapemanager.data.local.SessionManager
import com.javiermontillaarias.escapemanager.data.model.Booking
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.BookingRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentBookingDetailBinding
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.launch

class BookingDetailFragment : Fragment() {

    private var _binding: FragmentBookingDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel by lazy {
        val api = RetrofitClient.getApiService(SessionManager(requireContext()))
        BookingDetailViewModel(BookingRepository(api))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // In a real app load specific booking by ID
        // For now, show the fragment structure
        val bookingId = arguments?.getInt("bookingId", -1) ?: -1
        binding.tvBookingId.text = "Reserva #$bookingId"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class BookingDetailViewModel(private val repo: BookingRepository) : ViewModel() {
    private val _bookings = MutableLiveData<Resource<List<Booking>>>()
    val bookings: LiveData<Resource<List<Booking>>> = _bookings

    fun load() {
        viewModelScope.launch {
            _bookings.value = Resource.Loading
            _bookings.value = repo.getBookings()
        }
    }
}