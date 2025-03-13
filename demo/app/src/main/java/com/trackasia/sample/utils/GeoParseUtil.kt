package com.trackasia.sample.utils

import android.content.Context
import android.text.TextUtils
import com.trackasia.android.geometry.LatLng
import com.trackasia.geojson.FeatureCollection
import com.trackasia.geojson.Point
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.Charset

object GeoParseUtil {

    @Throws(IOException::class)
    fun loadStringFromAssets(context: Context, fileName: String): String {
        require(!TextUtils.isEmpty(fileName)) { "No GeoJSON File Name passed in." }
        val inputStream: InputStream = context.assets.open(fileName)
        val rd = BufferedReader(InputStreamReader(inputStream, Charset.forName("UTF-8")))
        return readAll(rd)
    }

    fun parseGeoJsonCoordinates(geojsonStr: String): List<LatLng> {
        val latLngs = ArrayList<LatLng>()
        val featureCollection = FeatureCollection.fromJson(geojsonStr)
        for (feature in featureCollection.features()!!) {
            if (feature.geometry() is Point) {
                val point = feature.geometry() as Point
                latLngs.add(LatLng(point.latitude(), point.longitude()))
            }
        }
        return latLngs
    }

    @Throws(IOException::class)
    private fun readAll(rd: Reader): String {
        val sb = StringBuilder()
        var cp: Int
        while (rd.read().also { cp = it } != -1) {
            sb.append(cp.toChar())
        }
        return sb.toString()
    }
}
