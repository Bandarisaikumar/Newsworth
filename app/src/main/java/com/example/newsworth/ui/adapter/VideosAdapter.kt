package com.example.newsworth.ui.adapter

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

    private var currentlyPlaying: VideoView? = null
    private var currentPlayIcon: ImageView? = null

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val videoView: VideoView = itemView.findViewById(R.id.video)
        val playIcon: ImageView = itemView.findViewById(R.id.playIcon)
        val contentTitle: TextView = itemView.findViewById(R.id.video_title)
        val contentDescription: TextView = itemView.findViewById(R.id.content_description)
        val uploadedBy: TextView = itemView.findViewById(R.id.uploaded_by)
        val priceSection: TextView = itemView.findViewById(R.id.price_section)
        val ageInDays: TextView = itemView.findViewById(R.id.age_in_days)
        val gpsLocation: TextView = itemView.findViewById(R.id.gps_location)
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
        holder.ageInDays.text = video.age_in_days
        holder.gpsLocation.text = video.gps_location

        formatPrice(holder, video)

        val videoLink = video.Video_link
        if (!videoLink.isNullOrBlank()) {
            holder.videoView.setVideoPath(videoLink)

            holder.videoView.setOnPreparedListener { }

            holder.videoView.setOnErrorListener { _, what, extra ->
                Log.e("VideoPlayback", "Error: what=$what, extra=$extra")
                holder.playIcon.visibility = View.VISIBLE
                Toast.makeText(holder.videoView.context, "Cannot play this video", Toast.LENGTH_SHORT).show()
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

    private fun formatPrice(holder: VideoViewHolder, video: ImageModel) {
        try {
            val originalPrice = video.price.toDouble()
            val discountPercentage = video.discount.toDouble()

            val originalPriceText = SpannableString("₹$originalPrice")
            originalPriceText.setSpan(
                StrikethroughSpan(),
                0,
                originalPriceText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            val discountedPrice = originalPrice - (originalPrice * discountPercentage / 100)

            val formattedDiscount = "${discountPercentage.toInt()}%"

            val finalText = TextUtils.concat(
                "Price ₹${discountedPrice.toInt()} ",
                originalPriceText,
                " at Discount $formattedDiscount"
            )
            holder.priceSection.text = finalText
        } catch (e: NumberFormatException) {
            Log.e("VideosAdapter", "Error parsing price or discount: ", e)
            holder.priceSection.text = "Price Unavailable"
        }
    }

    override fun getItemCount(): Int = videosList.size

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        if (currentlyPlaying == holder.videoView) {
            stopAndResetVideoView(holder.videoView, holder.playIcon)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        currentlyPlaying?.stopPlayback()
        currentlyPlaying = null
        currentPlayIcon?.visibility = View.VISIBLE
        currentPlayIcon = null
    }

    private fun stopAndResetVideoView(videoView: VideoView, playIcon: ImageView) {
        videoView.stopPlayback()
        playIcon.visibility = View.VISIBLE
        if (currentlyPlaying == videoView) {
            currentlyPlaying = null
            currentPlayIcon = null
        }
    }

    fun updateVideos(newVideosList: List<ImageModel>) {
        this.videosList = newVideosList
        notifyDataSetChanged()
    }
}