package com.example.newsworth.ui.adapter

import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.newsworth.R
import com.example.newsworth.data.model.CircleItem

class CircleAdapter(
    private val circleList: MutableList<CircleItem>,
    private val itemClickCallback: (Int) -> Unit
) : RecyclerView.Adapter<CircleAdapter.CircleViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    fun setSelectedPosition(position: Int) {
        val previousSelectedPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousSelectedPosition)
        notifyItemChanged(selectedPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CircleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.circle_item, parent, false)
        return CircleViewHolder(view, itemClickCallback)
    }

    override fun onBindViewHolder(holder: CircleViewHolder, position: Int) {
        val circleItem = circleList.getOrNull(position)

        if (circleItem != null) {
            holder.circleName.text = circleItem.name ?: ""
            holder.circleImage.setImageResource(
                circleItem.imageResId ?: R.drawable.ic_launcher_background
            )

            val typeface = ResourcesCompat.getFont(holder.itemView.context, R.font.poppins_regular)

            val textColor = if (selectedPosition == position) {
                ContextCompat.getColor(holder.itemView.context, R.color.mehrun_color)
            } else {
                if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.white
                    )
                } else {
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.black
                    )
                }
            }

            holder.circleName.setTextColor(textColor)
            holder.circleName.setTypeface(
                typeface,
                if (selectedPosition == position) Typeface.BOLD else Typeface.NORMAL
            )

        } else {
            Log.e("CircleAdapter", "Circle item is null at position: $position")
        }
    }

    override fun getItemCount(): Int {
        return circleList.size
    }

    inner class CircleViewHolder(itemView: View, private val itemClickCallback: (Int) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        val circleName: TextView = itemView.findViewById(R.id.circleName)
        val circleImage: ImageView = itemView.findViewById(R.id.circleImage)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    Log.d("CircleClick", "Circle item clicked at position: $position")
                    itemClickCallback(position)
                }
            }
        }
    }
}