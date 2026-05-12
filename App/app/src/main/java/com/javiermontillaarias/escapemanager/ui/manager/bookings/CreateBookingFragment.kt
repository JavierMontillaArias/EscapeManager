package com.javiermontillaarias.escapemanager.ui.manager.bookings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.javiermontillaarias.escapemanager.data.local.SessionManager
import com.javiermontillaarias.escapemanager.data.model.BookingRequest
import com.javiermontillaarias.escapemanager.data.model.Room
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.BookingRepository
import com.javiermontillaarias.escapemanager.data.repository.RoomRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentCreateBookingBinding
import com.javiermontillaarias.escapemanager.util.Resource
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class CreateBookingFragment : Fragment() {

    private var _binding: FragmentCreateBookingBinding? = null
    private val binding get() = _binding!!

    private var rooms: List<Room> = emptyList()

    // Inline ViewModel for this fragment
    private val viewModel by viewModels<CreateBookingViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val sm = SessionManager(requireContext())
                val api = RetrofitClient.getApiService(sm)
                @Suppress("UNCHECKED_CAST")
                return CreateBookingViewModel(BookingRepository(api), RoomRepository(api)) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.rooms.observe(viewLifecycleOwner) { state ->
            if (state is Resource.Success) {
                rooms = state.data
                val names = rooms.map { it.name }
                binding.spinnerRoom.adapter = ArrayAdapter(
                    requireContext(), android.R.layout.simple_spinner_item, names
                ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            }
        }

        viewModel.createResult.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Success -> {
                    Snackbar.make(binding.root, "Reserva creada", Snackbar.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is Resource.Error -> Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                else -> {}
            }
        }

        binding.btnCreate.setOnClickListener { createBooking() }
    }

    private fun createBooking() {
        val groupName = binding.etGroupName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val numPeopleStr = binding.etNumPeople.text.toString().trim()
        val date = binding.etDate.text.toString().trim()
        val time = binding.etTime.text.toString().trim()

        if (groupName.isEmpty() || email.isEmpty() || numPeopleStr.isEmpty() || date.isEmpty() || time.isEmpty()) {
            Snackbar.make(binding.root, "Completa todos los campos", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (rooms.isEmpty()) {
            Snackbar.make(binding.root, "No hay salas disponibles", Snackbar.LENGTH_SHORT).show()
            return
        }

        val roomId = rooms[binding.spinnerRoom.selectedItemPosition].id
        val numPeople = numPeopleStr.toIntOrNull() ?: return

        viewModel.createBooking(BookingRequest(roomId, groupName, numPeople, email, date, time))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class CreateBookingViewModel(
    private val bookingRepo: BookingRepository,
    private val roomRepo: RoomRepository
) : ViewModel() {

    private val _rooms = MutableLiveData<Resource<List<Room>>>()
    val rooms: LiveData<Resource<List<Room>>> = _rooms

    private val _createResult = MutableLiveData<Resource<Any>>()
    val createResult: LiveData<Resource<Any>> = _createResult

    init {
        viewModelScope.launch {
            _rooms.value = roomRepo.getRooms()
        }
    }

    fun createBooking(request: BookingRequest) {
        viewModelScope.launch {
            _createResult.value = Resource.Loading
            val result = bookingRepo.createBooking(request)
            _createResult.value = when (result) {
                is Resource.Success -> Resource.Success(result.data)
                is Resource.Error -> Resource.Error(result.message)
                is Resource.Loading -> Resource.Loading
                is Resource.Idle -> Resource.Idle
            }
        }
    }
}