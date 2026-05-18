package com.javiermontillaarias.escapemanager.ui.manager.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.StatsRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentManagerDashboardBinding
import com.javiermontillaarias.escapemanager.util.Resource
import com.javiermontillaarias.escapemanager.util.appSessionManager

class ManagerDashboardFragment : Fragment() {

    private var _binding: FragmentManagerDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ManagerDashboardViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val api = RetrofitClient.getApiService(appSessionManager)
                @Suppress("UNCHECKED_CAST")
                return ManagerDashboardViewModel(StatsRepository(api)) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagerDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvWelcome.text = "Bienvenido, ${appSessionManager.userName ?: "Manager"}"

        viewModel.summary.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Idle -> Unit
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvError.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val data = state.data
                    binding.tvTotalBookings.text    = data.totalBookings.toString()
                    binding.tvActiveGames.text      = data.activeGames.toString()
                    binding.tvTotalRooms.text       = data.totalRooms.toString()
                    binding.tvPendingIncidents.text = data.pendingIncidents.toString()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.text = state.message
                    binding.tvError.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSummary()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
