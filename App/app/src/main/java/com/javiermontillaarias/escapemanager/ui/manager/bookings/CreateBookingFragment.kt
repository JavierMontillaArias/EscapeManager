package com.javiermontillaarias.escapemanager.ui.manager.bookings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.javiermontillaarias.escapemanager.data.local.SessionManager
import com.javiermontillaarias.escapemanager.data.model.Booking
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

    private val viewModel by lazy {
        val sm = SessionManager(requireContext())
        val api = RetrofitClient.getApiService(sm)
        CreateBookingViewModel(BookingRepository(api), RoomRepository(api))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.rooms.observe(viewLifecycleOwner) { state ->
            if (state is Resource.Success) {
                rooms = state.data
                val names = rooms.map { "${it.name} (${it.capacity} personas)" }
                binding.spinnerRoom.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    names
                ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            }
        }

        viewModel.createResult.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnCreate.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreate.isEnabled = true
                    Snackbar.make(
                        binding.root,
                        "Reserva creada. QR enviado a ${state.data.email}",
                        Snackbar.LENGTH_LONG
                    ).show()
                    findNavController().popBackStack()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreate.isEnabled = true
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> {}
            }
        }

        binding.btnCreate.setOnClickListener { createBooking() }
    }

    private fun createBooking() {
        val groupName = binding.etGroupName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val numPeopleStr = binding.etNumPeople.text.toString().trim()
        val fecha = binding.etDate.text.toString().trim()
        val horaInicio = binding.etHoraInicio.text.toString().trim()
        val horaFin = binding.etHoraFin.text.toString().trim()

        if (groupName.isEmpty() || email.isEmpty() || numPeopleStr.isEmpty()
            || fecha.isEmpty() || horaInicio.isEmpty() || horaFin.isEmpty()) {
            Snackbar.make(binding.root, "Completa todos los campos", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Snackbar.make(binding.root, "Email no válido", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (rooms.isEmpty()) {
            Snackbar.make(binding.root, "No hay salas disponibles", Snackbar.LENGTH_SHORT).show()
            return
        }

        val roomId = rooms[binding.spinnerRoom.selectedItemPosition].id
        val numPeople = numPeopleStr.toIntOrNull() ?: run {
            Snackbar.make(binding.root, "Número de personas inválido", Snackbar.LENGTH_SHORT).show()
            return
        }

        // Formatea hora a HH:MM:SS si el usuario pone HH:MM
        val horaInicioFmt = if (horaInicio.length == 5) "$horaInicio:00" else horaInicio
        val horaFinFmt = if (horaFin.length == 5) "$horaFin:00" else horaFin

        viewModel.createBooking(
            BookingRequest(roomId, groupName, numPeople, email, fecha, horaInicioFmt, horaFinFmt)
        )
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

    private val _createResult = MutableLiveData<Resource<Booking>>(Resource.Idle)
    val createResult: LiveData<Resource<Booking>> = _createResult

    init {
        viewModelScope.launch { _rooms.value = roomRepo.getRooms() }
    }

    fun createBooking(request: BookingRequest) {
        viewModelScope.launch {
            _createResult.value = Resource.Loading
            _createResult.value = bookingRepo.createBooking(request)
        }
    }
}