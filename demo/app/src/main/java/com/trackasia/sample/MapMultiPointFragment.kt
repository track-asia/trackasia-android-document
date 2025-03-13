package com.trackasia.sample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
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
import com.trackasia.android.location.permissions.PermissionsListener
import com.trackasia.android.location.permissions.PermissionsManager
import com.trackasia.android.maps.Style
import com.trackasia.android.maps.TrackAsiaMap
import com.trackasia.geojson.Point
import com.trackasia.navigation.android.navigation.ui.v5.NavigationLauncher
import com.trackasia.navigation.android.navigation.ui.v5.NavigationLauncherOptions
import com.trackasia.navigation.android.navigation.ui.v5.instruction.InstructionView
import com.trackasia.navigation.android.navigation.ui.v5.route.NavigationMapRoute
import com.trackasia.navigation.android.navigation.ui.v5.route.NavigationRoute.builder
import com.trackasia.navigation.android.navigation.v5.models.DirectionsCriteria
import com.trackasia.navigation.android.navigation.v5.models.DirectionsResponse
import com.trackasia.navigation.android.navigation.v5.models.DirectionsRoute
import com.trackasia.navigation.android.navigation.v5.navigation.TrackAsiaNavigation
import com.trackasia.navigation.android.navigation.v5.routeprogress.ProgressChangeListener
import com.trackasia.navigation.android.navigation.v5.routeprogress.RouteProgress
import com.trackasia.sample.adapter.AddressRouterAdapter
import com.trackasia.sample.databinding.FragmentMapMultiPointBinding
import com.trackasia.sample.utils.MapUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MapMultiPointFragment : Fragment(), PermissionsListener, ProgressChangeListener {

    private var _binding: FragmentMapMultiPointBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapboxMap: TrackAsiaMap
    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private var navigationMapRoute: NavigationMapRoute? = null
    private val instructionView: InstructionView? = null
    private var simulateRoute = false
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
                mapboxMap.addOnMapClickListener { point ->
                    showPointMap(point)
                    caculaterDirectionMap(point)
                    return@addOnMapClickListener true
                }
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
                    showPointMap(latLngLocation!!)
                }
            } catch (e: Exception) {
                Log.d("ERROR:", e.toString())
            }
        }
    }

    private fun initControl() {
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
        binding.clearPoints.setOnClickListener {
            pointNumber = 0
            mapboxMap.markers.clear()
            mapboxMap.clear()
            if (navigationMapRoute != null) {
                navigationMapRoute!!.removeRoute()
            }
            points.clear()
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
        pointNumber++
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
        var destination = Point.fromLngLat(point.longitude, point.latitude)
        points.add(destination)

        var navigation = builder(context).accessToken(getString(R.string.mapbox_access_token)).origin(points[0])
            .voiceUnits(DirectionsCriteria.METRIC).alternatives(true).profile("car")
            .baseUrl(MapUtils(requireActivity()).urlDomain(idCountry)).destination(points[points.lastIndex])
        for (item: Point in points) {
            navigation.addWaypoint(Point.fromLngLat(item.longitude(), item.latitude()))
        }
        navigation.build().getRoute(object : Callback<DirectionsResponse> {
            override fun onResponse(
                call: Call<DirectionsResponse>, response: Response<DirectionsResponse>
            ) {
                response.body()?.let { response ->
                    if (response.routes.isNotEmpty()) {
                        val trackasiaResponse = DirectionsResponse.fromJson(response.toJson());
                        route = trackasiaResponse.routes.first()
                        navigationMapRoute?.addRoutes(trackasiaResponse.routes)
                        if (route != null) {
                            displayInstructions(route!!)
                        }
                        cameraAnimation(LatLng(point.latitude, point.longitude), zoomLevel)
                    }
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {}
        })
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
        var a = addressCurrent
        //        addressRouterAdapter.addData(RouterAddressModel("", data.name, "", null))
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

    override fun onProgressChange(location: Location, routeProgress: RouteProgress) {
        TODO("Not yet implemented")
    }
}

data class RouterAddressModel(
    val router: String? = null,
    val name: String,
    val distance: String? = null,
    val directionData: MutableList<RouterAddressModel>? = null
)