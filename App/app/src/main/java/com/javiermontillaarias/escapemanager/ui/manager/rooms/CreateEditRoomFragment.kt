package com.javiermontillaarias.escapemanager.ui.manager.rooms

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
import com.javiermontillaarias.escapemanager.data.local.SessionManager
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.RoomRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentCreateEditRoomBinding
import com.javiermontillaarias.escapemanager.util.Resource
import com.google.android.material.snackbar.Snackbar

class CreateEditRoomFragment : Fragment() {

    private var _binding: FragmentCreateEditRoomBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RoomsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val api = RetrofitClient.getApiService(SessionManager(requireContext()))
                @Suppress("UNCHECKED_CAST")
                return RoomsViewModel(RoomRepository(api)) as T
            }
        }
    }

    private val difficulties = listOf("baja", "media", "alta", "extrema")
    private var editingRoomId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateEditRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinner()

        editingRoomId = arguments?.getInt("roomId", -1) ?: -1

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        requireActivity().supportFragmentManager.setFragmentResultListener(
            "selected_room", viewLifecycleOwner
        ) { _, bundle ->
            binding.tvTitle.text = "Editar Sala"
            binding.etName.setText(bundle.getString("name"))
            binding.etTheme.setText(bundle.getString("theme"))
            binding.etCapacity.setText(bundle.getInt("capacity").toString())
            val diffIndex = difficulties.indexOf(bundle.getString("difficulty"))
            if (diffIndex >= 0) binding.spinnerDifficulty.setSelection(diffIndex)
        }

        binding.btnSave.setOnClickListener { saveRoom() }

        viewModel.operationResult.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Success -> {
                    Snackbar.make(binding.root, "Sala guardada", Snackbar.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is Resource.Error -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun setupSpinner() {
        val displayNames = listOf("Baja", "Media", "Alta", "Extrema")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, displayNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDifficulty.adapter = adapter
    }

    private fun saveRoom() {
        val name = binding.etName.text.toString().trim()
        val theme = binding.etTheme.text.toString().trim()
        val capacityStr = binding.etCapacity.text.toString().trim()
        val difficulty = difficulties[binding.spinnerDifficulty.selectedItemPosition]

        if (name.isEmpty() || theme.isEmpty() || capacityStr.isEmpty()) {
            Snackbar.make(binding.root, "Completa todos los campos", Snackbar.LENGTH_SHORT).show()
            return
        }

        val capacity = capacityStr.toIntOrNull() ?: run {
            Snackbar.make(binding.root, "Capacidad inválida", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (editingRoomId != -1) {
            viewModel.updateRoom(editingRoomId, name, theme, capacity, difficulty)
        } else {
            viewModel.createRoom(name, theme, capacity, difficulty)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}