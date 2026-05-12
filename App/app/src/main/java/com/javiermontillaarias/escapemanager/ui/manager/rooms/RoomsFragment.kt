package com.javiermontillaarias.escapemanager.ui.manager.rooms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.javiermontillaarias.escapemanager.R
import com.javiermontillaarias.escapemanager.data.local.SessionManager
import com.javiermontillaarias.escapemanager.data.model.Room
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.RoomRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentRoomsBinding
import com.javiermontillaarias.escapemanager.ui.adapters.RoomsAdapter
import com.javiermontillaarias.escapemanager.util.Resource
import com.google.android.material.snackbar.Snackbar

class RoomsFragment : Fragment() {

    private var _binding: FragmentRoomsBinding? = null
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

    private lateinit var adapter: RoomsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoomsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = RoomsAdapter(
            onEdit = { room ->
                // Navegar al fragment de edición pasando el ID de la sala
                val bundle = Bundle().apply { putInt("roomId", room.id) }
                // Guardamos la sala seleccionada para usarla en el fragment
                requireActivity().supportFragmentManager.setFragmentResult(
                    "selected_room", Bundle().apply {
                        putInt("id", room.id)
                        putString("name", room.name)
                        putString("theme", room.theme)
                        putInt("capacity", room.capacity)
                        putString("difficulty", room.difficulty)
                    }
                )
                findNavController().navigate(R.id.createEditRoomFragment, bundle)
            },
            onDelete = { room -> showDeleteDialog(room) }
        )
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeViewModel() {
        viewModel.rooms.observe(viewLifecycleOwner) { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                is Resource.Idle -> Unit
                is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                    adapter.submitList(state.data)
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.text = state.message
                    binding.tvError.visibility = View.VISIBLE
                }
            }
        }

        viewModel.operationResult.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Success ->
                    Snackbar.make(binding.root, "Operación realizada", Snackbar.LENGTH_SHORT).show()
                is Resource.Error ->
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    private fun setupListeners() {
        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.createEditRoomFragment)
        }
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadRooms() }
    }

    private fun showDeleteDialog(room: Room) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar sala")
            .setMessage("¿Eliminar \"${room.name}\"? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ -> viewModel.deleteRoom(room.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}