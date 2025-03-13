package com.trackasia.sample

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.trackasia.android.TrackAsia
import com.trackasia.android.camera.CameraPosition
import com.trackasia.android.camera.CameraUpdateFactory
import com.trackasia.android.geometry.LatLng
import com.trackasia.android.location.permissions.PermissionsListener
import com.trackasia.android.location.permissions.PermissionsManager
import com.trackasia.android.maps.Style
import com.trackasia.android.maps.TrackAsiaMap
import com.trackasia.android.style.expressions.Expression
import com.trackasia.android.style.layers.CircleLayer
import com.trackasia.android.style.layers.PropertyFactory
import com.trackasia.android.style.layers.SymbolLayer
import com.trackasia.android.style.sources.GeoJsonOptions
import com.trackasia.android.style.sources.GeoJsonSource
import com.trackasia.sample.databinding.FragmentMapClusterBinding
import com.trackasia.sample.utils.MapUtils
import java.net.URI
import java.net.URISyntaxException

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MapClusterFragment : Fragment(), PermissionsListener, View.OnClickListener {

    private var _binding: FragmentMapClusterBinding? = null
    private lateinit var mapboxMap: TrackAsiaMap
    private lateinit var styleFab: FloatingActionButton
    private lateinit var routeFab: FloatingActionButton
    private var layer: CircleLayer? = null
    private var source: GeoJsonSource? = null
    private var currentStyleIndex = 0
    private var isLoadingStyle = true

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
        _binding = FragmentMapClusterBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)
        initMap()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initMap() {
        if (_binding != null) {
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
                                latLngLocation!!, zoomLocation
                            )
                        }
                    }
                    this.mapboxMap = map
                    cameraAnimation(latLngLocation!!)
                    binding.mapView.addOnDidFinishLoadingStyleListener {
                        val style = mapboxMap!!.style
                        addBusStopSource(style)
                        addBusStopCircleLayer(style)
                        initFloatingActionButtons()
                        isLoadingStyle = false
                    }
                } catch (e: Exception) {
                    Log.d("ERROR MAP:", e.toString())
                }
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
                        latLngLocation!!, zoomLocation
                    )
                }
            }
        } else {
            // Xử lý khi quyền truy cập vị trí không được cấp
        }
    }

    private fun addBusStopSource(style: Style?) {
        try {
            source = GeoJsonSource(SOURCE_ID, URI(URL_BUS_ROUTES))
        } catch (exception: URISyntaxException) {
//            Timber.e(exception, "That's not an url... ")
        }
        style!!.addSource(source!!)
    }

    private fun addBusStopCircleLayer(style: Style?) {
        layer = CircleLayer(LAYER_ID, SOURCE_ID)
        layer!!.setProperties(
//            PropertyFactory.circleStrokeWidth(2F),
//            PropertyFactory.circleStrokeColor(Color.parseColor("#009933")),

//            PropertyFactory.circleBlur(4F),
            PropertyFactory.circleColor(Color.parseColor("#FF9800")),
            PropertyFactory.circleRadius(6.0f)
        )
        style!!.addLayerBelow(layer!!, "water_intermittent")
    }

    private fun initFloatingActionButtons() {
        if (_binding != null) {
            binding.fabRoute.setColorFilter(
                ContextCompat.getColor(
                    requireActivity(),
                    R.color.holo_green_dark
                )
            )
            binding.fabRoute.setOnClickListener(this)
            binding.fabStyle.setOnClickListener(this)
        }
    }

    override fun onClick(view: View) {
        if (isLoadingStyle) {
            return
        }
        if (view.id == R.id.fab_route) {
            showBusCluster()
        } else if (view.id == R.id.fab_style) {
            changeMapStyle()
        }
    }

    private fun showBusCluster() {
        removeFabs()
        removeOldSource()
        addClusteredSource()
    }

    private fun removeOldSource() {
        mapboxMap!!.style!!.removeSource(SOURCE_ID)
        mapboxMap!!.style!!.removeLayer(LAYER_ID)
    }

    private fun addClusteredSource() {
        try {
            mapboxMap!!.style!!.addSource(
                GeoJsonSource(
                    SOURCE_ID_CLUSTER,
                    URI(URL_BUS_ROUTES),
                    GeoJsonOptions().withCluster(true).withClusterMaxZoom(14).withClusterRadius(50)
                )
            )
        } catch (malformedUrlException: URISyntaxException) {
//            Timber.e(malformedUrlException, "That's not an url... ")
        }

        // Add unclustered layer
        val layers = arrayOf(
            intArrayOf(
                150, ResourcesCompat.getColor(
                    resources, R.color.holo_yellow_dark, activity?.theme
                )
            ),
            intArrayOf(
                20,
                ResourcesCompat.getColor(resources, R.color.holo_green_dark, activity?.theme)
            ),
            intArrayOf(
                0, ResourcesCompat.getColor(
                    resources, R.color.holo_blue_dark, activity?.theme
                )
            )
        )
        val unclustered = SymbolLayer("unclustered-points", SOURCE_ID_CLUSTER)
        unclustered.setProperties(
            PropertyFactory.iconImage("bus-15")
        )
        mapboxMap!!.style!!.addLayer(unclustered)
        for (i in layers.indices) {
            // Add some nice circles
            val circles = CircleLayer("cluster-$i", SOURCE_ID_CLUSTER)
            circles.setProperties(
                PropertyFactory.circleColor(layers[i][1]), PropertyFactory.circleRadius(18f)
            )
            val pointCount = Expression.toNumber(Expression.get("point_count"))
            circles.setFilter(
                if (i == 0) {
                    Expression.all(
                        Expression.has("point_count"), Expression.gte(
                            pointCount, Expression.literal(
                                layers[i][0]
                            )
                        )

                    )
                } else {
                    Expression.all(
                        Expression.has("point_count"), Expression.gt(
                            pointCount, Expression.literal(
                                layers[i][0]
                            )
                        ), Expression.lt(
                            pointCount, Expression.literal(
                                layers[i - 1][0]
                            )
                        )
                    )
                }
            )
            mapboxMap!!.style!!.addLayer(circles)
        }

        // Add the count labels
        val count = SymbolLayer("count", SOURCE_ID_CLUSTER)
        count.setProperties(
            PropertyFactory.textField(Expression.toString(Expression.get("point_count"))),
            PropertyFactory.textSize(12f),
            PropertyFactory.textColor(Color.WHITE),
            PropertyFactory.textIgnorePlacement(true),
            PropertyFactory.textAllowOverlap(true)
        )
        mapboxMap!!.style!!.addLayer(count)
    }

    private fun removeFabs() {
        binding.fabRoute!!.visibility = View.GONE
        binding.fabStyle!!.visibility = View.GONE
    }

    private fun changeMapStyle() {
        isLoadingStyle = true
        removeBusStop()
        loadNewStyle()
    }

    private fun removeBusStop() {
        mapboxMap!!.style!!.removeLayer(layer!!)
        mapboxMap!!.style!!.removeSource(source!!)
    }

    private fun loadNewStyle() {
        mapboxMap!!.setStyle(Style.Builder().fromUri(styleUrl))
    }

    private fun addBusStop() {
        mapboxMap!!.style!!.addLayer(layer!!)
        mapboxMap!!.style!!.addSource(source!!)
    }

    companion object {
        const val SOURCE_ID = "bus_stop"
        const val SOURCE_ID_CLUSTER = "bus_stop_cluster"
        const val URL_BUS_ROUTES =
            "https://panel.hainong.vn/api/v2/diagnostics/pets_map"
        const val LAYER_ID = "stops_layer"
    }


}