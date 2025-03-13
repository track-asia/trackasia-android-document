package com.trackasia.sample

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.gson.JsonObject
import com.mapbox.core.constants.Constants.PRECISION_6
import com.trackasia.android.TrackAsia
import com.trackasia.android.camera.CameraPosition
import com.trackasia.android.camera.CameraUpdateFactory
import com.trackasia.android.geometry.LatLng
import com.trackasia.android.geometry.LatLngBounds
import com.trackasia.android.location.permissions.PermissionsListener
import com.trackasia.android.location.permissions.PermissionsManager
import com.trackasia.android.maps.Style
import com.trackasia.android.maps.TrackAsiaMap
import com.trackasia.android.style.expressions.Expression
import com.trackasia.android.style.layers.LineLayer
import com.trackasia.android.style.layers.PropertyFactory
import com.trackasia.android.style.layers.SymbolLayer
import com.trackasia.android.style.sources.GeoJsonSource
import com.trackasia.geojson.Feature
import com.trackasia.geojson.FeatureCollection
import com.trackasia.geojson.LineString
import com.trackasia.geojson.Point
import com.trackasia.navigation.android.navigation.ui.v5.route.NavigationRoute
import com.trackasia.navigation.android.navigation.v5.models.DirectionsCriteria
import com.trackasia.navigation.android.navigation.v5.models.DirectionsResponse
import com.trackasia.sample.databinding.FragmentMapAnimationBinding
import com.trackasia.sample.utils.MapUtils
import com.trackasia.turf.TurfMeasurement
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MapAnimationFragment : Fragment(), PermissionsListener {

    private var _binding: FragmentMapAnimationBinding? = null
    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private lateinit var mapboxMap: TrackAsiaMap
    private val PROPERTY_BEARING = "bearing"
    private val randomCars: MutableList<Car> = ArrayList()
    private var style: Style? = null
    private var randomCarSource: GeoJsonSource? = null
    private var taxi: Car? = null
    private var taxiSource: GeoJsonSource? = null
    private var passenger: LatLng? = null
    private val animators: MutableList<Animator> = ArrayList()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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
        _binding = FragmentMapAnimationBinding.inflate(inflater, container, false)
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
        binding.mapView.getMapAsync { map ->
            try {
                Log.d("DOMAIN URL STYLE:", styleUrl)
                map.setStyle(
                    Style.Builder().fromUri(styleUrl)
                ) { style ->
                    this.style = style
                    if (activity != null) {
                        MapUtils(requireActivity()).enableLocationComponent(
                            style,
                            idCountry,
                            mapboxMap,
                            permissionsManager,
                            latLngLocation!!,
                            zoomLocation
                        )
                        setupCars()
                        animateRandomRoutes()
                        animateTaxi()
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

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        TODO("Not yet implemented")
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

    private fun setupCars() {
        addRandomCars()
        addPassenger()
        addMainCar()
    }

    var indexCount: Double = 100.0


    private val duration: Long
        private get() = (Random.nextInt(DURATION_RANDOM_MAX) + DURATION_BASE).toLong()
    private val latLngInBounds: LatLng
        get() {
            try {
                val userLocation = mapboxMap.locationComponent.lastKnownLocation
                var lat = userLocation!!.latitude + generateRandomDecimal()
                var lng = userLocation!!.longitude + generateRandomDecimal()
                Log.d("LAT CAR---->", lat.toString())
                Log.d("LNG CAR---->", lng.toString())
                return LatLng(lat, lng)
            } catch (e: Exception) {
                Log.d("ERROR LATLNG", e.toString())
                var lat = 10.728073
                var lng = 106.624054
                return LatLng(lat, lng)
            }
        }

    fun generateRandomDecimal(): Double {
        val randomDecimal = Random.nextDouble(0.1, 1.0)
        return (randomDecimal * 10000).roundToInt() / 10000.0
    }

    fun roundTo3DecimalPlaces(number: Double, count: Double): Double {
        val rounded = (number * (1000 + count)).toInt() / 1000.0
        return rounded
    }


    private fun convertToLatLngBounds(latLng: LatLng): LatLngBounds {
        try {
            val builder = LatLngBounds.Builder()
            builder.include(latLng)
            return builder.build()
        } catch (e: Exception) {
            return mapboxMap!!.projection.visibleRegion.latLngBounds
        }
    }

    private val longestDrive: Car?
        private get() {
            var longestDrive: Car? = null
            for (randomCar in randomCars) {
                if (longestDrive == null) {
                    longestDrive = randomCar
                } else if (longestDrive.duration < randomCar.duration) {
                    longestDrive = randomCar
                }
            }
            return longestDrive
        }

    private fun updateTaxiSource() {
        taxi!!.updateFeature()
        taxiSource!!.setGeoJson(taxi!!.feature)
    }

    private fun updateRandomDestinations() {
        for (randomCar in randomCars) {
            randomCar.setNext(latLngInBounds)
        }
    }

    private fun updatePassenger() {
        passenger = latLngInBounds
        updatePassengerSource()
        taxi!!.setNext(passenger)
    }

    private fun updatePassengerSource() {
        val source = style!!.getSourceAs<GeoJsonSource>(PASSENGER_SOURCE)
        val featureCollection = FeatureCollection.fromFeatures(
            arrayOf(
                Feature.fromGeometry(
                    Point.fromLngLat(
                        passenger!!.longitude, passenger!!.latitude
                    )
                )
            )
        )
        source!!.setGeoJson(featureCollection)
    }

    private fun updateRandomCarSource() {
        for (randomCarsRoute in randomCars) {
            randomCarsRoute.updateFeature()
        }
        randomCarSource!!.setGeoJson(featuresFromRoutes())
    }

    private fun featuresFromRoutes(): FeatureCollection {
        val features: MutableList<Feature> = ArrayList()
        for (randomCarsRoute in randomCars) {
            features.add(randomCarsRoute.feature)
        }
        return FeatureCollection.fromFeatures(features)
    }

    private fun addRandomCars() {
        try {
            var latLng: LatLng
            var next: LatLng
            for (i in 0..20) {
                latLng = latLngInBounds
                next = latLngInBounds
                val properties = JsonObject()
                properties.addProperty(PROPERTY_BEARING, Car.getBearing(latLng, next))
                val feature = Feature.fromGeometry(
                    Point.fromLngLat(
                        latLng.longitude, latLng.latitude
                    ), properties
                )
                randomCars.add(
                    Car(feature, next, duration)
                )
            }
            randomCarSource = GeoJsonSource(RANDOM_CAR_SOURCE, featuresFromRoutes())
            style!!.addSource(randomCarSource!!)
            style!!.addImage(
                RANDOM_CAR_IMAGE_ID, (ResourcesCompat.getDrawable(
                    resources, R.drawable.ic_taxi_top, null
                ) as BitmapDrawable).bitmap
            )
            val symbolLayer = SymbolLayer(RANDOM_CAR_LAYER, RANDOM_CAR_SOURCE)
            symbolLayer.withProperties(
                PropertyFactory.iconImage(RANDOM_CAR_IMAGE_ID),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconSize(0.4F),
                PropertyFactory.iconRotate(Expression.get(PROPERTY_BEARING)),
                PropertyFactory.iconIgnorePlacement(true)
            )
            style!!.addLayerBelow(symbolLayer, WATERWAY_LAYER_ID)
        } catch (e: Exception) {
            Log.d("error addRandomCars", e.toString())
        }
    }

    private fun addPassenger() {
        try {
            passenger =
                latLngInBounds
            val featureCollection = FeatureCollection.fromFeatures(
                arrayOf(
                    Feature.fromGeometry(
                        Point.fromLngLat(
                            passenger!!.longitude, passenger!!.latitude
                        )
                    )
                )
            )
            style!!.addImage(
                PASSENGER, (ResourcesCompat.getDrawable(
                    resources, R.drawable.ic_car_top, null
                ) as BitmapDrawable).bitmap
            )
            val geoJsonSource = GeoJsonSource(PASSENGER_SOURCE, featureCollection)
            style!!.addSource(geoJsonSource)
            val symbolLayer = SymbolLayer(PASSENGER_LAYER, PASSENGER_SOURCE)
            symbolLayer.withProperties(
                PropertyFactory.iconImage(PASSENGER),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconSize(0.4F),
            )
            style!!.addLayerBelow(symbolLayer, RANDOM_CAR_LAYER)
        } catch (e: Exception) {
            Log.d("Error addRandomCars 2", e.toString())
        }
    }

    private fun addMainCar() {
        try {
            val latLng = latLngInBounds
            val properties = JsonObject()
            properties.addProperty(PROPERTY_BEARING, Car.getBearing(latLng, passenger))
            val feature = Feature.fromGeometry(
                Point.fromLngLat(
                    latLng.longitude, latLng.latitude
                ), properties
            )
            val featureCollection = FeatureCollection.fromFeatures(arrayOf(feature))
            taxi = Car(feature, passenger, duration)
            style!!.addImage(
                RANDOM_CAR_IMAGE_ID, (ResourcesCompat.getDrawable(
                    resources, R.drawable.ic_taxi_top, null
                ) as BitmapDrawable).bitmap
            )
            taxiSource = GeoJsonSource(TAXI_SOURCE, featureCollection)
            style!!.addSource(taxiSource!!)
            val symbolLayer = SymbolLayer(TAXI_LAYER, TAXI_SOURCE)
            symbolLayer.withProperties(
                PropertyFactory.iconImage(TAXI),
                PropertyFactory.iconSize(4F),
                PropertyFactory.iconRotate(Expression.get(PROPERTY_BEARING)),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconSize(0.4F),
                PropertyFactory.iconIgnorePlacement(true)
            )
            style!!.addLayer(symbolLayer)
        } catch (e: Exception) {
            Log.d("error addRandomCars 3", e.toString())
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun animateRandomRoutes() {
        val longestDrive = longestDrive
        for (car in randomCars) {
            val isLongestDrive = longestDrive == car
            val valueAnimator = ValueAnimator.ofObject(LatLngEvaluator(), car.current, car.next)
            valueAnimator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
                private var latLng: LatLng? = null
                override fun onAnimationUpdate(animation: ValueAnimator) {
                    latLng = animation.animatedValue as LatLng
                    car.current = latLng
                    if (isLongestDrive) {
                        updateRandomCarSource()
                    }
                }
            })
            if (isLongestDrive) {
                valueAnimator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        updateRandomDestinations()
                        animateRandomRoutes()
                    }
                })
            }
            valueAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    car.feature.properties()!!
                        .addProperty("bearing", Car.getBearing(car.current, car.next))
                }
            })
            val offset = if (Random.nextInt(2) == 0) 0 else Random.nextInt(1000) + 250
            var currentDuration = car.duration - offset
            valueAnimator.startDelay = offset.toLong()
            if (currentDuration < 0) {
                valueAnimator.duration = 2000
            } else {
                valueAnimator.duration = currentDuration
            }
            valueAnimator.interpolator = LinearInterpolator()
            valueAnimator.start()
            animators.add(valueAnimator)
        }
    }

    private var isShowRouter: Boolean = true
    private var indexRouter: Int = 0

    private fun animateTaxi() {
        val valueAnimator = ValueAnimator.ofObject(LatLngEvaluator(), taxi!!.current, taxi!!.next)
        valueAnimator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            private var latLng: LatLng? = null
            override fun onAnimationUpdate(animation: ValueAnimator) {
                latLng = animation.animatedValue as LatLng
                taxi!!.current = latLng
                updateTaxiSource()
                indexRouter++
                movePassengerAlongRoute(indexRouter)

            }
        })
        valueAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                updatePassenger()
                animateTaxi()

            }
        })
        valueAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                taxi!!.feature.properties()!!
                    .addProperty("bearing", Car.getBearing(taxi!!.current, taxi!!.next))
                if (taxi?.current != null && passenger != null) {
                    drawRoute(
                        Point.fromLngLat(
                            taxi?.current?.longitude!!, taxi?.current?.latitude!!
                        ), Point.fromLngLat(
                            passenger!!.longitude, passenger!!.latitude
                        ),
                        indexRouter
                    )
                }
            }
        })
        valueAnimator.duration = (70 * taxi!!.current!!.distanceTo(taxi!!.next!!)).toLong()
        valueAnimator.interpolator = AccelerateDecelerateInterpolator()
        valueAnimator.start()
        animators.add(valueAnimator)
    }

    private class LatLngEvaluator : TypeEvaluator<LatLng> {
        private val latLng = LatLng()
        override fun evaluate(fraction: Float, startValue: LatLng, endValue: LatLng): LatLng {
            latLng.latitude =
                (startValue.latitude + (endValue.latitude - startValue.latitude) * fraction)
            latLng.longitude =
                (startValue.longitude + (endValue.longitude - startValue.longitude) * fraction)
            return latLng
        }
    }


    private var passengerIndex: Int = 0
    private fun movePassengerAlongRoute(index: Int) {
        try {
            val source = style?.getSourceAs<GeoJsonSource>("route-source")
            if (source != null) {
                val lineString = source.querySourceFeatures(Expression.literal(true))?.first()
                    ?.geometry() as LineString?
                val coordinates = lineString?.coordinates()
                if (coordinates != null && passengerIndex < coordinates.size) {
                    val passengerLatLng = LatLng(
                        coordinates[passengerIndex].latitude(),
                        coordinates[passengerIndex].longitude()
                    )

                    // Cập nhật vị trí của passenger và vẽ lại
                    passenger = passengerLatLng
                    updatePassengerSource()
                    passengerIndex++
                }
            }
        } catch (e: Exception) {
            Log.e("Error moving passenger", e.toString())
        }
    }

    private fun drawRoute(origin: Point?, destination: Point?, index: Int) {
        if (origin != null && destination != null) {
            var navigation = NavigationRoute.builder(context).accessToken(getString(R.string.mapbox_access_token)).origin(origin)
                .voiceUnits(DirectionsCriteria.METRIC).alternatives(true).profile("car")
                .baseUrl(MapUtils(requireActivity()).urlDomain(idCountry)).destination(destination)
            navigation.build().getRoute(object : Callback<DirectionsResponse> {
                override fun onResponse(
                    call: Call<DirectionsResponse>, response: Response<DirectionsResponse>
                ) {
                    response.body()?.let { response ->
                        if (response.routes.isNotEmpty()) {
                            try {
                                val trackasiaResponse =
                                    DirectionsResponse.fromJson(response.toJson());
                                var route = trackasiaResponse.routes.first()
                                val lineString = LineString.fromPolyline(
                                    route.geometry, PRECISION_6
                                )
                                val routeFeature = Feature.fromGeometry(
                                    LineString.fromLngLats(lineString.coordinates())
                                )

                                style?.addSource(
                                    GeoJsonSource(
                                        "route-source",
                                        routeFeature
                                    )
                                )
                                style?.addLayer(
                                    LineLayer(
                                        "route-layer",
                                        "route-source"
                                    ).withProperties(
                                        PropertyFactory.lineWidth(5f),
                                        PropertyFactory.lineColor(Color.parseColor("#009688"))
                                    )
                                )
                            } catch (e: Exception) {
                                print(e.message)
                            }
                        }
                    }
                }


                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    Log.e("Directions API", "Error fetching directions", t)
                }
            })
        }
    }

    private class Car internal constructor(
        var feature: Feature, next: LatLng?, duration: Long
    ) {
        var next: LatLng?
        var current: LatLng?
        val duration: Long

        @JvmName("setNext1")
        fun setNext(next: LatLng?) {
            this.next = next
        }

        fun updateFeature() {
            feature = Feature.fromGeometry(
                Point.fromLngLat(
                    current!!.longitude, current!!.latitude
                )
            )
            feature.properties()!!.addProperty("bearing", getBearing(current, next))
        }

        companion object {
            fun getBearing(from: LatLng?, to: LatLng?): Float {
                return TurfMeasurement.bearing(
                    Point.fromLngLat(from!!.longitude, from.latitude),
                    Point.fromLngLat(to!!.longitude, to.latitude)
                ).toFloat()
            }
        }

        init {
            val point = feature.geometry() as Point?
            current = LatLng(point!!.latitude(), point.longitude())
            this.duration = duration
            this.next = next
        }
    }

    companion object {
        private const val PASSENGER = "passenger"
        private const val PASSENGER_LAYER = "passenger-layer"
        private const val PASSENGER_SOURCE = "passenger-source"
        private const val TAXI = "taxi"
        private const val TAXI_LAYER = "taxi-layer"
        private const val TAXI_SOURCE = "taxi-source"
        private const val RANDOM_CAR_LAYER = "random-car-layer"
        private const val RANDOM_CAR_SOURCE = "random-car-source"
        private const val RANDOM_CAR_IMAGE_ID = "random-car"
        private const val PROPERTY_BEARING = "bearing"
        private const val WATERWAY_LAYER_ID = "water_intermittent"
        private const val DURATION_RANDOM_MAX = 3000
        private const val DURATION_BASE = 6000
    }
}

//Point.fromLngLat(105.537809, 21.203176)
//Point.fromLngLat(106.650305, 10.770668)


