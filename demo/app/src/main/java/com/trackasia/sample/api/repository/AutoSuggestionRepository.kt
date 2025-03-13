package com.trackasia.sample.api.repository

import android.app.Activity
import android.util.Log
import com.trackasia.sample.api.RetrofitClient
import com.trackasia.sample.api.model.AutoSuggestionResponse
import com.trackasia.sample.api.service.ApiSuggestionService
import com.trackasia.sample.utils.MapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getAutoSuggestion(text: String, idCountry: String, context: Activity): ResultAPI<AutoSuggestionResponse> {
    return withContext(Dispatchers.IO) {
        try {
            val domain = MapUtils(context).urlDomain(idCountry)
            Log.i("DOMAIN","DOMAIN ============>: $domain")
            val service: ApiSuggestionService = RetrofitClient.getInstance(domain).create(
                ApiSuggestionService::class.java)
            val call = service.getSuggestions(text)
            val response = call.execute()
            if (response.isSuccessful) {
                val data = response.body()
                if (data?.features != null && data.features.isNotEmpty()) {
                    ResultAPI.Success(AutoSuggestionResponse(features = data.features))
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