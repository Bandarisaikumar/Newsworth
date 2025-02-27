package com.example.newsworth.ui.adapter

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.newsworth.R
import com.example.newsworth.data.model.ImageModel

class AudioAdapter(private var audioList: List<ImageModel>) :
    RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var currentPlayIcon: ImageView? = null
    private var currentSeekBar: SeekBar? = null
    private var currentTimer: TextView? = null
    private var handler: Handler = Handler(Looper.getMainLooper())

    class AudioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playAudioIcon: ImageView = itemView.findViewById(R.id.audio)
        val audioSeekBar: SeekBar = itemView.findViewById(R.id.audio_seekbar)
        val audioTimer: TextView = itemView.findViewById(R.id.audio_timer)
        val contentTitle: TextView = itemView.findViewById(R.id.audio_title)
        val contentDescription: TextView = itemView.findViewById(R.id.audio_description)
        val uploadedBy: TextView = itemView.findViewById(R.id.user_name)
        val priceSection: TextView = itemView.findViewById(R.id.price)
//        val cartIcon: ImageView = itemView.findViewById(R.id.cart_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_audios, parent, false)
        return AudioViewHolder(view)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        val audio = audioList[position]
        holder.contentTitle.text = audio.content_title
        holder.contentDescription.text = audio.content_description
        holder.uploadedBy.text = audio.uploaded_by
        holder.priceSection.text = audio.price.toString()

        // Reset UI elements for recycled views
        holder.audioSeekBar.progress = 0
        holder.audioTimer.text = "00:00"
        holder.playAudioIcon.setImageResource(R.drawable.play_button)

//        holder.cartIcon.setOnClickListener {
//            Toast.makeText(holder.itemView.context, "${audio.content_title} added to cart!", Toast.LENGTH_SHORT).show()
//        }

        holder.playAudioIcon.setOnClickListener {
            val audioLink = audio.Audio_link
            if (!audioLink.isNullOrBlank()) {
                handleAudioPlay(holder, audioLink)
            } else {
                Toast.makeText(holder.itemView.context, "No audio available!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleAudioPlay(holder: AudioViewHolder, audioLink: String) {
        try {
            // Handle play/pause logic
            if (mediaPlayer != null && currentPlayIcon == holder.playAudioIcon && isPlaying) {
                mediaPlayer?.pause()
                stopSeekBarUpdates()
                holder.playAudioIcon.setImageResource(R.drawable.play_button)
                isPlaying = false
                return
            }

            if (mediaPlayer != null && currentPlayIcon == holder.playAudioIcon) {
                mediaPlayer?.start()
                startSeekBarUpdates(holder)
                holder.playAudioIcon.setImageResource(R.drawable.pause_button)
                isPlaying = true
                return
            }

            // Stop previous media and reset UI
            stopAndResetMediaPlayer()

            // Initialize and play new audio
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioLink)
                prepare()
                start()
            }

            isPlaying = true
            currentPlayIcon = holder.playAudioIcon
            currentSeekBar = holder.audioSeekBar
            currentTimer = holder.audioTimer
            holder.playAudioIcon.setImageResource(R.drawable.pause_button)

            // Initialize SeekBar
            holder.audioSeekBar.max = mediaPlayer?.duration ?: 0
            startSeekBarUpdates(holder)

            // Handle SeekBar scrubbing
            holder.audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
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
                stopAndResetMediaPlayer()
                resetUI(holder)
            }
        } catch (e: Exception) {
            Toast.makeText(holder.itemView.context, "Error playing audio: ${e.message}", Toast.LENGTH_SHORT).show()
            stopAndResetMediaPlayer()
            resetUI(holder)
        }
    }

    private fun startSeekBarUpdates(holder: AudioViewHolder) {
        handler.post(object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    holder.audioSeekBar.progress = player.currentPosition
                    holder.audioTimer.text = formatTime(player.currentPosition)
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
        holder.audioSeekBar.progress = 0
        holder.audioTimer.text = "00:00"
        holder.playAudioIcon.setImageResource(R.drawable.play_button)
        isPlaying = false
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = milliseconds / 1000 / 60
        val seconds = milliseconds / 1000 % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun getItemCount(): Int = audioList.size

    fun releaseMediaPlayer() {
        stopAndResetMediaPlayer()
    }
    fun updateAudios(newAudiosList: List<ImageModel>) {
        this.audioList = newAudiosList
        notifyDataSetChanged()
    }
}
