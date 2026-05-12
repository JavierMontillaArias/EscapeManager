package com.javiermontillaarias.escapemanager.ui.manager.stats

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.javiermontillaarias.escapemanager.data.local.SessionManager
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.StatsRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentStatsBinding
import com.javiermontillaarias.escapemanager.util.Resource
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val api = RetrofitClient.getApiService(SessionManager(requireContext()))
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

        viewModel.escapeRate.observe(viewLifecycleOwner) { state ->
            if (state is Resource.Success && state.data.isNotEmpty()) {
                val data = state.data
                val entries = data.mapIndexed { i, it -> BarEntry(i.toFloat(), it.rate.toFloat()) }
                val labels = data.map { it.room ?: "Sin nombre" }
                val dataSet = BarDataSet(entries, "Tasa de escape (%)").apply {
                    color = Color.parseColor("#2E75B6")
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

        viewModel.hintsAvg.observe(viewLifecycleOwner) { state ->
            if (state is Resource.Success && state.data.isNotEmpty()) {
                val data = state.data
                val entries = data.mapIndexed { i, it -> BarEntry(i.toFloat(), it.average.toFloat()) }
                val labels = data.map { it.room ?: "Sin nombre" }
                val dataSet = BarDataSet(entries, "Promedio pistas").apply {
                    color = Color.parseColor("#1F4E79")
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

        viewModel.ranking.observe(viewLifecycleOwner) { state ->
            if (state is Resource.Success) {
                binding.rankingContainer.removeAllViews()
                state.data.forEachIndexed { index, entry ->
                    val tv = TextView(requireContext()).apply {
                        text = "${index + 1}. ${entry.room} — ${entry.totalGames} partidas"
                        textSize = 14f
                        setTextColor(Color.parseColor("#1A1A2E"))
                        setPadding(0, 8, 0, 8)
                    }
                    binding.rankingContainer.addView(tv)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}