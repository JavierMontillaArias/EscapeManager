package com.javiermontillaarias.escapemanager.ui.manager.bookings

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
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.javiermontillaarias.escapemanager.R
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.BookingRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentBookingsBinding
import com.javiermontillaarias.escapemanager.ui.adapters.BookingsAdapter
import com.javiermontillaarias.escapemanager.util.Resource
import com.javiermontillaarias.escapemanager.util.appSessionManager
import androidx.fragment.app.setFragmentResultListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookingsFragment : Fragment() {

    private var _binding: FragmentBookingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookingsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val api = RetrofitClient.getApiService(appSessionManager)
                @Suppress("UNCHECKED_CAST")
                return BookingsViewModel(BookingRepository(api)) as T
            }
        }
    }

    private lateinit var adapter: BookingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BookingsAdapter { booking ->
            val bundle = Bundle().apply { putInt("bookingId", booking.id) }
            findNavController().navigate(R.id.bookingDetailFragment, bundle)
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        setFragmentResultListener("booking_created") { _, _ ->
            viewModel.loadBookings()
        }

        viewModel.bookings.observe(viewLifecycleOwner) { state ->
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

        // BUG-04: etDateFilter no es editable — abre MaterialDatePicker al tocarlo
        binding.etDateFilter.isFocusable = false
        binding.etDateFilter.isClickable = true
        binding.etDateFilter.setOnClickListener { showDatePicker() }

        binding.btnFilter.setOnClickListener {
            val date = binding.etDateFilter.text.toString().trim()
            if (date.isEmpty()) {
                Snackbar.make(binding.root, "Selecciona una fecha primero", Snackbar.LENGTH_SHORT).show()
            } else {
                viewModel.loadBookings(date)
            }
        }

        binding.btnClearFilter.setOnClickListener {
            binding.etDateFilter.text?.clear()
            viewModel.loadBookings(null)
        }

        // P-1: force=true en swipe manual para ignorar el stale check
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadBookings(force = true) }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.createBookingFragment)
        }
    }

    // BUG-04: sin DateValidatorPointForward — el manager puede filtrar por fechas pasadas
    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Filtrar por fecha")
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            binding.etDateFilter.setText(sdf.format(Date(selection)))
        }
        picker.show(parentFragmentManager, "date_picker_filter")
    }

    override fun onResume() {
        super.onResume()
        // BUG-07: sincronizar el campo de texto con el filtro activo del ViewModel
        // para evitar inconsistencia visual si el usuario borró el texto manualmente
        // sin pulsar "Limpiar filtro" antes de navegar al detalle.
        val filterText = viewModel.currentFilter ?: ""
        if (binding.etDateFilter.text.toString() != filterText) {
            binding.etDateFilter.setText(filterText)
        }
        viewModel.loadBookings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
