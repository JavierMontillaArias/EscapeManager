package com.javiermontillaarias.escapemanager.ui.manager.incidents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.IncidentRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentIncidentsBinding
import com.javiermontillaarias.escapemanager.ui.adapters.IncidentsAdapter
import com.javiermontillaarias.escapemanager.util.Resource
import com.javiermontillaarias.escapemanager.data.model.Incident
import com.javiermontillaarias.escapemanager.util.appSessionManager
import com.google.android.material.snackbar.Snackbar

class IncidentsFragment : Fragment() {

    private var _binding: FragmentIncidentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncidentsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val api = RetrofitClient.getApiService(appSessionManager)
                @Suppress("UNCHECKED_CAST")
                return IncidentsViewModel(IncidentRepository(api)) as T
            }
        }
    }

    private lateinit var adapter: IncidentsAdapter
    private var showOnlyPending = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncidentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // BUG-05: restaurar el estado del filtro antes de configurar el listener,
        // evita que el chip visualmente activo tras rotación muestre todos los registros.
        showOnlyPending = savedInstanceState?.getBoolean("showOnlyPending") ?: false
        binding.chipPendientes.isChecked = showOnlyPending

        adapter = IncidentsAdapter { incident ->
            viewModel.resolve(incident.id)
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.chipPendientes.setOnCheckedChangeListener { _, isChecked ->
            showOnlyPending = isChecked
            applyFilter((viewModel.incidents.value as? Resource.Success)?.data)
        }

        viewModel.incidents.observe(viewLifecycleOwner) { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                is Resource.Idle -> Unit
                is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                    applyFilter(state.data)
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.text = state.message
                    binding.tvError.visibility = View.VISIBLE
                }
            }
        }

        viewModel.resolveResult.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Success ->
                    Snackbar.make(binding.root, "Incidencia resuelta", Snackbar.LENGTH_SHORT).show()
                is Resource.Error ->
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                else -> Unit
            }
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadIncidents() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("showOnlyPending", showOnlyPending)
    }

    private fun applyFilter(data: List<Incident>?) {
        val list = if (showOnlyPending) data?.filter { !it.resuelta } else data
        adapter.submitList(list ?: emptyList())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
