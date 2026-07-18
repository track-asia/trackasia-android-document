package com.trackasia.trackasiademotest

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.trackasia.android.TrackAsia
import com.trackasia.android.camera.CameraPosition
import com.trackasia.android.camera.CameraUpdateFactory
import com.trackasia.android.geometry.LatLng
import com.trackasia.android.location.LocationComponentActivationOptions
import com.trackasia.android.location.modes.CameraMode
import com.trackasia.android.location.modes.RenderMode
import com.trackasia.android.location.permissions.PermissionsListener
import com.trackasia.android.location.permissions.PermissionsManager
import com.trackasia.android.maps.Style
import com.trackasia.android.maps.TrackAsiaMap
import com.trackasia.trackasiademotest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), PermissionsListener {

    private val styleUrl = "https://maps.track-asia.com/styles/v2/streets.json?key=public"
    private lateinit var trackasiaMap: TrackAsiaMap
    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionsManager: PermissionsManager

    private val defaultLocation = LatLng(10.7769, 106.7009)
    private val defaultZoom = 12.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TrackAsia.getInstance(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        permissionsManager = PermissionsManager(this)
        initMap()
    }

    private fun initMap() {
        binding.mapTrack.getMapAsync { map ->
            this.trackasiaMap = map
            map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                enableLocationComponent(style)
            }
            val cameraPosition = CameraPosition.Builder()
                .target(defaultLocation)
                .zoom(defaultZoom)
                .build()
            map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun enableLocationComponent(style: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            val locationComponent = trackasiaMap.locationComponent
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(this, style)
                    .useDefaultLocationEngine(true)
                    .build()
            )
            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.COMPASS
        } else {
            permissionsManager.requestLocationPermissions(this)
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

    override fun onDestroy() {
        super.onDestroy()
        binding.mapTrack.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapTrack.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapTrack.onLowMemory()
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, "Location permission needed for map features", Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            trackasiaMap.getStyle { style ->
                enableLocationComponent(style)
            }
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}