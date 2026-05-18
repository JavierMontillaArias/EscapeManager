package com.javiermontillaarias.escapemanager.ui.gamemaster.incidents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.javiermontillaarias.escapemanager.EscapeManagerApp
import com.javiermontillaarias.escapemanager.data.model.IncidentRequest
import com.javiermontillaarias.escapemanager.data.model.Room
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.IncidentRepository
import com.javiermontillaarias.escapemanager.data.repository.RoomRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentCreateIncidentBinding
import com.javiermontillaarias.escapemanager.util.Resource

class CreateIncidentFragment : Fragment() {

    private var _binding: FragmentCreateIncidentBinding? = null
    private val binding get() = _binding!!

    private var rooms: List<Room> = emptyList()

    private val viewModel: CreateIncidentViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // B-03: singleton SessionManager desde la Application
                val sm = (requireActivity().application as EscapeManagerApp).sessionManager
                val api = RetrofitClient.getApiService(sm)
                @Suppress("UNCHECKED_CAST")
                return CreateIncidentViewModel(IncidentRepository(api), RoomRepository(api)) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateIncidentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // BUG-03: manejar Error y Loading para feedback al usuario
        viewModel.rooms.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Success -> {
                    rooms = state.data
                    val names = rooms.map { it.name }
                    binding.spinnerRoom.adapter = ArrayAdapter(
                        requireContext(), android.R.layout.simple_spinner_item, names
                    ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                }
                is Resource.Error -> Snackbar.make(
                    binding.root, "Error al cargar salas: ${state.message}", Snackbar.LENGTH_LONG
                ).setAction("Reintentar") { viewModel.loadRooms() }.show()
                is Resource.Loading -> { /* carga inicial silenciosa */ }
                else -> Unit
            }
        }

        viewModel.createResult.observe(viewLifecycleOwner) { state ->
            binding.btnSubmit.isEnabled = state !is Resource.Loading
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
            // I-03: alineado con min_length=10 de la API
            if (description.length < 10) {
                Snackbar.make(binding.root, "La descripción debe tener al menos 10 caracteres", Snackbar.LENGTH_SHORT).show()
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
