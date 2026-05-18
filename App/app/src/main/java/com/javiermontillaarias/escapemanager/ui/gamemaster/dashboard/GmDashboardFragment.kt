package com.javiermontillaarias.escapemanager.ui.gamemaster.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.javiermontillaarias.escapemanager.EscapeManagerApp
import com.javiermontillaarias.escapemanager.R
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.GameRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentGmDashboardBinding
import com.javiermontillaarias.escapemanager.util.Resource

class GmDashboardFragment : Fragment() {

    private var _binding: FragmentGmDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GmDashboardViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val sm = (requireActivity().application as EscapeManagerApp).sessionManager
                val api = RetrofitClient.getApiService(sm)
                @Suppress("UNCHECKED_CAST")
                return GmDashboardViewModel(GameRepository(api)) as T
            }
        }
    }

    private lateinit var gamesAdapter: GmGamesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGmDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sm = (requireActivity().application as EscapeManagerApp).sessionManager
        binding.tvWelcome.text = getString(R.string.welcome_gm, sm.userName ?: "Game Master")

        gamesAdapter = GmGamesAdapter { game ->
            val bundle = Bundle().apply {
                putInt("gameId", game.id)
                putString("groupName", "Partida #${game.id}")
                putString("roomName", "")
                putString("startTime", game.startTime)
            }
            findNavController().navigate(R.id.activeGameFragment, bundle)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = gamesAdapter

        viewModel.games.observe(viewLifecycleOwner) { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val games = state.data
                    if (games.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        gamesAdapter.submitList(games)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> {}
            }
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadGames() }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadGamesIfStale()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}