package com.javiermontillaarias.escapemanager.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.javiermontillaarias.escapemanager.R
import com.javiermontillaarias.escapemanager.data.model.Booking
import com.javiermontillaarias.escapemanager.databinding.ItemBookingBinding
import com.javiermontillaarias.escapemanager.util.EstadoReserva

class BookingsAdapter(
    private val onClick: (Booking) -> Unit
) : ListAdapter<Booking, BookingsAdapter.ViewHolder>(BookingDiffCallback()) {

    inner class ViewHolder(private val binding: ItemBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.tvGroupName.text = booking.groupName
            binding.tvRoomName.text = booking.sala?.name ?: "Sala #${booking.roomId}"
            // I-01: horaInicio (campo correcto) en lugar del campo inexistente 'hora'
            binding.tvDateTime.text = "${booking.fecha} · ${booking.horaInicio}"
            binding.tvNumPeople.text = "${booking.numPeople} personas"
            applyStatus(booking.estado)
            binding.root.setOnClickListener { onClick(booking) }
        }

        fun bindPartial(booking: Booking, changed: Set<String>) {
            if ("estado" in changed) applyStatus(booking.estado)
            if ("datetime" in changed) binding.tvDateTime.text = "${booking.fecha} · ${booking.horaInicio}"
            if ("numPeople" in changed) binding.tvNumPeople.text = "${booking.numPeople} personas"
        }

        // CAL-05: usar constantes EstadoReserva en lugar de strings literales
        private fun applyStatus(estado: String) {
            binding.tvStatus.text = estado.replaceFirstChar { it.uppercase() }
            // Q-06: colores desde recursos en lugar de literales hex
            val color = when (estado) {
                EstadoReserva.PENDIENTE  -> ContextCompat.getColor(binding.root.context, R.color.status_pending)
                EstadoReserva.CONFIRMADA -> ContextCompat.getColor(binding.root.context, R.color.status_confirmed)
                EstadoReserva.EN_CURSO   -> ContextCompat.getColor(binding.root.context, R.color.status_active)
                EstadoReserva.COMPLETADA -> ContextCompat.getColor(binding.root.context, R.color.status_completed)
                EstadoReserva.CANCELADA  -> ContextCompat.getColor(binding.root.context, R.color.status_cancelled)
                else                     -> ContextCompat.getColor(binding.root.context, R.color.status_completed)
            }
            binding.tvStatus.setBackgroundColor(color)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            holder.bind(getItem(position))
            return
        }
        @Suppress("UNCHECKED_CAST")
        val changed = payloads.flatMap { it as Set<String> }.toSet()
        holder.bindPartial(getItem(position), changed)
    }

    class BookingDiffCallback : DiffUtil.ItemCallback<Booking>() {
        override fun areItemsTheSame(oldItem: Booking, newItem: Booking) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Booking, newItem: Booking) = oldItem == newItem

        override fun getChangePayload(oldItem: Booking, newItem: Booking): Any? {
            val changes = mutableSetOf<String>()
            if (oldItem.estado != newItem.estado) changes.add("estado")
            if (oldItem.horaInicio != newItem.horaInicio || oldItem.fecha != newItem.fecha) changes.add("datetime")
            if (oldItem.numPeople != newItem.numPeople) changes.add("numPeople")
            return if (changes.isEmpty()) null else changes
        }
    }
}
