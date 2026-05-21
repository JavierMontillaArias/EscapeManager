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
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class GmGamesAdapter(
    private val onGameClick: (Game) -> Unit
) : ListAdapter<Game, GmGamesAdapter.ViewHolder>(GameDiffCallback()) {

    class GameDiffCallback : DiffUtil.ItemCallback<Game>() {
        override fun areItemsTheSame(oldItem: Game, newItem: Game) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Game, newItem: Game) = oldItem == newItem
    }

    inner class ViewHolder(private val binding: ItemGameBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(game: Game) {
            binding.tvGameTitle.text  = "Partida #${game.id}"
            binding.tvGamemaster.text = "GM: ${game.gamemaster.name}"
            binding.tvStartTime.text  = "Inicio: ${formatLocalTime(game.startTime)}"
            binding.tvHints.text      = "Pistas: ${game.hintsUsed}"

            val statusText = when {
                game.endTime != null && game.escaparon == true  -> "Escaparon"
                game.endTime != null && game.escaparon == false -> "No escaparon"
                game.endTime != null                            -> "Completada"
                else                                            -> "En curso"
            }
            binding.tvStatus.text = statusText

            val colorRes = when {
                game.endTime != null && game.escaparon == true  -> R.color.status_active
                game.endTime != null && game.escaparon == false -> R.color.status_cancelled
                game.endTime != null                            -> R.color.status_completed
                else                                            -> R.color.status_confirmed
            }
            binding.tvStatus.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, colorRes)
            )

            if (game.endTime == null) {
                binding.root.setOnClickListener { onGameClick(game) }
                binding.root.alpha = 1.0f
            } else {
                binding.root.setOnClickListener(null)
                binding.root.alpha = 0.7f
            }
        }

        private fun formatLocalTime(utcTime: String?): String {
            if (utcTime.isNullOrBlank()) return "-"
            return try {
                val instant = Instant.parse(utcTime + "Z")
                val madrid = ZoneId.of("Europe/Madrid")
                val local = ZonedDateTime.ofInstant(instant, madrid)
                local.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            } catch (e: Exception) {
                utcTime.take(16).replace("T", " ")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
}