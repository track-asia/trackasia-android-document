package com.trackasia.sample

import SuggestionAdapter
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.trackasia.android.TrackAsia
import com.trackasia.android.annotations.IconFactory
import com.trackasia.android.annotations.MarkerOptions
import com.trackasia.android.camera.CameraPosition
import com.trackasia.android.camera.CameraUpdateFactory
import com.trackasia.android.geometry.LatLng
import com.trackasia.android.location.permissions.PermissionsListener
import com.trackasia.android.location.permissions.PermissionsManager
import com.trackasia.android.maps.Style
import com.trackasia.android.maps.TrackAsiaMap
import com.trackasia.geojson.Point
import com.trackasia.navigation.android.navigation.ui.v5.NavigationLauncher
import com.trackasia.navigation.android.navigation.ui.v5.NavigationLauncherOptions
import com.trackasia.navigation.android.navigation.ui.v5.instruction.InstructionView
import com.trackasia.navigation.android.navigation.ui.v5.route.NavigationMapRoute
import com.trackasia.navigation.android.navigation.ui.v5.route.NavigationRoute
import com.trackasia.navigation.android.navigation.v5.models.DirectionsCriteria
import com.trackasia.navigation.android.navigation.v5.models.DirectionsResponse
import com.trackasia.navigation.android.navigation.v5.models.DirectionsRoute
import com.trackasia.navigation.android.navigation.v5.navigation.TrackAsiaNavigation
import com.trackasia.navigation.android.navigation.v5.routeprogress.ProgressChangeListener
import com.trackasia.navigation.android.navigation.v5.routeprogress.RouteProgress
import com.trackasia.sample.adapter.AddressRouterAdapter
import com.trackasia.sample.api.model.Feature
import com.trackasia.sample.databinding.FragmentMapWayPointBinding
import com.trackasia.sample.utils.LoadingDialog
import com.trackasia.sample.utils.MapUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MapWayPointFragment : Fragment(), PermissionsListener, ProgressChangeListener {

    private var _binding: FragmentMapWayPointBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapboxMap: TrackAsiaMap
    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private var navigationMapRoute: NavigationMapRoute? = null
    private val instructionView: InstructionView? = null
    private var simulateRoute = false
    private var route: DirectionsRoute? = null
    private val mapUtils: MapUtils by lazy { MapUtils(requireActivity()) }
    private var pointNumber = 1
    private var addressFrom: Point? = null
    private var addressTo: Point? = null
    private var addressList: List<String>? = null
    private var addressListData: List<Feature>? = null
    private var isAddressFrom = true
    private var points: MutableList<Point> = mutableListOf()
    private val viewModel: MainViewModel by viewModels()


    private var styleUrl = "https://maps.track-asia.com/styles/v1/streets.json?key=public"
    private var latLngLocation: LatLng? = LatLng(10.728073, 106.624054)
    private var zoomLocation: Double = 10.0
    private var zoomLevel: Double = 16.0
    private var idCountry: String? = "vn"
    private var isStyle3D = true
    private var addressCurrent: RouterAddressModel? = null
    private var distance: String? = ""
    private var timer: String? = ""
    private lateinit var addressRouterAdapter: AddressRouterAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var loading: LoadingDialog

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
        _binding = FragmentMapWayPointBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)
        initControl()
        initListener()
        initMap()
        initAddressRouter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initAddressRouter() {
        addressRouterAdapter = AddressRouterAdapter(mutableListOf())
        val layoutManager = LinearLayoutManager(context)
        binding.rvAddressRouter.layoutManager = layoutManager
        binding.rvAddressRouter.adapter = addressRouterAdapter

    }

    private fun initMap() {
        binding.mapView.getMapAsync { map ->
            try {
                Log.d("DOMAIN URL STYLE:", styleUrl)
                map.setStyle(
                    Style.Builder().fromUri(styleUrl)
                ) { style ->
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
                this.mapboxMap = map
//                mapboxMap.addOnMapClickListener { point ->
//                    showPointMap(point)
//                    caculaterDirectionMap(point)
//                    return@addOnMapClickListener true
//                }
                navigationMapRoute = NavigationMapRoute(binding.mapView, map)
                cameraAnimation(latLngLocation!!, zoomLevel)
            } catch (e: Exception) {
                Log.d("ERROR MAP:", e.toString())
            }
        }

        binding.locationMy.setOnClickListener {
            try {
                val userLocation = mapboxMap.locationComponent.lastKnownLocation
                if (userLocation != null) {
                    if (userLocation.longitude > 0) { //emulator location error
                        latLngLocation = LatLng(userLocation.latitude, userLocation.longitude)
                    }
                    cameraAnimation(latLngLocation!!, zoomLevel)
//                    showPointMap(latLngLocation!!)
                }
            } catch (e: Exception) {
                Log.d("ERROR:", e.toString())
            }
        }
        binding.navigationRouter.setOnClickListener {
            if (addressFrom != null && addressTo != null) {
                val startLocation = "${addressFrom!!.latitude()},${addressFrom!!.longitude()}"
                val endLocation = "${addressTo!!.latitude()},${addressTo!!.longitude()}"
                val navigationIntentUri =
                    Uri.parse("google.navigation:q=$endLocation&origin=$startLocation")
                val navigationIntent = Intent(Intent.ACTION_VIEW, navigationIntentUri)
                navigationIntent.setPackage("com.google.android.apps.maps")
                navigationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (navigationIntent.resolveActivity(context?.packageManager!!) != null) {
                    navigationIntent.resolveActivity(requireActivity().packageManager)?.let {
                        requireContext().startActivity(navigationIntent)
                    }
                } else {
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$startLocation&destination=$endLocation")
                    )
                    requireContext().startActivity(webIntent)
                }
            } else {
                Toast.makeText(activity, "Input Positon to Navigation", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun initControl() {
        loading = LoadingDialog(requireContext())
        binding.btnMyLocation1.visibility = View.VISIBLE
        binding.btnMyLocation2.visibility = View.VISIBLE
        binding.clDistance.visibility = View.GONE
        binding.clDistance2.visibility = View.GONE
        binding.clDirection.visibility = View.GONE
        binding.btnClear1.visibility = View.GONE
        binding.btnClear2.visibility = View.GONE
        binding.startRouteButton.setOnClickListener {
            route?.let { route ->
                val userLocation = mapboxMap.locationComponent.lastKnownLocation ?: return@let

                val options = NavigationLauncherOptions.builder().directionsRoute(route)
                    .shouldSimulateRoute(simulateRoute).initialMapCameraPosition(
                        CameraPosition.Builder()
                            .target(LatLng(userLocation.latitude, userLocation.longitude)).build()
                    ).lightThemeResId(R.style.TestNavigationViewLight)
                    .darkThemeResId(R.style.TestNavigationViewDark).build()
                NavigationLauncher.startNavigation(activity, options)
            }
        }
        binding.edtAddressFrom.addTextChangedListener(edtTextChange(true))
        binding.edtAddressFrom.setOnItemClickListener { parent, view, position, id ->
            if (addressListData?.isNotEmpty() == true) {
                val item = addressListData?.get(position)
                val lat = item?.geometry?.coordinates!![1]
                val lng = item?.geometry?.coordinates!![0]
                val currentLatlng = LatLng(lat, lng)
                showPointMap(currentLatlng)
                addressFrom = Point.fromLngLat(lng, lat)
                pointNumber = 1
                caculaterDirectionMap(currentLatlng)
                cameraAnimation(point = currentLatlng, zoomLevel)
                hideKeyboard(requireActivity())
            }
        }
        binding.btnClear1.setOnClickListener {
            binding.edtAddressFrom.clearFocus()
            binding.edtAddressFrom.setText("")
            binding.btnMyLocation1.visibility = View.VISIBLE
            binding.btnClear1.visibility = View.GONE
            if (mapboxMap.markers.size > 0) {
                mapboxMap.removeMarker(mapboxMap.markers.first())
            }
            if (navigationMapRoute != null) {
                navigationMapRoute!!.removeRoute()
            }
        }
        binding.btnClear2.setOnClickListener {
            if (mapboxMap.markers.size > 0) {
                mapboxMap.removeMarker(mapboxMap.markers.last())
            }
            if (navigationMapRoute != null) {
                navigationMapRoute!!.removeRoute()
            }
            binding.edtAddressTo.clearFocus()
            binding.edtAddressTo.setText("")
            binding.btnMyLocation2.visibility = View.VISIBLE
            binding.btnClear2.visibility = View.GONE

        }
        binding.edtAddressTo.addTextChangedListener(edtTextChange(false))
        binding.edtAddressTo.setOnItemClickListener { parent, view, position, id ->
            if (addressListData?.isNotEmpty() == true) {
                val item = addressListData?.get(position)
                val lat = item?.geometry?.coordinates!![1]
                val lng = item?.geometry?.coordinates!![0]
                val currentLatlng = LatLng(lat, lng)
                pointNumber = 2
                addressTo = Point.fromLngLat(lng, lat)
                showPointMap(currentLatlng)
                caculaterDirectionMap(currentLatlng)
                cameraAnimation(point = currentLatlng, zoomLevel)
                hideKeyboard(requireActivity())
            }
        }
        binding.btnMyLocation1.setOnClickListener {
            loading.show()
            val userLocation = mapboxMap.locationComponent.lastKnownLocation
            isAddressFrom = true
            if (userLocation != null) {
                if (userLocation.longitude > 0) {
                    latLngLocation = LatLng(userLocation.latitude, userLocation.longitude)
                }
                cameraAnimation(latLngLocation!!, zoomLevel)
                showPointMap(latLngLocation!!)
            }
        }
        binding.btnMyLocation2.setOnClickListener {
            loading.show()
            val userLocation = mapboxMap.locationComponent.lastKnownLocation
            isAddressFrom = false
            if (userLocation != null) {
                if (userLocation.longitude > 0) {
                    latLngLocation = LatLng(userLocation.latitude, userLocation.longitude)
                }
                cameraAnimation(latLngLocation!!, zoomLevel)
                showPointMap(latLngLocation!!)
            }
        }
    }

    private fun edtTextChange(isAddressChange: Boolean): TextWatcher? {
        return object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, count: Int) {

            }

            override fun afterTextChanged(editable: Editable) {
                isAddressFrom = isAddressChange
                if (isAddressFrom) {
                    if (editable.toString().isNotEmpty()) {
                        binding.btnMyLocation1.visibility = View.GONE
                        binding.btnClear1.visibility = View.VISIBLE
                    } else {
                        binding.btnMyLocation1.visibility = View.VISIBLE
                        binding.btnClear1.visibility = View.GONE
                    }
                } else {
                    if (editable.toString().isNotEmpty()) {
                        binding.btnMyLocation2.visibility = View.GONE
                        binding.btnClear2.visibility = View.VISIBLE
                    } else {
                        binding.btnMyLocation2.visibility = View.VISIBLE
                        binding.btnClear2.visibility = View.GONE
                    }
                }
                if (editable.toString().length >= 3) {
                    getAutoSuggestion(editable.toString())
                }
            }
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

    private class MyBroadcastReceiver(navigation: TrackAsiaNavigation) : BroadcastReceiver() {
        private val weakNavigation: WeakReference<TrackAsiaNavigation> = WeakReference(navigation)

        override fun onReceive(context: Context, intent: Intent) {
            val navigation = weakNavigation.get()
            navigation?.stopNavigation()
        }
    }

    private fun initListener() {
        viewModel.geoCodingData.observe(requireActivity()) { data ->
            loading.dismiss()
            if (data != null) {
                if (data.lat != null && data.lat != "" && data.long != null && data.long != "") {
                    val position = LatLng(data.lat.toDouble(), data.long.toDouble())
                    setPositionMap(position, data.name)
                    if (data.name != null) {
                        if (isAddressFrom) {
                            addressFrom =
                                Point.fromLngLat(data.long.toDouble(), data.lat.toDouble())
                            binding.edtAddressFrom.setText(data.name)
                        } else {
                            addressTo = Point.fromLngLat(data.long.toDouble(), data.lat.toDouble())
                            binding.edtAddressTo.setText(data.name)
                        }
                        addressCurrent = RouterAddressModel("", data.name, "", null)
                    }
                } else {
                    val latlng = MapUtils(requireActivity()).getLatlng(idCountry)
                    cameraAnimation(latlng, zoomLevel)
                }
            }
        }
        viewModel.autoSuggestionData.observe(requireActivity()) { data ->
            loading.dismiss()
            Log.i("DATA SEARCH: ========>", data.toString())
            if (data != null) {
                addressListData = data
                addressList = data.map { it.properties.label }
                if (addressList?.isNotEmpty() == true) {
                    val adapter = SuggestionAdapter(
                        requireContext(), android.R.layout.simple_dropdown_item_1line, addressList!!
                    )
                    if (isAddressFrom) {
                        binding.edtAddressFrom.setAdapter(adapter)
                    } else {
                        binding.edtAddressTo.setAdapter(adapter)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
        }
        binding.clearPoints.setOnClickListener {
            loading.dismiss()
            pointNumber = 0
            mapboxMap.markers.clear()
            mapboxMap.clear()
            if (navigationMapRoute != null) {
                navigationMapRoute!!.removeRoute()
            }
            points.clear()
            binding.edtAddressFrom.clearFocus()
            binding.edtAddressFrom.setText("")
            binding.edtAddressTo.clearFocus()
            binding.edtAddressTo.setText("")
            binding.btnMyLocation1.visibility = View.VISIBLE
            binding.btnMyLocation2.visibility = View.VISIBLE
            binding.btnClear1.visibility = View.GONE
            binding.btnClear2.visibility = View.GONE
            binding.clDirection.visibility = View.GONE
            addressRouterAdapter.clearData()
        }

        binding.map3d.setOnClickListener {
            styleUrl = MapUtils(requireActivity()).urlStyle(idCountry, isStyle3D)
            binding.map3d.setImageResource(if (!isStyle3D) R.drawable.ic_map_3d else R.drawable.ic_map_2d)
            isStyle3D = !isStyle3D
            mapboxMap.setStyle(styleUrl)
        }
    }


    private fun setPositionMap(point: LatLng, name: String?) {
        pointNumber
        cameraAnimation(point, zoomLevel)
        val colorResId = if (pointNumber == 1) {
            R.color.holo_blue_dark
        } else if (pointNumber == points.size) {
            R.color.holo_green_dark
        } else {
            R.color.holo_yellow_dark
        }
        var snippet = "Lat: ${point.latitude} Lng: ${point.longitude}"
        val markerOptionsFirst = MarkerOptions().position(point).title(name).snippet(snippet)
            .icon(mapUtils.createStoreMarker(
                pointNumber.toString(), colorResId
            )?.let { IconFactory.getInstance(requireContext()).fromBitmap(it) })
        mapboxMap.addMarker(markerOptionsFirst)
    }


    private fun caculaterDirectionMap(point: LatLng) {
        if (addressFrom != null && addressTo != null) {
//            var routeOptions = RouteOptions(
//                // These dummy route options are not not used to create directions,
//                // but currently they are necessary to start the navigation
//                // and to use the voice instructions.
//                // Again, this isn't ideal, but it is a requirement of the framework.
//                baseUrl = "https://maps.track-asia.com/route/v1",
//                profile = "car",
//                accessToken = "public_key",
//                steps = true,
//
//                geometries = "polyline",
//                overview = "full",
//            )
//            var navigation =
//                NavigationRoute.builder(context).accessToken(getString(R.string.mapbox_access_token))
//                    .voiceUnits(DirectionsCriteria.METRIC).profile("car")
//                    .baseUrl("https://api.mapbox.com")
//                    .origin(addressFrom!!)
//                    .destination(addressTo!!)

            var navigation = NavigationRoute.builder(context).apply {
                this.accessToken(getString(R.string.mapbox_access_token))
                this.origin(addressFrom!!)
                this.destination(addressTo!!)
                this.voiceUnits(DirectionsCriteria.METRIC)
                this.alternatives(true)
                    .baseUrl("https://api.mapbox.com")
            }
            navigation.build().getRoute(object : Callback<DirectionsResponse> {
                override fun onResponse(
                    call: Call<DirectionsResponse>, response: Response<DirectionsResponse>
                ) {
                    print("NavigationRoute ====>")
                    response.body()?.let { response ->
                        if (response.routes.isNotEmpty()) {
                            val trackasiaResponse = DirectionsResponse.fromJson(response.toJson())
                            binding.clDirection.visibility = View.VISIBLE
                            if (trackasiaResponse.routes.size > 1) {
                                route = trackasiaResponse.routes.first()
                                binding.textDistanceValue.text =
                                    String.format("%.2fkm", route?.distance?.div(1000))
                                binding.textTimeValue.text =
                                    route?.duration?.toLong()?.let { formatDuration(it) }
                                binding.clDistance.visibility = View.VISIBLE
                                route = trackasiaResponse.routes[1]
                                binding.textDistanceValue2.text =
                                    String.format("%.2fkm", route?.distance?.div(1000))
                                binding.textTimeValue2.text =
                                    route?.duration?.toLong()?.let { formatDuration(it) }
                                binding.clDistance2.visibility = View.VISIBLE
                            } else {
                                route = trackasiaResponse.routes.first()
                                binding.textDistanceValue.text =
                                    String.format("%.2fkm", route?.distance?.div(1000))
                                binding.textTimeValue.text =
                                    route?.duration?.toLong()?.let { formatDuration(it) }
                                binding.clDistance.visibility = View.VISIBLE
                                binding.clDistance2.visibility = View.GONE
                            }
                            navigationMapRoute?.addRoutes(trackasiaResponse.routes)
                        }
                    }
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    print(t.message)
                }

            })
        }
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        return when {
            hours > 0 -> String.format("%02dh:%02dm:%02ds", hours, minutes, remainingSeconds)
            minutes > 0 -> String.format("%02dm:%02ds", minutes, remainingSeconds)
            else -> String.format("%02d", remainingSeconds)
        }
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
        idCountry?.let { viewModel.funCallAutoSuggestion(text, it, requireActivity()) }
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
            // Xử lý khi quyền truy cập vị trí không được cấp
        }
    }

    override fun onProgressChange(p0: Location, routeProgress: RouteProgress) {
//        binding.instructionView.updateDistanceWith(routeProgress);
    }
}