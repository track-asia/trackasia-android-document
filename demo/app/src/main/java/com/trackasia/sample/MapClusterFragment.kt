package com.trackasia.sample

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.JsonArray
import com.google.gson.JsonObject
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
import com.trackasia.geojson.Feature
import com.trackasia.geojson.FeatureCollection
import com.trackasia.geojson.Point
import com.trackasia.sample.databinding.FragmentMapClusterBinding
import com.trackasia.sample.utils.MapUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Extension function to convert a Drawable to a Bitmap
 */
fun Drawable.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(
        intrinsicWidth,
        intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}

/**
 * Extension property to get the FeatureCollection from a GeoJsonSource
 */
private val GeoJsonSource.featureCollection: FeatureCollection?
    get() {
        val source = this
        return try {
            // Access the current GeoJSON data
            val field = GeoJsonSource::class.java.getDeclaredField("geoJson")
            field.isAccessible = true
            val geoJson = field.get(source)
            
            if (geoJson is FeatureCollection) {
                geoJson
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("MapClusterFragment", "Error accessing featureCollection: ${e.message}")
            null
        }
    }

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MapClusterFragment : Fragment(), PermissionsListener, View.OnClickListener,
    DefaultLifecycleObserver {

    /**
     * Convert JSON to Map and Lists
     */
    private fun jsonToMap(json: JSONObject): Map<String, Any> {
        val result = HashMap<String, Any>()
        
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.get(key)
            
            result[key] = when(value) {
                is JSONObject -> jsonToMap(value)
                is JSONArray -> jsonToList(value)
                else -> value
            }
        }
        
        return result
    }

    /**
     * Convert JSON array to List
     */
    private fun jsonToList(json: JSONArray): List<Any> {
        val result = ArrayList<Any>(json.length())
        
        for (i in 0 until json.length()) {
            val value = json.get(i)
            
            result.add(
                when(value) {
                    is JSONObject -> jsonToMap(value)
                    is JSONArray -> jsonToList(value)
                    else -> value
                }
            )
        }
        
        return result
    }

    private var _binding: FragmentMapClusterBinding? = null
    private val binding get() = _binding!!

    private var permissionsManager: PermissionsManager? = null
    private lateinit var trackasiaMap: TrackAsiaMap
    private var source: GeoJsonSource? = null
    private var layer: CircleLayer? = null
    private var mapUtils: MapUtils? = null
    private var styleUrl: String = ""
    private var idCountry: String? = ""
    private var zoomLocation: Double = 10.0
    private var latLngLocation: LatLng? = null
    private lateinit var routeFab: FloatingActionButton
    private lateinit var styleFab: FloatingActionButton
    private lateinit var myLocationFab: FloatingActionButton
    private lateinit var pestDiseaseFab: FloatingActionButton
    private lateinit var poiFab: FloatingActionButton
    private lateinit var sharedPreferences: SharedPreferences
    private var executor: ExecutorService? = null
    private var client: OkHttpClient? = null
    private var networkClient: OkHttpClient? = null
    private var progressBar: ProgressBar? = null
    private var progressText: TextView? = null
    private var isLoadingStyle = false
    private var isDataLoading = false
    private var isDataProcessed = false

    // Tracking current map mode
    private enum class MapMode {
        NORMAL, BUS_CLUSTER, PEST_DISEASE, POI, NONE
    }

    private var currentMapMode = MapMode.NORMAL

    // Pet map constants
    private var isLoadingPetData = false
    private var isClustersLoaded = false
    private var isPestDiseaseLoaded = false

    // tag for logging
    companion object {
        private const val TAG = "MapClusterFragment"
        const val SOURCE_ID = "bus-stops"
        const val SOURCE_ID_CLUSTER = "bus-stops-cluster"
        const val SOURCE_ID_POI = "poi-source"
        const val LAYER_ID = "bus-stops-layer"
        const val LAYER_ID_POI = "poi-layer"

        // Pet map constants
        const val SOURCE_ID_PET_MAP = "pet-map-source"
        const val LAYER_ID_UNCLUSTERED = "unclustered-pest-points"
        const val LAYER_ID_CLUSTERS = "clustered-pest-points"
        const val LAYER_ID_CLUSTER_COUNT = "cluster-pest-count"
        const val URL_DATA_MAP = "https://panel.hainong.vn/api/v2/diagnostics/pets_map"

        // Maximum items to process in a single chunk
        const val MAX_ITEMS_PER_CHUNK = 1000

        // Hạn chế số lượng feature để tránh tràn bộ nhớ
        const val MAX_FEATURES = 500
    }

    // Track running background tasks
    private val activeBackgroundTasks = mutableListOf<Thread>()
    private val isDataProcessingActive = AtomicBoolean(false)
    
    // Track layers and sources for better cleanup
    private val activeSources = mutableSetOf<String>()
    private val activeLayers = mutableSetOf<String>()

    // Clicked feature reference for click handlers
    private var clickedFeature: Feature? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super<Fragment>.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing MapClusterFragment")
        lifecycle.addObserver(this)
        
        // Initialize PermissionsManager if location permissions aren't granted
        if (!PermissionsManager.areLocationPermissionsGranted(requireActivity())) {
            permissionsManager = PermissionsManager(this)
        }
        
        networkClient = OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val fragmentContext: Context? = context
        fragmentContext?.let {
            Log.d(TAG, "onCreate: Setting up preferences and configuration")
            sharedPreferences = it.getSharedPreferences("trackasia", Context.MODE_PRIVATE)
            idCountry = sharedPreferences.getString("country", "vn")
            styleUrl = MapUtils(requireActivity()).urlStyle(idCountry)
            latLngLocation = MapUtils(requireActivity()).getLatlng(idCountry)
            zoomLocation = MapUtils(requireActivity()).zoom(idCountry)
            mapUtils = MapUtils(requireActivity())

            // Log style URL
            Log.d(
                TAG,
                "onCreate: Map style URL: $styleUrl, Location: $latLngLocation, Zoom: $zoomLocation"
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: Creating view for MapClusterFragment")
        TrackAsia.getInstance(requireActivity())
        _binding = FragmentMapClusterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Setting up map view")
        binding.mapView.onCreate(savedInstanceState)
        
        // Initialize direct references to UI elements
        progressBar = binding.progressBar
        progressText = binding.progressText
        
        // Initialize executor service
        executor = Executors.newFixedThreadPool(3)
        
        initMap()
        binding.progressBar.visibility = View.GONE
        binding.progressText.text = ""
        Log.d(TAG, "onViewCreated: Initial setup completed")
    }

    override fun onDestroyView() {
        cleanupMapResources()
        super.onDestroyView()
        _binding = null
    }

    private fun initMap() {
        Log.d(TAG, "initMap: Starting map initialization")
        if (_binding != null) {
            binding.mapView.getMapAsync { map ->
                try {
                    Log.d(TAG, "initMap: Map async loaded, setting style: $styleUrl")
                    map.setStyle(
                        Style.Builder().fromUri(styleUrl)
                    ) { style ->
                        if (activity != null) {
                            Log.d(TAG, "initMap: Style loaded, enabling location component")
                            try {
                                val hasLocationPermissions = PermissionsManager.areLocationPermissionsGranted(requireActivity())
                                
                                if (hasLocationPermissions) {
                                    // Directly enable location component if permissions already granted
                                    enableLocationComponent(style)
                                } else {
                                    // Create a new permissions manager if needed
                                    if (permissionsManager == null) {
                                        permissionsManager = PermissionsManager(this)
                                    }
                                    // Request location permissions
                                    permissionsManager?.requestLocationPermissions(requireActivity())
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error enabling location component: ${e.message}", e)
                            }
                            loadMapIcons(style)
                        }
                    }
                    this.trackasiaMap = map
                    cameraAnimation(latLngLocation!!)
                    map.addOnMapClickListener { point ->
                        Log.d(TAG, "Map clicked at: $point")
                        handleMapClick(point)
                        false 
                    }

                    binding.mapView.addOnDidFinishLoadingStyleListener {
                        Log.d(TAG, "initMap: Style finished loading")
                        initFloatingActionButtons()
                        showMyLocationButtonWithAnimation()
                        isLoadingStyle = false
                        Log.d(TAG, "initMap: Map fully initialized and ready")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "ERROR MAP: ${e.message}", e)
                }
            }
        } else {
            Log.e(TAG, "initMap: Binding is null, cannot initialize map")
        }
    }

    /**
     * Load necessary map icons efficiently
     */
    private fun loadMapIcons(style: Style) {
        try {
            Log.d(TAG, "loadMapIcons: Loading map icons")
            
            // Load bus icon
            ResourcesCompat.getDrawable(resources, R.drawable.ic_directions_bus_black, null)?.let {
                style.addImage("marker-15", it.toBitmap())
            }
            
            // Load pest icon
            ResourcesCompat.getDrawable(resources, R.drawable.ic_pest, null)?.let {
                style.addImage("park-15", it.toBitmap()) 
            }
            
            Log.d(TAG, "loadMapIcons: All map icons loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading map icons: ${e.message}", e)
        }
    }

    private fun cameraAnimation(point: LatLng) {
        try {
            val cameraPosition = CameraPosition.Builder().target(point).zoom(zoomLocation).build()
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)
            trackasiaMap.animateCamera(cameraUpdate, 1000)
        } catch (e: Exception) {
            Log.d("ERROR CAMERA", e.toString())
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {

    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            trackasiaMap.getStyle { style ->
                if (activity != null) {
                    try {
                        enableLocationComponent(style)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error enabling location after permission: ${e.message}", e)
                    }
                }
            }
        } else {
            // Xử lý khi quyền truy cập vị trí không được cấp
            context?.let { ctx ->
                Toast.makeText(
                    ctx,
                    "Ứng dụng cần quyền truy cập vị trí để hoạt động đầy đủ",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun addBusStopSource(style: Style?) {
        try {
            Log.d(TAG, "Adding bus stop source from URL: $URL_DATA_MAP")
            source = GeoJsonSource(SOURCE_ID, URI(URL_DATA_MAP))
        } catch (exception: URISyntaxException) {
            Log.e(TAG, "Error with URL: $URL_DATA_MAP", exception)
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

    /**
     * Helper method to enable location after permissions are granted
     */
    private fun enableLocationComponent(style: Style) {
        context?.let { ctx ->
            if (ctx is Activity) {
                MapUtils(ctx).enableLocationComponent(
                    style,
                    idCountry,
                    trackasiaMap,
                    PermissionsManager(this), // Create a new instance every time to avoid NPE
                    latLngLocation ?: LatLng(10.728073, 106.624054),
                    zoomLocation
                )
            }
        }
    }

    private fun initFloatingActionButtons() {
        if (_binding != null) {
            Log.d(TAG, "initFloatingActionButtons: Setting up floating action buttons")

            // Khởi tạo style button (thay đổi kiểu bản đồ)
            styleFab = binding.fabStyle
            styleFab.setOnClickListener(this)

            // Khởi tạo route button (hiển thị bus) - now hidden by default
            routeFab = binding.fabRoute
            routeFab.setOnClickListener(this)
            routeFab.visibility = View.GONE // Hide the button itself

            // Khởi tạo nút vị trí hiện tại
            myLocationFab = binding.fabMyLocation
            myLocationFab.setOnClickListener(this)

            // Khởi tạo nút bản đồ sâu bệnh
            pestDiseaseFab = binding.fabDisease
            pestDiseaseFab.setOnClickListener(this)
            pestDiseaseFab.setColorFilter(
                ContextCompat.getColor(requireActivity(), R.color.colorRed)
            )

            // Khởi tạo nút hiển thị POI
            poiFab = binding.fabPoi
            poiFab.setOnClickListener(this)
            poiFab.setColorFilter(
                ContextCompat.getColor(requireActivity(), R.color.colorBlue)
            )

            Log.d(TAG, "initFloatingActionButtons: Buttons initialized with routeFab hidden")
        }
    }

    override fun onClick(view: View) {
        if (isLoadingStyle) {
            Log.d(TAG, "onClick: Ignoring click as style is still loading")
            return
        }
        when (view.id) {
            R.id.fab_route -> {
                // Nút hiển thị bus cluster
                Log.d(TAG, "onClick: Bus cluster button clicked")
                resetMapLayers()
                switchMapMode(MapMode.BUS_CLUSTER)
                showBusCluster()
            }

            R.id.fab_style -> {
                // Nút thay đổi kiểu bản đồ
                Log.d(TAG, "onClick: Style change button clicked")
                changeMapStyle()
            }

            R.id.fab_my_location -> {
                // Nút vị trí hiện tại
                Log.d(TAG, "onClick: My location button clicked")
                moveToCurrentLocation()
            }

            R.id.fab_disease -> {
                // Nút hiển thị sâu bệnh
                Log.d(TAG, "onClick: Pest disease button clicked")
                resetMapLayers()
                switchMapMode(MapMode.PEST_DISEASE)
                showPestDiseaseMap()
            }

            R.id.fab_poi -> {
                // Nút hiển thị điểm POI
                Log.d(TAG, "onClick: POI button clicked")
                resetMapLayers()
                switchMapMode(MapMode.POI)
                showPOIMap()
            }
        }
    }

    /**
     * Reset tất cả các layer trên map trước khi chuyển mode
     */
    private fun resetMapLayers() {
        // Thay vì xử lý từng chế độ riêng, sử dụng hàm switchMapMode với chế độ NORMAL tạm thời
        switchMapMode(MapMode.NORMAL)

        // Khôi phục trạng thái mặc định của các nút
        resetButtonsAppearance()
    }

    /**
     * Khôi phục trạng thái mặc định của các nút
     */
    private fun resetButtonsAppearance() {
        // When using a single container, just make sure all buttons are visible
        routeFab.visibility = View.VISIBLE
        styleFab.visibility = View.VISIBLE
        pestDiseaseFab.visibility = View.VISIBLE
        poiFab.visibility = View.VISIBLE

        // Đặt lại màu sắc mặc định
        routeFab.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.colorBlue))
        pestDiseaseFab.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.colorRed))
        poiFab.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.colorBlue))
    }

    private fun switchMapMode(newMode: MapMode) {
        try {
            if (currentMapMode == newMode) return
            cleanupCurrentMode()
            currentMapMode = newMode
            System.gc()
            Log.d(TAG, "Switched to map mode: $newMode")
        } catch (e: Exception) {
            Log.e(TAG, "Error switching map modes: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun cleanupCurrentMode() {
        trackasiaMap.style?.let { style ->
            try {
                when (currentMapMode) {
                    MapMode.BUS_CLUSTER -> {
                        removeBusClusterLayers(style)
                    }

                    MapMode.PEST_DISEASE -> {
                        removePestDiseaseLayers(style)
                    }

                    MapMode.POI -> {
                        removePOILayers(style)
                    }

                    else -> {
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up current mode: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Xóa các layer bus cluster
     */
    private fun removeBusClusterLayers(style: Style) {
        try {
            // Xóa các layer bus cluster
            val layersToRemove = listOf("unclustered-points", "count")
            layersToRemove.forEach { layerId ->
                try {
                    if (style.getLayer(layerId) != null) {
                        style.removeLayer(layerId)
                        synchronized(activeLayers) {
                            activeLayers.remove(layerId)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing layer $layerId: ${e.message}")
                }
            }

            // Remove cluster layers
            for (i in 0..2) {
                val clusterId = "cluster-$i"
                try {
                    if (style.getLayer(clusterId) != null) {
                        style.removeLayer(clusterId)
                        synchronized(activeLayers) {
                            activeLayers.remove(clusterId)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing cluster layer $clusterId: ${e.message}")
                }
            }

            // Remove the source
            try {
                if (style.getSource(SOURCE_ID_CLUSTER) != null) {
                    style.removeSource(SOURCE_ID_CLUSTER)
                    synchronized(activeSources) {
                        activeSources.remove(SOURCE_ID_CLUSTER)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing source: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing bus cluster layers: ${e.message}")
        }
    }

    /**
     * Xóa các layer pest disease
     */
    private fun removePestDiseaseLayers(style: Style) {
        try {
            // Xóa các layer pest disease
            style.removeLayer(LAYER_ID_UNCLUSTERED)
            style.removeLayer(LAYER_ID_CLUSTERS)
            style.removeLayer(LAYER_ID_CLUSTER_COUNT)
            style.removeSource(SOURCE_ID_PET_MAP)
        } catch (e: Exception) {
            Log.e("MapClusterFragment", "Error removing pest disease layers: ${e.message}")
        }
    }

    /**
     * Xóa các layer POI
     */
    private fun removePOILayers(style: Style) {
        try {
            // Xóa POI layers
            style.removeLayer(LAYER_ID_POI)
            style.removeSource(SOURCE_ID_POI)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing POI layers: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun showBusCluster() {
        try {
            context?.let { ctx ->
                Toast.makeText(
                    ctx,
                    "Đang tải dữ liệu điểm dừng xe buýt...",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Remove old sources and layers
            trackasiaMap.style?.let { style ->
                if (style.getSource(SOURCE_ID) != null) {
                    style.removeLayer(LAYER_ID)
                    style.removeSource(SOURCE_ID)
                }
                
                if (style.getSource(SOURCE_ID_CLUSTER) != null) {
                    // Remove cluster layers
                    val layersToRemove = listOf("unclustered-points", "count", "cluster-0", "cluster-1", "cluster-2")
                    for (layerId in layersToRemove) {
                        if (style.getLayer(layerId) != null) {
                            style.removeLayer(layerId)
                        }
                    }
                    style.removeSource(SOURCE_ID_CLUSTER)
                }
            }

            // Create empty GeoJSON source with clustering options
            trackasiaMap.style?.let { style ->
                val emptyCollection = FeatureCollection.fromFeatures(arrayOf())
                val source = GeoJsonSource(
                    SOURCE_ID_CLUSTER,
                    emptyCollection,
                    GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(14)
                        .withClusterRadius(50)
                )
                style.addSource(source)
                trackAddedSource(SOURCE_ID_CLUSTER)
                
                // Add cluster layers
                addClusterLayers(style)
                
                // Fetch real data if available, otherwise skip
                if (URL_DATA_MAP.startsWith("http")) {
                    fetchBusClusterData()
                } else {
                    Log.d(TAG, "showBusCluster: No valid API URL, skipping data fetch")
                    context?.let { ctx ->
                        Toast.makeText(
                            ctx, 
                            "Không có URL API hợp lệ để tải dữ liệu",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            currentMapMode = MapMode.BUS_CLUSTER
        } catch (e: Exception) {
            Log.e(TAG, "Error showing bus cluster: ${e.message}", e)
            context?.let { ctx ->
                Toast.makeText(
                    ctx,
                    "Không thể tải dữ liệu xe buýt. Vui lòng thử lại sau.",
                    Toast.LENGTH_LONG
                ).show()
            }
            resetButtonsAppearance()
            currentMapMode = MapMode.NORMAL
        }
    }

    private fun removeOldSource() {
        trackasiaMap.style?.let { style ->
            style.removeSource(SOURCE_ID)
            style.removeLayer(LAYER_ID)
        }
    }

    private fun addClusteredSource() {
        try {
            val features = ArrayList<Feature>()
            val baseLocation = latLngLocation ?: LatLng(10.728073, 106.624054)

            // Tạo danh sách các điểm dừng xe buýt mẫu
            val busStopNames = listOf(
                "Bến xe Miền Đông", "Bến xe Miền Tây", "Đại học Bách Khoa",
                "Công viên 23/9", "Bến Thành", "Chợ Bình Tây", "Nhà thờ Đức Bà",
                "Sân bay Tân Sơn Nhất", "Bến xe An Sương", "Suối Tiên"
            )

            for (i in 0 until 40) {
                val latOffset = (Math.random() - 0.5) * 0.05
                val lngOffset = (Math.random() - 0.5) * 0.05
                val busLocation = LatLng(
                    baseLocation.latitude + latOffset, 
                    baseLocation.longitude + lngOffset
                )

                val jsonProps = JsonObject()
                jsonProps.addProperty("id", i.toString())
                jsonProps.addProperty("name", busStopNames[i % busStopNames.size])
                jsonProps.addProperty("route_count", (Math.random() * 10 + 1).toInt().toString())

                val geometry = Point.fromLngLat(busLocation.longitude, busLocation.latitude)
                val feature = Feature.fromGeometry(geometry, jsonProps)
                features.add(feature)
            }

            val featureCollection = FeatureCollection.fromFeatures(features)

            trackasiaMap.style?.let { style ->
                if (style.getSource(SOURCE_ID_CLUSTER) != null) {
                    if (style.getLayer("unclustered-points") != null)
                        style.removeLayer("unclustered-points")
                    if (style.getLayer("cluster-0") != null)
                        style.removeLayer("cluster-0")
                    if (style.getLayer("count") != null)
                        style.removeLayer("count")
                    style.removeSource(SOURCE_ID_CLUSTER)
                }

                val source = GeoJsonSource(
                    SOURCE_ID_CLUSTER,
                    featureCollection,
                    GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(14)
                        .withClusterRadius(50)
                )

                style.addSource(source)

                addClusterLayers(style)

                binding.progressBar.visibility = View.GONE
                binding.progressText.text = ""

                context?.let { ctx ->
                    Toast.makeText(ctx, "Đã tải xong ${features.size} điểm dừng xe buýt", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            currentMapMode = MapMode.BUS_CLUSTER

        } catch (e: Exception) {
            Log.e(TAG, "Error in addClusteredSource: ${e.message}")
            e.printStackTrace()

            binding.progressBar.visibility = View.GONE
            binding.progressText.text = ""

            context?.let { ctx ->
                Toast.makeText(
                    ctx,
                    "Không thể tạo dữ liệu điểm dừng: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun addBusClusterFromMockData() {
        try {
            val baseLocation = latLngLocation ?: LatLng(10.728073, 106.624054)

            // Tạo danh sách feature dưới dạng JSON
            val featuresJson = JSONArray()

            // Tạo một số điểm dừng xe buýt mẫu
            val busStopNames = listOf(
                "Bến xe Miền Đông", "Bến xe Miền Tây", "Đại học Bách Khoa",
                "Công viên 23/9", "Bến Thành", "Chợ Bình Tây", "Nhà thờ Đức Bà",
                "Sân bay Tân Sơn Nhất", "Bến xe An Sương", "Suối Tiên"
            )

            // Tạo 20 điểm dừng ngẫu nhiên
            for (i in 0 until 20) {
                // Tạo offset ngẫu nhiên từ vị trí base
                val latOffset = (Math.random() - 0.5) * 0.05
                val lngOffset = (Math.random() - 0.5) * 0.05
                val busLocation =
                    LatLng(baseLocation.latitude + latOffset, baseLocation.longitude + lngOffset)

                // Tạo feature JSON
                val featureJson = JSONObject()
                featureJson.put("type", "Feature")

                // Tạo geometry
                val geometryJson = JSONObject()
                geometryJson.put("type", "Point")
                val coordinatesJson = JSONArray()
                coordinatesJson.put(busLocation.longitude) // longitude first
                coordinatesJson.put(busLocation.latitude)
                geometryJson.put("coordinates", coordinatesJson)
                featureJson.put("geometry", geometryJson)

                // Tạo properties
                val propertiesJson = JSONObject()
                propertiesJson.put("id", i)
                propertiesJson.put("name", busStopNames[i % busStopNames.size])
                propertiesJson.put("route_count", (Math.random() * 10 + 1).toInt())
                featureJson.put("properties", propertiesJson)

                featuresJson.put(featureJson)
            }

            // Tạo FeatureCollection JSON
            val featureCollectionJson = JSONObject()
            featureCollectionJson.put("type", "FeatureCollection")
            featureCollectionJson.put("features", featuresJson)

            // Chuyển đổi thành FeatureCollection
            val featureCollection = FeatureCollection.fromJson(featureCollectionJson.toString())

            trackasiaMap.style?.let { style ->
                // Tạo source với GeoJsonOptions
                val source = GeoJsonSource(
                    SOURCE_ID_CLUSTER,
                    featureCollection,
                    GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(14)
                        .withClusterRadius(50)
                )

                style.addSource(source)

                // Thêm các layer
                addClusterLayers(style)

                // Thông báo hoàn thành
                context?.let { ctx ->
                    Toast.makeText(ctx, "Đã tải dữ liệu mẫu điểm dừng xe buýt", Toast.LENGTH_SHORT)
                        .show()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error creating mock bus data: ${e.message}")
            e.printStackTrace()
            // Thông báo lỗi cho người dùng
            context?.let { ctx ->
                Toast.makeText(
                    ctx,
                    "Không thể tải dữ liệu điểm dừng xe buýt",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Hiển thị bản đồ sâu bệnh
     */
    private fun showPestDiseaseMap() {
        try {
            // Thông báo đang tải dữ liệu
            context?.let { ctx ->
                Toast.makeText(
                    ctx,
                    "Đang tải dữ liệu điểm trên bản đồ",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Hiển thị UI loading
            binding.progressBar.visibility = View.VISIBLE
            binding.progressText.text = "Đang chuẩn bị dữ liệu..."
            binding.progressBar.progress = 0

            // Xóa dữ liệu cũ trước khi tải mới
            trackasiaMap.style?.let { style ->
                if (style.getSource(SOURCE_ID_PET_MAP) != null) {
                    removeLayerAndSource(style, SOURCE_ID_PET_MAP)
                    Log.d(TAG, "Removed existing pest disease data")
                }
            }
            
            // Set current map mode
            currentMapMode = MapMode.PEST_DISEASE

            // Tạo dữ liệu mẫu
            val features = createSampleFeatures(300) // Tạo 300 điểm mẫu
            
            // Kiểm tra số lượng điểm
            Log.d(TAG, "Created ${features.size} sample data points")

            // Sử dụng phương thức chunking để hiển thị dữ liệu
            addFeaturesToMapInChunks(features)

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi hiển thị bản đồ dữ liệu: ${e.message}", e)
            binding.progressBar.visibility = View.GONE
            binding.progressText.text = ""

            // Thông báo lỗi
            context?.let { ctx ->
                Toast.makeText(
                    ctx,
                    "Lỗi khi tải dữ liệu: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Tạo danh sách các điểm dữ liệu mẫu có thông tin điểm và cluster
     */
    private fun createSampleFeatures(count: Int): List<Feature> {
        val result = ArrayList<Feature>(count)
        val baseLocation = latLngLocation ?: LatLng(10.728073, 106.624054)
        
        // Danh sách các khu vực với offset vị trí
        val regions = listOf(
            Pair("Khu vực A", Pair(0.03, 0.02)),  // Offset cho vùng 1
            Pair("Khu vực B", Pair(-0.02, 0.01)), // Offset cho vùng 2
            Pair("Khu vực C", Pair(-0.01, -0.03)) // Offset cho vùng 3
        )
        
        val pointsPerRegion = count / regions.size
        
        // Tạo dữ liệu cho từng vùng
        for ((regionIndex, region) in regions.withIndex()) {
            val (regionName, offsetPair) = region
            val (latOffset, lngOffset) = offsetPair
            
            for (i in 0 until pointsPerRegion) {
                try {
                    // Tạo offset ngẫu nhiên từ vị trí cơ sở của vùng
                    val randomLatOffset = latOffset + (Math.random() - 0.5) * 0.02
                    val randomLngOffset = lngOffset + (Math.random() - 0.5) * 0.02
                    
                    val pointLocation = LatLng(
                        baseLocation.latitude + randomLatOffset,
                        baseLocation.longitude + randomLngOffset
                    )
                    
                    val point = Point.fromLngLat(pointLocation.longitude, pointLocation.latitude)
                    val id = regionIndex * pointsPerRegion + i
                    
                    // Create a JsonObject for properties
                    val jsonProps = JsonObject()
                    jsonProps.addProperty("id", id.toString())
                    jsonProps.addProperty("name", "Điểm ${id + 1}")
                    jsonProps.addProperty("type", "Loại ${(id % 3) + 1}")
                    jsonProps.addProperty("severity", when(id % 3) {
                        0 -> "Nhẹ"
                        1 -> "Trung bình"
                        else -> "Nghiêm trọng"
                    })
                    jsonProps.addProperty("region", regionName)
                    jsonProps.addProperty("cluster_group", regionIndex.toString())
                    
                    // Create feature using Feature.fromGeometry
                    val feature = Feature.fromGeometry(point, jsonProps)
                    result.add(feature)
                    
                    if (i % 50 == 0) {
                        Log.d(TAG, "Created feature $id for region $regionName")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi tạo điểm dữ liệu: ${e.message}")
                }
            }
        }
        
        Log.d(TAG, "Created ${result.size} sample features for clustering")
        return result
    }

    /**
     * Fetch pet map data from the server
     */
    private fun fetchPetMapData(callback: (Boolean) -> Unit) {
        binding.progressBar.visibility = View.VISIBLE

        val client = networkClient ?: OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val request = Request.Builder()
            .url(URL_DATA_MAP)
            .build()

        val weakContext = WeakReference(context)
        val weakActivity = WeakReference(activity)

        val requestThread = Thread {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)

                var retryCount = 0
                var success = false
                var lastException: Exception? = null

                while (retryCount < 3 && !success && !Thread.currentThread().isInterrupted) {
                    try {
                        if (retryCount > 0) {
                            Thread.sleep((1000L * (retryCount * retryCount)).coerceAtMost(10000))
                        }
                        client.newCall(request).execute().use { response ->
                            try {
                                if (response.isSuccessful) {
                                    val jsonBody = response.body?.string()
                                    if (jsonBody != null) {
                                        val jsonObject = JSONObject(jsonBody)
                                        val features = jsonObject.getJSONArray("features")
                                        val featuresList = mutableListOf<Map<String, Any>>()
                                        for (i in 0 until features.length()) {
                                            try {
                                                val feature = features.getJSONObject(i)
                                                featuresList.add(jsonToMap(feature))
                                            } catch (e: Exception) {
                                                Log.e(
                                                    TAG,
                                                    "Error converting feature at index $i: ${e.message}"
                                                )
                                            }
                                        }
                                        if (weakActivity.get() == null || !isAdded || Thread.currentThread().isInterrupted)
                                            return@use
                                        Handler(Looper.getMainLooper()).post {
                                            try {
                                                if (!isAdded) return@post
                                                onPetMapDataReceived(featuresList)
                                                success = true
                                                callback(true)
                                            } catch (e: Exception) {
                                                Log.e(
                                                    TAG,
                                                    "Error in pet map data callback: ${e.message}"
                                                )
                                                callback(false)
                                            } finally {
                                                binding.progressBar.visibility = View.GONE
                                            }
                                        }
                                    } else {
                                        throw IOException("Empty response body")
                                    }
                                } else {
                                    throw IOException("Server returned ${response.code}: ${response.message}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, e.message.toString())
                            }
                        }
                        success = true
                    } catch (e: Exception) {
                        lastException = e
                        Log.e(TAG,"fetchPetMapData: Error on attempt ${retryCount + 1}/3: ${e.message}")
                        retryCount++
                    }
                }

                if (!success && isAdded && weakActivity.get() != null) {
                    Handler(Looper.getMainLooper()).post {
                        if (!isAdded) return@post
                        binding.progressBar.visibility = View.GONE
                        weakContext.get()?.let { ctx ->
                            Toast.makeText(
                                ctx,
                                "Có lỗi khi tải dữ liệu: ${lastException?.message ?: "Unknown error"}. Đang sử dụng dữ liệu mẫu.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        // Thay thế addPetMapFromMockData() bằng createSampleFeatures + addFeaturesToMapInChunks
                        val sampleFeatures = createSampleFeatures(300)
                        addFeaturesToMapInChunks(sampleFeatures)
                        callback(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetchPetMapData: Fatal error: ${e.message}", e)

                if (isAdded && weakActivity.get() != null) {
                    Handler(Looper.getMainLooper()).post {
                        if (!isAdded) return@post

                        binding.progressBar.visibility = View.GONE

                        weakContext.get()?.let { ctx ->
                            Toast.makeText(
                                ctx,
                                "Lỗi tải dữ liệu: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        // Thay thế addPetMapFromMockData() bằng createSampleFeatures + addFeaturesToMapInChunks
                        val sampleFeatures = createSampleFeatures(300)
                        addFeaturesToMapInChunks(sampleFeatures)
                        callback(false)
                    }
                }
            } finally {
                if (isAdded && weakActivity.get() != null) {
                    Handler(Looper.getMainLooper()).post {
                        binding.progressBar.visibility = View.GONE
                    }
                }

                synchronized(activeBackgroundTasks) {
                    activeBackgroundTasks.remove(Thread.currentThread())
                }
            }
        }

        synchronized(activeBackgroundTasks) {
            activeBackgroundTasks.add(requestThread)
        }
        requestThread.start()
    }

    /**
     * Process pet map data after successful fetch
     */
    private fun onPetMapDataReceived(features: List<Map<String, Any>>) {
        try {
            Log.d(TAG, "Xử lý ${features.size} điểm dữ liệu")

            // Chuyển đổi dữ liệu Map thành Feature
            val featuresList = convertMapToFeatures(features)
            Log.d(TAG, "Đã chuyển đổi ${featuresList.size} điểm thành Feature")

            // Giới hạn số lượng features để tránh OOM
            val maxFeatures = MAX_FEATURES
            val limitedFeatures = if (featuresList.size > maxFeatures) {
                Log.w(TAG, "Giới hạn số lượng điểm từ ${featuresList.size} xuống $maxFeatures")
                featuresList.subList(0, maxFeatures)
            } else {
                featuresList
            }

            // Dùng phương thức chunking để xử lý dữ liệu
            addFeaturesToMapInChunks(limitedFeatures)

            // Đánh dấu dữ liệu đã được tải
            isPestDiseaseLoaded = true

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi xử lý dữ liệu: ${e.message}", e)
            context?.let { ctx ->
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        ctx,
                        "Không thể xử lý dữ liệu: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Dọn dẹp layer và source sâu bệnh
     */
    private fun cleanupPestDiseaseLayers(style: Style) {
        try {
            if (style.getSource(SOURCE_ID_PET_MAP) != null) {
                Log.d(TAG, "Xóa source và layer sâu bệnh cũ")
                listOf(LAYER_ID_UNCLUSTERED, LAYER_ID_CLUSTERS, LAYER_ID_CLUSTER_COUNT).forEach { layerId ->
                    if (style.getLayer(layerId) != null) {
                        style.removeLayer(layerId)
                        synchronized(activeLayers) {
                            activeLayers.remove(layerId)
                        }
                    }
                }
                style.removeSource(SOURCE_ID_PET_MAP)
                synchronized(activeSources) {
                    activeSources.remove(SOURCE_ID_PET_MAP)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi dọn dẹp layer: ${e.message}")
        }
    }

    private fun createSampleFeature(): Feature {
        try {
            val baseLocation = latLngLocation ?: LatLng(10.728073, 106.624054)
            val point = Point.fromLngLat(baseLocation.longitude, baseLocation.latitude)

            val jsonProps = JsonObject()
            jsonProps.addProperty("id", 1)
            jsonProps.addProperty("name", "Test Point")
            jsonProps.addProperty("severity", "High")

            // Convert to a Feature
            return Feature.fromGeometry(point, jsonProps)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating sample feature: ${e.message}", e)
            // Create minimal feature as fallback
            val point = Point.fromLngLat(106.624054, 10.728073)
            return Feature.fromGeometry(point)
        }
    }

    // Helper function to move the camera to see the sample point
    private fun moveToSampleLocation() {
        try {
            if (::trackasiaMap.isInitialized) {
                // Move to the default or configured location
                latLngLocation?.let { location ->
                    val cameraPosition = CameraPosition.Builder()
                        .target(location)
                        .zoom(10.0) // Zoom out to see clusters
                        .build()
                        
                    trackasiaMap.animateCamera(
                        CameraUpdateFactory.newCameraPosition(cameraPosition),
                        1000
                    )
                    Log.d(TAG, "moveToSampleLocation: Camera moved to sample location at $location")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error moving to sample location: ${e.message}", e)
        }
    }

    /**
     * Process large pet data in chunks to prevent memory issues
     */
    private fun processLargePetData(style: Style, features: List<Map<String, Any>>) {
        if (isDataProcessingActive.getAndSet(true)) {
            Log.d(TAG, "processLargePetData: Already processing data, skipping")
            return
        }

        // Validate input data
        if (features.isEmpty()) {
            Log.e(TAG, "processLargePetData: Features list is empty, aborting")
            isDataProcessingActive.set(false)
            return
        }

        // Start thread for background processing
        val processingThread = Thread {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)

                // Limit features to prevent OOM
                val maxFeatures = 300 // Significantly reduced to prevent memory issues
                val limitedFeatures = if (features.size > maxFeatures) {
                    Log.w(TAG, "processLargePetData: Limiting features from ${features.size} to $maxFeatures")
                    features.subList(0, maxFeatures)
                } else {
                    features
                }

                // Set optimal chunk size - smaller chunks for better performance
                val chunkSize = 50
                val totalFeatures = limitedFeatures.size
                val weakStyle = WeakReference(style)

                // Process data in smaller chunks
                val featuresList = mutableListOf<Feature>()

                // First, convert all features to avoid repeated JSON parsing
                for (feature in limitedFeatures) {
                    try {
                        if (!isAdded || Thread.currentThread().isInterrupted) break

                        if (feature.containsKey("type") && feature.containsKey("geometry")) {
                            val jsonFeature = JsonObject()
                            jsonFeature.addProperty("type", "Feature")

                            val geometry = feature["geometry"] as? Map<*, *>
                            if (geometry != null && geometry.containsKey("type") && geometry.containsKey("coordinates")) {
                                val jsonGeometry = JsonObject()
                                jsonGeometry.addProperty("type", geometry["type"].toString())
                                val coordinates = geometry["coordinates"] as? List<*>
                                if (coordinates != null && coordinates.isNotEmpty()) {
                                    val jsonCoordinates = JsonArray()
                                    for (coord in coordinates) {
                                        when (coord) {
                                            is Number -> jsonCoordinates.add(coord)
                                            else -> jsonCoordinates.add(coord.toString())
                                        }
                                    }
                                    jsonGeometry.add("coordinates", jsonCoordinates)
                                    jsonFeature.add("geometry", jsonGeometry)

                                    val properties = feature["properties"] as? Map<*, *>
                                    if (properties != null) {
                                        val jsonProperties = JsonObject()
                                        for ((key, value) in properties) {
                                            if (key != null && value != null) {
                                                jsonProperties.addProperty(key.toString(), value.toString())
                                            }
                                        }
                                        jsonFeature.add("properties", jsonProperties)
                                    } else {
                                        jsonFeature.add("properties", JsonObject())
                                    }

                                    try {
                                        val jsonString = jsonFeature.toString()
                                        val featureObj = Feature.fromJson(jsonString)
                                        if (featureObj.geometry() != null) {
                                            featuresList.add(featureObj)
                                        }
                                    } catch (e: Exception) {
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing feature: ${e.message}")
                    }

                    if (featuresList.size % 10 == 0) {
                        Thread.sleep(5)
                    }
                }

                if (featuresList.isNotEmpty() && isAdded && !Thread.currentThread().isInterrupted) {
                    for (i in 0 until featuresList.size step chunkSize) {
                        if (!isAdded || Thread.currentThread().isInterrupted) break

                        val endIndex = minOf(i + chunkSize, featuresList.size)
                        val chunk = featuresList.subList(i, endIndex)

                        Handler(Looper.getMainLooper()).post {
                            try {
                                if (!isAdded) return@post

                                val currentStyle = weakStyle.get()
                                if (currentStyle == null || !currentStyle.isFullyLoaded) {
                                    return@post
                                }

                                val featureCollection = FeatureCollection.fromFeatures(chunk.toTypedArray())
                                val source = currentStyle.getSourceAs<GeoJsonSource>(SOURCE_ID_PET_MAP)

                                if (source == null) {
                                    val newSource = GeoJsonSource(
                                        SOURCE_ID_PET_MAP,
                                        featureCollection,
                                        GeoJsonOptions()
                                            .withCluster(true)
                                            .withClusterMaxZoom(14)
                                            .withClusterRadius(50)
                                    )
                                    currentStyle.addSource(newSource)
                                    trackAddedSource(SOURCE_ID_PET_MAP)

                                    if (currentStyle.getLayer(LAYER_ID_UNCLUSTERED) == null ||
                                        currentStyle.getLayer(LAYER_ID_CLUSTERS) == null ||
                                        currentStyle.getLayer(LAYER_ID_CLUSTER_COUNT) == null) {
                                        addPetClusterLayers(currentStyle)
                                    }
                                } else {
                                    source.setGeoJson(featureCollection)
                                }

                            } catch (e: Exception) {
                                Log.e(TAG, "Error updating source: ${e.message}")
                            }
                        }

                        Thread.sleep(150)
                    }
                }

                // Show how many features were processed
                if (isAdded) {
                    Handler(Looper.getMainLooper()).post {
                        context?.let { ctx ->
                            if (isAdded) {
                                Toast.makeText(
                                    ctx,
                                    "Đã xử lý ${featuresList.size} điểm dữ liệu",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing pet map data: ${e.message}", e)
            } finally {
                isDataProcessingActive.set(false)

                synchronized(activeBackgroundTasks) {
                    activeBackgroundTasks.remove(Thread.currentThread())
                }

                System.gc()
            }
        }

        synchronized(activeBackgroundTasks) {
            activeBackgroundTasks.add(processingThread)
        }
        processingThread.start()
    }

    private fun showPestDiseaseLayers() {
        trackasiaMap.style?.let { style ->
            try {
                if (style.getSource(SOURCE_ID_PET_MAP) == null) {
                    // Nếu source không còn tồn tại, fetch lại dữ liệu
                    Log.d(TAG, "Source not found, fetching data again from URL: $URL_DATA_MAP")
                    // Update to use proper callback
                    fetchPetMapData { success ->
                        Log.d(TAG, "Fetched pet map data successfully: $success")
                    }
                } else {
                    // Nếu source còn tồn tại, chỉ cần thêm lại các layer
                    Log.d(TAG, "Reusing existing source data")
                    addPetClusterLayers(style)

                    // Thông báo hoàn thành
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Đã hiển thị lại dữ liệu sâu bệnh", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing pest disease layers: ${e.message}")
                e.printStackTrace()

                // Nếu lỗi, tạo dữ liệu mẫu
                Log.d(TAG, "Error loading layers, using mock data")
                val sampleFeatures = createSampleFeatures(300)
                addFeaturesToMapInChunks(sampleFeatures)
            }
        }
    }

    private fun changeMapStyle() {
        isLoadingStyle = true
        removeBusStop()
        loadNewStyle()
    }

    private fun removeBusStop() {
        trackasiaMap.style?.let { style ->
            layer?.let { style.removeLayer(it) }
            source?.let { style.removeSource(it) }
        }
    }

    private fun loadNewStyle() {
        trackasiaMap.setStyle(Style.Builder().fromUri(styleUrl))
    }

    private fun addBusStop() {
        trackasiaMap.style?.let { style ->
            layer?.let { style.addLayer(it) }
            source?.let { style.addSource(it) }
        }
    }

    /**
     * Hiển thị bản đồ POI
     */
    private fun showPOIMap() {
        try {
            // Nổi bật nút POI
            poiFab.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.colorGreen))

            // Hiển thị feedback loading
            context?.let { ctx ->
                Toast.makeText(
                    ctx,
                    "Đang tải dữ liệu điểm quan tâm...",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Tạo một số điểm POI mẫu vào bản đồ
            addSamplePOIs()

            // Đặt chế độ map
            currentMapMode = MapMode.POI
        } catch (e: Exception) {
            Log.e(TAG, "Error showing POI map: ${e.message}")
            e.printStackTrace()

            // Thông báo lỗi và khôi phục trạng thái
            context?.let { ctx ->
                Toast.makeText(
                    ctx,
                    "Không thể tải dữ liệu điểm quan tâm. Vui lòng thử lại sau.",
                    Toast.LENGTH_LONG
                ).show()
            }
            resetButtonsAppearance()
            currentMapMode = MapMode.NORMAL
        }
    }

    /**
     * Thêm một số điểm POI mẫu vào bản đồ
     */
    private fun addSamplePOIs() {
        try {
            // Lấy vị trí hiện tại làm cơ sở
            val baseLocation = latLngLocation ?: LatLng(10.728073, 106.624054)

            // Tạo danh sách feature dưới dạng JSON
            val featuresJson = JSONArray()

            // Tạo một số điểm POI ngẫu nhiên gần vị trí hiện tại
            val poiNames = listOf("Nhà hàng", "Trường học", "Bệnh viện", "Công viên", "Siêu thị")
            for (i in 0 until 10) {
                // Tạo offset ngẫu nhiên từ vị trí base
                val latOffset = (Math.random() - 0.5) * 0.03
                val lngOffset = (Math.random() - 0.5) * 0.03
                val poiLocation =
                    LatLng(baseLocation.latitude + latOffset, baseLocation.longitude + lngOffset)

                // Tạo feature JSON
                val featureJson = JSONObject()
                featureJson.put("type", "Feature")

                // Tạo geometry
                val geometryJson = JSONObject()
                geometryJson.put("type", "Point")
                val coordinatesJson = JSONArray()
                coordinatesJson.put(poiLocation.longitude) // longitude first
                coordinatesJson.put(poiLocation.latitude)
                geometryJson.put("coordinates", coordinatesJson)
                featureJson.put("geometry", geometryJson)

                // Tạo properties
                val propertiesJson = JSONObject()
                propertiesJson.put("id", i)
                propertiesJson.put("name", poiNames[i % poiNames.size])
                propertiesJson.put("type", "POI")
                featureJson.put("properties", propertiesJson)

                featuresJson.put(featureJson)
            }
            val featureCollectionJson = JSONObject()
            featureCollectionJson.put("type", "FeatureCollection")
            featureCollectionJson.put("features", featuresJson)
            val featureCollection = FeatureCollection.fromJson(featureCollectionJson.toString())

            trackasiaMap.style?.let { style ->
                val source = GeoJsonSource(SOURCE_ID_POI, featureCollection)
                style.addSource(source)
                val layer = SymbolLayer(LAYER_ID_POI, SOURCE_ID_POI)
                layer.setProperties(
                    PropertyFactory.iconImage("marker-15"),
                    PropertyFactory.iconSize(1.5f),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.textField(Expression.get("name")),
                    PropertyFactory.textSize(10f),
                    PropertyFactory.textOffset(arrayOf(0f, 2f)),
                    PropertyFactory.textColor(Color.BLACK),
                    PropertyFactory.textHaloColor(Color.WHITE),
                    PropertyFactory.textHaloWidth(1f)
                )
                style.addLayer(layer)

                // Thông báo hoàn thành
                context?.let { ctx ->
                    Toast.makeText(ctx, "Đã tải xong điểm quan tâm", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding sample POIs: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun handleMapClick(point: LatLng) {
        when (currentMapMode) {
            MapMode.BUS_CLUSTER -> handleBusClusterClick(point)
            MapMode.PEST_DISEASE -> handlePestDiseaseClick(point)
            MapMode.POI -> handlePOIClick(point)
            else -> { /* Do nothing in normal mode */ }
        }
    }

    private fun handleBusClusterClick(point: LatLng) {
        try {
            val screenPoint = trackasiaMap.projection.toScreenLocation(point)
            val clusterFeatures = trackasiaMap.queryRenderedFeatures(screenPoint, "count")

            if (clusterFeatures.isNotEmpty()) {
                val feature = clusterFeatures[0]
                val jsonObject = JSONObject(feature.toJson())
                val geometry = jsonObject.optJSONObject("geometry")
                if (geometry != null && geometry.getString("type") == "Point") {
                    val coordinates = geometry.getJSONArray("coordinates")
                    val lng = coordinates.getDouble(0)
                    val lat = coordinates.getDouble(1)
                    val clusterPoint = LatLng(lat, lng)
                    val properties = jsonObject.optJSONObject("properties")
                    val clusterSize = properties?.optInt("point_count", 0) ?: 0
                    val currentZoom = trackasiaMap.cameraPosition.zoom
                    val zoomIncrement = when {
                        clusterSize > 100 -> 2.0
                        clusterSize > 50 -> 1.5
                        else -> 1.0
                    }
                    val cameraPosition = CameraPosition.Builder()
                        .target(clusterPoint)
                        .zoom(currentZoom + zoomIncrement)
                        .build()

                    trackasiaMap.animateCamera(
                        CameraUpdateFactory.newCameraPosition(cameraPosition),
                        800
                    )
                    context?.let { ctx ->
                        Toast.makeText(
                            ctx,
                            "Nhóm chứa $clusterSize điểm dừng. Đang phóng to...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    return
                }
            }
            val pointFeatures = trackasiaMap.queryRenderedFeatures(
                screenPoint,
                "unclustered-points"
            )

            if (pointFeatures.isNotEmpty()) {
                val feature = pointFeatures[0]
                val properties = feature.properties()
                val name = properties?.get("name")?.asString ?: "Không xác định"
                val id = properties?.get("id")?.asLong?.toInt() ?: 0

                context?.let { ctx ->
                    Toast.makeText(
                        ctx,
                        "Điểm dừng: $name (ID: $id)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling bus cluster click: ${e.message}")
        }
    }

    /**
     * Handle pest disease click on map
     */
    private fun handlePestDiseaseClick(): Boolean {
        // Get the clicked feature
        val feature = clickedFeature ?: return false

        try {
            val properties = feature.properties()
            
            if (properties == null) {
                return false
            }
            
            when {
                properties.has("cluster") && properties.get("cluster").asBoolean -> {
                    // Handle cluster click
                    val pointCount = properties.get("point_count")?.asInt ?: 0
                    Toast.makeText(
                        context,
                        "Cụm chứa $pointCount điểm",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Zoom in to cluster
                    val coordinates = (feature.geometry() as Point).coordinates()
                    val position = CameraPosition.Builder()
                        .target(LatLng(coordinates[1], coordinates[0]))
                        .zoom(trackasiaMap.cameraPosition.zoom + 2)
                        .build()
                    trackasiaMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 500)
                    return true
                }
                else -> {
                    // Get pest disease properties
                    val id = properties.get("id")?.asString ?: ""
                    val name = properties.get("name")?.asString ?: ""
                    val region = properties.get("region")?.asString ?: ""
                    val severity = properties.get("severity")?.asInt ?: 0

                    // Get severity string
                    val severityStr = when {
                        severity > 7 -> "Cao"
                        severity > 4 -> "Trung bình"
                        else -> "Thấp"
                    }

                    // Show dialog with pest disease details
                    context?.let { ctx ->
                        AlertDialog.Builder(ctx)
                            .setTitle("Thông tin sâu bệnh")
                            .setMessage("ID: $id\nTên: $name\nKhu vực: $region\nMức độ: $severityStr")
                            .setPositiveButton("Đóng") { dialog, _ -> dialog.dismiss() }
                            .show()
                    }
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling pest disease click: ${e.message}", e)
        }

        return false
    }

    /**
     * Optimized POI click handling
     */
    private fun handlePOIClick(point: LatLng) {
        try {
            val screenPoint = trackasiaMap.projection.toScreenLocation(point)

            // Query for POI features
            val poiFeatures = trackasiaMap.queryRenderedFeatures(
                screenPoint,
                LAYER_ID_POI
            )

            if (poiFeatures.isNotEmpty()) {
                val feature = poiFeatures[0]
                val properties = feature.properties()

                // Get POI information
                val name = properties?.get("name")?.asString ?: "Không xác định"
                val type = properties?.get("type")?.asString ?: "Không xác định"

                // Show dialog
                context?.let { ctx ->
                    androidx.appcompat.app.AlertDialog.Builder(ctx)
                        .setTitle("Chi Tiết Địa Điểm")
                        .setMessage(
                            """
                            Tên: $name
                            Loại: $type
                            """.trimIndent()
                        )
                        .setPositiveButton("Đóng") { dialog, _ -> dialog.dismiss() }
                        .create()
                        .show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling POI click: ${e.message}")
        }
    }

    /**
     * Add cluster layers for bus stops
     */
    private fun addClusterLayers(style: Style) {
        try {
            // 1. Layer for individual points
            val unclustered = SymbolLayer("unclustered-points", SOURCE_ID_CLUSTER)
            unclustered.setProperties(
                PropertyFactory.iconImage("marker-15"),
                PropertyFactory.iconSize(1.0f),
                PropertyFactory.iconAllowOverlap(true)
            )
            unclustered.setFilter(Expression.not(Expression.has("point_count")))
            style.addLayer(unclustered)
            trackAddedLayer("unclustered-points")

            // 2. Circle layers for clusters
            val clusterLayer = CircleLayer("cluster-0", SOURCE_ID_CLUSTER)
            clusterLayer.setProperties(
                PropertyFactory.circleColor(Color.parseColor("#FFA500")),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleRadius(
                    Expression.interpolate(
                        Expression.exponential(1.5f),
                        Expression.get("point_count"),
                        Expression.stop(1, 15f),
                        Expression.stop(10, 20f),
                        Expression.stop(50, 25f),
                        Expression.stop(100, 30f)
                    )
                )
            )
            clusterLayer.setFilter(Expression.has("point_count"))
            style.addLayer(clusterLayer)
            trackAddedLayer("cluster-0")

            // 3. Count layer
            val count = SymbolLayer("count", SOURCE_ID_CLUSTER)
            count.setProperties(
                PropertyFactory.textField(Expression.toString(Expression.get("point_count"))),
                PropertyFactory.textSize(12f),
                PropertyFactory.textColor(Color.WHITE),
                PropertyFactory.textIgnorePlacement(true),
                PropertyFactory.textAllowOverlap(true)
            )
            count.setFilter(Expression.has("point_count"))
            style.addLayer(count)
            trackAddedLayer("count")

            Log.d(TAG, "addClusterLayers: Added optimized cluster layers")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding cluster layers: ${e.message}", e)
        }
    }

    private fun addPetClusterLayers(style: Style) {
        try {
            Log.d(TAG, "addPetClusterLayers: Starting to add layers")
            val source = style.getSourceAs<GeoJsonSource>(SOURCE_ID_PET_MAP)
            if (source == null) {
                Log.e(TAG, "addPetClusterLayers: Source $SOURCE_ID_PET_MAP doesn't exist, cannot create layers")
                return
            }

            // Simple layer structure with fewer clusters for better performance

            // 1. Use simplified circle layers for clusters
            val clustersLayer = CircleLayer(LAYER_ID_CLUSTERS, SOURCE_ID_PET_MAP)
            clustersLayer.setProperties(
                PropertyFactory.circleColor(Color.parseColor("#00B4D8")),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleRadius(
                    Expression.interpolate(
                        Expression.exponential(1.75f),
                        Expression.get("point_count"),
                        Expression.stop(1, 15f),
                        Expression.stop(10, 20f),
                        Expression.stop(50, 25f),
                        Expression.stop(100, 30f)
                    )
                )
            )

            // Filter for clustered points
            clustersLayer.setFilter(Expression.has("point_count"))
            style.addLayer(clustersLayer)
            trackAddedLayer(LAYER_ID_CLUSTERS)

            // 2. Add count labels layer with simplified properties
            val countLayer = SymbolLayer(LAYER_ID_CLUSTER_COUNT, SOURCE_ID_PET_MAP)
            countLayer.setProperties(
                PropertyFactory.textField(Expression.toString(Expression.get("point_count"))),
                PropertyFactory.textSize(12f),
                PropertyFactory.textColor(Color.WHITE),
                PropertyFactory.textIgnorePlacement(true),
                PropertyFactory.textAllowOverlap(true)
            )
            countLayer.setFilter(Expression.has("point_count"))
            style.addLayer(countLayer)
            trackAddedLayer(LAYER_ID_CLUSTER_COUNT)

            // 3. Individual point layer with simplified properties
            val unclusteredLayer = CircleLayer(LAYER_ID_UNCLUSTERED, SOURCE_ID_PET_MAP)
            unclusteredLayer.setProperties(
                PropertyFactory.circleColor(Color.parseColor("#00FF99")),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleStrokeWidth(1f),
                PropertyFactory.circleRadius(8f)
            )

            // Filter for unclustered points
            unclusteredLayer.setFilter(Expression.not(Expression.has("point_count")))
            style.addLayer(unclusteredLayer)
            trackAddedLayer(LAYER_ID_UNCLUSTERED)

            Log.d(TAG, "addPetClusterLayers: Successfully added simplified cluster layers")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding pet cluster layers: ${e.message}", e)
        }
    }

    /**
     * Tạo các icon cho cluster biểu đồ tròn với các phân đoạn màu khác nhau
     * Dựa trên cách tiếp cận từ TrackasiaUtils.createDonutChartPng
     */
    private fun createClusterIcons(style: Style) {
        try {
            Log.d(TAG, "createClusterIcons: Creating donut chart icons for clusters")

            // Danh sách màu sắc - tương tự colors trong TrackasiaUtils
            val colors = listOf(
                Color.parseColor("#00FF99"),
                Color.parseColor("#00B4D8"),
                Color.parseColor("#FFA500"),
                Color.parseColor("#006600"),
                Color.parseColor("#FF4500"),
                Color.parseColor("#0304AF")
            )

            // Định nghĩa các kích thước cluster và tạo biểu đồ tròn tương ứng
            val clusterSizes = listOf(
                Pair("cluster-small-icon", 5),
                Pair("cluster-medium-icon", 25),
                Pair("cluster-large-icon", 75),
                Pair("cluster-xlarge-icon", 250),
                Pair("cluster-xxlarge-icon", 750),
                Pair("cluster-mega-icon", 2000)
            )

            for ((iconId, clusterSize) in clusterSizes) {
                // Tạo hình ảnh donut chart cho kích thước cluster này
                val bitmap = createDonutChartBitmap(clusterSize, colors)
                style.addImage(iconId, bitmap)
                Log.d(TAG, "createClusterIcons: Created icon $iconId for cluster size $clusterSize")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating cluster icons: ${e.message}", e)
        }
    }

    /**
     * Tạo bitmap biểu đồ tròn (donut chart) cho cluster với kích thước nhất định
     * Dựa trên TrackasiaUtils.createDonutChartPng và getClusterSegments
     */
    private fun createDonutChartBitmap(clusterSize: Int, colors: List<Int>): Bitmap {
        val width = 70
        val height = 70
        val strokeWidth = 12f
        val radius = Math.min(width, height) / 2f - strokeWidth / 2

        // Tạo bitmap trống
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = android.graphics.PointF(width / 2f, height / 2f)

        // Tính toán các phân đoạn dựa trên kích thước cluster
        val segments = getClusterSegments(clusterSize, colors)

        var startAngle = -90f // Bắt đầu từ trên cùng (-90 độ)

        // Vẽ từng phân đoạn của biểu đồ tròn
        for ((color, percentage) in segments) {
            val paint = Paint().apply {
                this.color = color
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                isAntiAlias = true
            }

            val sweepAngle = 360f * percentage
            canvas.drawArc(
                RectF(
                    center.x - radius,
                    center.y - radius,
                    center.x + radius,
                    center.y + radius
                ),
                startAngle,
                sweepAngle,
                false,
                paint
            )

            startAngle += sweepAngle
        }

        // Thêm hình tròn trung tâm để hiển thị số lượng
        val bgCirclePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Vẽ hình tròn trung tâm
        canvas.drawCircle(center.x, center.y, radius - strokeWidth / 2, bgCirclePaint)

        return bitmap
    }

    /**
     * Tính toán các phân đoạn cho biểu đồ tròn dựa trên kích thước cluster
     * Dựa trên TrackasiaUtils.getClusterSegments
     */
    private fun getClusterSegments(clusterSize: Int, colors: List<Int>): Map<Int, Float> {
        val segments = mutableMapOf<Int, Float>()

        when {
            clusterSize < 10 -> {
                segments[colors[0]] = 1.0f // Một màu duy nhất cho cluster nhỏ
            }
            clusterSize < 50 -> {
                segments[colors[0]] = 0.3f
                segments[colors[1]] = 0.7f
            }
            clusterSize < 100 -> {
                segments[colors[0]] = 0.25f
                segments[colors[1]] = 0.35f
                segments[colors[2]] = 0.4f
            }
            clusterSize < 200 -> {
                segments[colors[0]] = 0.2f
                segments[colors[1]] = 0.3f
                segments[colors[2]] = 0.5f
            }
            clusterSize < 500 -> {
                segments[colors[0]] = 0.35f
                segments[colors[1]] = 0.25f
                segments[colors[2]] = 0.3f
                segments[colors[3]] = 0.1f
            }
            clusterSize < 1000 -> {
                segments[colors[0]] = 0.25f
                segments[colors[1]] = 0.2f
                segments[colors[2]] = 0.25f
                segments[colors[3]] = 0.2f
                segments[colors[4]] = 0.1f
            }
            clusterSize < 2000 -> {
                segments[colors[0]] = 0.35f
                segments[colors[1]] = 0.15f
                segments[colors[2]] = 0.2f
                segments[colors[3]] = 0.25f
                segments[colors[5]] = 0.05f
            }
            clusterSize < 2500 -> {
                segments[colors[0]] = 0.4f
                segments[colors[1]] = 0.15f
                segments[colors[2]] = 0.15f
                segments[colors[3]] = 0.2f
                segments[colors[5]] = 0.1f
            }
            clusterSize < 5000 -> {
                segments[colors[0]] = 0.5f
                segments[colors[1]] = 0.1f
                segments[colors[2]] = 0.15f
                segments[colors[3]] = 0.15f
                segments[colors[5]] = 0.1f
            }
            clusterSize < 10000 -> {
                segments[colors[0]] = 0.5f
                segments[colors[1]] = 0.25f
                segments[colors[2]] = 0.10f
                segments[colors[3]] = 0.10f
                segments[colors[5]] = 0.05f
            }
            else -> {
                // 10000 trở lên
                segments[colors[0]] = 0.5f
                segments[colors[1]] = 0.25f
                segments[colors[2]] = 0.1f
                segments[colors[3]] = 0.1f
                segments[colors[5]] = 0.05f
            }
        }

        return segments
    }

    /**
     * Handle click on pest disease cluster or point
     * Now includes improved cluster information handling
     */
    private fun handlePestDiseaseClick(point: LatLng) {
        try {
            Log.d(TAG, "handlePestDiseaseClick: Processing click at $point")
            val screenPoint = trackasiaMap.projection.toScreenLocation(point)

            // Các layer ID cho cluster có thể click
            val clusterLayerIds = arrayOf(
                "cluster-small", "cluster-medium", "cluster-large",
                "cluster-xlarge", "cluster-xxlarge", "cluster-mega"
            )

            Log.d(TAG, "handlePestDiseaseClick: Querying for cluster features")
            val clusterFeatures = trackasiaMap.queryRenderedFeatures(
                screenPoint,
                *clusterLayerIds // Truyền tất cả các layer ID cho cluster
            )

            Log.d(TAG, "handlePestDiseaseClick: Querying for point features")
            val pointFeatures = trackasiaMap.queryRenderedFeatures(
                screenPoint,
                LAYER_ID_UNCLUSTERED // Tìm kiếm trên layer điểm riêng lẻ
            )

            if (clusterFeatures.isNotEmpty()) {
                // Xử lý click vào cluster
                Log.d(TAG, "handlePestDiseaseClick: Cluster feature found, handling click")
                val feature = clusterFeatures[0]
                val jsonFeature = JSONObject(feature.toJson())
                val geoJsonFeature = Feature.fromJson(jsonFeature.toString())

                // Lấy điểm cluster để camera nhắm vào
                val clusterPoint = getClusterPoint(geoJsonFeature)

                // Lấy kích thước cluster để tính toán mức độ zoom
                val properties = geoJsonFeature.properties()
                val clusterSize = properties?.get("point_count")?.asLong?.toInt() ?: 0

                // Try to get cluster group if available
                val clusterGroup = properties?.get("cluster_group")?.asString

                Log.d(TAG, "handlePestDiseaseClick: Cluster contains $clusterSize points")

                if (clusterPoint != null) {
                    // Tính toán mức độ zoom phù hợp dựa trên kích thước cluster
                    val newZoom = calculateZoomLevel(clusterSize)
                    Log.d(TAG, "handlePestDiseaseClick: Zooming to level $newZoom")

                    // Di chuyển camera đến cluster với mức độ zoom thích hợp
                    val cameraPosition = CameraPosition.Builder()
                        .target(clusterPoint)
                        .zoom(newZoom)
                        .build()

                    trackasiaMap.animateCamera(
                        CameraUpdateFactory.newCameraPosition(cameraPosition),
                        1000
                    )

                    // Hiển thị thông tin tổng quát về cluster với thông tin vùng nếu có
                    context?.let { ctx ->
                        val message = if (clusterGroup != null) {
                            "Nhóm $clusterGroup chứa $clusterSize điểm sâu bệnh. Đang phóng to..."
                        } else {
                            "Nhóm chứa $clusterSize điểm sâu bệnh. Đang phóng to..."
                        }

                        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (pointFeatures.isNotEmpty()) {
                // Xử lý click vào điểm riêng lẻ
                Log.d(TAG, "handlePestDiseaseClick: Individual point found, showing details")
                val feature = pointFeatures[0]
                val jsonFeature = JSONObject(feature.toJson())
                val geoJsonFeature = Feature.fromJson(jsonFeature.toString())

                // Hiển thị chi tiết về điểm được click
                showPestDiseaseDetails(geoJsonFeature)
            } else {
                Log.d(TAG, "handlePestDiseaseClick: No features found at click point")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling pest disease click: ${e.message}", e)
        }
    }

    /**
     * Show details for a specific pest disease point
     * Now includes region information
     */
    private fun showPestDiseaseDetails(feature: Feature) {
        try {
            val properties = feature.properties() ?: return
            val geometry = feature.geometry()

            if (geometry !is Point) return

            // Get pest disease information
            val id = properties.get("id").asLong.toInt()
            val name = properties.get("name").asString ?: "Không xác định"
            val type = properties.get("type").asString ?: "Không xác định"
            val severity = properties.get("severity").asString ?: "Không xác định"

            // Get region information instead of treatment
            val region = properties.get("region")?.asString ?: "Không xác định"

            // Get location
            val coordinates = geometry.coordinates()
            val latLng = LatLng(coordinates[1], coordinates[0])

            // Show dialog with pest disease details
            context?.let { ctx ->
                val alertDialog = androidx.appcompat.app.AlertDialog.Builder(ctx)
                    .setTitle("Chi Tiết Sâu Bệnh")
                    .setIcon(R.drawable.ic_pest)
                    .setMessage(
                        """
                        Tên: $name
                        Loại: $type
                        Mức độ: $severity
                        Khu vực: $region
                        Vị trí: ${String.format("%.5f", latLng.latitude)}, ${
                            String.format(
                                "%.5f",
                                latLng.longitude
                            )
                        }
                        """.trimIndent()
                    )
                    .setPositiveButton("Đóng") { dialog, _ -> dialog.dismiss() }
                    .create()

                alertDialog.show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing pest disease details: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun addLimitedMockPestDiseaseData() {
        // Tạo dữ liệu mẫu với số lượng giới hạn
        val features = createSampleFeatures(150) // Chỉ tạo 150 điểm để tránh quá tải

        // Sử dụng chunking để hiển thị dữ liệu
        addFeaturesToMapInChunks(features)
    }

    // New helper method to clean up resources properly
    private fun cleanupResources() {
        try {
            Log.d(TAG, "Starting resource cleanup")
            
            // Reset UI elements
            progressBar?.visibility = View.GONE
            progressText?.text = ""
            
            // Cancel all background tasks
            executor?.shutdownNow()
            Log.d(TAG, "Canceled background tasks - fragment is being destroyed")
            
            // Close and clear network client
            try {
                if (client != null) {
                    client?.dispatcher?.cancelAll()
                    client?.connectionPool?.evictAll()
                    client = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error closing network client: ${e.message}", e)
            }
            
            // Clear data processing flags
            isDataLoading = false
            isDataProcessed = false
            
            // Clear mode flags
            currentMapMode = MapMode.NONE
            
            // Force garbage collection
            System.gc()
            
            Log.d(TAG, "Resource cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during resource cleanup: ${e.message}", e)
        }
    }

    // Improved map resources cleanup
    private fun cleanupMapResources() {
        try {
            // Only clean up if map is initialized
            if (::trackasiaMap.isInitialized) {
                trackasiaMap.style?.let { style ->
                    Log.d(TAG, "cleanupMapResources: Removing all added layers and sources")

                    // Remove all added layers
                    synchronized(activeLayers) {
                        activeLayers.forEach { layerId ->
                            try {
                                if (style.getLayer(layerId) != null) {
                                    style.removeLayer(layerId)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error removing layer $layerId: ${e.message}")
                            }
                        }
                        activeLayers.clear()
                    }

                    // Remove all added sources
                    synchronized(activeSources) {
                        activeSources.forEach { sourceId ->
                            try {
                                if (style.getSource(sourceId) != null) {
                                    style.removeSource(sourceId)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error removing source $sourceId: ${e.message}")
                            }
                        }
                        activeSources.clear()
                    }

                    // Clear references to map objects
                    layer = null
                    source = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up map resources: ${e.message}", e)
        }
    }

    // Method to cancel all background tasks
    private fun cancelAllBackgroundTasks(reason: String) {
        Log.d(TAG, "Canceling all background tasks: $reason")
        isDataProcessingActive.set(false)

        synchronized(activeBackgroundTasks) {
            activeBackgroundTasks.forEach { thread ->
                try {
                    if (thread.isAlive) {
                        thread.interrupt()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error interrupting thread: ${e.message}")
                }
            }
            activeBackgroundTasks.clear()
        }
    }

    // Add this method to track added layers
    private fun trackAddedLayer(layerId: String) {
        synchronized(activeLayers) {
            activeLayers.add(layerId)
        }
    }

    // Add this method to track added sources
    private fun trackAddedSource(sourceId: String) {
        synchronized(activeSources) {
            activeSources.add(sourceId)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Cleaning up resources")

        // Giải phóng tài nguyên khi destroy fragment
        try {
            cleanupMapResources()
            cleanupResources()
            lifecycle.removeObserver(this)
            Log.d(TAG, "onDestroy: Fragment fully destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}", e)
        }

        super<Fragment>.onDestroy()
    }

    // Fix the lifecycle methods by specifying which supertype to call
    override fun onStop(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onStop(owner)
        cancelAllBackgroundTasks("onStop called")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onDestroy(owner)
        cleanupResources()
    }

    /**
     * Fetch bus cluster data from API
     */
    private fun fetchBusClusterData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.text = "Đang tải dữ liệu điểm dừng..."
        
        val client = networkClient ?: OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val request = Request.Builder()
            .url(URL_DATA_MAP)
            .build()
        
        val requestThread = Thread {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to fetch bus data: ${response.code}")
                        activity?.runOnUiThread {
                            addClusteredSource() // Sử dụng phương pháp trực tiếp thay vì cache
                        }
                        return@use
                    }
                    
                    val responseBody = response.body?.string()
                    if (responseBody == null) {
                        Log.e(TAG, "Empty response body")
                        activity?.runOnUiThread {
                            addClusteredSource() // Sử dụng phương pháp trực tiếp thay vì cache
                        }
                        return@use
                    }
                    
                    try {
                        // Parse JSON và xử lý dữ liệu
                        val jsonObject = JSONObject(responseBody)
                        val jsonData = jsonToMap(jsonObject)
                        
                        // Lấy danh sách các điểm
                        val features = (jsonData["features"] as? List<*>) ?: listOf<Any>()
                        Log.d(TAG, "Total bus stop features: ${features.size}")
                        
                        // Xử lý dữ liệu trên UI thread
                        if (features.isNotEmpty() && isAdded) {
                            activity?.runOnUiThread {
                                if (!isAdded) return@runOnUiThread
                                
                                try {
                                    val geoJsonFeatures = ArrayList<Feature>()
                                    // Chỉ lấy tối đa 200 điểm để tránh quá tải
                                    val featureCount = Math.min(200, features.size)
                                    
                                    for (i in 0 until featureCount) {
                                        try {
                                            val item = features[i] as? Map<*, *> ?: continue
                                            val geometry = item["geometry"] as? Map<*, *> ?: continue
                                            val type = geometry["type"] as? String ?: continue
                                            val coordinates = geometry["coordinates"] as? List<*> ?: continue
                                            
                                            if (type == "Point" && coordinates.size >= 2) {
                                                val lon = coordinates[0] as? Number ?: continue
                                                val lat = coordinates[1] as? Number ?: continue
                                                val point = Point.fromLngLat(lon.toDouble(), lat.toDouble())
                                                
                                                // Tạo properties
                                                val jsonProps = JsonObject()
                                                val properties = item["properties"] as? Map<*, *>
                                                if (properties != null) {
                                                    for ((key, value) in properties) {
                                                        if (key != null && value != null) {
                                                            jsonProps.addProperty(key.toString(), value.toString())
                                                        }
                                                    }
                                                }
                                                
                                                // Tạo feature
                                                val feature = Feature.fromGeometry(point, jsonProps)
                                                geoJsonFeatures.add(feature)
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error processing feature $i: ${e.message}")
                                        }
                                    }
                                    
                                    // Thêm vào bản đồ
                                    trackasiaMap.style?.let { style ->
                                        // Xóa các layer và source cũ
                                        if (style.getSource(SOURCE_ID_CLUSTER) != null) {
                                            if (style.getLayer("unclustered-points") != null) 
                                                style.removeLayer("unclustered-points")
                                            if (style.getLayer("cluster-0") != null) 
                                                style.removeLayer("cluster-0")
                                            if (style.getLayer("count") != null) 
                                                style.removeLayer("count")
                                            style.removeSource(SOURCE_ID_CLUSTER)
                                        }
                                        
                                        // Tạo FeatureCollection
                                        val featureCollection = FeatureCollection.fromFeatures(geoJsonFeatures.toTypedArray())
                                        
                                        // Tạo source
                                        val source = GeoJsonSource(
                                            SOURCE_ID_CLUSTER,
                                            featureCollection,
                                            GeoJsonOptions()
                                                .withCluster(true)
                                                .withClusterMaxZoom(14)
                                                .withClusterRadius(50)
                                        )
                                        
                                        style.addSource(source)
                                        addClusterLayers(style)
                                        
                                        // Thông báo hoàn thành
                                        binding.progressBar.visibility = View.GONE
                                        binding.progressText.text = ""
                                        
                                        context?.let { ctx ->
                                            Toast.makeText(
                                                ctx,
                                                "Đã tải ${geoJsonFeatures.size} điểm dừng xe buýt",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        
                                        // Đặt chế độ bản đồ hiện tại
                                        currentMapMode = MapMode.BUS_CLUSTER
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error adding features to map: ${e.message}")
                                    addClusteredSource() // Fallback nếu xử lý lỗi
                                } finally {
                                    binding.progressBar.visibility = View.GONE
                                    binding.progressText.text = ""
                                }
                            }
                        } else {
                            activity?.runOnUiThread {
                                addClusteredSource() // Không có dữ liệu, sử dụng phương pháp trực tiếp
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing bus stop data: ${e.message}")
                        activity?.runOnUiThread {
                            addClusteredSource() // Phân tích lỗi, sử dụng phương pháp trực tiếp
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error fetching bus data: ${e.message}")
                activity?.runOnUiThread {
                    addClusteredSource() // Lỗi mạng, sử dụng phương pháp trực tiếp
                }
            } finally {
                activity?.runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.progressText.text = ""
                }
                
                synchronized(activeBackgroundTasks) {
                    activeBackgroundTasks.remove(Thread.currentThread())
                }
            }
        }
        
        synchronized(activeBackgroundTasks) {
            activeBackgroundTasks.add(requestThread)
        }
        requestThread.start()
    }
    
    /**
     * Add improved pest cluster layers with better visualization
     */
    private fun addImprovedPetClusterLayers(style: Style) {
        try {
            Log.d(TAG, "addImprovedPetClusterLayers: Adding pest cluster layers")
            
            // Make sure we have a source first
            val source = style.getSourceAs<GeoJsonSource>(SOURCE_ID_PET_MAP)
            if (source == null) {
                Log.e(TAG, "Source $SOURCE_ID_PET_MAP doesn't exist, creating empty source")
                val newSource = GeoJsonSource(
                    SOURCE_ID_PET_MAP,
                    FeatureCollection.fromFeatures(arrayOf()),
                    GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(14)
                        .withClusterRadius(50)
                        .withClusterProperty(
                            "point_count",
                            Expression.literal("point_count"),
                            Expression.get("point_count")
                        )
                )
                style.addSource(newSource)
                Log.d(TAG, "Created GeoJsonSource with clustering enabled")
                trackAddedSource(SOURCE_ID_PET_MAP)
            }
            
            // Remove old layers if they exist
            val layersToRemove = listOf(
                "unclustered-pest-points", 
                "clustered-pest-points", 
                "pest-cluster-count",
                LAYER_ID_UNCLUSTERED,
                LAYER_ID_CLUSTERS,
                LAYER_ID_CLUSTER_COUNT
            )
            
            for (layerId in layersToRemove) {
                if (style.getLayer(layerId) != null) {
                    style.removeLayer(layerId)
                    Log.d(TAG, "Removed existing layer: $layerId")
                }
            }
            
            // Uncluttered point layer
            val unclustered = CircleLayer("unclustered-pest-points", SOURCE_ID_PET_MAP)
            unclustered.setProperties(
                PropertyFactory.circleColor(Color.parseColor("#00FF99")),
                PropertyFactory.circleRadius(8f),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor(Color.WHITE)
            )
            unclustered.setFilter(Expression.not(Expression.has("point_count")))
            style.addLayer(unclustered)
            trackAddedLayer("unclustered-pest-points")
            
            // Cluster circles
            val clusteredPoints = CircleLayer("clustered-pest-points", SOURCE_ID_PET_MAP)
            clusteredPoints.setProperties(
                PropertyFactory.circleColor(Color.parseColor("#FF9800")),
                PropertyFactory.circleRadius(
                    Expression.step(
                        Expression.get("point_count"),
                        15f,
                        Expression.stop(5, 20f),
                        Expression.stop(10, 25f),
                        Expression.stop(50, 30f),
                        Expression.stop(100, 40f)
                    )
                ),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleOpacity(0.9f)
            )
            
            // Important: Make sure filter is correctly checking for point_count
            clusteredPoints.setFilter(
                Expression.has("point_count")
            )
            style.addLayer(clusteredPoints)
            trackAddedLayer("clustered-pest-points")
            
            // Cluster count labels
            val count = SymbolLayer("pest-cluster-count", SOURCE_ID_PET_MAP)
            count.setProperties(
                PropertyFactory.textField(Expression.toString(Expression.get("point_count"))),
                PropertyFactory.textFont(arrayOf("DIN Offc Pro Medium", "Arial Unicode MS Bold")),
                PropertyFactory.textSize(14f),
                PropertyFactory.textColor(Color.WHITE),
                PropertyFactory.textIgnorePlacement(true),
                PropertyFactory.textAllowOverlap(true)
            )
            // Important: Make sure filter is correctly checking for point_count
            count.setFilter(
                Expression.has("point_count")
            )
            style.addLayer(count)
            trackAddedLayer("pest-cluster-count")
            
            Log.d(TAG, "Successfully added improved pest disease cluster layers")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding pest cluster layers: ${e.message}", e)
        }
    }

    /**
     * Simplified method for adding pet cluster layers, redirects to improved version
     */
    private fun addSimplifiedPetClusterLayers(style: Style) {
        Log.d(TAG, "Using improved pet cluster layers instead of simplified version")
        addImprovedPetClusterLayers(style)
    }

    /**
     * Shows my location button with a fade-in animation
     */
    private fun showMyLocationButtonWithAnimation() {
        try {
            if (myLocationFab != null) {
                myLocationFab.alpha = 0f
                myLocationFab.visibility = View.VISIBLE
                myLocationFab.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setListener(null)
                Log.d(TAG, "showMyLocationButtonWithAnimation: Button animated into view")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing location button: ${e.message}", e)
        }
    }

    /**
     * Moves map camera to current user location
     */
    private fun moveToCurrentLocation() {
        try {
            if (::trackasiaMap.isInitialized) {
                val locationComponent = trackasiaMap.locationComponent
                if (locationComponent.isLocationComponentActivated) {
                    val lastKnownLocation = locationComponent.lastKnownLocation
                    if (lastKnownLocation != null) {
                        val position = CameraPosition.Builder()
                            .target(LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude))
                            .zoom(15.0)
                            .build()
                        
                        trackasiaMap.animateCamera(
                            CameraUpdateFactory.newCameraPosition(position),
                            1000
                        )
                        Log.d(TAG, "moveToCurrentLocation: Camera moved to current location")
                    } else {
                        // Fallback to default location if no last known location
                        latLngLocation?.let { location ->
                            trackasiaMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(location, 15.0),
                                1000
                            )
                        }
                        Log.d(TAG, "moveToCurrentLocation: No location available, using default")
                    }
                } else {
                    Log.d(TAG, "moveToCurrentLocation: Location component not activated")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error moving to current location: ${e.message}", e)
        }
    }

    /**
     * Safely removes a source and all associated layers from the map style
     */
    private fun removeLayerAndSource(style: Style, sourceId: String) {
        try {
            Log.d(TAG, "removeLayerAndSource: Removing layers and source for $sourceId")
            
            // Find all layers that use this source
            val layersToRemove = mutableListOf<String>()
            
            // Check for known layer patterns
            if (sourceId == SOURCE_ID_PET_MAP) {
                if (style.getLayer(LAYER_ID_UNCLUSTERED) != null) {
                    layersToRemove.add(LAYER_ID_UNCLUSTERED)
                }
                if (style.getLayer(LAYER_ID_CLUSTERS) != null) {
                    layersToRemove.add(LAYER_ID_CLUSTERS)
                }
                if (style.getLayer(LAYER_ID_CLUSTER_COUNT) != null) {
                    layersToRemove.add(LAYER_ID_CLUSTER_COUNT)
                }
                if (style.getLayer("unclustered-pest-points") != null) {
                    layersToRemove.add("unclustered-pest-points")
                }
                if (style.getLayer("clustered-pest-points") != null) {
                    layersToRemove.add("clustered-pest-points")
                }
                if (style.getLayer("pest-cluster-count") != null) {
                    layersToRemove.add("pest-cluster-count")
                }
            }
            
            // Remove all identified layers
            for (layerId in layersToRemove) {
                try {
                    style.removeLayer(layerId)
                    Log.d(TAG, "removeLayerAndSource: Removed layer $layerId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to remove layer $layerId: ${e.message}")
                }
            }
            
            // Remove the source
            if (style.getSource(sourceId) != null) {
                try {
                    style.removeSource(sourceId)
                    Log.d(TAG, "removeLayerAndSource: Removed source $sourceId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to remove source $sourceId: ${e.message}")
                }
            }
            
            // Update tracking collections
            synchronized(activeLayers) {
                activeLayers.removeAll(layersToRemove)
            }
            synchronized(activeSources) {
                activeSources.remove(sourceId)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in removeLayerAndSource: ${e.message}", e)
        }
    }

    /**
     * Extract the geographic point from a cluster feature
     */
    private fun getClusterPoint(feature: Feature): LatLng? {
        try {
            val geometry = feature.geometry()
            if (geometry is Point) {
                val coordinates = geometry.coordinates()
                return LatLng(coordinates[1], coordinates[0])
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cluster point: ${e.message}", e)
        }
        return null
    }
    
    /**
     * Calculate appropriate zoom level based on cluster size
     */
    private fun calculateZoomLevel(clusterSize: Int): Double {
        return when {
            clusterSize > 100 -> 11.0
            clusterSize > 50 -> 12.0
            clusterSize > 20 -> 13.0
            clusterSize > 10 -> 14.0
            else -> 15.0
        }
    }

    /**
     * Xử lý dữ liệu theo chunks và thêm vào GeoJSON
     * @param features Danh sách features cần xử lý
     */
    private fun addFeaturesToMapInChunks(features: List<Feature>) {
        try {
            Log.d(TAG, "Bắt đầu xử lý ${features.size} điểm dữ liệu theo chunks")
            
            binding.progressBar.visibility = View.VISIBLE
            binding.progressText.text = "Đang chuẩn bị dữ liệu..."
            isDataProcessingActive.set(true)
            
            trackasiaMap.style?.let { style ->
                // Xóa source và layer cũ nếu có
                if (style.getSource(SOURCE_ID_PET_MAP) != null) {
                    removeLayerAndSource(style, SOURCE_ID_PET_MAP)
                }
                
                // Tạo source trống với cấu hình cluster
                val source = GeoJsonSource(
                    SOURCE_ID_PET_MAP,
                    FeatureCollection.fromFeatures(arrayOf()),
                    GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(14)
                        .withClusterRadius(50)
                        .withClusterProperty(
                            "point_count",
                            Expression.literal("point_count"),
                            Expression.get("point_count")
                        )
                )
                style.addSource(source)
                Log.d(TAG, "Created GeoJsonSource with clustering enabled")
                trackAddedSource(SOURCE_ID_PET_MAP)
                
                // Thêm layer hiển thị
                addImprovedPetClusterLayers(style)
                
                // Nếu danh sách features trống, hiển thị thông báo và thoát
                if (features.isEmpty()) {
                    binding.progressBar.visibility = View.GONE
                    binding.progressText.text = ""
                    Toast.makeText(context, "Không có dữ liệu để hiển thị", Toast.LENGTH_SHORT).show()
                    isDataProcessingActive.set(false)
                    return@let
                }
                
                // Chia dữ liệu thành 3 phần để xử lý
                val chunkCount = 3
                val chunkSize = features.size / chunkCount
                val dataThread = Thread {
                    try {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                        
                        for (chunkIndex in 0 until chunkCount) {
                            if (!isDataProcessingActive.get() || !isAdded || Thread.currentThread().isInterrupted) {
                                Log.d(TAG, "Quá trình xử lý dữ liệu bị hủy")
                                break
                            }
                            
                            // Tính toán phạm vi chunk hiện tại
                            val startIndex = chunkIndex * chunkSize
                            val endIndex = if (chunkIndex == chunkCount - 1) features.size else (chunkIndex + 1) * chunkSize
                            val chunk = features.subList(startIndex, endIndex)
                            
                            // Cập nhật UI hiển thị tiến trình
                            val progress = ((chunkIndex + 1) * 100 / chunkCount)
                            activity?.runOnUiThread {
                                if (!isAdded) return@runOnUiThread
                                binding.progressText.text = "Đang xử lý dữ liệu (${progress}%)..."
                                binding.progressBar.progress = progress
                            }
                            
                            // Lấy dữ liệu hiện có từ chunks trước (nếu có)
                            val currentFeatures = ArrayList<Feature>()
                            if (chunkIndex > 0) {
                                try {
                                    val existingSource = style.getSource(SOURCE_ID_PET_MAP) as? GeoJsonSource
                                    val existingCollection = existingSource?.featureCollection
                                    if (existingCollection != null) {
                                        existingCollection.features()?.let { currentFeatures.addAll(it) }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error getting existing features: ${e.message}")
                                }
                            }
                            
                            // Thêm chunk mới vào danh sách hiện tại
                            currentFeatures.addAll(chunk)
                            
                            // Cập nhật source trên UI thread
                            activity?.runOnUiThread {
                                if (!isAdded) return@runOnUiThread
                                try {
                                    val featureCollection = FeatureCollection.fromFeatures(currentFeatures.toTypedArray())
                                    val source = style.getSource(SOURCE_ID_PET_MAP) as? GeoJsonSource
                                    if (source != null) {
                                        // Force update by creating a new collection
                                        source.setGeoJson(featureCollection)
                                        Log.d(TAG, "Đã thêm chunk ${chunkIndex + 1}/$chunkCount (${chunk.size} điểm, tổng: ${currentFeatures.size})")
                                    } else {
                                        Log.e(TAG, "Source is null when trying to update GeoJSON")
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error updating source: ${e.message}")
                                }
                            }
                            
                            // Tạm dừng để không overload UI thread
                            Thread.sleep(200)
                        }
                        
                        // Cập nhật UI khi hoàn thành
                        activity?.runOnUiThread {
                            if (!isAdded) return@runOnUiThread
                            
                            try {
                                // Ensure one final update with the complete dataset
                                if (currentMapMode == MapMode.PEST_DISEASE) {
                                    // Get the current features from the source
                                    val source = style.getSource(SOURCE_ID_PET_MAP) as? GeoJsonSource
                                    if (source != null) {
                                        // Force a final update of all features
                                        val finalFeatureCollection = FeatureCollection.fromFeatures(features.toTypedArray())
                                        source.setGeoJson(finalFeatureCollection)
                                        Log.d(TAG, "Final update: Set all ${features.size} features to source")
                                        
                                        // Đánh dấu dữ liệu đã được tải
                                        isPestDiseaseLoaded = true
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in final update: ${e.message}")
                            }
                            
                            // Di chuyển camera để xem dữ liệu
                            moveToSampleLocation()
                            
                            // Ẩn thanh tiến trình
                            binding.progressBar.visibility = View.GONE
                            binding.progressText.text = ""
                            
                            // Thông báo hoàn thành
                            context?.let { ctx ->
                                Toast.makeText(
                                    ctx,
                                    "Đã tải ${features.size} điểm dữ liệu",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi xử lý dữ liệu theo chunks: ${e.message}", e)
                        activity?.runOnUiThread {
                            if (!isAdded) return@runOnUiThread
                            binding.progressBar.visibility = View.GONE
                            binding.progressText.text = ""
                            
                            context?.let { ctx ->
                                Toast.makeText(
                                    ctx,
                                    "Lỗi khi tải dữ liệu: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } finally {
                        isDataProcessingActive.set(false)
                        synchronized(activeBackgroundTasks) {
                            activeBackgroundTasks.remove(Thread.currentThread())
                        }
                    }
                }
                
                // Đăng ký và bắt đầu thread xử lý
                synchronized(activeBackgroundTasks) {
                    activeBackgroundTasks.add(dataThread)
                }
                dataThread.start()
                Log.d(TAG, "Đã bắt đầu thread xử lý dữ liệu")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi chuẩn bị hiển thị dữ liệu: ${e.message}", e)
            binding.progressBar.visibility = View.GONE
            binding.progressText.text = ""
            isDataProcessingActive.set(false)
            
            context?.let { ctx ->
                Toast.makeText(
                    ctx,
                    "Không thể hiển thị dữ liệu: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Chuyển đổi dữ liệu từ Map thành Feature
     */
    private fun convertMapToFeatures(features: List<Map<String, Any>>): List<Feature> {
        val result = ArrayList<Feature>(features.size)
        
        for (feature in features) {
            try {
                if (feature.containsKey("type") && feature.containsKey("geometry")) {
                    val jsonFeature = JsonObject()
                    jsonFeature.addProperty("type", "Feature")
                    
                    val geometry = feature["geometry"] as? Map<*, *>
                    if (geometry != null && geometry.containsKey("type") && geometry.containsKey("coordinates")) {
                        val jsonGeometry = JsonObject()
                        jsonGeometry.addProperty("type", geometry["type"].toString())
                        val coordinates = geometry["coordinates"] as? List<*>
                        if (coordinates != null && coordinates.isNotEmpty()) {
                            val jsonCoordinates = JsonArray()
                            for (coord in coordinates) {
                                when (coord) {
                                    is Number -> jsonCoordinates.add(coord.toDouble())
                                    else -> jsonCoordinates.add(coord.toString())
                                }
                            }
                            jsonGeometry.add("coordinates", jsonCoordinates)
                            jsonFeature.add("geometry", jsonGeometry)
                            
                            val properties = feature["properties"] as? Map<*, *>
                            if (properties != null) {
                                val jsonProperties = JsonObject()
                                for ((key, value) in properties) {
                                    if (key != null && value != null) {
                                        when (value) {
                                            is String -> jsonProperties.addProperty(key.toString(), value)
                                            is Number -> {
                                                when (value) {
                                                    is Int -> jsonProperties.addProperty(key.toString(), value)
                                                    is Long -> jsonProperties.addProperty(key.toString(), value)
                                                    is Float -> jsonProperties.addProperty(key.toString(), value)
                                                    is Double -> jsonProperties.addProperty(key.toString(), value)
                                                    else -> jsonProperties.addProperty(key.toString(), value.toString())
                                                }
                                            }
                                            is Boolean -> jsonProperties.addProperty(key.toString(), value)
                                            else -> jsonProperties.addProperty(key.toString(), value.toString())
                                        }
                                    }
                                }
                                jsonFeature.add("properties", jsonProperties)
                            } else {
                                jsonFeature.add("properties", JsonObject())
                            }
                            
                            try {
                                val jsonString = jsonFeature.toString()
                                val featureObj = Feature.fromJson(jsonString)
                                if (featureObj.geometry() != null) {
                                    result.add(featureObj)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error converting JSON to Feature: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing feature map to Feature: ${e.message}")
            }
        }
        
        return result
    }

    /**
     * Add mock pest disease data to map using chunking technique for efficient processing
     */
    private fun addPetMapFromMockData() {
        try {
            Log.d(TAG, "Creating cluster data with chunking method")
            
            binding.progressBar.visibility = View.VISIBLE
            binding.progressText.text = "Đang tạo dữ liệu điểm..."
            
            val baseLocation = latLngLocation ?: LatLng(10.728073, 106.624054)
            val totalPoints = 300
            val pointsPerRegion = totalPoints / 3
            val totalFeatures = ArrayList<Feature>(totalPoints)
            
            // List of regions with position offsets
            val regions = listOf(
                Pair("Miền Bắc", Pair(0.03, 0.02)),  // Offset for region 1
                Pair("Miền Trung", Pair(-0.02, 0.01)), // Offset for region 2
                Pair("Miền Nam", Pair(-0.01, -0.03)) // Offset for region 3
            )
            
            val pestNames = listOf(
                "Rầy nâu", "Rệp xanh", "Sâu ăn lá", "Bệnh vàng lá", 
                "Đạo ôn", "Bọ trĩ", "Bệnh đốm nâu", "Sâu đục thân",
                "Bệnh thối rễ", "Bệnh bạc lá"
            )
            val severityLevels = listOf("Nhẹ", "Trung bình", "Nghiêm trọng")
            
            // Create data for each region
            for ((regionIndex, region) in regions.withIndex()) {
                val (regionName, offsetPair) = region
                val (latOffset, lngOffset) = offsetPair
                
                for (i in 0 until pointsPerRegion) {
                    // Create random offset from base position of region
                    val randomLatOffset = latOffset + (Math.random() - 0.5) * 0.02
                    val randomLngOffset = lngOffset + (Math.random() - 0.5) * 0.02
                    
                    val pointLocation = LatLng(
                        baseLocation.latitude + randomLatOffset,
                        baseLocation.longitude + randomLngOffset
                    )
                    
                    val point = Point.fromLngLat(pointLocation.longitude, pointLocation.latitude)
                    val id = regionIndex * pointsPerRegion + i
                    
                    // Create simple properties
                    val jsonProps = JsonObject()
                    jsonProps.addProperty("id", id.toString())
                    jsonProps.addProperty("name", pestNames[i % pestNames.size])
                    jsonProps.addProperty("region", regionName)
                    jsonProps.addProperty("severity", severityLevels[i % severityLevels.size])
                    jsonProps.addProperty("color", when(regionIndex) {
                        0 -> "#FF0000" // Red
                        1 -> "#00FF00" // Green
                        else -> "#0000FF" // Blue
                    })
                    jsonProps.addProperty("cluster_group", regionIndex.toString())
                    
                    val feature = Feature.fromGeometry(point, jsonProps)
                    totalFeatures.add(feature)
                }
            }
            
            // Add data to map in chunks
            Log.d(TAG, "Created ${totalFeatures.size} points for 3 regions")
            
            trackasiaMap.style?.let { style ->
                // Remove old source and layers if they exist
                if (style.getSource(SOURCE_ID_PET_MAP) != null) {
                    removeLayerAndSource(style, SOURCE_ID_PET_MAP)
                }
                
                // Create empty source with cluster configuration
                val source = GeoJsonSource(
                    SOURCE_ID_PET_MAP,
                    FeatureCollection.fromFeatures(arrayOf()),
                    GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(14)
                        .withClusterRadius(50)
                        .withClusterProperty(
                            "point_count",
                            Expression.literal("point_count"),
                            Expression.get("point_count")
                        )
                )
                style.addSource(source)
                Log.d(TAG, "Created GeoJsonSource with clustering enabled")
                trackAddedSource(SOURCE_ID_PET_MAP)
                
                // Add display layers
                addImprovedPetClusterLayers(style)
                
                // Split data into 3 parts for processing
                val chunkSize = totalFeatures.size / 3
                
                // Create processing thread to not block UI
                val dataThread = Thread {
                    try {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                        
                        for (chunkIndex in 0..2) {
                            if (!isDataProcessingActive.get()) {
                                Log.d(TAG, "Data processing cancelled")
                                break
                            }
                            
                            // Calculate current chunk range
                            val startIndex = chunkIndex * chunkSize
                            val endIndex = if (chunkIndex == 2) totalFeatures.size else (chunkIndex + 1) * chunkSize
                            val chunk = totalFeatures.subList(startIndex, endIndex)
                            
                            // Update UI to show progress
                            val progress = ((chunkIndex + 1) * 100 / 3)
                            activity?.runOnUiThread {
                                if (!isAdded) return@runOnUiThread
                                binding.progressText.text = "Đang xử lý dữ liệu (${progress}%)..."
                                binding.progressBar.progress = progress
                            }
                            
                            // Pause to let user see progress
                            Thread.sleep(300)
                            
                            // Get existing data from previous chunks (if any)
                            val currentFeatures = ArrayList<Feature>()
                            if (chunkIndex > 0) {
                                val existingSource = style.getSource(SOURCE_ID_PET_MAP) as? GeoJsonSource
                                val existingCollection = existingSource?.featureCollection
                                if (existingCollection != null) {
                                    existingCollection.features()?.let { currentFeatures.addAll(it) }
                                }
                            }
                            
                            // Add new chunk to current list
                            currentFeatures.addAll(chunk)
                            
                            // Update source on UI thread
                            activity?.runOnUiThread {
                                if (!isAdded) return@runOnUiThread
                                
                                val featureCollection = FeatureCollection.fromFeatures(currentFeatures.toTypedArray())
                                val source = style.getSource(SOURCE_ID_PET_MAP) as? GeoJsonSource
                                source?.setGeoJson(featureCollection)
                                
                                Log.d(TAG, "Added chunk ${chunkIndex + 1}/3 (${chunk.size} points)")
                            }
                        }
                        
                        // Update UI when complete
                        activity?.runOnUiThread {
                            if (!isAdded) return@runOnUiThread
                            
                            // Mark data as loaded
                            isPestDiseaseLoaded = true
                            
                            // Move camera to view data
                            moveToSampleLocation()
                            
                            // Hide progress bar
                            binding.progressBar.visibility = View.GONE
                            binding.progressText.text = ""
                            
                            // Notify completion
                            context?.let { ctx ->
                                Toast.makeText(
                                    ctx,
                                    "Đã tải ${totalFeatures.size} điểm dữ liệu theo phương pháp chunk",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing data in chunks: ${e.message}", e)
                        activity?.runOnUiThread {
                            if (!isAdded) return@runOnUiThread
                            binding.progressBar.visibility = View.GONE
                            binding.progressText.text = ""
                            
                            context?.let { ctx ->
                                Toast.makeText(
                                    ctx,
                                    "Lỗi khi tải dữ liệu: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } finally {
                        synchronized(activeBackgroundTasks) {
                            activeBackgroundTasks.remove(Thread.currentThread())
                        }
                    }
                }
                
                // Register and start processing thread
                synchronized(activeBackgroundTasks) {
                    activeBackgroundTasks.add(dataThread)
                }
                isDataProcessingActive.set(true)
                dataThread.start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating data points: ${e.message}", e)
            binding.progressBar.visibility = View.GONE
            binding.progressText.text = ""
            
            context?.let { ctx ->
                Toast.makeText(
                    ctx,
                    "Không thể tạo dữ liệu điểm: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Shows details of a pest disease in a dialog
     */
    private fun displayPestDiseaseDetails(feature: Feature?) {
        try {
            feature?.properties()?.let { properties ->
                val name = properties.get("name")?.asString ?: "Unknown Pest"
                val region = properties.get("region")?.asString ?: "Unknown Region"
                val severity = properties.get("severity")?.asString ?: "Unknown Severity"
                
                val alertDialog = AlertDialog.Builder(requireContext())
                    .setTitle("Thông tin sâu bệnh")
                    .setMessage(
                        """
                        Tên: $name
                        Khu vực: $region
                        Mức độ: $severity
                        """.trimIndent()
                    )
                    .setPositiveButton("Đóng") { dialog, _ -> dialog.dismiss() }
                    .create()

                alertDialog.show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing pest disease details: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Handle click on feature based on current map mode
     */
    private fun handleFeatureClick(feature: Feature) {
        try {
            val properties = feature.properties()
            
            when {
                properties?.has("cluster") == true && properties.get("cluster").asBoolean -> {
                    // Handle cluster click
                    val pointCount = properties.get("point_count")?.asInt ?: 0
                    Toast.makeText(
                        context,
                        "Nhóm chứa $pointCount điểm. Đang phóng to...",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Zoom to cluster
                    val clusterPoint = getClusterPoint(feature)
                    if (clusterPoint != null) {
                        val newZoom = calculateZoomLevel(pointCount)
                        val cameraPosition = CameraPosition.Builder()
                            .target(clusterPoint)
                            .zoom(newZoom)
                            .build()
                        
                        trackasiaMap.animateCamera(
                            CameraUpdateFactory.newCameraPosition(cameraPosition),
                            1000
                        )
                    }
                }
                
                currentMapMode == MapMode.PEST_DISEASE -> {
                    // Handle single pest disease point click
                    val name = properties?.get("name")?.asString ?: "Unknown"
                    val region = properties?.get("region")?.asString ?: "Unknown"
                    val severity = properties?.get("severity")?.asInt ?: 0
                    
                    // Get severity string
                    val severityStr = when {
                        severity > 7 -> "Cao"
                        severity > 4 -> "Trung bình"
                        else -> "Thấp"
                    }
                    
                    // Show alert dialog
                    context?.let { ctx ->
                        val alertDialog = AlertDialog.Builder(ctx)
                            .setTitle("Chi tiết sâu bệnh")
                            .setMessage("Tên: $name\nKhu vực: $region\nMức độ: $severityStr")
                            .setPositiveButton("Đóng") { dialog, _ -> dialog.dismiss() }
                            .create()
                        alertDialog.show()
                    }
                }
                
                else -> {
                    // Handle other feature types
                    val message = StringBuilder()
                    
                    properties?.entrySet()?.forEach { entry ->
                        val key = entry.key
                        val value = entry.value
                        if (key != "id" && value != null) {
                            message.append("$key: ${value.asString}\n")
                        }
                    }
                    
                    context?.let { ctx ->
                        val alertDialog = AlertDialog.Builder(ctx)
                            .setTitle("Thông tin điểm")
                            .setMessage(message.toString())
                            .setPositiveButton("Đóng") { dialog, _ -> dialog.dismiss() }
                            .create()
                        alertDialog.show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling feature click: ${e.message}", e)
        }
    }
}