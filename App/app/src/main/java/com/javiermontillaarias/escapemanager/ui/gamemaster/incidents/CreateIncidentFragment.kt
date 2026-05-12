package com.javiermontillaarias.escapemanager.ui.gamemaster.incidents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import com.javiermontillaarias.escapemanager.data.local.SessionManager
import com.javiermontillaarias.escapemanager.data.model.Incident
import com.javiermontillaarias.escapemanager.data.model.IncidentRequest
import com.javiermontillaarias.escapemanager.data.model.Room
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.IncidentRepository
import com.javiermontillaarias.escapemanager.data.repository.RoomRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentCreateIncidentBinding
import com.javiermontillaarias.escapemanager.util.Resource
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class CreateIncidentFragment : Fragment() {

    private var _binding: FragmentCreateIncidentBinding? = null
    private val binding get() = _binding!!

    private var rooms: List<Room> = emptyList()

    private val viewModel by lazy {
        val sm = SessionManager(requireContext())
        val api = RetrofitClient.getApiService(sm)
        CreateIncidentViewModel(IncidentRepository(api), RoomRepository(api))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateIncidentBinding.inflate(inflater, container, false)
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
                    Snackbar.make(binding.root, "Incidencia reportada", Snackbar.LENGTH_SHORT).show()
                    binding.etDescription.text?.clear()
                }
                is Resource.Error -> Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                else -> {}
            }
        }

        binding.btnSubmit.setOnClickListener {
            val description = binding.etDescription.text.toString().trim()
            if (description.isEmpty()) {
                Snackbar.make(binding.root, "Escribe una descripción", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (rooms.isEmpty()) return@setOnClickListener
            val roomId = rooms[binding.spinnerRoom.selectedItemPosition].id
            viewModel.createIncident(IncidentRequest(roomId, description))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class CreateIncidentViewModel(
    private val incidentRepo: IncidentRepository,
    private val roomRepo: RoomRepository
) : ViewModel() {

    private val _rooms = MutableLiveData<Resource<List<Room>>>()
    val rooms: LiveData<Resource<List<Room>>> = _rooms

    private val _createResult = MutableLiveData<Resource<Incident>>()
    val createResult: LiveData<Resource<Incident>> = _createResult

    init {
        viewModelScope.launch { _rooms.value = roomRepo.getRooms() }
    }

    fun createIncident(request: IncidentRequest) {
        viewModelScope.launch {
            _createResult.value = Resource.Loading
            _createResult.value = incidentRepo.createIncident(request)
        }
    }
}