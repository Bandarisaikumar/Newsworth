package com.example.newsworth.ui.adapter

import android.media.MediaPlayer
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.StrikethroughSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.example.newsworth.R
import com.example.newsworth.data.model.ImageModel

class VideosAdapter(private var videosList: List<ImageModel>) :
    RecyclerView.Adapter<VideosAdapter.VideoViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null


    private var currentlyPlaying: VideoView? = null
    private var currentPlayIcon: ImageView? = null

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val videoView: VideoView = itemView.findViewById(R.id.video)
        val playIcon: ImageView = itemView.findViewById(R.id.playIcon)
        val contentTitle: TextView = itemView.findViewById(R.id.video_title)
        val contentDescription: TextView = itemView.findViewById(R.id.content_description)
        val uploadedBy: TextView = itemView.findViewById(R.id.uploaded_by)
        val price_section: TextView = itemView.findViewById(R.id.price_section)
        val age_in_days: TextView = itemView.findViewById(R.id.age_in_days)
        val gps_location: TextView = itemView.findViewById(R.id.gps_location)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_videos, parent, false)
        return VideoViewHolder(view)
    }
    fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videosList[position]

        holder.contentTitle.text = video.content_title
        holder.contentDescription.text = video.content_description
        holder.uploadedBy.text = video.uploaded_by
        holder.age_in_days.text = video.age_in_days
        holder.gps_location.text = video.gps_location

        val originalPrice = video.price.toDoubleOrNull() ?: 0.0
        val discountPercentage = video.discount.toDoubleOrNull() ?: 0.0

        val originalPriceText = SpannableString("₹${originalPrice}")
        originalPriceText.setSpan(
            StrikethroughSpan(),
            0,
            originalPriceText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val discountedPrice = originalPrice - (originalPrice * discountPercentage / 100)
        val formattedDiscount = discountPercentage.toInt().toString() + "%"

        val finalText = TextUtils.concat(
            "Price ₹${discountedPrice.toInt()} ",
            originalPriceText,
            " at Discount $formattedDiscount"
        )
        holder.price_section.text = finalText

        val videoLink = video.Video_link
        if (!videoLink.isNullOrBlank()) {
            holder.videoView.setVideoPath(videoLink)

            holder.videoView.setOnPreparedListener { mp ->
            }

            holder.videoView.setOnErrorListener { mp, what, extra ->
                Log.e("VideoPlayback", "Error: what=$what, extra=$extra")
                holder.playIcon.visibility = View.VISIBLE
//                Toast.makeText(holder.videoView.context, "Cannot play this video", Toast.LENGTH_SHORT).show()
                true
            }

        } else {
            Toast.makeText(holder.videoView.context, "Video link is empty", Toast.LENGTH_SHORT).show()
            holder.playIcon.visibility = View.VISIBLE
        }

        holder.playIcon.setOnClickListener {
            handleVideoPlay(holder.videoView, holder.playIcon)
        }
    }

    private fun handleVideoPlay(videoView: VideoView, playIcon: ImageView) {
        if (currentlyPlaying == videoView) {
            if (videoView.isPlaying) {
                videoView.pause()
                playIcon.visibility = View.VISIBLE
            } else {
                videoView.start()
                playIcon.visibility = View.GONE
            }
        } else {
            currentlyPlaying?.pause()
            currentPlayIcon?.visibility = View.VISIBLE

            videoView.start()
            playIcon.visibility = View.GONE

            currentlyPlaying = videoView
            currentPlayIcon = playIcon
        }
    }

    override fun getItemCount(): Int = videosList.size
    fun updateVideos(newVideosList: List<ImageModel>) {
        this.videosList = newVideosList
        notifyDataSetChanged()
    }
}
