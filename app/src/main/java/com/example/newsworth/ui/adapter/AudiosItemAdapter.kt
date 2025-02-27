package com.example.newsworth.ui.adapter

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.newsworth.R
import com.example.newsworth.data.model.ImageModel

class AudiosItemAdapter(private var audiosList: List<ImageModel>) :
    RecyclerView.Adapter<AudiosItemAdapter.AudioViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var isPaused = false
    private var currentPlayIcon: ImageView? = null
    private var currentSeekBar: SeekBar? = null
    private var currentTimer: TextView? = null
    private var handler: Handler = Handler(Looper.getMainLooper())

    inner class AudioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val play_audio_icon: ImageView = itemView.findViewById(R.id.audio)
        val content_title: TextView = itemView.findViewById(R.id.audio_title)
        val content_description: TextView = itemView.findViewById(R.id.audio_description)
        val uploaded_by: TextView = itemView.findViewById(R.id.user_name)
        val price_section: TextView = itemView.findViewById(R.id.price)
        val audio_seekbar: SeekBar = itemView.findViewById(R.id.audio_seekbar)
        val audio_timer: TextView = itemView.findViewById(R.id.audio_timer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_audio_card, parent, false)
        return AudioViewHolder(view)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        val audio = audiosList[position]
        holder.content_title.text = audio.content_title
        holder.content_description.text = audio.content_description
        holder.uploaded_by.text = audio.uploaded_by
        holder.price_section.text = audio.price.toString()

        // Reset UI elements for recycled views
        holder.audio_seekbar.progress = 0
        holder.audio_timer.text = "00:00"
        holder.play_audio_icon.setImageResource(R.drawable.play_button)


        holder.play_audio_icon.setOnClickListener {
            val audioLink = audio.Audio_link
            if (!audioLink.isNullOrBlank()) {
                handleAudioPlay(holder, audioLink)
            } else {
                Toast.makeText(holder.itemView.context, "No audio available!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    fun updateAudios(newAudios: List<ImageModel>) {
        this.audiosList = newAudios
        notifyDataSetChanged()
    }


    private fun handleAudioPlay(holder: AudioViewHolder, audioLink: String) {
        try {
            // Handle play/pause logic
            if (mediaPlayer != null && currentPlayIcon == holder.play_audio_icon && isPlaying) {
                mediaPlayer?.pause()
                stopSeekBarUpdates()
                holder.play_audio_icon.setImageResource(R.drawable.play_button)
                isPlaying = false
                isPaused = true
                return
            }

            if (mediaPlayer != null && currentPlayIcon == holder.play_audio_icon && isPaused) {
                mediaPlayer?.start()
                startSeekBarUpdates(holder)
                holder.play_audio_icon.setImageResource(R.drawable.pause_button)
                isPlaying = true
                isPaused = false
                return
            }

            // Stop and reset any previously playing audio
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                    it.release()
                    currentPlayIcon?.setImageResource(R.drawable.play_button)
                }
            }

            // Initialize and play new audio
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioLink)
                prepare()
                start()
            }

            // Update play state and icon
            isPlaying = true
            isPaused = false
            currentPlayIcon = holder.play_audio_icon
            currentSeekBar = holder.audio_seekbar
            currentTimer = holder.audio_timer
            holder.play_audio_icon.setImageResource(R.drawable.pause_button)

            // Initialize SeekBar
            holder.audio_seekbar.max = mediaPlayer?.duration ?: 0
            startSeekBarUpdates(holder)

            // Handle SeekBar scrubbing
            holder.audio_seekbar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        mediaPlayer?.seekTo(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    mediaPlayer?.pause()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    mediaPlayer?.start()
                    startSeekBarUpdates(holder)
                }
            })

            // Handle playback completion
            mediaPlayer?.setOnCompletionListener {
                Toast.makeText(
                    holder.itemView.context,
                    "Audio playback completed!",
                    Toast.LENGTH_SHORT
                ).show()
                stopAndResetMediaPlayer()
                resetUI(holder)
            }
        } catch (e: Exception) {
            Toast.makeText(
                holder.itemView.context,
                "Error playing audio: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            stopAndResetMediaPlayer()
            resetUI(holder)
        }
    }

    private fun startSeekBarUpdates(holder: AudioViewHolder) {
        handler.post(object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    holder.audio_seekbar.progress = player.currentPosition
                    holder.audio_timer.text = formatTime(player.currentPosition)
                    if (player.isPlaying) {
                        handler.postDelayed(this, 500)
                    }
                }
            }
        })
    }

    private fun stopSeekBarUpdates() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun stopAndResetMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        stopSeekBarUpdates()
        currentPlayIcon?.setImageResource(R.drawable.play_button)
        currentPlayIcon = null
    }

    private fun resetUI(holder: AudioViewHolder) {
        holder.audio_seekbar.progress = 0
        holder.audio_timer.text = "00:00"
        holder.play_audio_icon.setImageResource(R.drawable.play_button)
        isPlaying = false
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = milliseconds / 1000 / 60
        val seconds = milliseconds / 1000 % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun getItemCount(): Int = audiosList.size

    fun releaseMediaPlayer() {
        stopAndResetMediaPlayer()
    }
}
