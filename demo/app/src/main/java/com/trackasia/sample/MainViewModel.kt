package com.trackasia.sample

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackasia.android.geometry.LatLng
import com.trackasia.sample.api.model.Feature
import com.trackasia.sample.api.model.GeoCodingData
import com.trackasia.sample.api.repository.ResultAPI
import com.trackasia.sample.api.repository.getAutoSuggestion
import com.trackasia.sample.api.repository.showPointMap
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _geoCodingData = MutableLiveData<GeoCodingData>()
    val geoCodingData: LiveData<GeoCodingData> get() = _geoCodingData
    private val _autoSuggestionData = MutableLiveData<List<Feature>>()
    val autoSuggestionData: LiveData<List<Feature>> get() = _autoSuggestionData

    fun funCallShowPointMap(point: LatLng, context: Activity) {
        viewModelScope.launch {
            when (val result = showPointMap(point, context)) {
                is ResultAPI.Success -> {
                    val geoCodingData = result.data
                    _geoCodingData.value = geoCodingData
                }

                is ResultAPI.Error -> {
                    val exception = result.exception
                    _geoCodingData.value = GeoCodingData(name = "", long = "", lat = "")
                }
            }
        }
    }

    fun funCallAutoSuggestion(text: String, idCountry: String, context: Activity) {
        viewModelScope.launch {
            when (val result = getAutoSuggestion(text, idCountry, context)) {
                is ResultAPI.Success -> {
                    val autoSuggestionData = result.data.features
                    _autoSuggestionData.value = autoSuggestionData
                }

                is ResultAPI.Error -> {
                    val exception = result.exception
                    _geoCodingData.value = GeoCodingData(name = "", long = "", lat = "")
                }
            }
        }
    }
}