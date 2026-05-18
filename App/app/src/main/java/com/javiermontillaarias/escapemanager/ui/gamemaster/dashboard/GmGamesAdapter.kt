package com.javiermontillaarias.escapemanager.ui.gamemaster.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.javiermontillaarias.escapemanager.R
import com.javiermontillaarias.escapemanager.data.model.Game
import com.javiermontillaarias.escapemanager.databinding.ItemGameBinding

// B-07: usa ItemGameBinding (layout dedicado) en lugar de reutilizar ItemBookingBinding
class GmGamesAdapter : ListAdapter<Game, GmGamesAdapter.ViewHolder>(GameDiffCallback()) {

    class GameDiffCallback : DiffUtil.ItemCallback<Game>() {
        override fun areItemsTheSame(oldItem: Game, newItem: Game) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Game, newItem: Game) = oldItem == newItem
    }

    inner class ViewHolder(private val binding: ItemGameBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(game: Game) {
            binding.tvGameTitle.text  = "Partida #${game.id}"
            binding.tvGamemaster.text = "GM: ${game.gamemaster.name}"
            binding.tvStartTime.text  = "Inicio: ${game.startTime?.take(16)?.replace("T", " ") ?: "-"}"
            binding.tvHints.text      = "Pistas: ${game.hintsUsed}"

            val statusText = when {
                game.endTime != null && game.escaparon == true  -> "Escaparon"
                game.endTime != null && game.escaparon == false -> "No escaparon"
                game.endTime != null -> "Completada"
                else -> "En curso"
            }
            binding.tvStatus.text = statusText

            // Q-06: colores desde recursos en lugar de literales hex
            val colorRes = when {
                game.endTime != null && game.escaparon == true  -> R.color.status_active
                game.endTime != null && game.escaparon == false -> R.color.status_cancelled
                game.endTime != null -> R.color.status_completed
                else -> R.color.status_confirmed
            }
            binding.tvStatus.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, colorRes)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
}
