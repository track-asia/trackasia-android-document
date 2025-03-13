package com.trackasia.trackasiademotest

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.trackasia.android.TrackAsia
import com.trackasia.android.camera.CameraUpdateFactory
import com.trackasia.android.geometry.LatLng
import com.trackasia.android.maps.Style
import com.trackasia.android.maps.TrackAsiaMap
import com.trackasia.trackasiademotest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var styleUrl = "https://maps.track-asia.com/styles/v1/streets.json?key=public_key"
    private lateinit var trackasiaMap: TrackAsiaMap
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TrackAsia.getInstance(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initMap()
    }

    private fun initMap() {
        binding.mapTrack.getMapAsync { map ->
            this.trackasiaMap = map
            map.setStyle(Style.Builder().fromUri(styleUrl))
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(10.7769, 106.7009), 12.0) // Ví dụ tọa độ TP.HCM
            map.moveCamera(cameraUpdate)
        }
    }

    override fun onStart() {
        super.onStart()
        binding.mapTrack.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapTrack.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapTrack.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapTrack.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapTrack.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapTrack.onLowMemory()
    }
}