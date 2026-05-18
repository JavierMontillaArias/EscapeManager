package com.javiermontillaarias.escapemanager.ui.manager.bookings

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
import com.google.android.material.snackbar.Snackbar
import com.javiermontillaarias.escapemanager.EscapeManagerApp
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.BookingRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentBookingDetailBinding
import com.javiermontillaarias.escapemanager.util.EstadoReserva
import com.javiermontillaarias.escapemanager.util.Resource

class BookingDetailFragment : Fragment() {

    private var _binding: FragmentBookingDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookingDetailViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // B-03: singleton SessionManager desde la Application
                val sm = (requireActivity().application as EscapeManagerApp).sessionManager
                val api = RetrofitClient.getApiService(sm)
                @Suppress("UNCHECKED_CAST")
                return BookingDetailViewModel(BookingRepository(api)) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        val bookingId = arguments?.getInt("bookingId", -1) ?: -1

        // B-06: manejar navegación inválida en lugar de dejar pantalla en blanco
        if (bookingId == -1) {
            Snackbar.make(binding.root, "Error: reserva no válida", Snackbar.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }

        if (viewModel.booking.value == null || viewModel.booking.value is Resource.Idle || viewModel.booking.value is Resource.Error) {
            viewModel.load(bookingId)
        }

        viewModel.booking.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.tvGroupName.text = "Cargando..."
                }
                is Resource.Success -> {
                    val b = state.data
                    binding.tvBookingId.text = "Reserva #${b.id}"
                    binding.tvGroupName.text = b.groupName
                    binding.tvDetails.text =
                        "${b.sala?.name ?: "Sala #${b.roomId}"}\n" +
                                "${b.fecha}\n" +
                                "${b.horaInicio} → ${b.horaFin}\n" +
                                "${b.numPeople} personas\n" +
                                b.email
                    binding.tvStatus.text = "Estado: ${b.estado.replaceFirstChar { it.uppercase() }}"
                    val qrSent = if (b.qrEmailEnviado) "QR enviado por email" else "QR pendiente de envío"
                    binding.tvQrToken.text = b.qrToken?.let { "QR: ...${it.takeLast(4)}\n$qrSent" } ?: "Sin QR"
                    // CAL-05: usar constantes EstadoReserva en lugar de strings literales
                    val accionable = b.estado == EstadoReserva.PENDIENTE || b.estado == EstadoReserva.CONFIRMADA
                    binding.layoutStatusButtons.visibility = if (accionable) View.VISIBLE else View.GONE
                    binding.btnResendQr.visibility = if (accionable) View.VISIBLE else View.GONE
                    // B-8: visibilidad explícita de btnCancelBooking según estado accionable
                    binding.btnCancelBooking.visibility = if (accionable) View.VISIBLE else View.GONE
                }
                is Resource.Error -> {
                    binding.tvGroupName.text = state.message
                }
                else -> {}
            }
        }

        binding.btnResendQr.setOnClickListener { viewModel.resendQr(bookingId) }

        // B-2: diálogo de confirmación antes de cancelar — acción irreversible
        binding.btnCancelBooking.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Cancelar reserva")
                .setMessage("Esta acción no se puede deshacer. ¿Cancelar la reserva?")
                .setPositiveButton("Cancelar reserva") { _, _ -> viewModel.cancelBooking(bookingId) }
                .setNegativeButton("Volver", null)
                .show()
        }

        // INC-04: cancelación vía DELETE — navegar atrás si tiene éxito
        // BUG-06: deshabilitar btnResendQr también durante la cancelación
        viewModel.cancelResult.observe(viewLifecycleOwner) { state ->
            val inProgress = state is Resource.Loading
            binding.btnCancelBooking.isEnabled = !inProgress
            binding.btnResendQr.isEnabled = !inProgress
            when (state) {
                is Resource.Success -> {
                    Snackbar.make(binding.root, "Reserva cancelada", Snackbar.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is Resource.Error ->
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                else -> {}
            }
        }

        viewModel.resendResult.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> binding.btnResendQr.isEnabled = false
                is Resource.Success -> {
                    binding.btnResendQr.isEnabled = true
                    Snackbar.make(binding.root, state.data.message, Snackbar.LENGTH_SHORT).show()
                    viewModel.load(bookingId)
                }
                is Resource.Error -> {
                    binding.btnResendQr.isEnabled = true
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> binding.btnResendQr.isEnabled = true
            }
        }
    }

    // BUG-02: reintentar carga automáticamente al volver al Fragment si hubo error
    override fun onResume() {
        super.onResume()
        if (viewModel.booking.value is Resource.Error) {
            val bookingId = arguments?.getInt("bookingId", -1) ?: -1
            if (bookingId != -1) viewModel.load(bookingId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
