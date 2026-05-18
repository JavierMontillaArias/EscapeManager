package com.javiermontillaarias.escapemanager.ui.manager.bookings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.setFragmentResult
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.javiermontillaarias.escapemanager.EscapeManagerApp
import com.javiermontillaarias.escapemanager.R
import com.javiermontillaarias.escapemanager.data.model.BookingRequest
import com.javiermontillaarias.escapemanager.data.model.Room
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.BookingRepository
import com.javiermontillaarias.escapemanager.data.repository.RoomRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentCreateBookingBinding
import com.javiermontillaarias.escapemanager.util.Resource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateBookingFragment : Fragment() {

    private var _binding: FragmentCreateBookingBinding? = null
    private val binding get() = _binding!!
    private var rooms: List<Room> = emptyList()

    private val viewModel: CreateBookingViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // B-03: singleton SessionManager desde la Application
                val sm = (requireActivity().application as EscapeManagerApp).sessionManager
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

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // PR-03: DatePicker — evita fechas inválidas por teclado libre
        binding.etDate.isFocusable = false
        binding.etDate.isClickable = true
        binding.etDate.setOnClickListener { showDatePicker() }

        // PR-04: TimePicker — garantiza horas válidas en formato 24h
        binding.etHoraInicio.isFocusable = false
        binding.etHoraInicio.isClickable = true
        binding.etHoraInicio.setOnClickListener { showTimePicker(isInicio = true) }

        binding.etHoraFin.isFocusable = false
        binding.etHoraFin.isClickable = true
        binding.etHoraFin.setOnClickListener { showTimePicker(isInicio = false) }

        // B-7: manejar Error y Loading para que el usuario sepa que hay un problema
        viewModel.rooms.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Success -> {
                    rooms = state.data
                    val names = rooms.map { "${it.name} (${it.capacity} personas)" }
                    binding.spinnerRoom.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        names
                    ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                }
                is Resource.Error -> Snackbar.make(
                    binding.root,
                    "Error al cargar salas: ${state.message}",
                    Snackbar.LENGTH_LONG
                ).setAction("Reintentar") { viewModel.loadRooms() }.show()
                is Resource.Loading -> { /* carga inicial silenciosa */ }
                else -> Unit
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
                    setFragmentResult("booking_created", Bundle.EMPTY)
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

    // PR-03: muestra un MaterialDatePicker restringido a fechas futuras
    private fun showDatePicker() {
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val constraints = CalendarConstraints.Builder()
            .setStart(today)
            .build()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona la fecha")
            .setSelection(today)
            .setCalendarConstraints(constraints)
            .setTheme(R.style.CustomDatePickerTheme)
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            binding.etDate.setText(sdf.format(Date(selection)))
        }
        picker.show(parentFragmentManager, "date_picker")
    }

    // PR-04: muestra un MaterialTimePicker en formato 24h
    private fun showTimePicker(isInicio: Boolean) {
        val title = if (isInicio) "Hora de inicio" else "Hora de fin"
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setTitleText(title)
            .build()
        picker.addOnPositiveButtonClickListener {
            val formatted = String.format("%02d:%02d", picker.hour, picker.minute)
            if (isInicio) binding.etHoraInicio.setText(formatted)
            else          binding.etHoraFin.setText(formatted)
        }
        picker.show(parentFragmentManager, if (isInicio) "time_picker_inicio" else "time_picker_fin")
    }

    private fun createBooking() {
        val groupName    = binding.etGroupName.text.toString().trim()
        val email        = binding.etEmail.text.toString().trim()
        val numPeopleStr = binding.etNumPeople.text.toString().trim()
        val fecha        = binding.etDate.text.toString().trim()
        val horaInicio   = binding.etHoraInicio.text.toString().trim()
        val horaFin      = binding.etHoraFin.text.toString().trim()

        if (groupName.isEmpty() || email.isEmpty() || numPeopleStr.isEmpty()
            || fecha.isEmpty() || horaInicio.isEmpty() || horaFin.isEmpty()) {
            Snackbar.make(binding.root, "Completa todos los campos", Snackbar.LENGTH_SHORT).show()
            return
        }

        val emailRegex = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
        if (!emailRegex.matches(email)) {
            Snackbar.make(binding.root, "Email no válido", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (rooms.isEmpty()) {
            Snackbar.make(binding.root, "No hay salas disponibles", Snackbar.LENGTH_SHORT).show()
            return
        }

        val numPeople = numPeopleStr.toIntOrNull() ?: 0
        if (numPeople <= 0) {
            Snackbar.make(binding.root, getString(R.string.error_personas), Snackbar.LENGTH_SHORT).show()
            return
        }

        // INC-04: validar que numPeople no supere la capacidad máxima de la sala seleccionada
        val selectedRoom = rooms[binding.spinnerRoom.selectedItemPosition]
        if (numPeople > selectedRoom.capacity) {
            Snackbar.make(binding.root, "Máximo ${selectedRoom.capacity} personas para esta sala", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (!fecha.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            Snackbar.make(binding.root, getString(R.string.error_fecha_invalida), Snackbar.LENGTH_SHORT).show()
            return
        }

        val timeRegex = Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")
        if (!timeRegex.matches(horaInicio)) {
            Snackbar.make(binding.root, getString(R.string.error_hora_invalida), Snackbar.LENGTH_SHORT).show()
            return
        }

        // I-04: validar horaFin igual que horaInicio
        if (!timeRegex.matches(horaFin)) {
            Snackbar.make(binding.root, getString(R.string.error_hora_invalida), Snackbar.LENGTH_SHORT).show()
            return
        }

        val horaInicioFmt = if (horaInicio.length == 5) "$horaInicio:00" else horaInicio
        val horaFinFmt    = if (horaFin.length == 5) "$horaFin:00" else horaFin

        // Reservas que cruzan medianoche (horaFin < horaInicio) no están soportadas por diseño;
        // tanto la API como esta validación las rechazan correctamente.
        if (horaInicioFmt >= horaFinFmt) {
            Snackbar.make(binding.root, getString(R.string.error_horas), Snackbar.LENGTH_SHORT).show()
            return
        }

        val roomId = rooms[binding.spinnerRoom.selectedItemPosition].id
        viewModel.createBooking(
            BookingRequest(roomId, groupName, numPeople, email, fecha, horaInicioFmt, horaFinFmt)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
