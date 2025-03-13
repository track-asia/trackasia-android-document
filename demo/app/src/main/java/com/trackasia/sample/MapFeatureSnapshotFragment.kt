package com.trackasia.sample

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.trackasia.android.TrackAsia
import com.trackasia.android.camera.CameraPosition
import com.trackasia.android.geometry.LatLng
import com.trackasia.android.location.permissions.PermissionsListener
import com.trackasia.android.location.permissions.PermissionsManager
import com.trackasia.android.maps.MapView
import com.trackasia.android.maps.Style
import com.trackasia.android.maps.TrackAsiaMap
import com.trackasia.sample.databinding.FragmentFeatureSnapshotBinding
import com.trackasia.sample.utils.MapUtils


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MapFeatureSnapshotFragment : Fragment(), PermissionsListener, View.OnClickListener {

    private var _binding: FragmentFeatureSnapshotBinding? = null
    private lateinit var mapboxMap: TrackAsiaMap
    private lateinit var googleMap: GoogleMap

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding
    private var permissionsManager: PermissionsManager = PermissionsManager(this)

    private var styleUrl = "https://maps.track-asia.com/styles/v1/streets.json?key=public"
    private var latLngLocation: LatLng? = LatLng(10.728073, 106.624054)
    private var zoomLocation: Double = 10.0
    private var idCountry: String? = "vn"
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragmentContext: Context? = context
        fragmentContext?.let {
            sharedPreferences = it.getSharedPreferences("trackasia", Context.MODE_PRIVATE)
            idCountry = sharedPreferences.getString("country", "vn")
            styleUrl = MapUtils(requireActivity()).urlStyle(idCountry)
            latLngLocation = MapUtils(requireActivity()).getLatlng(idCountry)
            zoomLocation = MapUtils(requireActivity()).zoom(idCountry)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        TrackAsia.getInstance(requireActivity())
        _binding = FragmentFeatureSnapshotBinding.inflate(inflater, container, false)
        return binding?.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.mapView2?.onCreate(savedInstanceState)
        initMap()
        initControl()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("MissingPermission")
    private fun initMap() {
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapView1) as? SupportMapFragment
        mapFragment?.getMapAsync { map ->
            googleMap = map
            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.isZoomControlsEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true
            var lat = 10.728073
            var lng = 106.624054
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    com.google.android.gms.maps.model.LatLng(
                        lat,
                        lng
                    ), 10.0F
                )
            )
        }
        binding?.mapView2?.getMapAsync { map ->
            Log.d("DOMAIN URL STYLE:", styleUrl)
            this.mapboxMap = map
            map.setStyle(
                Style.Builder().fromUri(styleUrl)
            ) { style ->
                if(activity != null) {
                    MapUtils(requireActivity()).enableLocationComponent(
                        style,
                        idCountry,
                        mapboxMap,
                        permissionsManager,
                        latLngLocation!!, zoomLocation
                    )
                }
            }
            cameraAnimation(latLngLocation!!)
            binding?.mapView2?.addOnDidFinishRenderingFrameListener(idleListener)
        }
        binding?.locationMy?.setOnClickListener {
            val userLocation = mapboxMap.locationComponent.lastKnownLocation
            if (userLocation != null) {
                mapboxMap.cameraPosition =
                    CameraPosition.Builder()
                        .target(LatLng(userLocation.latitude, userLocation.longitude)).zoom(zoomLocation)
                        .build()
            }
        }
    }

    private fun cameraAnimation(point: LatLng) {
        try {
            val cameraPosition = CameraPosition.Builder().target(point).zoom(zoomLocation).build()
            val cameraUpdate = com.trackasia.android.camera.CameraUpdateFactory.newCameraPosition(cameraPosition)
            mapboxMap.animateCamera(cameraUpdate, 1000)
        } catch (e: Exception) {
            Log.d("ERROR CAMERA", e.toString())
        }
    }

    private fun initControl() {
        binding?.btnBack?.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {

    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapboxMap.getStyle { style ->
                if(activity != null) {
                    MapUtils(requireActivity()).enableLocationComponent(
                        style,
                        idCountry,
                        mapboxMap,
                        permissionsManager,
                        latLngLocation!!, zoomLocation
                    )
                }
            }
        } else {
            // Xử lý khi quyền truy cập vị trí không được cấp
        }
    }

    override fun onClick(v: View?) {
        TODO("Not yet implemented")
    }

    private val idleListener = object : MapView.OnDidFinishRenderingFrameListener {
        override fun onDidFinishRenderingFrame(fully: Boolean, p1: Double, p2: Double) {
            try {
                if (fully) {
                    binding?.mapView2?.removeOnDidFinishRenderingFrameListener(this)
                    mapboxMap.snapshot { snapshot ->
                        val targetLatLng: LatLng? = mapboxMap.cameraPosition.target
                        val zoomLevel: Double = mapboxMap.cameraPosition.zoom
                        if (targetLatLng != null) {
                            googleMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    com.google.android.gms.maps.model.LatLng(
                                        targetLatLng.latitude,
                                        targetLatLng.longitude
                                    ),
                                    (zoomLevel + 1).toFloat()
                                )
                            )
                        }
                        binding?.mapView2?.addOnDidFinishRenderingFrameListener(this)
                    }
                }
            } catch (e: Exception) {

            }
        }
    }

}