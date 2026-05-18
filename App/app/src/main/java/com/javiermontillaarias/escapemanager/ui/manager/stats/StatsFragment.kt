package com.javiermontillaarias.escapemanager.ui.manager.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.snackbar.Snackbar
import com.javiermontillaarias.escapemanager.R
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.StatsRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentStatsBinding
import com.javiermontillaarias.escapemanager.util.Resource
import com.javiermontillaarias.escapemanager.util.appSessionManager

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val api = RetrofitClient.getApiService(appSessionManager)
                @Suppress("UNCHECKED_CAST")
                return StatsViewModel(StatsRepository(api)) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadAll() }

        viewModel.summary.observe(viewLifecycleOwner) { state ->
            if (state !is Resource.Loading) binding.swipeRefresh.isRefreshing = false
            if (state is Resource.Success) {
                val d = state.data
                binding.tvStatsTotalBookings.text = "Reservas totales: ${d.totalBookings}"
                binding.tvStatsEscapeRate.text = "Tasa escape global: ${"%.1f".format(d.escapeRate)}%"
                binding.tvStatsTopRoom.text = "Sala más popular: ${d.topRoom ?: "—"}"
                binding.tvStatsPendingIncidents.text = "Incidencias pendientes: ${d.pendingIncidents}"
            }
        }

        viewModel.escapeRate.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> { /* carga cubierta por el estado global */ }
                is Resource.Success -> {
                    if (state.data.isNotEmpty()) {
                        val data = state.data
                        val entries = data.mapIndexed { i, it -> BarEntry(i.toFloat(), it.rate.toFloat()) }
                        val labels = data.map { it.room ?: "Sin nombre" }
                        val dataSet = BarDataSet(entries, "Tasa de escape (%)").apply {
                            color = ContextCompat.getColor(requireContext(), R.color.secondary)
                            valueTextSize = 10f
                        }
                        binding.chartEscapeRate.apply {
                            this.data = BarData(dataSet)
                            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                            xAxis.granularity = 1f
                            xAxis.setDrawGridLines(false)
                            xAxis.labelRotationAngle = -30f
                            description.isEnabled = false
                            legend.isEnabled = false
                            setFitBars(true)
                            animateY(1000)
                            invalidate()
                        }
                    }
                }
                is Resource.Error -> Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                else -> {}
            }
        }

        viewModel.hintsAvg.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Success -> {
                    if (state.data.isNotEmpty()) {
                        val data = state.data
                        val entries = data.mapIndexed { i, it -> BarEntry(i.toFloat(), it.average.toFloat()) }
                        val labels = data.map { it.room ?: "Sin nombre" }
                        val dataSet = BarDataSet(entries, "Promedio pistas").apply {
                            color = ContextCompat.getColor(requireContext(), R.color.primary)
                            valueTextSize = 10f
                        }
                        binding.chartHintsAvg.apply {
                            this.data = BarData(dataSet)
                            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                            xAxis.granularity = 1f
                            xAxis.setDrawGridLines(false)
                            description.isEnabled = false
                            legend.isEnabled = false
                            setFitBars(true)
                            animateY(1000)
                            invalidate()
                        }
                    }
                }
                is Resource.Error -> Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                else -> {}
            }
        }

        viewModel.occupancy.observe(viewLifecycleOwner) { state ->
            if (state !is Resource.Loading) binding.swipeRefresh.isRefreshing = false
            when (state) {
                is Resource.Success -> {
                    if (state.data.isNotEmpty()) {
                        val data = state.data
                        val entries = data.mapIndexed { i, it -> BarEntry(i.toFloat(), it.occupancy.toFloat()) }
                        val labels = data.map { it.franja }
                        val dataSet = BarDataSet(entries, "Ocupación (%)").apply {
                            color = ContextCompat.getColor(requireContext(), R.color.secondary)
                            valueTextSize = 10f
                        }
                        binding.chartOccupancy.apply {
                            this.data = BarData(dataSet)
                            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                            xAxis.granularity = 1f
                            xAxis.setDrawGridLines(false)
                            xAxis.labelRotationAngle = -30f
                            description.isEnabled = false
                            legend.isEnabled = false
                            setFitBars(true)
                            animateY(1000)
                            invalidate()
                        }
                    }
                }
                is Resource.Error -> Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                else -> {}
            }
        }

        viewModel.ranking.observe(viewLifecycleOwner) { state ->
            if (state !is Resource.Loading) binding.swipeRefresh.isRefreshing = false
            when (state) {
                is Resource.Success -> {
                    binding.rankingContainer.removeAllViews()
                    // C-1: mostrar score y tasaEscape además de nombre y partidas
                    state.data.forEachIndexed { index, entry ->
                        val tv = TextView(requireContext()).apply {
                            text = "${index + 1}. ${entry.room} — ${entry.totalGames} partidas" +
                                   " · ${"%.1f".format(entry.tasaEscape)}% escape" +
                                   " · score ${"%.1f".format(entry.score)}"
                            textSize = 14f
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                            setPadding(0, 8, 0, 8)
                        }
                        binding.rankingContainer.addView(tv)
                    }
                }
                is Resource.Error -> Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadAllIfStale()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
