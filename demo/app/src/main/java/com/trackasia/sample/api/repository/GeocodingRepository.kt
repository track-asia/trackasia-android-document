package com.trackasia.sample.api.repository

import android.app.Activity
import android.content.Context
import com.trackasia.android.geometry.LatLng
import com.trackasia.sample.api.RetrofitClient
import com.trackasia.sample.api.model.GeoCodingData
import com.trackasia.sample.api.service.APIService
import com.trackasia.sample.utils.MapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


sealed class ResultAPI<out T> {
    data class Success<out T>(val data: T) : ResultAPI<T>()
    data class Error(val exception: Exception) : ResultAPI<Nothing>()
}

suspend fun showPointMap(point: LatLng, context: Activity): ResultAPI<GeoCodingData> {
    return withContext(Dispatchers.IO) {
        try {
            val sharedPreferences = context.getSharedPreferences("trackasia", Context.MODE_PRIVATE)
            val idCountry = sharedPreferences.getString("country", "vn")
            val domain = MapUtils(context).urlDomain(idCountry)
            val geocodingService: APIService = RetrofitClient.getInstance(domain).create(APIService::class.java)
            val call = geocodingService.reverseGeocode("vi", point.longitude, point.latitude)
            val response = call.execute()
            if (response.isSuccessful) {
                val geocodingResponse = response.body()
                if (geocodingResponse != null) {
                    val name = geocodingResponse.features?.firstOrNull()?.properties?.name
                    val long = geocodingResponse.features?.firstOrNull()?.geometry?.coordinates?.get(0)
                    val lat = geocodingResponse.features?.firstOrNull()?.geometry?.coordinates?.get(1)
                    ResultAPI.Success(GeoCodingData(name, long?.toString() ?: "", lat?.toString() ?: ""))
                } else {
                    ResultAPI.Error(Exception("Response body is null"))
                }
            } else {
                ResultAPI.Error(Exception("Unsuccessful response"))
            }
        } catch (e: Exception) {
            ResultAPI.Error(e)
        }
    }
}