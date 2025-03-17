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
import androidx.recyclerview.widget.DiffUtil
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
        val imageLink: ImageView = itemView.findViewById(R.id.image)
        val contentTitle: TextView = itemView.findViewById(R.id.image_title)
        val contentDescription: TextView = itemView.findViewById(R.id.content_description)
        val ageInDays: TextView = itemView.findViewById(R.id.age_in_days)
        val gpsLocation: TextView = itemView.findViewById(R.id.gps_location)
        val uploadedBy: TextView = itemView.findViewById(R.id.uploaded_by)
        val priceSection: TextView = itemView.findViewById(R.id.price_section)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        if (imagesList.isEmpty() || position >= imagesList.size) {
            Log.e("ImagesAdapter", "Invalid imagesList or position: $position or holder is null")
            return
        }

        val image = imagesList[position]

        holder.contentTitle.text = image.content_title
        holder.contentDescription.text = image.content_description
        holder.ageInDays.text = image.age_in_days
        holder.gpsLocation.text = image.gps_location
        holder.uploadedBy.text = image.uploaded_by

        formatPrice(holder, image)

        val imageUrl = image.Image_link

        if (imageUrl.isNullOrEmpty()) {
            Log.w("ImageAdapter", "Image URL is null or empty for position: $position")
            Glide.with(holder.itemView.context)
                .load(R.drawable.no_image)
                .placeholder(R.drawable.no_image)
                .fitCenter()
                .into(holder.imageLink)
        } else {
            Log.d("ImageAdapter", "Loading image from URL: $imageUrl")
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.no_image)
                .error(R.drawable.no_image)
                .fitCenter()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("GlideError", "Image load failed for URL: $imageUrl, Exception: ", e)
                        holder.imageLink.setImageResource(R.drawable.no_image)
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
                .into(holder.imageLink)
        }
    }

    private fun formatPrice(holder: ImageViewHolder, image: ImageModel) {
        try {
            val originalPrice = image.price?.toDoubleOrNull() ?: 0.0
            val discountPercentage = image.discount?.toDoubleOrNull() ?: 0.0

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
        } catch (e: Exception) {
            Log.e("ImagesAdapter", "Error formatting price: ", e)
            holder.priceSection.text = "Price Unavailable"
        }
    }

    override fun getItemCount(): Int {
        return imagesList.size
    }

    fun updateImages(newImagesList: List<ImageModel>) {
        val diffCallback = ImageDiffCallback(imagesList, newImagesList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.imagesList = newImagesList
        diffResult.dispatchUpdatesTo(this)
    }

    class ImageDiffCallback(
        private val oldList: List<ImageModel>,
        private val newList: List<ImageModel>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].content_title == newList[newItemPosition].content_title
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}