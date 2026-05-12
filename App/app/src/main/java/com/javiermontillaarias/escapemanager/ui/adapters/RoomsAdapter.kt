package com.javiermontillaarias.escapemanager.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.javiermontillaarias.escapemanager.data.model.Room
import com.javiermontillaarias.escapemanager.databinding.ItemRoomBinding

class RoomsAdapter(
    private val onEdit: (Room) -> Unit,
    private val onDelete: (Room) -> Unit
) : ListAdapter<Room, RoomsAdapter.ViewHolder>(RoomDiffCallback()) {

    inner class ViewHolder(private val binding: ItemRoomBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(room: Room) {
            binding.tvRoomName.text = room.name
            binding.tvRoomTheme.text = "${room.theme}"
            binding.tvCapacity.text = "${room.capacity} personas"
            binding.tvDifficulty.text = room.difficulty.replaceFirstChar { it.uppercase() }
            binding.btnEdit.setOnClickListener { onEdit(room) }
            binding.btnDelete.setOnClickListener { onDelete(room) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RoomDiffCallback : DiffUtil.ItemCallback<Room>() {
        override fun areItemsTheSame(oldItem: Room, newItem: Room) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Room, newItem: Room) = oldItem == newItem
    }
}