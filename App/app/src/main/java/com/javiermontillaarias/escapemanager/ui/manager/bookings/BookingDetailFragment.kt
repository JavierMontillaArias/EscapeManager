package com.javiermontillaarias.escapemanager.ui.manager.bookings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
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

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        val bookingId = arguments?.getInt("bookingId", -1) ?: -1
        if (bookingId != -1) viewModel.load(bookingId)

        viewModel.booking.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.tvGroupName.text = "Cargando..."
                }
                is Resource.Success -> {
                    val b = state.data
                    binding.tvBookingId.text = "Reserva #${b.id}"
                    binding.tvGroupName.text = "${b.groupName}"
                    binding.tvDetails.text =
                        "${b.sala?.name ?: "Sala #${b.roomId}"}\n" +
                                "${b.fecha}\n" +
                                "${b.hora} → ${b.horaFin ?: "-"}\n" +
                                "${b.numPeople} personas\n" +
                                "${b.email}"
                    binding.tvStatus.text = "Estado: ${b.estado.replaceFirstChar { it.uppercase() }}"
                    binding.tvQrToken.text = b.qrToken ?: "Sin QR"
                }
                is Resource.Error -> {
                    binding.tvGroupName.text = state.message
                }
                else -> {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class BookingDetailViewModel(private val repo: BookingRepository) : ViewModel() {
    private val _booking = MutableLiveData<Resource<Booking>>(Resource.Idle)
    val booking: LiveData<Resource<Booking>> = _booking

    fun load(id: Int) {
        viewModelScope.launch {
            _booking.value = Resource.Loading
            _booking.value = repo.getBooking(id)
        }
    }
}