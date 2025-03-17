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
    private var currentPlayingPosition: Int = -1
    private val handler: Handler = Handler(Looper.getMainLooper())

    class AudioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playAudioIcon: ImageView = itemView.findViewById(R.id.audio)
        val audioSeekBar: SeekBar = itemView.findViewById(R.id.audio_seekbar)
        val audioTimer: TextView = itemView.findViewById(R.id.audio_timer)
        val contentTitle: TextView = itemView.findViewById(R.id.audio_title)
        val contentDescription: TextView = itemView.findViewById(R.id.audio_description)
        val uploadedBy: TextView = itemView.findViewById(R.id.user_name)
        val priceSection: TextView = itemView.findViewById(R.id.price)
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

        holder.audioSeekBar.progress = 0
        holder.audioTimer.text = "00:00"
        holder.playAudioIcon.setImageResource(R.drawable.play_button)

        holder.playAudioIcon.setOnClickListener {
            val audioLink = audio.Audio_link
            if (!audioLink.isNullOrBlank()) {
                handleAudioPlay(holder, audioLink, position)
            } else {
                Toast.makeText(holder.itemView.context, "No audio available!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleAudioPlay(holder: AudioViewHolder, audioLink: String, position: Int) {
        try {
            if (mediaPlayer != null && currentPlayingPosition == position && isPlaying) {
                pauseMediaPlayer(holder)
                return
            }

            if (mediaPlayer != null && currentPlayingPosition == position) {
                resumeMediaPlayer(holder)
                return
            }

            stopAndResetMediaPlayer()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioLink)
                setOnPreparedListener {
                    startMediaPlayer(holder, position)
                }
                setOnCompletionListener {
                    stopAndResetMediaPlayer()
                    resetUI(holder)
                }
                setOnErrorListener { _, _, _ ->
                    handleMediaPlayerError(holder)
                    return@setOnErrorListener true
                }
                prepareAsync() // Use prepareAsync() to avoid blocking the main thread
            }
        } catch (e: Exception) {
            handleMediaPlayerGeneralError(holder, e)
        }
    }

    private fun startMediaPlayer(holder: AudioViewHolder, position: Int) {
        mediaPlayer?.start()
        isPlaying = true
        currentPlayingPosition = position
        holder.playAudioIcon.setImageResource(R.drawable.pause_button)
        holder.audioSeekBar.max = mediaPlayer?.duration ?: 0
        startSeekBarUpdates(holder)
    }

    private fun pauseMediaPlayer(holder: AudioViewHolder) {
        mediaPlayer?.pause()
        stopSeekBarUpdates()
        holder.playAudioIcon.setImageResource(R.drawable.play_button)
        isPlaying = false
    }

    private fun resumeMediaPlayer(holder: AudioViewHolder) {
        mediaPlayer?.start()
        startSeekBarUpdates(holder)
        holder.playAudioIcon.setImageResource(R.drawable.pause_button)
        isPlaying = true
    }

    private fun handleMediaPlayerError(holder: AudioViewHolder) {
        Toast.makeText(holder.itemView.context, "Error playing audio", Toast.LENGTH_SHORT).show()
        stopAndResetMediaPlayer()
        resetUI(holder)
    }

    private fun handleMediaPlayerGeneralError(holder: AudioViewHolder, e: Exception) {
        Toast.makeText(holder.itemView.context, "Error playing audio: ${e.message}", Toast.LENGTH_SHORT).show()
        stopAndResetMediaPlayer()
        resetUI(holder)
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
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }
        stopSeekBarUpdates()
        currentPlayingPosition = -1
        isPlaying = false
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

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        releaseMediaPlayer()
    }

    override fun onViewRecycled(holder: AudioViewHolder) {
        super.onViewRecycled(holder)
        if (currentPlayingPosition == holder.adapterPosition) {
            releaseMediaPlayer()
        }
    }

    fun releaseMediaPlayer() {
        stopAndResetMediaPlayer()
    }

    fun updateAudios(newAudiosList: List<ImageModel>) {
        this.audioList = newAudiosList
        notifyDataSetChanged()
    }
}