package com.example.newsworth.ui.adapter

import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.StrikethroughSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.example.newsworth.R
import com.example.newsworth.data.model.ImageModel

class VideosAdapter(private val videosList: List<ImageModel>) :
    RecyclerView.Adapter<VideosAdapter.VideoViewHolder>() {

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

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videosList[position]

        holder.contentTitle.text = video.content_title
        holder.contentDescription.text = video.content_description
        holder.uploadedBy.text = video.uploaded_by
        holder.age_in_days.text = video.age_in_days
        holder.gps_location.text = video.gps_location

        val originalPrice = video.price
        val discountPercentage = video.discount

        val originalPriceText = SpannableString("₹${originalPrice}")
        originalPriceText.setSpan(StrikethroughSpan(), 0, originalPriceText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Calculate discounted price
        val discountedPrice = originalPrice - (originalPrice * discountPercentage / 100)

        // Combine discounted price, original price (with strike-through), and discount percentage
        val finalText = TextUtils.concat("Price ₹${discountedPrice.toInt()} ", originalPriceText, " at Discount ${discountPercentage}%")

        holder.price_section.text = finalText

        val videoLink = video.Video_link
        if (!videoLink.isNullOrBlank()) {
            holder.videoView.setVideoPath(videoLink)
        }

        holder.playIcon.setOnClickListener {
            handleVideoPlay(holder.videoView, holder.playIcon)
        }
    }

    private fun handleVideoPlay(videoView: VideoView, playIcon: ImageView) {
        if (currentlyPlaying == videoView) {
            // Toggle play/pause for the same video
            if (videoView.isPlaying) {
                videoView.pause()
                playIcon.visibility = View.VISIBLE
            } else {
                videoView.start()
                playIcon.visibility = View.GONE
            }
        } else {
            // Stop the previously playing video
            currentlyPlaying?.pause()
            currentPlayIcon?.visibility = View.VISIBLE

            // Start the new video
            videoView.start()
            playIcon.visibility = View.GONE

            // Update the currently playing video references
            currentlyPlaying = videoView
            currentPlayIcon = playIcon
        }
    }

    override fun getItemCount(): Int = videosList.size
}
