package com.trackasia.sample

import SuggestionAdapter
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.trackasia.android.TrackAsia
import com.trackasia.android.annotations.MarkerOptions
import com.trackasia.android.camera.CameraPosition
import com.trackasia.android.camera.CameraUpdateFactory
import com.trackasia.android.geometry.LatLng
import com.trackasia.android.location.permissions.PermissionsListener
import com.trackasia.android.location.permissions.PermissionsManager
import com.trackasia.android.maps.Style
import com.trackasia.android.maps.TrackAsiaMap
import com.trackasia.geojson.Point
import com.trackasia.navigation.android.navigation.ui.v5.route.NavigationMapRoute
import com.trackasia.navigation.android.navigation.v5.navigation.TrackAsiaNavigation
import com.trackasia.sample.api.model.Feature
import com.trackasia.sample.api.model.GeoCodingData
import com.trackasia.sample.databinding.FragmentMapSinglePointBinding
import com.trackasia.sample.utils.MapUtils
import java.lang.ref.WeakReference

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class MapSinglePointFragment : Fragment(), PermissionsListener {

    private var _binding: FragmentMapSinglePointBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapboxMap: TrackAsiaMap
    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private var navigationMapRoute: NavigationMapRoute? = null
    private var addressTo: Point? = null
    private var addressList: List<String>? = null
    private var addressListData: List<Feature>? = null
    private var styleUrl = "https://maps.track-asia.com/styles/v1/streets.json?key=public"
    private lateinit var sharedPreferences: SharedPreferences
    private var latLngLocation: LatLng? = LatLng(10.728073, 106.624054)
    private var zoomLocation: Double = 10.0
    private var zoomLevel: Double = 16.0
    private var isStyle3D = true
    private var idCountry: String? = "vn"

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragmentContext: Context? = context
        fragmentContext?.let {
            sharedPreferences = it.getSharedPreferences("trackasia", Context.MODE_PRIVATE)
            idCountry = sharedPreferences.getString("country", "vn")
            styleUrl = MapUtils(requireActivity()).urlStyle(idCountry, false)
            latLngLocation = MapUtils(requireActivity()).getLatlng(idCountry)
            zoomLocation = MapUtils(requireActivity()).zoom(idCountry)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        TrackAsia.getInstance(requireActivity())
        _binding = FragmentMapSinglePointBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)
        initControl()
        initListener()
        initMap()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initMap() {
        binding.mapView.getMapAsync { map ->
            try {
                Log.d("DOMAIN URL STYLE:", styleUrl)
                this.mapboxMap = map
                map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                    if (activity != null) {
                        MapUtils(requireActivity()).enableLocationComponent(
                            style,
                            idCountry,
                            mapboxMap,
                            permissionsManager,
                            latLngLocation!!,
                            zoomLocation
                        )
                    }
                }
                mapboxMap.addOnMapClickListener { point ->
                    showPointMap(point)
                    return@addOnMapClickListener true
                }
                navigationMapRoute = NavigationMapRoute(binding.mapView, map)
                cameraAnimation(latLngLocation!!, zoomLevel)
            } catch (e: Exception) {
                Log.d("ERROR MAP:", e.toString())
            }
        }
    }

    private fun initControl() {
        binding.edtAddressTo.addTextChangedListener(edtTextChange())
        binding.edtAddressTo.setOnItemClickListener { parent, view, position, id ->
            if (addressListData?.isNotEmpty() == true) {
                val item = addressListData?.get(position)
                val lat = item?.geometry?.coordinates!![1]
                val lng = item?.geometry?.coordinates!![0]
                val currentLatlng = LatLng(lat, lng)
                val snippet = "Lat: $lat Lng: $lng"
                addressTo = Point.fromLngLat(lat, lng)
                mapboxMap.clear()
                mapboxMap.addMarker(
                    MarkerOptions().position(currentLatlng).title(item.properties.name)
                        .snippet(snippet)
                )
                cameraAnimation(point = currentLatlng, zoomLevel)
                hideKeyboard(requireActivity())
            }
        }
    }

    private class MyBroadcastReceiver(navigation: TrackAsiaNavigation) : BroadcastReceiver() {
        private val weakNavigation: WeakReference<TrackAsiaNavigation> = WeakReference(navigation)

        override fun onReceive(context: Context, intent: Intent) {
            val navigation = weakNavigation.get()
            navigation?.stopNavigation()
        }
    }


    fun hideKeyboard(activity: Activity) {
        val imm =
            activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }


    private fun initListener() {
        viewModel.geoCodingData.observe(requireActivity()) { data ->
            if (data != null) {
                setPositionMap(data)
            }
        }
        viewModel.autoSuggestionData.observe(requireActivity()) { data ->
            Log.i("DATA SEARCH: ========>", data.toString())
            if (data != null) {
                addressListData = data
                addressList = data.map { it.properties.label }
                if (addressList?.isNotEmpty() == true) {
                    val adapter = SuggestionAdapter(
                        requireContext(), android.R.layout.simple_dropdown_item_1line, addressList!!
                    )
                    binding.edtAddressTo.setAdapter(adapter)
                    adapter.notifyDataSetChanged()
                }
            }
        }
        binding.btnClear2.setOnClickListener {
            binding.edtAddressTo.clearFocus()
            binding.edtAddressTo.setText("")
        }
        binding.locationMy.setOnClickListener {
            try {
                val userLocation = mapboxMap.locationComponent.lastKnownLocation
                if (userLocation != null) {
                    if (userLocation.longitude > 0) {
                        latLngLocation = LatLng(userLocation.latitude, userLocation.longitude)
                    }
                    showPointMap(latLngLocation!!)
                }
            } catch (e: Exception) {
                Log.d("ERROR:", e.toString())
            }
        }
        binding.map3d.setOnClickListener {
            styleUrl = MapUtils(requireActivity()).urlStyle(idCountry, isStyle3D)
            binding.map3d.setImageResource(if (!isStyle3D) R.drawable.ic_map_3d else R.drawable.ic_map_2d)
            isStyle3D = !isStyle3D
            mapboxMap.setStyle(styleUrl)
        }
        binding.navigationRouter.setOnClickListener {
            if (addressTo != null) {
                val endLocation = "${addressTo!!.longitude()},${addressTo!!.latitude()}"
                val navigationIntentUri =
                    Uri.parse("google.navigation:q=$endLocation")
                val navigationIntent = Intent(Intent.ACTION_VIEW, navigationIntentUri)
                navigationIntent.setPackage("com.google.android.apps.maps")
                navigationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (navigationIntent.resolveActivity(context?.packageManager!!) != null) {
                    requireContext().startActivity(navigationIntent)
                } else {
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$endLocation")
                    )
                    requireContext().startActivity(webIntent)
                }
            }else{
                Toast.makeText(context, "Input Positon to Navigation", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun edtTextChange(): TextWatcher? {
        return object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, count: Int) {

            }

            override fun afterTextChanged(editable: Editable) {
                if (editable.toString().length >= 3) {
                    getAutoSuggestion(editable.toString())
                }
            }
        }
    }


    private fun setPositionMap(data: GeoCodingData) {
        try {
            if (data.lat != null && data.lat != "" && data.long != null && data.long != "") {
                val point = Point.fromLngLat(data.lat.toDouble(), data.long.toDouble())
                val latlng = LatLng(data.lat.toDouble(), data.long.toDouble())
                addressTo = point
                if (data.name != null) {
                    binding.edtAddressTo.setText(data.name)
                }
                val snippet = "Lat: ${data.lat} Lng: ${data.long}"
                mapboxMap.clear()
                mapboxMap.addMarker(
                    MarkerOptions().position(LatLng(data.lat.toDouble(), data.long.toDouble()))
                        .title(data.name).snippet(snippet)
                )
                cameraAnimation(latlng, zoomLevel)
                data.name?.let { setToastMessage(name = it, snippet) }
            } else {
                val latlng = MapUtils(requireActivity()).getLatlng(idCountry)
                cameraAnimation(latlng, zoomLevel)
            }
        } catch (e: Exception) {
            Log.d("ERROR", e.toString())
        }
    }

    private fun setToastMessage(name: String, snippet: String) {
        Toast.makeText(
            requireActivity(), "Title: $name, Snippet: $snippet", Toast.LENGTH_SHORT
        ).show()
    }

    private fun cameraAnimation(point: LatLng, zoomLevel: Double?) {
        try {
            val cameraPosition =
                CameraPosition.Builder().target(point).zoom(zoomLevel ?: zoomLocation).build()
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)
            mapboxMap.animateCamera(cameraUpdate, 1000)
        } catch (e: Exception) {
            Log.d("ERROR CAMERA", e.toString())
        }
    }

    private fun showPointMap(point: LatLng) {
        var point = LatLng(point.latitude, point.longitude)
        viewModel.funCallShowPointMap(point, requireActivity())
    }

    private fun getAutoSuggestion(text: String) {
        viewModel.funCallAutoSuggestion(text, idCountry!!, requireActivity())
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }


    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {

    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapboxMap.getStyle { style ->
                if (activity != null) {
                    MapUtils(requireActivity()).enableLocationComponent(
                        style,
                        idCountry,
                        mapboxMap,
                        permissionsManager,
                        latLngLocation!!,
                        zoomLocation
                    )
                }
            }
        } else {
        }
    }

}