package com.javiermontillaarias.escapemanager.ui.gamemaster.dashboard

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.javiermontillaarias.escapemanager.data.model.Game
import com.javiermontillaarias.escapemanager.databinding.ItemBookingBinding

class GmGamesAdapter(private val games: List<Game>) :
    RecyclerView.Adapter<GmGamesAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(game: Game) {
            binding.tvGroupName.text = "Partida #${game.id}"
            binding.tvRoomName.text = "${game.booking?.room?.name ?: "Sala"}"
            binding.tvDateTime.text = "Inicio: ${game.startTime?.take(16) ?: "-"}"
            binding.tvNumPeople.text = "Pistas: ${game.hintsUsed}"
            val statusText = when {
                game.endTime != null -> if (game.escaped == true) "Escaparon" else "No escaparon"
                else -> "En curso"
            }
            binding.tvStatus.text = statusText
            val color = if (game.endTime != null) "#6B7280" else "#2E7D32"
            binding.tvStatus.setBackgroundColor(Color.parseColor(color))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(games[position])
    override fun getItemCount() = games.size
}