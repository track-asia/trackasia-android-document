package com.trackasia.sample.model

import android.util.Log
import com.trackasia.android.geometry.LatLng
import org.json.JSONObject

data class PoiResult(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    var category: String? = null
) {
    val location: LatLng
        get() = LatLng(latitude, longitude)

    companion object {
        private const val TAG = "PoiResult"
        
        fun fromJson(json: JSONObject, defaultCategory: String = ""): PoiResult? {
            return try {
                // Log the input for debugging
                Log.d(TAG, "Parsing JSON: ${json.toString().take(200)}...")
                
                // Extract basic information
                val placeId = json.optString("place_id", json.optString("id", ""))
                val name = json.optString("name", json.optString("title", "Không có tên"))
                
                // Try multiple fields for address
                val address = json.optString("formatted_address", 
                               json.optString("vicinity", 
                               json.optString("address", "Không có địa chỉ")))
                
                // Try multiple approaches to extract coordinates
                var lat = 0.0
                var lng = 0.0
                
                // Approach 1: Check for geometry.location object
                val geometry = json.optJSONObject("geometry")
                if (geometry != null) {
                    val location = geometry.optJSONObject("location")
                    if (location != null) {
                        lat = location.optDouble("lat", 0.0)
                        lng = location.optDouble("lng", 0.0)
                    } else {
                        // Try coordinates array
                        val coordinates = geometry.optJSONArray("coordinates")
                        if (coordinates != null && coordinates.length() >= 2) {
                            // GeoJSON format usually has [lng, lat]
                            lng = coordinates.optDouble(0, 0.0)
                            lat = coordinates.optDouble(1, 0.0)
                        }
                    }
                }
                
                // Approach 2: Check for direct lat/lng properties
                if (lat == 0.0 && lng == 0.0) {
                    lat = json.optDouble("latitude", json.optDouble("lat", 0.0))
                    lng = json.optDouble("longitude", json.optDouble("lng", 0.0))
                }
                
                // Approach 3: Check for a position array
                if (lat == 0.0 && lng == 0.0) {
                    val position = json.optJSONArray("position")
                    if (position != null && position.length() >= 2) {
                        lat = position.optDouble(0, 0.0)
                        lng = position.optDouble(1, 0.0)
                    }
                }
                
                // Approach 4: Check for location object
                if (lat == 0.0 && lng == 0.0) {
                    val location = json.optJSONObject("location")
                    if (location != null) {
                        lat = location.optDouble("lat", location.optDouble("latitude", 0.0))
                        lng = location.optDouble("lng", location.optDouble("longitude", 0.0))
                    }
                }
                
                // Log what we found for coordinates
                Log.d(TAG, "Extracted coordinates: lat=$lat, lng=$lng")
                
                // If still no valid coordinates, this POI is not usable
                if (lat == 0.0 && lng == 0.0) {
                    Log.e(TAG, "Could not extract valid coordinates from JSON")
                    return null
                }
                
                // Try to extract category from various fields
                val category = json.optString("type", 
                              json.optString("category", 
                              json.optString("poi_category",
                              json.optString("place_type", defaultCategory))))
                
                Log.d(TAG, "Successfully parsed POI: $name at ($lat,$lng)")
                
                PoiResult(
                    placeId = placeId,
                    name = name,
                    address = address,
                    latitude = lat,
                    longitude = lng,
                    category = category
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing POI data: ${e.message}", e)
                null
            }
        }
        
        // Overload for backward compatibility
        fun fromJson(json: JSONObject): PoiResult? {
            return fromJson(json, "")
        }
    }
} 