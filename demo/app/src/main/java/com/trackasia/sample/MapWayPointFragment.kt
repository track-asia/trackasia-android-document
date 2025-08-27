package com.trackasia.sample

import SuggestionAdapter
import android.annotation.SuppressLint
import android.app.Activity
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
import android.widget.TextView
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
import com.trackasia.android.geometry.LatLngBounds
import com.trackasia.android.location.LocationComponentActivationOptions
import com.trackasia.android.location.modes.CameraMode
import com.trackasia.android.location.modes.RenderMode
import com.trackasia.android.location.permissions.PermissionsListener
import com.trackasia.android.location.permissions.PermissionsManager
import com.trackasia.android.maps.Style
import com.trackasia.android.maps.TrackAsiaMap
import com.trackasia.geojson.Point
import com.trackasia.navigation.android.navigation.ui.v5.NavigationLauncher
import com.trackasia.navigation.android.navigation.ui.v5.NavigationLauncherOptions
import com.trackasia.navigation.android.navigation.ui.v5.route.NavigationMapRoute
import com.trackasia.navigation.core.models.DirectionsResponse
import com.trackasia.navigation.core.models.DirectionsRoute
import com.trackasia.navigation.core.models.RouteOptions
import com.trackasia.navigation.core.routeprogress.ProgressChangeListener
import com.trackasia.navigation.core.routeprogress.RouteProgress
import com.trackasia.sample.adapter.AddressRouterAdapter
import com.trackasia.sample.api.model.Feature
import com.trackasia.sample.api.model.GeoCodingData
import com.trackasia.sample.databinding.FragmentMapWayPointBinding
import com.trackasia.sample.utils.LoadingDialog
import com.trackasia.sample.utils.MapUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.Locale
import kotlin.math.pow

/**
 * Fragment hiển thị bản đồ và tìm đường đi giữa hai điểm
 */
class MapWayPointFragment : Fragment(), PermissionsListener, ProgressChangeListener {

    private var _binding: FragmentMapWayPointBinding? = null
    private val binding get() = _binding!!

    // Map components
    private lateinit var trackasiaMap: TrackAsiaMap
    private var navigationMapRoute: NavigationMapRoute? = null
    private var route: DirectionsRoute? = null
    private var points: MutableList<Point> = mutableListOf()
    private var permissionsManager: PermissionsManager? = null

    // Location and styling
    private var styleUrl = "https://maps.track-asia.com/styles/v1/streets.json?key=public"
    private var latLngLocation = LatLng(10.728073, 106.624054)
    private var zoomLocation = 10.0
    private var zoomLevel = 16.0
    private var idCountry: String? = "vn"
    private var isStyle3D = true

    // Navigation and route options
    private var simulateRoute = false
    private var pointNumber = 1
    private var isAddressFrom = true
    private var addressFrom: Point? = null
    private var addressTo: Point? = null

    // Search related
    private var addressCurrent: RouterAddressModel? = null
    private var addressList: List<String>? = null
    private var addressListData: List<Feature>? = null

    // UI components
    private lateinit var addressRouterAdapter: AddressRouterAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var loading: LoadingDialog

    // API
    private val language = Locale.getDefault().language
    private val apiKey = "public_key" // Replace with your API key
    private val client = OkHttpClient()
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo PermissionsManager
        permissionsManager = PermissionsManager(this)

        context?.let {
            sharedPreferences = it.getSharedPreferences("trackasia", Context.MODE_PRIVATE)
            idCountry = sharedPreferences.getString("country", "vn")
            styleUrl = MapUtils(requireActivity()).urlStyle(idCountry)
            latLngLocation = MapUtils(requireActivity()).getLatlng(idCountry)
            zoomLocation = MapUtils(requireActivity()).zoom(idCountry)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        TrackAsia.getInstance(requireActivity())
        _binding = FragmentMapWayPointBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)
        setupUI()
        setupObservers()
        initMap()
        initAddressRouter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initAddressRouter() {
        addressRouterAdapter = AddressRouterAdapter(mutableListOf())
        binding.rvAddressRouter.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = addressRouterAdapter
        }
    }

    private fun initMap() {
        binding.mapView.getMapAsync { map ->
            try {
                trackasiaMap = map
                map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                    setupMapComponents(style)
                }
                cameraAnimation(latLngLocation, zoomLevel)
            } catch (e: Exception) {
                Log.e("MAP_ERROR", "Error initializing map: ${e.message}")
                showToast("Lỗi khởi tạo bản đồ: ${e.message}")
            }
        }
    }

    private fun setupMapComponents(style: Style) {
        if (activity == null) return

        // Setup location component
        try {
            enableLocationComponent(style)
        } catch (e: Exception) {
            Log.e("LOCATION_ERROR", "Error enabling location: ${e.message}")
            showToast("Lỗi khi kích hoạt định vị: ${e.message}")
        }

        // Setup navigation route
        try {
            navigationMapRoute = NavigationMapRoute(null, binding.mapView, trackasiaMap)
            Log.d("NAVIGATION_ROUTE", "NavigationMapRoute initialized successfully")
        } catch (e: Exception) {
            Log.e("NAVIGATION_ROUTE", "Error initializing NavigationMapRoute: ${e.message}")
            showToast("Lỗi khởi tạo NavigationMapRoute: ${e.message}")
        }

        // Setup map click listener
        trackasiaMap.addOnMapClickListener { point ->
            handleMapClick(point)
            true
        }
    }

    private fun setupUI() {
        // Khởi tạo UI components
        loading = LoadingDialog(requireContext())

        // Thiết lập trạng thái mặc định
        binding.apply {
            btnMyLocation1.visibility = View.VISIBLE
            btnMyLocation2.visibility = View.VISIBLE
            clDistance.visibility = View.GONE
            clDistance2.visibility = View.GONE
            clDirection.visibility = View.GONE
            btnClear1.visibility = View.GONE
            btnClear2.visibility = View.GONE
            
            // Thiết lập trạng thái cho popup POI search
            poiSearchContainer.visibility = View.GONE
            
            // Prevent clicks on the popup from closing it
            val poiSearchCard = poiSearchContainer.findViewById<View>(R.id.poiSearchLayout)
            poiSearchCard?.setOnClickListener { 
                // Do nothing, prevent click propagation to container
                Log.d("POI_POPUP", "Clicked on POI search card")
            }
        }

        // Thiết lập các listener
        setupClickListeners()
        setupTextWatchers()
        setupAutoCompleteListeners()
    }

    private fun setupClickListeners() {
        with(binding) {
            // Navigation buttons
            trackNavigationRouter.setOnClickListener { handleTrackAsiaNavigation() }
            navigationRouter.setOnClickListener { handleGoogleNavigation() }
            simulateSwitch.setOnCheckedChangeListener { _, checked -> simulateRoute = checked }

            // Location buttons
            locationMy.setOnClickListener { navigateToUserLocation() }
            btnMyLocation1.setOnClickListener { handleLocationButton(true) }
            btnMyLocation2.setOnClickListener { handleLocationButton(false) }

            // Clear buttons
            btnClear1.setOnClickListener { clearOriginPoint() }
            btnClear2.setOnClickListener { clearDestinationPoint() }
            clearPoints.setOnClickListener { clearAllPoints() }

            // 3D map toggle
            map3d.setOnClickListener { toggle3DMap() }

            // Close popup when clicking outside
            poiSearchContainer.setOnClickListener { 
                Log.d("POI_POPUP", "Container clicked")
                hidePoiSearchPopup() 
            }
            
            // Setup POI category click listeners
            setupPoiCategoryListeners()
        }
    }

    private fun handleTrackAsiaNavigation() {
        if (route == null) {
            showToast("Tuyến đường chưa được tính toán")
            return
        }

        try {
            val userLocation = trackasiaMap.locationComponent.lastKnownLocation
            if (userLocation == null) {
                showToast("Không thể xác định vị trí hiện tại")
                return
            }

            startNavigation(userLocation)
        } catch (e: Exception) {
            Log.e("NAVIGATION_ERROR", "Error preparing navigation", e)
            showToast("Lỗi chuẩn bị điều hướng: ${e.message}")
        }
    }

    private fun handleGoogleNavigation() {
        if (addressFrom == null || addressTo == null) {
            showToast("Vui lòng chọn điểm đi và điểm đến")
            return
        }

        val startLocation = "${addressFrom!!.latitude()},${addressFrom!!.longitude()}"
        val endLocation = "${addressTo!!.latitude()},${addressTo!!.longitude()}"

        val navigationIntentUri =
            Uri.parse("google.navigation:q=$endLocation&origin=$startLocation")
        val navigationIntent = Intent(Intent.ACTION_VIEW, navigationIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            if (navigationIntent.resolveActivity(requireActivity().packageManager) != null) {
                requireContext().startActivity(navigationIntent)
            } else {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$startLocation&destination=$endLocation")
                )
                requireContext().startActivity(webIntent)
            }
        } catch (e: Exception) {
            showToast("Không thể mở ứng dụng bản đồ: ${e.message}")
        }
    }

    private fun navigateToUserLocation() {
        try {
            val userLocation = trackasiaMap.locationComponent.lastKnownLocation
            if (userLocation != null) {
                if (userLocation.longitude > 0) { // ignore emulator location error
                    latLngLocation = LatLng(userLocation.latitude, userLocation.longitude)
                }
                cameraAnimation(latLngLocation!!, zoomLevel)
            }
        } catch (e: Exception) {
            Log.d("LOCATION_ERROR", e.toString())
        }
    }

    private fun handleLocationButton(isOrigin: Boolean) {
        loading.show()
        val userLocation = trackasiaMap.locationComponent.lastKnownLocation
        isAddressFrom = isOrigin

        if (userLocation != null) {
            if (userLocation.longitude > 0) {
                latLngLocation = LatLng(userLocation.latitude, userLocation.longitude)
            }
            cameraAnimation(latLngLocation!!, zoomLevel)
            showPointMap(latLngLocation!!)
        } else {
            loading.dismiss()
            showToast("Không thể xác định vị trí hiện tại")
        }
    }

    private fun clearOriginPoint() {
        binding.edtAddressFrom.clearFocus()
        binding.edtAddressFrom.setText("")
        binding.btnMyLocation1.visibility = View.VISIBLE
        binding.btnClear1.visibility = View.GONE

        if (trackasiaMap.markers.isNotEmpty()) {
            trackasiaMap.removeMarker(trackasiaMap.markers.first())
        }
        addressFrom = null
        clearRouteIfNeeded()
    }

    private fun clearDestinationPoint() {
        if (trackasiaMap.markers.size > 1) {
            trackasiaMap.removeMarker(trackasiaMap.markers.last())
        }

        binding.edtAddressTo.clearFocus()
        binding.edtAddressTo.setText("")
        binding.btnMyLocation2.visibility = View.VISIBLE
        binding.btnClear2.visibility = View.GONE
        addressTo = null
        clearRouteIfNeeded()
    }

    private fun clearRouteIfNeeded() {
        if (navigationMapRoute != null) {
            navigationMapRoute!!.removeRoute()
            binding.clDirection.visibility = View.GONE
        }
    }

    private fun clearAllPoints() {
        loading.dismiss()
        pointNumber = 0
        addressFrom = null
        addressTo = null
        trackasiaMap.markers.clear()
        trackasiaMap.clear()
        clearRouteIfNeeded()
        points.clear()

        binding.apply {
            edtAddressFrom.clearFocus()
            edtAddressFrom.setText("")
            edtAddressTo.clearFocus()
            edtAddressTo.setText("")
            btnMyLocation1.visibility = View.VISIBLE
            btnMyLocation2.visibility = View.VISIBLE
            btnClear1.visibility = View.GONE
            btnClear2.visibility = View.GONE
        }

        addressRouterAdapter.clearData()
    }

    private fun toggle3DMap() {
        isStyle3D = !isStyle3D
        styleUrl = MapUtils(requireActivity()).urlStyle(idCountry, isStyle3D)
        binding.map3d.setImageResource(if (!isStyle3D) R.drawable.ic_map_3d else R.drawable.ic_map_2d)
        trackasiaMap.setStyle(styleUrl)
    }

    private fun setupTextWatchers() {
        binding.edtAddressFrom.addTextChangedListener(edtTextChange(true))
        binding.edtAddressTo.addTextChangedListener(edtTextChange(false))
    }

    private fun edtTextChange(isAddressFrom: Boolean): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable) {
                this@MapWayPointFragment.isAddressFrom = isAddressFrom
                val text = editable.toString()

                updateVisibility(isAddressFrom, text.isNotEmpty())

                if (text.length >= 3) {
                    getAutoSuggestion(text)
                }
            }
        }
    }

    private fun updateVisibility(isAddressFrom: Boolean, isNotEmpty: Boolean) {
        if (isAddressFrom) {
            binding.btnMyLocation1.visibility = if (isNotEmpty) View.GONE else View.VISIBLE
            binding.btnClear1.visibility = if (isNotEmpty) View.VISIBLE else View.GONE
        } else {
            binding.btnMyLocation2.visibility = if (isNotEmpty) View.GONE else View.VISIBLE
            binding.btnClear2.visibility = if (isNotEmpty) View.VISIBLE else View.GONE
        }
    }

    private fun setupAutoCompleteListeners() {
        binding.edtAddressFrom.setOnItemClickListener { _, _, position, _ ->
            handleAutoCompleteSelection(position, true)
        }

        binding.edtAddressTo.setOnItemClickListener { _, _, position, _ ->
            handleAutoCompleteSelection(position, false)
        }
    }

    private fun handleAutoCompleteSelection(position: Int, isFrom: Boolean) {
        if (addressListData?.isNotEmpty() != true) return

        val item = addressListData?.get(position) ?: return
        val coordinates = item.geometry.coordinates
        if (coordinates.size < 2) return

        val lng = coordinates[0]
        val lat = coordinates[1]
        val point = LatLng(lat, lng)

        showPointMap(point)

        if (isFrom) {
            addressFrom = Point.fromLngLat(lng, lat)
            pointNumber = 1
        } else {
            addressTo = Point.fromLngLat(lng, lat)
            pointNumber = 2
        }

        caculaterDirectionMap()
        cameraAnimation(point, zoomLevel)
        hideKeyboard(requireActivity())
    }

    private fun cameraAnimation(point: LatLng, zoomLevel: Double?) {
        try {
            trackasiaMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(point)
                        .zoom(zoomLevel ?: zoomLocation)
                        .build()
                ),
                1000
            )
        } catch (e: Exception) {
            Log.d("ERROR CAMERA", e.toString())
        }
    }

    private fun setPositionMap(point: LatLng, name: String?) {
        cameraAnimation(point, zoomLevel)
        val colorResId = when (pointNumber) {
            1 -> R.color.holo_blue_dark
            points.size -> R.color.holo_green_dark
            else -> R.color.holo_yellow_dark
        }

        val snippet = "Lat: ${point.latitude} Lng: ${point.longitude}"
        val mapUtils = MapUtils(requireActivity())
        val markerIcon = mapUtils.createStoreMarker(pointNumber.toString(), colorResId)?.let {
            IconFactory.getInstance(requireContext()).fromBitmap(it)
        }
        trackasiaMap.addMarker(
            MarkerOptions()
                .position(point)
                .title(name)
                .snippet(snippet)
                .icon(markerIcon)
        )
    }

    private fun caculaterDirectionMap() {
        if (addressFrom == null || addressTo == null) {
            val missingPoint = when {
                addressFrom == null && addressTo == null -> "xuất phát và điểm đến"
                addressFrom == null -> "xuất phát"
                else -> "điểm đến"
            }
            showToast("Vui lòng chọn điểm $missingPoint")
            return
        }
        loading.show()
        val distanceInMeters = calculateDistance(
            addressFrom!!.latitude(), addressFrom!!.longitude(),
            addressTo!!.latitude(), addressTo!!.longitude()
        )

        if (distanceInMeters < 50) {
            showToast("Điểm đến quá gần điểm xuất phát")
            loading.dismiss()
            return
        }

        navigationMapRoute?.removeRoute()
        requestRoute(addressFrom!!, addressTo!!)
    }

    private fun requestRoute(origin: Point, destination: Point) {
        val baseUrl = "https://maps.track-asia.com/route/v1/car"
        val originCoord = "${origin.longitude()},${origin.latitude()}"
        val destCoord = "${destination.longitude()},${destination.latitude()}"
        val url = "$baseUrl/$originCoord;$destCoord.json?geometries=polyline6&steps=true&overview=full&key=$apiKey"
        
        Timber.d("Requesting route: %s", url)
        
        client.newCall(
            Request.Builder()
                .header("User-Agent", "TrackAsia Android Navigation SDK Demo App")
                .url(url)
                .build()
        ).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                activity?.runOnUiThread {
                    loading.dismiss()
                    showToast("Không thể kết nối tới dịch vụ định tuyến")
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) {
                        activity?.runOnUiThread {
                            loading.dismiss()
                            showToast("Yêu cầu thất bại với mã: ${response.code}")
                        }
                        return
                    }

                    response.body?.string()?.let { json ->
                        processRouteResponse(json, origin, destination)
                    }
                }
            }
        })
    }

    private fun processRouteResponse(responseJson: String, origin: Point, destination: Point) {
        loading.dismiss()
        try {
            val response = DirectionsResponse.fromJson(responseJson)
            if (response.routes.isEmpty()) {
                activity?.runOnUiThread { showToast("Không tìm thấy tuyến đường nào") }
                return
            }
            val firstRoute = response.routes.first()
            if (firstRoute.geometry.isEmpty()) {
                activity?.runOnUiThread { showToast("Geometry của tuyến đường trống") }
                return
            }

            // Create a copy of the route with proper RouteOptions
            route = firstRoute.copy(
                routeOptions = RouteOptions(
                    baseUrl = "https://maps.track-asia.com/route/v1",
                    profile = "car",
                    user = "trackasia",
                    accessToken = apiKey,
                    voiceInstructions = true,
                    bannerInstructions = true,
                    language = language,
                    coordinates = listOf(origin, destination),
                    geometries = "polyline6",
                    steps = true,
                    overview = "full",
                    requestUuid = "trackasia-nav-${System.currentTimeMillis()}"
                )
            )

            activity?.runOnUiThread { displayRouteOnMap(response.routes) }
        } catch (e: Exception) {
            activity?.runOnUiThread { showToast("Lỗi xử lý dữ liệu: ${e.message}") }
        }
    }

    private fun displayRouteOnMap(routes: List<DirectionsRoute>) {
        loading.dismiss()
        try {
            if (navigationMapRoute == null) {
                navigationMapRoute = NavigationMapRoute(null, binding.mapView, trackasiaMap)
            }
            navigationMapRoute?.removeRoute()
            navigationMapRoute?.addRoutes(routes)
            binding.clDirection.visibility = View.VISIBLE
            if (routes.size > 1) {
                setupRouteInfo(routes[0], binding.textDistanceValue, binding.textTimeValue)
                binding.clDistance.visibility = View.VISIBLE
                setupRouteInfo(routes[1], binding.textDistanceValue2, binding.textTimeValue2)
                binding.clDistance2.visibility = View.VISIBLE
            } else {
//                route = routes.first()
                setupRouteInfo(route!!, binding.textDistanceValue, binding.textTimeValue)
                binding.clDistance.visibility = View.VISIBLE
                binding.clDistance2.visibility = View.GONE
            }
            fitCameraToBounds()
        } catch (e: Exception) {
            showToast("Lỗi hiển thị route: ${e.message}")
        }
    }

    @SuppressLint("DefaultLocale")
    private fun setupRouteInfo(route: DirectionsRoute, distanceView: TextView, timeView: TextView) {
        distanceView.text = String.format("%.2fkm", route.distance.div(1000))
        timeView.text = formatDuration(route.duration.toLong())
        this.route = route
    }

    private fun fitCameraToBounds() {
        try {
            if (addressFrom != null && addressTo != null) {
                val bounds = LatLngBounds.Builder()
                    .include(LatLng(addressFrom!!.latitude(), addressFrom!!.longitude()))
                    .include(LatLng(addressTo!!.latitude(), addressTo!!.longitude()))
                    .build()

                trackasiaMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100), 1000)
            }
        } catch (e: Exception) {
            Log.e("CAMERA_ERROR", "Error animating camera", e)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371e3 // Earth's radius in meters
        val φ1 = Math.toRadians(lat1)
        val φ2 = Math.toRadians(lat2)
        val Δφ = Math.toRadians(lat2 - lat1)
        val Δλ = Math.toRadians(lon2 - lon1)

        val a = Math.sin(Δφ / 2).pow(2) + Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2).pow(2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        return when {
            hours > 0 -> String.format("%02dh:%02dm:%02ds", hours, minutes, remainingSeconds)
            minutes > 0 -> String.format("%02dm:%02ds", minutes, remainingSeconds)
            else -> String.format("%02ds", remainingSeconds)
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
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard(activity: Activity) {
        val imm =
            activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity.currentFocus ?: View(activity)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

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
        permissionsManager?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        // Có thể hiển thị dialog giải thích tại sao cần quyền
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            trackasiaMap.getStyle { style ->
                if (activity != null) {
                    // Sử dụng MapUtils để kích hoạt vị trí
                    try {
                        val mapUtils = MapUtils(requireActivity())
                        mapUtils.enableLocationComponent(
                            style,
                            idCountry,
                            trackasiaMap,
                            permissionsManager ?: PermissionsManager(this),
                            latLngLocation,
                            zoomLocation
                        )
                    } catch (e: Exception) {
                        Log.e(
                            "PERMISSION_ERROR",
                            "Error enabling location after permission: ${e.message}"
                        )
                        showToast("Lỗi khi kích hoạt vị trí: ${e.message}")
                    }
                }
            }
        } else {
            // Xử lý khi quyền truy cập vị trí không được cấp
            showToast("Cần cấp quyền vị trí để sử dụng tính năng này")
        }
    }

    /**
     * Handle map click events
     */
    private fun handleMapClick(point: LatLng) {
        // Xác định xem đang đặt điểm đi hay điểm đến
        if (!isAddressFrom) {
            // Đặt điểm đi
            setOriginPoint(point)
        } else {
            // Đặt điểm đến
            setDestinationPoint(point)
        }

        // Calculate route if both points are set
        if (addressFrom != null && addressTo != null) {
            caculaterDirectionMap()
        }
    }

    private fun setOriginPoint(point: LatLng) {
        // Xóa marker cũ nếu có
        if (trackasiaMap.markers.isNotEmpty()) {
            trackasiaMap.removeMarker(trackasiaMap.markers.firstOrNull() ?: return)
        }

        showPointMap(point)
        addressFrom = Point.fromLngLat(point.longitude, point.latitude)
        binding.edtAddressFrom.setText(
            "Vị trí đã chọn (${point.latitude.format(5)}, ${
                point.longitude.format(
                    5
                )
            })"
        )
        binding.btnClear1.visibility = View.VISIBLE
        binding.btnMyLocation1.visibility = View.GONE
    }

    private fun setDestinationPoint(point: LatLng) {
        // Xóa marker đích cũ nếu có
        if (trackasiaMap.markers.size > 1) {
            trackasiaMap.removeMarker(trackasiaMap.markers.last())
        }

        showPointMap(point)
        addressTo = Point.fromLngLat(point.longitude, point.latitude)
        binding.edtAddressTo.setText(
            "Vị trí đã chọn (${point.latitude.format(5)}, ${
                point.longitude.format(
                    5
                )
            })"
        )
        binding.btnClear2.visibility = View.VISIBLE
        binding.btnMyLocation2.visibility = View.GONE
    }

    /**
     * Enable location component with improved configuration
     */
    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(style: Style) {
        // Check if permissions are granted
        if (!PermissionsManager.areLocationPermissionsGranted(requireContext())) {
            permissionsManager = PermissionsManager(this)
            permissionsManager?.requestLocationPermissions(requireActivity())
            return
        }

        // Configure location component
        trackasiaMap.locationComponent.apply {
            activateLocationComponent(
                LocationComponentActivationOptions.builder(requireContext(), style).build()
            )

            // Enable the component
            isLocationComponentEnabled = true

            // Set tracking mode
            cameraMode = CameraMode.TRACKING_GPS_NORTH

            // Set render mode
            renderMode = RenderMode.NORMAL
        }
    }

    /**
     * Phương thức chỉ để đáp ứng interface ProgressChangeListener
     */
    override fun onProgressChange(
        location: com.trackasia.navigation.core.location.Location,
        routeProgress: RouteProgress
    ) {
        // Not implemented yet
    }

    private fun startNavigation(userLocation: Location) {
        try {
            if (route == null || route?.routeOptions == null) {
                return
            }

            // Build navigation launch options
            val options = NavigationLauncherOptions.builder()
                .directionsRoute(route)
                .initialMapCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(userLocation.latitude, userLocation.longitude))
                        .build()
                )
                .shouldSimulateRoute(true)
                .lightThemeResId(R.style.NavigationViewLight)
                .darkThemeResId(R.style.NavigationViewDark)
                .build()

            // Start the TrackAsiaNavigationActivity with the options
            NavigationLauncher.startNavigation(this.context, options)
        } catch (e: Exception) {
            Log.e("TAG", "Error starting navigation: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setupObservers() {
        // Theo dõi kết quả từ geocoding API
        viewModel.geoCodingData.observe(viewLifecycleOwner) { data ->
            loading.dismiss()
            handleGeoCodingResult(data)
        }

        // Theo dõi kết quả từ auto-suggestion API
        viewModel.autoSuggestionData.observe(viewLifecycleOwner) { data ->
            loading.dismiss()
            handleAutoSuggestionResult(data)
        }
    }

    private fun handleGeoCodingResult(data: GeoCodingData?) {
        if (data == null) return

        if (data.lat != null && data.lat.isNotEmpty() && data.long != null && data.long.isNotEmpty()) {
            val position = LatLng(data.lat.toDouble(), data.long.toDouble())
            setPositionMap(position, data.name)

            if (data.name != null) {
                if (isAddressFrom) {
                    addressFrom = Point.fromLngLat(data.long.toDouble(), data.lat.toDouble())
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

    private fun handleAutoSuggestionResult(data: List<Feature>?) {
        if (data.isNullOrEmpty()) return

        addressListData = data
        addressList = data.map { it.properties.label }

        if (addressList?.isNotEmpty() == true) {
            val adapter = SuggestionAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                addressList!!
            )

            if (isAddressFrom) {
                binding.edtAddressFrom.setAdapter(adapter)
            } else {
                binding.edtAddressTo.setAdapter(adapter)
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun showPoiSearchPopup() {
        // Ensure container is visible before animation
        binding.poiSearchContainer.visibility = View.VISIBLE
        
        // Set alpha to start animation
        binding.poiSearchContainer.alpha = 0f
        
        // Animate from 0 to 1 for fade-in effect
        binding.poiSearchContainer.animate()
            .alpha(1f)
            .setDuration(300)
            .withStartAction {
                // Log visibility state before animation
                Log.d("POI_POPUP", "Starting popup animation, visibility: ${binding.poiSearchContainer.visibility}")
            }
            .withEndAction {
                // Log visibility state after animation
                Log.d("POI_POPUP", "Popup animation ended, visibility: ${binding.poiSearchContainer.visibility}")
            }
            .start()
            
        // Show toast to indicate function was called
        showToast("Chọn loại địa điểm bạn muốn tìm kiếm")
    }
    
    private fun hidePoiSearchPopup() {
        // Only animate if container is visible
        if (binding.poiSearchContainer.visibility == View.VISIBLE) {
            // Animate from 1 to 0 for fade-out effect
            binding.poiSearchContainer.animate()
                .alpha(0f)
                .setDuration(300)
                .withStartAction {
                    // Log visibility state before animation
                    Log.d("POI_POPUP", "Starting hide popup animation")
                }
                .withEndAction {
                    // Set visibility to GONE after animation completes
                    binding.poiSearchContainer.visibility = View.GONE
                    Log.d("POI_POPUP", "Popup hidden")
                }
                .start()
        } else {
            Log.d("POI_POPUP", "Hide called but popup is already hidden")
        }
    }
    
    private fun setupPoiCategoryListeners() {
        // Truy cập view của popup từ container
        val poiLayout = binding.poiSearchContainer.findViewById<View>(R.id.poiSearchLayout)
        
        if (poiLayout == null) {
            Log.e("POI_POPUP", "Error: poiSearchLayout not found")
            return
        }
        
        // Restaurant category
        poiLayout.findViewById<View>(R.id.category_restaurant)?.setOnClickListener {
            searchNearbyPoi("restaurant")
            hidePoiSearchPopup()
        }
        
        // Hotel category
        poiLayout.findViewById<View>(R.id.category_hotel)?.setOnClickListener {
            searchNearbyPoi("hotel")
            hidePoiSearchPopup()
        }
        
        // Store category
        poiLayout.findViewById<View>(R.id.category_store)?.setOnClickListener {
            searchNearbyPoi("supermarket")
            hidePoiSearchPopup()
        }
        
        // Pharmacy category
        poiLayout.findViewById<View>(R.id.category_pharmacy)?.setOnClickListener {
            searchNearbyPoi("pharmacy")
            hidePoiSearchPopup()
        }
        
        // Entertainment category
        poiLayout.findViewById<View>(R.id.category_entertainment)?.setOnClickListener {
            searchNearbyPoi("entertainment")
            hidePoiSearchPopup()
        }
        
        // Government category
        poiLayout.findViewById<View>(R.id.category_government)?.setOnClickListener {
            searchNearbyPoi("government")
            hidePoiSearchPopup()
        }
        
        // Education category
        poiLayout.findViewById<View>(R.id.category_education)?.setOnClickListener {
            searchNearbyPoi("education")
            hidePoiSearchPopup()
        }
        
        // Bank category
        poiLayout.findViewById<View>(R.id.category_bank)?.setOnClickListener {
            searchNearbyPoi("bank")
            hidePoiSearchPopup()
        }
        
        // Camera category
        poiLayout.findViewById<View>(R.id.category_camera)?.setOnClickListener {
            searchNearbyPoi("traffic_camera")
            hidePoiSearchPopup()
        }
        
        // Traffic category
        poiLayout.findViewById<View>(R.id.category_traffic)?.setOnClickListener {
            searchNearbyPoi("traffic")
            hidePoiSearchPopup()
        }
        
        // Repair category
        poiLayout.findViewById<View>(R.id.category_repair)?.setOnClickListener {
            searchNearbyPoi("car_repair")
            hidePoiSearchPopup()
        }
    }
    
    private fun searchNearbyPoi(category: String) {
        // Get current map center
        val mapCenter = trackasiaMap.cameraPosition.target
        
        // Clear previous markers except route markers
        clearPoiMarkers()
        
        // Show loading
        loading.show()
        
        // Perform search based on category and location
        val radius = 2000 // 2km radius
        val apiUrl = "https://maps.track-asia.com/search/v1/poi?key=${apiKey}" +
                "&category=${category}" +
                "&lat=${mapCenter?.latitude}" +
                "&lng=${mapCenter?.longitude}" +
                "&radius=${radius}"
        
        searchPoi(apiUrl)
    }
    
    private fun clearPoiMarkers() {
        // Implementation would depend on how you're tracking POI markers
        // This is a simple example - you might need to adapt it
        val markersToRemove = trackasiaMap.markers.filter { marker -> 
            marker.title?.startsWith("POI:") == true 
        }
        
        markersToRemove.forEach { marker ->
            trackasiaMap.removeMarker(marker)
        }
    }
    
    private fun searchPoi(apiUrl: String) {
        // In a real implementation, you would make an API call here
        // For demonstration purposes, we'll simulate some POI results
        
        // Simulated POI results
        simulatePoiResults()
        
        // Hide loading
        loading.dismiss()
        
        // Show toast
        showToast("Đã tìm thấy các địa điểm gần đây")
    }
    
    private fun simulatePoiResults() {
        // Get current map center
        val mapCenter = trackasiaMap.cameraPosition.target
        val random = java.util.Random()
        
        // Create 5 random markers around the current location
        for (i in 0 until 5) {
            // Random offset within 2km
            val latOffset = (random.nextDouble() - 0.5) * 0.03
            val lngOffset = (random.nextDouble() - 0.5) * 0.03
            
            val poiLocation = LatLng(
                mapCenter?.latitude!! + latOffset,
                mapCenter?.longitude!! + lngOffset
            )
            
            // Add marker
            val marker = trackasiaMap.addMarker(
                MarkerOptions()
                    .position(poiLocation)
                    .title("POI: Địa điểm ${i+1}")
                    .snippet("Thông tin chi tiết về địa điểm ${i+1}")
            )
        }
        
        // Adjust camera to show all markers
        trackasiaMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(mapCenter!!, 14.0),
            1000
        )
    }
}