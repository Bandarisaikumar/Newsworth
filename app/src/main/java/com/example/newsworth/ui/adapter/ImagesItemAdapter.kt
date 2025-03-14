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
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.newsworth.R
import com.example.newsworth.data.model.ImageModel

class ImagesItemAdapter(private val imageList: List<ImageModel>) :
    RecyclerView.Adapter<ImagesItemAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val Image_link: ImageView = itemView.findViewById(R.id.image_view)
        val content_title: TextView = itemView.findViewById(R.id.image_title)
        val content_description: TextView = itemView.findViewById(R.id.content_description)
        val age_in_days: TextView = itemView.findViewById(R.id.age_in_days)
        val gps_location: TextView = itemView.findViewById(R.id.gps_location)
        val uploaded_by: TextView = itemView.findViewById(R.id.uploaded_by)
        val price_section: TextView = itemView.findViewById(R.id.price_section)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_image_card, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val item = imageList[position]
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


        Glide.with(holder.itemView.context)
            .load(item.Image_link)
            .into(holder.Image_link)

        if (item.Image_link.isNullOrBlank()) {
            Glide.with(holder.itemView.context)
                .load(R.drawable.no_image)
                .into(holder.Image_link)
        } else {
            Glide.with(holder.itemView.context)
                .load(item.Image_link)
                .into(holder.Image_link)

        }
    }

    override fun getItemCount(): Int = imageList.size


}
