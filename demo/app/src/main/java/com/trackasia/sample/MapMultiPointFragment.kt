package com.trackasia.sample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.trackasia.navigation.core.navigation.TrackAsiaNavigation
import com.trackasia.navigation.core.routeprogress.ProgressChangeListener
import com.trackasia.navigation.core.routeprogress.RouteProgress
import com.trackasia.sample.adapter.AddressRouterAdapter
import com.trackasia.sample.databinding.FragmentMapMultiPointBinding
import com.trackasia.sample.utils.MapUtils
import com.trackasia.sample.utils.TimeUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.ref.WeakReference

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MapMultiPointFragment : Fragment(), PermissionsListener, ProgressChangeListener {

    private var _binding: FragmentMapMultiPointBinding? = null
    private val binding get() = _binding!!
    private lateinit var trackasiaMap: TrackAsiaMap
    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private var navigationMapRoute: NavigationMapRoute? = null
    private var simulateRoute = true
    private var route: DirectionsRoute? = null
    private val mapUtils: MapUtils by lazy { MapUtils(requireActivity()) }
    private var pointNumber = 0
    private var points: MutableList<Point> = mutableListOf()
    private val viewModel: MainViewModel by viewModels()

    private var styleUrl = "https://maps.track-asia.com/styles/v1/streets.json?key=public"
    private var latLngLocation: LatLng? = LatLng(10.728073, 106.624054)
    private var zoomLocation: Double = 10.0
    private var zoomLevel: Double = 16.0
    private var idCountry: String? = "vn"
    private var isStyle3D = true
    private var addressCurrent: RouterAddressModel? = null
    private lateinit var addressRouterAdapter: AddressRouterAdapter
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
        _binding = FragmentMapMultiPointBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)
        initControl()
        initListener()
        initMap()
        initAddressRouter()
        
        // Initialize empty route list for multi-point navigation
        points = ArrayList()
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
                            trackasiaMap,
                            permissionsManager,
                            latLngLocation!!,
                            zoomLocation
                        )
                    }
                }
                this.trackasiaMap = map
                trackasiaMap.addOnMapClickListener { point ->
                    showPointMap(point)
                    caculaterDirectionMap(point)
                    return@addOnMapClickListener true
                }
                // Initialize navigation map route for future route display
                if (navigationMapRoute == null) {
                    navigationMapRoute = NavigationMapRoute(null, binding.mapView, trackasiaMap)
                }
                navigationMapRoute?.removeRoute()
                cameraAnimation(latLngLocation!!, zoomLevel)
            } catch (e: Exception) {
                Log.d("ERROR MAP:", e.toString())
            }

        }

        binding.locationMy.setOnClickListener {
            try {
                val userLocation = trackasiaMap.locationComponent.lastKnownLocation
                if (userLocation != null) {
                    if (userLocation.longitude > 0) { //emulator location error
                        latLngLocation = LatLng(userLocation.latitude, userLocation.longitude)
                    }
                    showPointMap(latLngLocation!!)
                }
            } catch (e: Exception) {
                Log.d("ERROR:", e.toString())
            }
        }
    }

    private fun initControl() {
        // Nút TrackAsia Navigation
        binding.trackNavigationRouter.setOnClickListener {
            handleTrackAsiaNavigation()
        }
        
        // Nút Google Navigation
        binding.navigationRouter.setOnClickListener {
            handleGoogleNavigation()
        }
        
        // Switch mô phỏng tuyến đường
        binding.simulateSwitch.setOnCheckedChangeListener { _, isChecked ->
            simulateRoute = isChecked
        }
    }
    
    private fun handleTrackAsiaNavigation() {
        if (points.size < 2) {
            showToast("Vui lòng chọn ít nhất 2 điểm trên bản đồ")
            return
        }

        if (route == null || route?.routeOptions == null) {
            showToast("Tuyến đường chưa được tính toán hoặc không có đủ thông tin")
            return
        }

        try {
            val userLocation = trackasiaMap.locationComponent.lastKnownLocation
            if (userLocation == null) {
                showToast("Không thể xác định vị trí hiện tại")
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
                .shouldSimulateRoute(simulateRoute)
                .lightThemeResId(R.style.NavigationViewLight)
                .darkThemeResId(R.style.NavigationViewDark)
                .build()

            // Start the TrackAsiaNavigationActivity with the options
            context?.let {
                NavigationLauncher.startNavigation(it, options)
            } ?: run {
                showToast("Không thể khởi động điều hướng: context không tồn tại")
            }
        } catch (e: Exception) {
            Log.e("NAVIGATION_ERROR", "Error starting navigation: ${e.message}")
            showToast("Lỗi bắt đầu điều hướng: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun handleGoogleNavigation() {
        if (points.size < 2) {
            showToast("Vui lòng chọn ít nhất 2 điểm trên bản đồ")
            return
        }

        val startPoint = points.first()
        val endPoint = points.last()
        
        val startLocation = "${startPoint.latitude()},${startPoint.longitude()}"
        val endLocation = "${endPoint.latitude()},${endPoint.longitude()}"

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

    private class MyBroadcastReceiver(navigation: TrackAsiaNavigation) : BroadcastReceiver() {
        private val weakNavigation: WeakReference<TrackAsiaNavigation> = WeakReference(navigation)
        override fun onReceive(context: Context, intent: Intent) {
            val navigation = weakNavigation.get()
            navigation?.stopNavigation()
        }
    }

    private fun initListener() {
        viewModel.geoCodingData.observe(requireActivity()) { data ->
            if (data != null) {
                if (data.lat != null && data.lat != "" && data.long != null && data.long != "") {
                    val position = LatLng(data.lat.toDouble(), data.long.toDouble())
                    setPositionMap(position, data.name)
                    if (data.name != null) {
                        addressCurrent = RouterAddressModel("", data.name, "", null)
                    }
                } else {
                    val latlng = MapUtils(requireActivity()).getLatlng(idCountry)
                    cameraAnimation(latlng, zoomLevel)
                }
            }
        }
        
        // Set up the clear points button in the route info section - use the parent CardView for better touch target
        binding.clearPointsCard.setOnClickListener {
            clearAllPoints()
        }

        binding.map3d.setOnClickListener {
            styleUrl = MapUtils(requireActivity()).urlStyle(idCountry, isStyle3D)
            binding.map3d.setImageResource(if (!isStyle3D) R.drawable.ic_map_3d else R.drawable.ic_map_2d)
            isStyle3D = !isStyle3D
            trackasiaMap.setStyle(styleUrl)
        }
    }

    private fun setPositionMap(point: LatLng, name: String?) {
        pointNumber = points.size
        cameraAnimation(point, zoomLevel)
        val colorResId = when (points.size) {
            1 -> R.color.holo_blue_dark
            2 -> R.color.holo_green_dark
            else -> R.color.holo_yellow_dark
        }
        
        val snippet = "Lat: ${point.latitude} Lng: ${point.longitude}"
        val markerOptionsFirst = MarkerOptions()
            .position(point)
            .title(name ?: "Point ${points.size}")
            .snippet(snippet)
            .icon(mapUtils.createStoreMarker(
                points.size.toString(), colorResId
            )?.let { IconFactory.getInstance(requireContext()).fromBitmap(it) })
        
        trackasiaMap.addMarker(markerOptionsFirst)

        updatePointInfoText()
    }
    
    private fun updatePointInfoText() {
        val infoText = when (points.size) {
            0 -> "Chưa có điểm nào"
            1 -> "Điểm 1: Xuất phát"
            2 -> "Điểm 1: Xuất phát | Điểm 2: Đích"
            else -> "Điểm 1: Xuất phát | Điểm ${points.size}: Đích | ${points.size-2} điểm dừng"
        }
        activity?.runOnUiThread {
            binding.pointInfoText.text = infoText
        }
    }

    private fun caculaterDirectionMap(point: LatLng) {
        val destination = Point.fromLngLat(point.longitude, point.latitude)
        points.add(destination)
        
        // Update point info display
        updatePointInfoText()
        
        if (points.size < 1) {
            return
        }
        
        // Show loading indication if available
        activity?.runOnUiThread {
            showToast("Đang tính toán tuyến đường...")
        }
        
        if (navigationMapRoute != null) {
            navigationMapRoute!!.removeRoute()
        }
        
        val coordinatesBuilder = StringBuilder()
        points.forEachIndexed { index, point ->
            if (index > 0) coordinatesBuilder.append(";")
            coordinatesBuilder.append("${point.longitude()},${point.latitude()}")
        }
        
        val baseUrl = "https://maps.track-asia.com/route/v1/car"
        val url = "$baseUrl/${coordinatesBuilder}.json?geometries=polyline6&steps=true&overview=full&key=public_key"
        Log.d("NAVIGATION", "Requesting route with ${points.size} points: $url")
        
        val client = OkHttpClient()
        client.newCall(
            Request.Builder()
                .header("User-Agent", "TrackAsia Android Navigation SDK Demo App")
                .url(url)
                .build()
        ).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                activity?.runOnUiThread {
                    showToast("Không thể kết nối tới dịch vụ định tuyến: ${e.message}")
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) {
                        activity?.runOnUiThread {
                            showToast("Yêu cầu thất bại với mã: ${response.code}")
                        }
                        return
                    }

                    response.body?.string()?.let { json ->
                        processRouteResponse(json)
                    }
                }
            }
        })
    }

    private fun processRouteResponse(responseJson: String) {
        Log.d("NAVIGATION", "Processing route response: ${responseJson.take(100)}...")
        
        try {
            val trackasiaResponse = DirectionsResponse.fromJson(responseJson)
            
            if (trackasiaResponse.routes.isEmpty()) {
                activity?.runOnUiThread { 
                    showToast("Không tìm thấy tuyến đường nào") 
                }
                return
            }
            
            val firstRoute = trackasiaResponse.routes.first()
            if (firstRoute.geometry.isEmpty()) {
                activity?.runOnUiThread { 
                    showToast("Không tìm thấy hình dạng tuyến đường") 
                }
                return
            }
            
            Log.d("NAVIGATION", "Route found: Distance=${firstRoute.distance}m, Duration=${firstRoute.duration}s")
            
            // Create proper RouteOptions with all required fields
            val routeOptions = RouteOptions(
                baseUrl = "https://maps.track-asia.com/route/v1",
                profile = "car",
                user = "trackasia",
                accessToken = "public_key",
                voiceInstructions = true,
                bannerInstructions = true,
                language = java.util.Locale.getDefault().language,
                coordinates = points,
                geometries = "polyline6",
                steps = true,
                overview = "full",
                requestUuid = "trackasia-nav-${System.currentTimeMillis()}"
            )

            // Create a new route with proper options
            route = firstRoute.copy(routeOptions = routeOptions)
            
            Log.d("NAVIGATION", "Route prepared for navigation with ${points.size} waypoints")
            
            activity?.runOnUiThread {
                displayRouteOnMap(trackasiaResponse.routes)
            }
        } catch (e: Exception) {
            Log.e("ROUTE_ERROR", "Route processing error", e)
            activity?.runOnUiThread { 
                showToast("Lỗi xử lý dữ liệu tuyến đường: ${e.message}") 
            }
        }
    }

    private fun displayRouteInfo(route: DirectionsRoute) {
        val distance = route.distance
        val duration = route.duration
        val formattedDistance = formatDistance(distance)
        val formattedDuration = TimeUtils.formatDuration(duration.toLong())
        binding.textDistanceValue.text = formattedDistance
        binding.textTimeValue.text = formattedDuration
    }
    
    private fun formatDistance(distanceInMeters: Double): String {
        return when {
            distanceInMeters < 1000 -> "${distanceInMeters.toInt()} m"
            else -> String.format("%.1f km", distanceInMeters / 1000)
        }
    }

    fun displayInstructions(routes: DirectionsRoute) {
        var directionModel: MutableList<RouterAddressModel> = mutableListOf()
        for ((index, route) in routes.legs.withIndex()) {
            if (route.summary?.isNotEmpty() == true) {
                for ((index, direction) in route.steps!!.withIndex()) {
                    if (direction.name?.isNotEmpty() == true && direction.distance != 0.0) {
                        directionModel.add(
                            RouterAddressModel(
                                router = direction.drivingSide,
                                name = direction.name!!,
                                distance = direction.distance.toString(),
                                directionData = null
                            )
                        )
                        println("Step ${index + 1}: $directionModel")
                    }
                }


            }
        }
        addressCurrent?.copy(directionData = directionModel)
            ?.let { addressRouterAdapter.addData(it) }
    }

    private fun cameraAnimation(point: LatLng, zoomLevel: Double?) {
        try {
            val cameraPosition =
                CameraPosition.Builder().target(point).zoom(zoomLevel ?: zoomLocation).build()
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)
            trackasiaMap.animateCamera(cameraUpdate, 1000)
        } catch (e: Exception) {
            Log.d("ERROR CAMERA", e.toString())
        }
    }
    
    private fun fitMapToPoints() {
        if (points.size < 2) return
        
        try {
            val latLngPoints = points.map { LatLng(it.latitude(), it.longitude()) }
            val boundsBuilder = LatLngBounds.Builder()
            
            // Add all points to bounds
            latLngPoints.forEach { boundsBuilder.include(it) }
            
            // Create bounds
            val bounds = boundsBuilder.build()
            
            // Calculate appropriate padding based on screen size
            val padding = resources.displayMetrics.density * 64
            
            // Animate camera to fit all points with padding
            trackasiaMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, padding.toInt()),
                1000
            )
        } catch (e: Exception) {
            Log.e("CAMERA_ERROR", "Error fitting map to points", e)
            // Fall back to showing the last point if bounds calculation fails
            if (points.isNotEmpty()) {
                val lastPoint = points.last()
                cameraAnimation(LatLng(lastPoint.latitude(), lastPoint.longitude()), zoomLevel)
            }
        }
    }

    private fun showPointMap(point: LatLng) {
        val newPoint = LatLng(point.latitude, point.longitude)
        viewModel.funCallShowPointMap(newPoint, requireActivity())
    }
    
    private fun clearAllPoints() {
        pointNumber = 0
        trackasiaMap.markers.clear()
        trackasiaMap.clear()
        
        // Clear navigation route if exists
        if (navigationMapRoute != null) {
            navigationMapRoute!!.removeRoute()
        }
        
        // Clear route data
        route = null
        points.clear()
        
        // Clear UI elements
        addressRouterAdapter.clearData()
        binding.clDirection.visibility = View.GONE
        
        // Update display
        updatePointInfoText()
        
        // Log the reset
        Log.d("NAVIGATION", "All points and routes cleared")
    }
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun displayRouteOnMap(routes: List<DirectionsRoute>) {
        try {
            // Create NavigationMapRoute if it doesn't exist
            if (navigationMapRoute == null) {
                navigationMapRoute = NavigationMapRoute(null, binding.mapView, trackasiaMap)
            }
            
            // Clear existing route
            navigationMapRoute?.removeRoute()
            
            // Add all routes
            navigationMapRoute?.addRoutes(routes)
            
            // Show route info UI
            binding.clDirection.visibility = View.VISIBLE
            
            // Hiển thị thông tin tuyến đường đầu tiên
            if (routes.isNotEmpty()) {
                // Store primary route for navigation
                route = routes.first()
                
                // Display route info
                displayRouteInfo(route!!)
                
                // Display turn-by-turn instructions
                displayInstructions(route!!)
                
                // Log successful route calculation
                Log.d("NAVIGATION", "Route calculated successfully: ${route?.distance?.div(1000)} km, ${route?.duration?.div(60)} minutes")
            }

            // Fit map to show all points
            fitMapToPoints()
        } catch (e: Exception) {
            Log.e("NAVIGATION", "Error displaying route", e)
            showToast("Lỗi hiển thị tuyến đường: ${e.message}")
        }
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
            trackasiaMap.getStyle { style ->
                if (activity != null) {
                    MapUtils(requireActivity()).enableLocationComponent(
                        style,
                        idCountry,
                        trackasiaMap,
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

    override fun onProgressChange(location: com.trackasia.navigation.core.location.Location, routeProgress: RouteProgress) {
        // TODO: Implement later if needed
    }
}

data class RouterAddressModel(
    val router: String? = null,
    val name: String,
    val distance: String? = null,
    val directionData: MutableList<RouterAddressModel>? = null
)