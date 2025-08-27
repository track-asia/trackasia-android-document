package com.trackasia.sample.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.location.Location
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.trackasia.android.camera.CameraUpdateFactory
import com.trackasia.android.geometry.LatLng
import com.trackasia.android.location.LocationComponent
import com.trackasia.android.location.LocationComponentActivationOptions
import com.trackasia.android.location.modes.CameraMode
import com.trackasia.android.location.modes.RenderMode
import com.trackasia.android.location.permissions.PermissionsManager
import com.trackasia.android.maps.Style
import com.trackasia.android.maps.TrackAsiaMap
import com.trackasia.sample.R
import com.trackasia.sample.api.Constants

class MapUtils(private val context: Activity) {

    fun createStoreMarker(text: String, color: Int): Bitmap? {
        val layoutInflater = LayoutInflater.from(context)
        val markerLayout: View = layoutInflater.inflate(R.layout.store_marker_layout, null)
        val markerImage = markerLayout.findViewById<View>(R.id.marker_image) as ImageView
        val markerRating = markerLayout.findViewById<View>(R.id.marker_text) as TextView
        markerImage.setImageResource(R.drawable.ic_home_marker)
        markerRating.text = text
        markerImage.setColorFilter(
            ContextCompat.getColor(context, color), PorterDuff.Mode.SRC_IN
        )
        markerRating.setTextColor(ContextCompat.getColor(context, color))
        markerLayout.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        markerLayout.layout(0, 0, markerLayout.measuredWidth, markerLayout.measuredHeight)
        val bitmap = Bitmap.createBitmap(
            markerLayout.measuredWidth, markerLayout.measuredHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        markerLayout.draw(canvas)
        return bitmap
    }

    @SuppressLint("MissingPermission")
    fun enableLocationComponent(
        loadedMapStyle: Style,
        idCountry: String?,
        trackasiaMap: TrackAsiaMap,
        permissionsManager: PermissionsManager,
        latLngLocation: LatLng,
        zoom: Double
    ) {
        try {
            if (PermissionsManager.areLocationPermissionsGranted(context)) {
                val locationComponent: LocationComponent = trackasiaMap.locationComponent
                locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(context, loadedMapStyle).build()
                )
                locationComponent.isLocationComponentEnabled = true
                locationComponent.cameraMode = CameraMode.TRACKING
                locationComponent.renderMode = RenderMode.COMPASS
                val lastLocation: Location? = locationComponent.lastKnownLocation
                if (lastLocation != null && idCountry != null && idCountry == "vn") {
                    var latlng = LatLng(
                        lastLocation.latitude, lastLocation.longitude
                    )
                    trackasiaMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom))
                } else {
                    trackasiaMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngLocation, zoom))
                    Toast.makeText(context, "Last known location not available", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                permissionsManager.requestLocationPermissions(context)
            }
        } catch (e: Exception) {
            Log.d("ERROR PERMISSTION", e.toString())
        }
    }

    fun urlStyle(idCountry: String?, is3D: Boolean? = false): String {
        return when (idCountry) {
            "vn" -> if (is3D == false) Constants.urlStyleVN else Constants.urlStyle3DVN
            "sg" -> if (is3D == false) Constants.urlStyleSG else Constants.urlStyle3DSG
            "th" -> if (is3D == false) Constants.urlStyleTH else Constants.urlStyle3DTH
            "tw" -> if (is3D == false) Constants.urlStyleTW else Constants.urlStyle3DTW
            "my" -> if (is3D == false) Constants.urlStyleMI else Constants.urlStyle3DMI
            else -> if (is3D == false) Constants.urlStyleVN else Constants.urlStyle3DVN
        }
    }

    /**
     * Returns the URL for the simple style based on country
     * @param idCountry Country identifier
     * @return URL for the simple style map
     */
    fun urlStyleSimple(idCountry: String?): String {
        return when (idCountry) {
            "vn" -> Constants.urlStyleSimpleVN
            "sg" -> Constants.urlStyleSimpleSG
            "th" -> Constants.urlStyleSimpleTH
            "tw" -> Constants.urlStyleSimpleTW
            "my" -> Constants.urlStyleSimpleMI
            else -> Constants.urlStyleSimpleVN
        }
    }

    fun urlDomain(idCountry: String?): String {
        return when (idCountry) {
            "vn" -> Constants.baseurl
            "sg" -> Constants.baseurlSG
            "th" -> Constants.baseurlTH
            "tw" -> Constants.baseurlTW
            "my" -> Constants.baseurlMI
            else -> Constants.baseurl
        }
    }

    fun zoom(idCountry: String?): Double {
        return when (idCountry) {
            "vn" -> 10.0
            "sg" -> 10.0
            "th" -> 6.0
            "tw" -> 6.0
            "my" -> 6.0
            else -> 10.0
        }
    }

    fun getNameContry(idCountry: String): String {
        return when (idCountry) {
            "vn" -> "Việt Nam"
            "sg" -> "Singapore"
            "th" -> "Thailand"
//            "tw" -> "Taiwan"
//            "my" -> "Malaysia"
            else -> "Việt Nam"
        }
    }

    fun getLatlng(idCountry: String?): LatLng {
        return when (idCountry) {
            "vn" -> LatLng(10.728073, 106.624054)
            "sg" -> LatLng(1.3302, 103.8104)
            "th" -> LatLng(13.27, 101.96)
            "tw" -> LatLng(23.670467, 120.960998)
            "my" -> LatLng(3.5799465, 102.2791128)
            else -> LatLng(10.728073, 106.624054)
        }
    }

    fun getRouterDirection(router: String?): Int {
        return when (router){
            "right" -> R.drawable.ic_arrow_forward
            "left" -> R.drawable.ic_arrow_back
//            "right" -> R.drawable.ic_arrow_forward
//            "right" -> R.drawable.ic_arrow_forward
//            "right" -> R.drawable.ic_arrow_forward
//            "right" -> R.drawable.ic_arrow_forward
            else -> R.drawable.ic_arrow_forward
        }
    }

    fun getNameDirection(router: String?): String {
        return when (router){
            "right" -> "quẹo trái vào "
            "left" -> "quẹo phải vào "
//            "right" -> R.drawable.ic_arrow_forward
//            "right" -> R.drawable.ic_arrow_forward
//            "right" -> R.drawable.ic_arrow_forward
//            "right" -> R.drawable.ic_arrow_forward
            else -> "quẹo phải vào "
        }
    }

    /**
     * Create a donut chart bitmap with segments colored according to cluster size
     * @param clusterSize The size of the cluster
     * @param width Width of the resulting bitmap
     * @param height Height of the resulting bitmap
     * @param strokeWidth Width of the donut stroke
     * @return Bitmap containing the donut chart
     */
    fun createDonutChartBitmap(
        clusterSize: Int,
        width: Int = 70,
        height: Int = 70,
        strokeWidth: Float = 12f
    ): Bitmap {
        val segments = getClusterSegments(clusterSize)
        val radius = minOf(width, height) / 2f - strokeWidth / 2f
        
        // Create a bitmap and canvas
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Define center point
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Draw the segments
        var startAngle = -90f // Start at top (-90 degrees)
        segments.forEach { (color, percentage) ->
            val paint = android.graphics.Paint().apply {
                this.color = color
                style = android.graphics.Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                isAntiAlias = true
            }
            
            val sweepAngle = 360f * percentage
            canvas.drawArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                startAngle,
                sweepAngle,
                false,
                paint
            )
            startAngle += sweepAngle
        }
        
        return bitmap
    }
    
    /**
     * Get the color segments for a cluster based on its size
     * @param clusterSize The size of the cluster
     * @return Map of color to percentage for each segment
     */
    fun getClusterSegments(clusterSize: Int): Map<Int, Float> {
        val segments = mutableMapOf<Int, Float>()
        val colors = listOf(
            ContextCompat.getColor(context, R.color.cluster_color_1), // Replace with your actual colors
            ContextCompat.getColor(context, R.color.cluster_color_2),
            ContextCompat.getColor(context, R.color.cluster_color_3),
            ContextCompat.getColor(context, R.color.cluster_color_4),
            ContextCompat.getColor(context, R.color.cluster_color_5),
            ContextCompat.getColor(context, R.color.cluster_color_6)
        )
        
        // Define segments based on cluster size (similar to Flutter implementation)
        when {
            clusterSize < 10 -> {
                segments[colors[0]] = 1.0f  // Single color for small clusters
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
     * Returns the URL for the night style based on country
     * @param idCountry Country identifier
     * @return URL for the night style map
     */
    fun urlStyleNight(idCountry: String?): String {
        return when (idCountry) {
            "vn" -> Constants.urlStyleNightVN
            "sg" -> Constants.urlStyleNightSG
            "th" -> Constants.urlStyleNightTH
            "tw" -> Constants.urlStyleNightTW
            "my" -> Constants.urlStyleNightMI
            else -> Constants.urlStyleNightVN
        }
    }

}