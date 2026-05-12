package com.javiermontillaarias.escapemanager.ui.manager.bookings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.javiermontillaarias.escapemanager.databinding.FragmentBookingDetailBinding

class BookingDetailFragment : Fragment() {

    private var _binding: FragmentBookingDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookingId = arguments?.getInt("bookingId", -1) ?: -1

        binding.tvBookingId.text = "Reserva #$bookingId"
        binding.tvGroupName.text = "Cargando datos..."
        binding.tvDetails.text = ""
        binding.tvQrToken.text = "QR no disponible"

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}