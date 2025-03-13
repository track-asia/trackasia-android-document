package com.trackasia.sample.api.service

import com.trackasia.sample.api.model.APIResponse
import com.trackasia.sample.api.model.AutoSuggestionResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface APIService {
    @GET("api/v1/reverse")
    fun reverseGeocode(
        @Query("lang") lang: String,
        @Query("point.lon") lon: Double,
        @Query("point.lat") lat: Double
    ): Call<APIResponse>
}

interface ApiSuggestionService {
    @GET("/api/v1/autocomplete?lang=vi&key=public")
    fun getSuggestions(@Query("text") text: String): Call<AutoSuggestionResponse>
}

