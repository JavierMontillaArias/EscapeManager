package com.javiermontillaarias.escapemanager.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.javiermontillaarias.escapemanager.R
import com.javiermontillaarias.escapemanager.data.model.Incident
import com.javiermontillaarias.escapemanager.databinding.ItemIncidentBinding

class IncidentsAdapter(private val onResolve: (Incident) -> Unit) : ListAdapter<Incident, IncidentsAdapter.ViewHolder>(IncidentDiffCallback()) {

    inner class ViewHolder(private val binding: ItemIncidentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(incident: Incident) {
            binding.tvRoomName.text = incident.sala?.nombre ?: "Sala #${incident.roomId}"
            binding.tvDescription.text = incident.descripcion
            binding.tvDate.text = incident.createdAt?.take(10) ?: ""

            // Q-06: colores desde recursos en lugar de literales hex
            if (incident.resuelta) {
                binding.tvStatus.text = "Resuelta"
                binding.tvStatus.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.status_active)
                )
                binding.btnResolve.visibility = View.GONE
            } else {
                binding.tvStatus.text = "Pendiente"
                binding.tvStatus.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.status_pending)
                )
                binding.btnResolve.visibility = View.VISIBLE
                binding.btnResolve.isEnabled = true
                binding.btnResolve.setOnClickListener {
                    binding.btnResolve.isEnabled = false
                    onResolve(incident)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIncidentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class IncidentDiffCallback : DiffUtil.ItemCallback<Incident>() {
        override fun areItemsTheSame(oldItem: Incident, newItem: Incident) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Incident, newItem: Incident) = oldItem == newItem
    }
}
