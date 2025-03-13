package com.trackasia.sample

import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.trackasia.android.TrackAsia
import com.trackasia.android.annotations.MarkerOptions
import com.trackasia.android.annotations.Polygon
import com.trackasia.android.annotations.PolygonOptions
import com.trackasia.android.annotations.Polyline
import com.trackasia.android.annotations.PolylineOptions
import com.trackasia.android.camera.CameraPosition
import com.trackasia.android.camera.CameraUpdateFactory
import com.trackasia.android.geometry.LatLng
import com.trackasia.android.location.permissions.PermissionsListener
import com.trackasia.android.location.permissions.PermissionsManager
import com.trackasia.android.maps.Style
import com.trackasia.android.maps.TrackAsiaMap
import com.trackasia.sample.databinding.FragmentFeatureBinding
import com.trackasia.sample.utils.GeoParseUtil
import com.trackasia.sample.utils.MapUtils
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.DecimalFormat
import java.util.Random

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MapFeatureFragment : Fragment(), PermissionsListener, View.OnClickListener {

    private var _binding: FragmentFeatureBinding? = null
    private lateinit var mapboxMap: TrackAsiaMap
    private var polygon: Polygon? = null
    private val polylines: List<Polyline>? = null
    private var locations: MutableList<LatLng> = mutableListOf()
    private var progressDialog: ProgressDialog? = null
    private val polylineOptions = java.util.ArrayList<PolylineOptions>()
    private var navController: NavController? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
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
        _binding = FragmentFeatureBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()
        binding.mapView.onCreate(savedInstanceState)
        initMap()
        initControl()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initMap() {
        polylineOptions.addAll(getAllPolylines()!!)
        binding.mapView.getMapAsync { map ->
            try {
                Log.d("DOMAIN URL STYLE:", styleUrl)
                map.setStyle(
                    Style.Builder().fromUri(styleUrl)
                ) { style ->
                    if(activity != null) {
                        MapUtils(requireActivity()).enableLocationComponent(
                            style, idCountry, mapboxMap,
                            permissionsManager,
                            latLngLocation!!, zoomLocation
                        )
                    }
                }
                this.mapboxMap = map
                cameraAnimation(latLngLocation!!)
            } catch (e: Exception) {
                Log.d("ERROR MAP:", e.toString())
            }

        }
    }

    private fun cameraAnimation(point: LatLng) {
        try {
            val cameraPosition = CameraPosition.Builder().target(point).zoom(zoomLocation).build()
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)
            mapboxMap.animateCamera(cameraUpdate, 1000)
        } catch (e: Exception) {
            Log.d("ERROR CAMERA", e.toString())
        }
    }

    private fun initControl() {
        binding.btnPolygon.setOnClickListener {
            mapboxMap!!.addPolygon(
                PolygonOptions().addAll(Config.STAR_SHAPE_POINTS).fillColor(Config.BLUE_COLOR)
            )
        }
        binding.btnPolyline.setOnClickListener {
            mapboxMap.addPolylines(polylineOptions);
        }
        binding.btnAddMarkets.setOnClickListener {
            locations.add(LatLng(11.105950, 106.656514))
            locations.add(LatLng(11.626593, 107.028255))
            locations.add(LatLng(10.966258, 107.476470))
            locations.add(LatLng(11.245575, 107.187573))
            locations.add(LatLng(11.118457, 106.792465))
            locations.add(LatLng(11.118457, 106.792465))
            locations.add(LatLng(11.114457, 106.792465))
            locations.add(LatLng(11.118457, 106.792465))
            locations.add(LatLng(11.118457, 106.767465))
            locations.add(LatLng(11.118457, 106.792346))
            locations.add(LatLng(11.118457, 106.792465))
            locations.add(LatLng(11.114457, 106.795675))
            locations.add(LatLng(11.128457, 106.123765))
            locations.add(LatLng(11.118657, 106.346465))
            if (locations == null) {
                progressDialog = ProgressDialog.show(context, "Loading", "Fetching markers", false)
                LoadLocationTask(this, 50).execute()
            } else {
                showMarkers(50)
            }
        }
        binding.btnMapCompare.setOnClickListener {
            if (navController != null) {
                findNavController().navigate(R.id.mapFeatureSnapshotFragment)
            }
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


    private val POINT1 = LatLng(11.105950, 106.656514)
    private val POINT2 = LatLng(11.626593, 107.028255)
    private val POINT3 = LatLng(10.966258, 107.476470)
    private val POINT4 = LatLng(11.245575, 107.187573)
    private val POINT5 = LatLng(11.118457, 106.792465)

    private fun getAllPolylines(): List<PolylineOptions>? {
        val options: MutableList<PolylineOptions> = java.util.ArrayList()
        generatePolyline(
            POINT1, POINT2, "#F44336"
        )?.let {
            options.add(
                it
            )
        }
        generatePolyline(
            POINT2, POINT3, "#FF5722"
        )?.let {
            options.add(
                it
            )
        }
        generatePolyline(
            POINT3, POINT4, "#673AB7"
        )?.let {
            options.add(
                it
            )
        }
        generatePolyline(
            POINT4, POINT5, "#009688"
        )?.let {
            options.add(
                it
            )
        }

        return options
    }

    private fun generatePolyline(start: LatLng, end: LatLng, color: String): PolylineOptions? {
        val line = PolylineOptions()
        line.add(start)
        line.add(end)
        line.color(Color.parseColor(color))
        return line
    }

    internal object Config {
        val BLUE_COLOR = Color.parseColor("#3bb2d0")
        val STAR_SHAPE_POINTS: ArrayList<LatLng?> = object : ArrayList<LatLng?>() {
            init {
                add(LatLng(11.105950, 106.656514))
                add(LatLng(11.626593, 107.028255))
                add(LatLng(10.966258, 107.476470))
                add(LatLng(11.245575, 107.187573))
                add(LatLng(11.118457, 106.792465))
            }
        }
    }

    private fun onLatLngListLoaded(latLngs: List<LatLng>?, amount: Int) {
        progressDialog!!.hide()
//        locations = latLngs
        showMarkers(amount)
    }

    private fun showMarkers(amount: Int) {
        var amount = amount
        if (mapboxMap == null || locations == null || binding.mapView.isDestroyed) {
            return
        }
        mapboxMap.clear()
        if (locations!!.size < amount) {
            amount = locations!!.size
        }
        showGlMarkers(amount)
    }

    private fun showGlMarkers(amount: Int) {

        val markerOptionsList: MutableList<MarkerOptions> = java.util.ArrayList()
        val formatter = DecimalFormat("#.#####")
        val random = Random()
        var randomIndex: Int
        for (i in 0 until amount) {
            randomIndex = random.nextInt(locations!!.size)
            val latLng = locations!![randomIndex]
            markerOptionsList.add(
                MarkerOptions().position(latLng).title(i.toString())
                    .snippet(formatter.format(latLng.latitude) + "`, " + formatter.format(latLng.longitude))
            )
        }
        mapboxMap.addMarkers(markerOptionsList)
    }

    private class LoadLocationTask constructor(
        activity: MapFeatureFragment, private val amount: Int
    ) : AsyncTask<Void?, Int?, List<LatLng>?>() {
        private val activity: WeakReference<MapFeatureFragment>
        override fun doInBackground(vararg p0: Void?): List<LatLng>? {
            val activity = activity.get()
            if (activity != null) {
                var json: String? = "https://panel.hainong.vn/api/v2/diagnostics/pets_map"
                try {
                    json = GeoParseUtil.loadStringFromAssets(
                        activity.requireContext(), "points.geojson"
                    )
                } catch (exception: IOException) {
                    Log.e("Could not add markers", exception.toString())
                }
                if (json != null) {
                    return GeoParseUtil.parseGeoJsonCoordinates(json)
                }
            }
            return null
        }

        override fun onPostExecute(locations: List<LatLng>?) {
            super.onPostExecute(locations)
            val activity = activity.get()
            activity?.onLatLngListLoaded(locations, amount)
        }

        init {
            this.activity = WeakReference(activity)
        }
    }


}