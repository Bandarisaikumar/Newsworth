package com.example.newsworth.ui.adapter

import android.graphics.drawable.Drawable
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
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.newsworth.R
import com.example.newsworth.data.model.ImageModel

class ImagesAdapter(private var imagesList: List<ImageModel>) :
    RecyclerView.Adapter<ImagesAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val Image_link: ImageView = itemView.findViewById(R.id.image)
        val content_title: TextView = itemView.findViewById(R.id.image_title)
        val content_description: TextView = itemView.findViewById(R.id.content_description)
        val age_in_days: TextView = itemView.findViewById(R.id.age_in_days)
        val gps_location: TextView = itemView.findViewById(R.id.gps_location)
        val uploaded_by: TextView = itemView.findViewById(R.id.uploaded_by)
        val price_section: TextView = itemView.findViewById(R.id.price_section)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        if (imagesList == null || imagesList.isEmpty() || position >= imagesList.size || holder == null) {
            Log.e("ImagesAdapter", "Invalid imagesList or position: $position or holder is null")
            return
        }

        val image = imagesList[position]

        holder.content_title.text = image.content_title
        holder.content_description.text = image.content_description
        holder.age_in_days.text = image.age_in_days
        holder.gps_location.text = image.gps_location
        holder.uploaded_by.text = image.uploaded_by

        val originalPrice = image.price.toDoubleOrNull() ?: 0.0
        val discountPercentage = image.discount.toDoubleOrNull() ?: 0.0

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
        val imageUrl = image.Image_link

        if (imageUrl.isNullOrEmpty()) {
            Log.w("ImageAdapter", "Image URL is null or empty for position: $position")
            Glide.with(holder.itemView.context)
                .load(R.drawable.no_image)
                .fitCenter()
                .into(holder.Image_link)
        } else {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .fitCenter()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("GlideError", "Image load failed for URL: $imageUrl, Exception: ", e)
                        holder.Image_link.setImageResource(R.drawable.no_image)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("GlideSuccess", "Image loaded successfully for URL: $imageUrl")
                        return false
                    }
                })
                .into(holder.Image_link)
        }
    }

    override fun getItemCount(): Int {
        return imagesList.size
    }

    fun updateImages(newImagesList: List<ImageModel>) {
        this.imagesList = newImagesList
        notifyDataSetChanged()
    }
}