package com.javiermontillaarias.escapemanager.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.javiermontillaarias.escapemanager.data.model.Booking
import com.javiermontillaarias.escapemanager.databinding.ItemBookingBinding

class BookingsAdapter(
    private val onClick: (Booking) -> Unit
) : ListAdapter<Booking, BookingsAdapter.ViewHolder>(BookingDiffCallback()) {

    inner class ViewHolder(private val binding: ItemBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.tvGroupName.text = booking.groupName
            binding.tvRoomName.text = "${booking.sala?.name ?: "Sala #${booking.roomId}"}"
            binding.tvDateTime.text = "${booking.fecha} · ${booking.hora}"
            binding.tvNumPeople.text = "${booking.numPeople} personas"
            binding.tvStatus.text = booking.estado.replaceFirstChar { it.uppercase() }

            val statusColor = when (booking.estado) {
                "pendiente" -> "#F57C00"
                "confirmada" -> "#2E75B6"
                "en_curso" -> "#2E7D32"
                "completada" -> "#6B7280"
                "cancelada" -> "#D32F2F"
                else -> "#6B7280"
            }
            binding.tvStatus.setBackgroundColor(Color.parseColor(statusColor))
            binding.root.setOnClickListener { onClick(booking) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BookingDiffCallback : DiffUtil.ItemCallback<Booking>() {
        override fun areItemsTheSame(oldItem: Booking, newItem: Booking) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Booking, newItem: Booking) = oldItem == newItem
    }
}