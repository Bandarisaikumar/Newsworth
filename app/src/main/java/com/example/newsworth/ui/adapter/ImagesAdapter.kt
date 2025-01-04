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
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.newsworth.data.model.ImageModel
import com.example.newsworth.R

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
//        val cartIcon: ImageView = itemView.findViewById(R.id.cart_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val image = imagesList[position]
        holder.content_title.text = image.content_title
        holder.content_description.text = image.content_description
        holder.age_in_days.text = image.age_in_days
        holder.gps_location.text = image.gps_location
        holder.uploaded_by.text = image.uploaded_by
        val originalPrice = image.price
        val discountPercentage = image.discount

        val originalPriceText = SpannableString("₹${originalPrice}")
        originalPriceText.setSpan(StrikethroughSpan(), 0, originalPriceText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Calculate discounted price
        val discountedPrice = originalPrice - (originalPrice * discountPercentage / 100)

        // Combine discounted price, original price (with strike-through), and discount percentage
        val finalText = TextUtils.concat("Price ₹${discountedPrice.toInt()} ", originalPriceText, " at Discount ${discountPercentage}%")

        // Create the formatted text
        holder.price_section.text = finalText

        // Check if the image URL is null or blank
        val Image_link = image.Image_link
        if (Image_link != null) {
            Log.e("Image",Image_link)
        }
        
        if (Image_link.isNullOrBlank()) {
            // You can use a placeholder image or handle this case differently
            Glide.with(holder.itemView.context)
                .load(R.drawable.no_image)
                .into(holder.Image_link)
        } else {
            Glide.with(holder.itemView.context)
                .load(Image_link)
                .into(holder.Image_link)
        }
    }

    override fun getItemCount(): Int {
        return imagesList.size
    }

    // Method to update the list of images
    fun updateImages(newImagesList: List<ImageModel>) {
        imagesList = newImagesList
        notifyDataSetChanged()  // Notify adapter to update the RecyclerView
    }
}
