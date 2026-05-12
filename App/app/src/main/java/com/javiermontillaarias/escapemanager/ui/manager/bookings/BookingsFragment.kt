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
import com.javiermontillaarias.escapemanager.R
import com.javiermontillaarias.escapemanager.data.local.SessionManager
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.BookingRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentBookingsBinding
import com.javiermontillaarias.escapemanager.ui.adapters.BookingsAdapter
import com.javiermontillaarias.escapemanager.util.Resource
import com.google.android.material.snackbar.Snackbar

class BookingsFragment : Fragment() {

    private var _binding: FragmentBookingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookingsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val api = RetrofitClient.getApiService(SessionManager(requireContext()))
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

        viewModel.bookings.observe(viewLifecycleOwner) { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
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

        binding.btnFilter.setOnClickListener {
            val date = binding.etDateFilter.text.toString().trim()
            if (date.isNotEmpty()) viewModel.loadBookings(date)
            else Snackbar.make(binding.root, "Introduce una fecha", Snackbar.LENGTH_SHORT).show()
        }

        binding.btnClearFilter.setOnClickListener {
            binding.etDateFilter.text?.clear()
            viewModel.loadBookings(null)
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadBookings() }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.createBookingFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}