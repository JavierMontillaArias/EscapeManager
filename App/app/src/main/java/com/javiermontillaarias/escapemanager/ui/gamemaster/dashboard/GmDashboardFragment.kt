package com.javiermontillaarias.escapemanager.ui.gamemaster.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.javiermontillaarias.escapemanager.data.local.SessionManager
import com.javiermontillaarias.escapemanager.data.model.Game
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.GameRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentGmDashboardBinding
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.launch

class GmDashboardFragment : Fragment() {

    private var _binding: FragmentGmDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel by lazy {
        val api = RetrofitClient.getApiService(SessionManager(requireContext()))
        GmDashboardViewModel(GameRepository(api))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGmDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sm = SessionManager(requireContext())
        binding.tvWelcome.text = "Hola, ${sm.userName ?: "Game Master"}"

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

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
                        // Simple text list for GM games
                        binding.recyclerView.adapter = GmGamesAdapter(games)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                }
                else -> {}
            }
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadGames() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class GmDashboardViewModel(private val repository: GameRepository) : ViewModel() {
    private val _games = MutableLiveData<Resource<List<Game>>>()
    val games: LiveData<Resource<List<Game>>> = _games

    init { loadGames() }

    fun loadGames() {
        viewModelScope.launch {
            _games.value = Resource.Loading
            _games.value = repository.getGames()
        }
    }
}