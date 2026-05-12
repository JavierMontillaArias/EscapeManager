package com.javiermontillaarias.escapemanager.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.javiermontillaarias.escapemanager.data.model.Incident
import com.javiermontillaarias.escapemanager.databinding.ItemIncidentBinding

class IncidentsAdapter(
    private val onResolve: (Incident) -> Unit
) : ListAdapter<Incident, IncidentsAdapter.ViewHolder>(IncidentDiffCallback()) {

    inner class ViewHolder(private val binding: ItemIncidentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(incident: Incident) {
            binding.tvRoomName.text = "${incident.room?.name ?: "Sala #${incident.roomId}"}"
            binding.tvDescription.text = incident.description
            binding.tvDate.text = incident.createdAt?.take(10) ?: ""

            if (incident.resolved) {
                binding.tvStatus.text = "Resuelta"
                binding.tvStatus.setBackgroundColor(Color.parseColor("#2E7D32"))
                binding.btnResolve.visibility = View.GONE
            } else {
                binding.tvStatus.text = "Pendiente"
                binding.tvStatus.setBackgroundColor(Color.parseColor("#F57C00"))
                binding.btnResolve.visibility = View.VISIBLE
                binding.btnResolve.setOnClickListener { onResolve(incident) }
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