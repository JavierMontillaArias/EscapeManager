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
            binding.tvRoomName.text = "GM: ${game.gamemaster?.name ?: "Sin asignar"}"
            binding.tvDateTime.text = "Inicio: ${game.startTime?.take(16)?.replace("T", " ") ?: "-"}"
            binding.tvNumPeople.text = "Pistas: ${game.hintsUsed}"

            val statusText = when {
                game.endTime != null && game.escaparon == true -> "Escaparon ✅"
                game.endTime != null && game.escaparon == false -> "No escaparon ❌"
                game.endTime != null -> "Completada"
                else -> "En curso"
            }
            binding.tvStatus.text = statusText

            val color = when {
                game.endTime != null && game.escaparon == true -> "#2E7D32"
                game.endTime != null && game.escaparon == false -> "#D32F2F"
                game.endTime != null -> "#6B7280"
                else -> "#2E75B6"
            }
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