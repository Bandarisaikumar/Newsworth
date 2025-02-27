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

class VideosItemAdapter(private val videoList: List<ImageModel>) :
    RecyclerView.Adapter<VideosItemAdapter.VideoViewHolder>() {

    private var currentlyPlaying: VideoView? = null
    private var currentPlayIcon: ImageView? = null

    inner class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val Video_link: VideoView = itemView.findViewById(R.id.video_view)
        val content_title: TextView = itemView.findViewById(R.id.video_title)
        val content_description: TextView = itemView.findViewById(R.id.content_description)
        val age_in_days: TextView = itemView.findViewById(R.id.age_in_days)
        val gps_location: TextView = itemView.findViewById(R.id.gps_location)
        val uploaded_by: TextView = itemView.findViewById(R.id.uploaded_by)
        val price_section: TextView = itemView.findViewById(R.id.price_section)
        val playIcon: ImageView = itemView.findViewById(R.id.playIcon)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_vedio_card, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideosItemAdapter.VideoViewHolder, position: Int) {
        val item = videoList[position]
        holder.content_title.text = item.content_title
        holder.content_description.text = item.content_description
        holder.age_in_days.text = item.age_in_days
        holder.uploaded_by.text = item.uploaded_by
        holder.gps_location.text = item.gps_location

        // Calculate discounted price
        val originalPrice = item.price.toDoubleOrNull() ?: 0.0
        val discountPercentage = item.discount.toDoubleOrNull() ?: 0.0

        val originalPriceText = SpannableString("₹${originalPrice}")
        originalPriceText.setSpan(
            StrikethroughSpan(),
            0,
            originalPriceText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val discountedPrice = originalPrice - (originalPrice * discountPercentage / 100)

// Format the discount percentage to remove the decimal
        val formattedDiscount = discountPercentage.toInt().toString() + "%"

// Combine discounted price, original price (with strike-through), and discount percentage
        val finalText = TextUtils.concat(
            "Price ₹${discountedPrice.toInt()} ",
            originalPriceText,
            " at Discount $formattedDiscount" // Use the formattedDiscount here
        )

        // Create the formatted text
        holder.price_section.text = finalText
        // Load the video link into the VideoView
        val videoLink = item.Video_link
        if (!videoLink.isNullOrBlank()) {
            holder.Video_link.setVideoPath(videoLink)
        }

        holder.playIcon.setOnClickListener {
            handleVideoPlay(holder.Video_link, holder.playIcon)
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


    override fun getItemCount(): Int = videoList.size
}
