package com.javiermontillaarias.escapemanager.ui.gamemaster.activegame

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
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.GameRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentActiveGameBinding
import com.javiermontillaarias.escapemanager.util.Resource
import com.javiermontillaarias.escapemanager.util.appSessionManager
import com.google.android.material.snackbar.Snackbar

class ActiveGameFragment : Fragment() {

    private var _binding: FragmentActiveGameBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ActiveGameViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val api = RetrofitClient.getApiService(appSessionManager)
                @Suppress("UNCHECKED_CAST")
                return ActiveGameViewModel(GameRepository(api)) as T
            }
        }
    }

    private var gameId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActiveGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gameId = arguments?.getInt("gameId", -1) ?: -1

        if (gameId == -1) {
            Snackbar.make(binding.root, "Error: partida no válida", Snackbar.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }

        val groupName = arguments?.getString("groupName") ?: "Grupo"
        val roomName = arguments?.getString("roomName") ?: ""
        val startTime = arguments?.getString("startTime")

        binding.tvGroupName.text = groupName
        binding.tvRoomName.text = roomName

        // I-07: pasar la hora de inicio real para que el timer refleje tiempo transcurrido real
        viewModel.startTimer(startTime)

        viewModel.elapsedSeconds.observe(viewLifecycleOwner) { seconds ->
            binding.tvTimer.text = formatTime(seconds)
        }

        // BUG-01: deshabilitar btnAddHint durante Loading para evitar doble-tap
        viewModel.hintsResult.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> binding.btnAddHint.isEnabled = false
                is Resource.Success -> {
                    binding.btnAddHint.isEnabled = true
                    binding.tvHints.text = state.data.hintsUsed.toString()
                }
                is Resource.Error -> {
                    binding.btnAddHint.isEnabled = true
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }

        viewModel.closeResult.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    showGameEndDialog(state.data.escaparon == true)
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> {}
            }
        }

        binding.btnAddHint.setOnClickListener {
            if (gameId != -1) viewModel.addHint(gameId)
        }

        binding.btnEscaped.setOnClickListener { confirmClose(true) }
        binding.btnNotEscaped.setOnClickListener { confirmClose(false) }
    }

    private fun confirmClose(escaped: Boolean) {
        val obs = binding.etObservations.text.toString().trim().ifEmpty { null }
        AlertDialog.Builder(requireContext())
            .setTitle(if (escaped) "¡Escaparon!" else "No escaparon")
            .setMessage("¿Confirmas el cierre de la partida?")
            .setPositiveButton("Confirmar") { _, _ ->
                if (gameId != -1) viewModel.closeGame(gameId, escaped, obs)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showGameEndDialog(escaped: Boolean) {
        AlertDialog.Builder(requireContext())
            .setTitle(if (escaped) "¡Partida cerrada!" else "Partida cerrada")
            .setMessage(if (escaped) "El grupo escapó con éxito." else "El grupo no consiguió escapar.")
            .setPositiveButton("Volver al dashboard") { _, _ ->
                findNavController().popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    private fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopTimer()
        _binding = null
    }
}