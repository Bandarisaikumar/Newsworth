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
import android.widget.VideoView
import android.widget.Toast
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vedio_card, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideosItemAdapter.VideoViewHolder, position: Int) {
        val item = videoList[position]
        holder.content_title.text = item.content_title
        holder.content_description.text = item.content_description
        holder.age_in_days.text = item.age_in_days
        holder.uploaded_by.text = item.uploaded_by
        holder.gps_location.text = item.gps_location

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
        val formattedDiscount = discountPercentage.toInt().toString() + "%"

        val finalText = TextUtils.concat(
            "Price ₹${discountedPrice.toInt()} ",
            originalPriceText,
            " at Discount $formattedDiscount"
        )

        holder.price_section.text = finalText

        val videoLink = item.Video_link
        if (!videoLink.isNullOrBlank()) {
            holder.Video_link.setVideoPath(videoLink)

            holder.Video_link.setOnPreparedListener { mp ->
            }

            holder.Video_link.setOnErrorListener { mp, what, extra ->
                Log.e("VideoPlayback", "Error: what=$what, extra=$extra")
                holder.playIcon.visibility = View.VISIBLE
//                Toast.makeText(holder.Video_link.context, "Cannot play this video", Toast.LENGTH_SHORT).show()
                true
            }

        } else {
            Toast.makeText(holder.Video_link.context, "Video link is empty", Toast.LENGTH_SHORT).show()
            holder.playIcon.visibility = View.VISIBLE
        }

        holder.playIcon.setOnClickListener {
            handleVideoPlay(holder.Video_link, holder.playIcon)
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

    override fun getItemCount(): Int = videoList.size

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        holder.Video_link.stopPlayback()
        holder.playIcon.visibility = View.VISIBLE
    }
}