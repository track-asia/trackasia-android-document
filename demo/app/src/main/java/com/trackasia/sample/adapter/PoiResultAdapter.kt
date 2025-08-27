package com.trackasia.sample.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.trackasia.sample.R
import com.trackasia.sample.model.PoiResult
import java.util.Collections
import kotlin.math.min

class PoiResultAdapter(
    private var items: MutableList<PoiResult> = mutableListOf(),
    private val onItemClick: (PoiResult) -> Unit,
    private val onNavigateClick: (PoiResult) -> Unit
) : RecyclerView.Adapter<PoiResultAdapter.ViewHolder>() {
    
    private val TAG = "PoiResultAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_poi_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            if (position < 0 || position >= items.size) {
                Log.e(TAG, "Invalid position: $position, items size: ${items.size}")
                return
            }
            
            val poi = items[position]
            holder.bind(poi)
        } catch (e: Exception) {
            Log.e(TAG, "Error binding ViewHolder at position $position: ${e.message}")
        }
    }

    override fun getItemCount(): Int = items.size

    /**
     * Update adapter data with new items.
     * This method is synchronized to prevent concurrent modification issues.
     */
    @Synchronized
    fun updateData(newItems: List<PoiResult>) {
        try {
            Log.d(TAG, "updateData called with ${newItems.size} items")
            
            // Create a defensive copy to prevent concurrent modifications
            val tempList = ArrayList<PoiResult>(newItems)
            
            // Log the first few items for debugging
            for (i in 0 until min(3, tempList.size)) {
                Log.d(TAG, "New item $i: ${tempList[i].name} at ${tempList[i].latitude},${tempList[i].longitude}")
            }
            
            // Clear and update in a synchronized block to avoid race conditions
            synchronized(items) {
                items.clear()
                items.addAll(tempList)
            }
            
            // Notify that data has changed - IMPORTANT: don't remove this!
            notifyDataSetChanged()
            
            Log.d(TAG, "Adapter data updated successfully, now has ${items.size} items")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating data: ${e.message}", e)
            // Try to recover - don't lose data if an exception occurs
            if (items.isEmpty() && newItems.isNotEmpty()) {
                try {
                    items.addAll(newItems)
                    Log.d(TAG, "Recovered from error, added ${newItems.size} items")
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to recover: ${e2.message}")
                }
            }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPoiIcon: ImageView = itemView.findViewById(R.id.ivPoiIcon)
        private val tvPoiName: TextView = itemView.findViewById(R.id.tvPoiName)
        private val tvPoiAddress: TextView = itemView.findViewById(R.id.tvPoiAddress)
        private val btnPoiNavigate: ImageButton = itemView.findViewById(R.id.btnPoiNavigate)

        fun bind(poi: PoiResult) {
            try {
                // Set text fields with fallbacks for nulls
                tvPoiName.text = poi.name.takeIf { it.isNotBlank() } ?: "Không có tên"
                tvPoiAddress.text = poi.address.takeIf { it.isNotBlank() } ?: "Không có địa chỉ"

                // Set icon based on POI category - null-safe checks
                val category = poi.category.orEmpty().lowercase()
                val iconResId = when {
                    category.contains("restaurant") || category.contains("food") || 
                    category.contains("drink") || category.contains("cafe") -> R.drawable.ic_restaurant
                    
                    category.contains("hotel") || category.contains("lodging") || 
                    category.contains("accommodation") -> R.drawable.ic_hotel
                    
                    category.contains("supermarket") || category.contains("shop") || 
                    category.contains("store") || category.contains("mall") -> R.drawable.ic_store
                    
                    category.contains("pharmacy") || category.contains("drugstore") || 
                    category.contains("hospital") || category.contains("clinic") -> R.drawable.ic_pharmacy
                    
                    else -> R.drawable.ic_location
                }
                
                // Set image resource
                ivPoiIcon.setImageResource(iconResId)
                
                // Apply tint using ImageViewCompat for better compatibility
                val tintColor = ContextCompat.getColor(itemView.context, R.color.colorBlue)
                ImageViewCompat.setImageTintList(ivPoiIcon, android.content.res.ColorStateList.valueOf(tintColor))
                ImageViewCompat.setImageTintList(btnPoiNavigate, android.content.res.ColorStateList.valueOf(tintColor))

                // Set click events
                itemView.setOnClickListener { 
                    try {
                        onItemClick(poi)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in item click: ${e.message}")
                    }
                }
                
                btnPoiNavigate.setOnClickListener { 
                    try {
                        onNavigateClick(poi)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in navigate click: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error binding POI: ${e.message}")
            }
        }
    }
} 