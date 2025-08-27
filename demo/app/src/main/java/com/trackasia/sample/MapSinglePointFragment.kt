package com.trackasia.sample

import SuggestionAdapter
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.trackasia.android.TrackAsia
import com.trackasia.android.annotations.MarkerOptions
import com.trackasia.android.camera.CameraPosition
import com.trackasia.android.camera.CameraUpdateFactory
import com.trackasia.android.geometry.LatLng
import com.trackasia.android.location.permissions.PermissionsListener
import com.trackasia.android.location.permissions.PermissionsManager
import com.trackasia.android.maps.Style
import com.trackasia.android.maps.TrackAsiaMap
import com.trackasia.geojson.Point
import com.trackasia.navigation.android.navigation.ui.v5.route.NavigationMapRoute
import com.trackasia.navigation.core.navigation.TrackAsiaNavigation
import com.trackasia.sample.adapter.PoiResultAdapter
import com.trackasia.sample.api.Constants
import com.trackasia.sample.api.model.Feature
import com.trackasia.sample.api.model.GeoCodingData
import com.trackasia.sample.databinding.FragmentMapSinglePointBinding
import com.trackasia.sample.model.PoiResult
import com.trackasia.sample.utils.LoadingDialog
import com.trackasia.sample.utils.MapUtils
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import kotlin.math.min

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class MapSinglePointFragment : Fragment(), PermissionsListener {

    private companion object {
        private const val TAG = "MapSinglePointFragment"
    }

    private var _binding: FragmentMapSinglePointBinding? = null
    private val binding get() = _binding!!
    private lateinit var trackasiaMap: TrackAsiaMap
    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private var navigationMapRoute: NavigationMapRoute? = null
    private var addressTo: Point? = null
    private var addressList: List<String>? = null
    private var addressListData: List<Feature>? = null
    private var styleUrl = "https://maps.track-asia.com/styles/v1/streets.json?key=public"
    private lateinit var sharedPreferences: SharedPreferences
    private var latLngLocation: LatLng? = LatLng(10.728073, 106.624054)
    private var zoomLocation: Double = 10.0
    private var zoomLevel: Double = 16.0
    private var styleType = 0 // 0 = standard, 1 = simple, 2 = night, 3 = 3D
    private var idCountry: String? = "vn"
    private var addressCurrent: RouterAddressModel? = null
    private lateinit var loading: LoadingDialog
    private lateinit var poiResultAdapter: PoiResultAdapter
    private val poiResults = mutableListOf<PoiResult>()
    private var currentPoiCategory: String = ""

    private val viewModel: MainViewModel by viewModels()

    // Các biến để lưu thông tin điểm đích
    private var targetLatLng: LatLng? = null
    private var targetName: String = "Điểm đến"
    private var addressAt: String = ""
    private var targetChoosen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragmentContext: Context? = context
        fragmentContext?.let {
            sharedPreferences = it.getSharedPreferences("trackasia", Context.MODE_PRIVATE)
            idCountry = sharedPreferences.getString("country", "vn")
            styleType = 0
            styleUrl = MapUtils(requireActivity()).urlStyle(idCountry, false)
            latLngLocation = MapUtils(requireActivity()).getLatlng(idCountry)
            zoomLocation = MapUtils(requireActivity()).zoom(idCountry)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        TrackAsia.getInstance(requireActivity())
        _binding = FragmentMapSinglePointBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)
        initControl()
        initListener()
        initMap()

        // Khởi tạo RecyclerView cho danh sách kết quả POI
        initPoiResultsList()

        // Thêm nút tính toán tuyến đường nếu chưa có
        addCalculateDirectionButtonIfNeeded()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initMap() {
        binding.mapView.getMapAsync { map ->
            try {
                Log.d("DOMAIN URL STYLE:", styleUrl)
                this.trackasiaMap = map
                map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
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
                trackasiaMap.addOnMapClickListener { point ->
                    showPointMap(point)
                    return@addOnMapClickListener true
                }
                navigationMapRoute = NavigationMapRoute(binding.mapView, map)
                cameraAnimation(latLngLocation!!, zoomLevel)
            } catch (e: Exception) {
                Log.d("ERROR MAP:", e.toString())
            }
        }
    }

    private fun initControl() {
        loading = LoadingDialog(requireContext())
        binding.btnMyLocation1.visibility = View.VISIBLE
        binding.edtAddressTo.addTextChangedListener(edtTextChange())
        binding.edtAddressTo.setOnItemClickListener { parent, view, position, id ->
            if (addressListData?.isNotEmpty() == true) {
                val item = addressListData?.get(position)
                val lat = item?.geometry?.coordinates!![1]
                val lng = item?.geometry?.coordinates!![0]
                val currentLatlng = LatLng(lat, lng)
                val snippet = "Lat: $lat Lng: $lng"
                addressTo = Point.fromLngLat(lat, lng)
                trackasiaMap.clear()
                trackasiaMap.addMarker(
                    MarkerOptions().position(currentLatlng).title(item.properties.name)
                        .snippet(snippet)
                )
                cameraAnimation(point = currentLatlng, zoomLevel)
                hideKeyboard(requireActivity())
            }
        }
        binding.btnMyLocation1.setOnClickListener {
            loading.show()
            val userLocation = trackasiaMap.locationComponent.lastKnownLocation
            if (userLocation != null) {
                if (userLocation.longitude > 0) {
                    latLngLocation = LatLng(userLocation.latitude, userLocation.longitude)
                }
                cameraAnimation(latLngLocation!!, zoomLevel)
                showPointMap(latLngLocation!!)
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


    fun hideKeyboard(activity: Activity) {
        val imm =
            activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }


    private fun initListener() {
        viewModel.geoCodingData.observe(requireActivity()) { data ->
            loading.dismiss()
            if (data != null) {
                setPositionMap(data)
                if (data.name != null && data.lat != null && data.lat != "" && data.long != null && data.long != "") {
                    addressCurrent = RouterAddressModel("", data.name, "", null)
                    binding.edtAddressTo.setText(data.name)
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
                    binding.edtAddressTo.setAdapter(adapter)
                    adapter.notifyDataSetChanged()
                }
            }
        }
        binding.btnClear2.setOnClickListener {
            binding.edtAddressTo.clearFocus()
            binding.edtAddressTo.setText("")
        }
        binding.locationMy.setOnClickListener {
            try {
                val userLocation = trackasiaMap.locationComponent.lastKnownLocation
                if (userLocation != null) {
                    if (userLocation.longitude > 0) {
                        latLngLocation = LatLng(userLocation.latitude, userLocation.longitude)
                    }
                    showPointMap(latLngLocation!!)
                }
            } catch (e: Exception) {
                Log.d("ERROR:", e.toString())
            }
        }

        // Xử lý nút tìm kiếm POI
        binding.fabPoiSearch.setOnClickListener {
            Log.d("POI_POPUP", "POI search button clicked in SinglePointFragment")
            showPoiSearchPopup()
        }

        binding.map3d.setOnClickListener {
            // Cycle through the four map styles in sequence: Standard -> Simple -> Night -> 3D -> (back to Standard)
            styleType = (styleType + 1) % 4

            // Set the appropriate style URL based on style type
            styleUrl = when(styleType) {
                0 -> MapUtils(requireActivity()).urlStyle(idCountry, false) // Standard style
                1 -> getSimpleStyleUrl() // Simple style
                2 -> getNightStyleUrl(idCountry) // Night style
                3 -> MapUtils(requireActivity()).urlStyle(idCountry, true)  // 3D satellite style
                else -> MapUtils(requireActivity()).urlStyle(idCountry, false)
            }

            // Update button icon to show the current style
            val iconResId = when(styleType) {
                0 -> R.drawable.ic_map_2d      // Standard map style icon
                1 -> R.drawable.ic_map_simple  // Simple style icon
                2 -> R.drawable.ic_map_night   // Night style icon
                3 -> R.drawable.ic_map_3d      // 3D satellite style icon
                else -> R.drawable.ic_map_2d
            }
            binding.map3d.setImageResource(iconResId)

            // Show toast to inform user of style change
            val styleMessage = when(styleType) {
                0 -> "Chuyển sang phong cách bản đồ chuẩn"
                1 -> "Chuyển sang phong cách bản đồ đơn giản"
                2 -> "Chuyển sang phong cách bản đồ đêm"
                3 -> "Chuyển sang phong cách bản đồ 3D"
                else -> ""
            }
            Toast.makeText(context, styleMessage, Toast.LENGTH_SHORT).show()

            // Apply the new style
            trackasiaMap.setStyle(styleUrl)
        }
    }

    private fun edtTextChange(): TextWatcher? {
        return object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, count: Int) {

            }

            override fun afterTextChanged(editable: Editable) {
                if (editable.toString().length >= 3) {
                    getAutoSuggestion(editable.toString())
                }
            }
        }
    }


    private fun setPositionMap(data: GeoCodingData) {
        try {
            if (data.lat != null && data.lat != "" && data.long != null && data.long != "") {
                val point = Point.fromLngLat(data.lat.toDouble(), data.long.toDouble())
                val latlng = LatLng(data.lat.toDouble(), data.long.toDouble())
                addressTo = point
                if (data.name != null) {
                    binding.edtAddressTo.setText(data.name)
                }
                val snippet = "Lat: ${data.lat} Lng: ${data.long}"
                trackasiaMap.clear()
                trackasiaMap.addMarker(
                    MarkerOptions().position(LatLng(data.lat.toDouble(), data.long.toDouble()))
                        .title(data.name).snippet(snippet)
                )
                cameraAnimation(latlng, zoomLevel)
                data.name?.let { setToastMessage(name = it, snippet) }
            } else {
                val latlng = MapUtils(requireActivity()).getLatlng(idCountry)
                cameraAnimation(latlng, zoomLevel)
            }
        } catch (e: Exception) {
            Log.d("ERROR", e.toString())
        }
    }

    private fun setToastMessage(name: String, snippet: String) {
        Toast.makeText(
            requireActivity(), "Title: $name, Snippet: $snippet", Toast.LENGTH_SHORT
        ).show()
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

    private fun showPointMap(point: LatLng) {
        var point = LatLng(point.latitude, point.longitude)
        viewModel.funCallShowPointMap(point, requireActivity())
    }

    private fun getAutoSuggestion(text: String) {
        viewModel.funCallAutoSuggestion(text, idCountry!!, requireActivity())
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Hiển thị popup tìm kiếm POI
     */
    private fun showPoiSearchPopup() {
        Log.d("POI_POPUP", "MapSinglePointFragment.showPoiSearchPopup() called")

        // Ẩn các nút menu để giao diện gọn gàng hơn
        binding.fabCardView.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.fabCardView.visibility = View.GONE
            }
            .start()

        binding.fabPoiSearch.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.fabPoiSearch.visibility = View.GONE
            }
            .start()

        // Hiển thị container với animation
        binding.poiSearchContainer.visibility = View.VISIBLE
        binding.poiSearchContainer.alpha = 0f
        binding.poiSearchContainer.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        // Thiết lập click listener cho container để ẩn popup khi nhấn bên ngoài
        binding.poiSearchContainer.setOnClickListener {
            hidePoiSearchPopup()
        }

        // Ngăn click bên ngoài propagation khi nhấn vào card
        binding.poiSearchLayout.root.setOnClickListener {
            // Chặn sự kiện click truyền xuống container
            Log.d("POI_POPUP", "Clicked on POI search card")
        }

        // Thiết lập click listener cho nút đóng
        binding.btnClosePoiPopup.setOnClickListener {
            Log.d("POI_POPUP", "Close button clicked")
            hidePoiSearchPopup()
        }

        // Thiết lập các listener cho các category
        setupPoiCategoryListeners()
    }

    /**
     * Ẩn popup tìm kiếm POI
     */
    private fun hidePoiSearchPopup() {
        Log.d("POI_POPUP", "MapSinglePointFragment.hidePoiSearchPopup() called")

        // Ẩn bàn phím nếu đang hiển thị
        val activity = activity
        if (activity != null) {
            hideKeyboard(activity)
        }

        // Animate fade out cho popup
        binding.poiSearchContainer.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.poiSearchContainer.visibility = View.GONE

                // Hiện lại các nút menu sau khi popup đã ẩn hoàn toàn
                binding.fabCardView.visibility = View.VISIBLE
                binding.fabCardView.alpha = 0f
                binding.fabCardView.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()

                binding.fabPoiSearch.visibility = View.VISIBLE
                binding.fabPoiSearch.alpha = 0f
                binding.fabPoiSearch.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    /**
     * Thiết lập các listeners cho các nút category trong popup
     */
    private fun setupPoiCategoryListeners() {
        // Danh sách các nút category và hành động tương ứng
        val categoryMap = mapOf(
            R.id.category_restaurant to "restaurant",
            R.id.category_hotel to "hotel",
            R.id.category_store to "supermarket",
            R.id.category_pharmacy to "pharmacy",
            R.id.category_entertainment to "entertainment",
            R.id.category_government to "government",
            R.id.category_education to "education",
            R.id.category_bank to "bank"
        )

        // Thiết lập listener cho mỗi category
        categoryMap.forEach { (id, category) ->
            binding.poiSearchLayout.root.findViewById<View>(id)?.setOnClickListener {
                searchNearbyPoi(category)
                hidePoiSearchPopup()
            }
        }
    }

    /**
     * Tìm kiếm POI gần vị trí hiện tại dựa trên category
     */
    private fun searchNearbyPoi(category: String) {
        // Lưu lại category hiện tại
        currentPoiCategory = category

        Log.d(TAG, "Starting search for category: $category")

        // Ẩn các nút menu để giao diện gọn gàng hơn
        binding.fabCardView.visibility = View.GONE
        binding.fabPoiSearch.visibility = View.GONE

        // Lấy vị trí trung tâm của bản đồ
        val mapCenter = trackasiaMap.cameraPosition.target ?: LatLng(0.0, 0.0)
        Log.d(TAG, "Search center: $mapCenter")

        // Xóa các marker POI trước đó (nếu có)
        clearPoiMarkers()

        // Xóa danh sách kết quả cũ
        poiResults.clear()

        // Hiển thị loading
        loading.show()

        // Thông báo cho người dùng
        showToast("Đang tìm kiếm ${getCategoryName(category)} gần đây...")

        // Gọi API để tìm kiếm địa điểm
        val apiType = when(category) {
            "restaurant" -> "food_and_drink"
            "hotel" -> "lodging"
            "supermarket" -> "shop"
            "pharmacy" -> "pharmacy"
            "entertainment" -> "leisure"
            "government" -> "government"
            "education" -> "education"
            "bank" -> "financial"
            else -> category
        }

        Log.d(TAG, "API type parameter: $apiType for category: $category")

        val radius = 30000 // 30km radius

        // Luôn sử dụng API thực tế, không dùng dữ liệu mẫu
        val url = "https://maps.track-asia.com/api/v2/place/nearbysearch/json" +
                "?location=${mapCenter.latitude}%2C${mapCenter.longitude}" +
                "&radius=$radius" +
                "&key=public_key" +
                "&type=$apiType" +
                "&size=40" +
                "&lang=vi"

        Log.d(TAG, "Calling API: $url")

        // Use OkHttp for the API request
        val client = OkHttpClient()
        val request = okhttp3.Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                activity?.runOnUiThread {
                    loading.dismiss()
                    val errorMsg = "Lỗi kết nối: ${e.message}"
                    showToast(errorMsg)
                    Log.e(TAG, "API call failed", e)

                    // Hiện lại các nút menu sau khi lỗi
                    binding.fabCardView.visibility = View.VISIBLE
                    binding.fabPoiSearch.visibility = View.VISIBLE
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val responseBody = response.body?.string()
                Log.d(TAG, "API response received, success=${response.isSuccessful}, code=${response.code}")

                activity?.runOnUiThread {
                    loading.dismiss()
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            Log.d(TAG, "Response preview: ${responseBody.take(500)}...")
                            processApiResponse(responseBody, category)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing response", e)
                            showToast("Lỗi xử lý dữ liệu: ${e.message}")

                            // Hiện lại các nút menu nếu có lỗi xử lý
                            binding.fabCardView.visibility = View.VISIBLE
                            binding.fabPoiSearch.visibility = View.VISIBLE
                        }
                    } else {
                        val errorMsg = "Lỗi từ máy chủ: ${response.code}"
                        showToast(errorMsg)
                        Log.e(TAG, errorMsg)

                        // Hiện lại các nút menu nếu có lỗi từ server
                        binding.fabCardView.visibility = View.VISIBLE
                        binding.fabPoiSearch.visibility = View.VISIBLE
                    }
                }
            }
        })
    }

    /**
     * Xử lý kết quả từ API POI search
     */
    private fun processApiResponse(responseBody: String?, category: String) {
        Log.d(TAG, "Xử lý kết quả API cho danh mục: $category")
        
        // Xóa kết quả cũ và kiểm tra dữ liệu đầu vào
        poiResults.clear()
        
        if (responseBody.isNullOrEmpty()) {
            Log.e(TAG, "Phản hồi API trống")
            showToast("Không có dữ liệu từ máy chủ")
            loading.dismiss()
            return
        }

        try {
            // Parse JSON
            val jsonResponse = JSONObject(responseBody)
            
            // Tìm mảng dữ liệu trong các định dạng khác nhau
            val dataArray = findDataArray(jsonResponse, responseBody)
            
            if (dataArray == null || dataArray.length() == 0) {
                Log.e(TAG, "Không tìm thấy dữ liệu trong phản hồi")
                showToast("Không tìm thấy kết quả nào")
                loading.dismiss()
                return
            }

            // Parse các kết quả POI
            val resultCount = dataArray.length()
            Log.d(TAG, "Tìm thấy $resultCount kết quả POI")

            for (i in 0 until dataArray.length()) {
                try {
                    val poiObject = dataArray.getJSONObject(i)
                    val poiResult = PoiResult.fromJson(poiObject, category)
                    poiResult?.let { 
                        poiResults.add(it)
                        Log.d(TAG, "Đã thêm POI: ${it.name} tại ${it.latitude}, ${it.longitude}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi parse POI tại index $i: ${e.message}")
                }
            }

            // Xử lý kết quả
            processResults(category)

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi xử lý phản hồi API", e)
            activity?.runOnUiThread {
                showToast("Lỗi xử lý dữ liệu: ${e.message}")
                loading.dismiss()
            }
        }
    }
    
    /**
     * Tìm mảng dữ liệu trong JSON với các định dạng khác nhau
     */
    private fun findDataArray(json: JSONObject, rawResponse: String): JSONArray? {
        // Thứ tự tìm kiếm theo các định dạng phổ biến
        val possibleArrayKeys = listOf("data", "features", "results", "pois", "places")
        
        // Thử các khóa phổ biến
        for (key in possibleArrayKeys) {
            json.optJSONArray(key)?.let { return it }
        }
        
        // Thử xem phản hồi có phải là một mảng trực tiếp không
        try {
            return JSONArray(rawResponse)
        } catch (e: Exception) {
            // Không phải mảng JSON
        }
        
        // Thử xem response có phải là một đối tượng đơn lẻ không
        try {
            PoiResult.fromJson(json, "")?.let {
                // Nếu parse được một đối tượng, tạo một mảng chứa đối tượng đó
                val singleItemArray = JSONArray()
                singleItemArray.put(json)
                return singleItemArray
            }
        } catch (e: Exception) {
            // Không thể parse là đối tượng đơn lẻ
        }
        
        return null
    }

    /**
     * Process the parsed POI results
     */
    private fun processResults(category: String) {
        // Check if we successfully parsed any results
        if (poiResults.isEmpty()) {
            Log.e(TAG, "No valid POIs could be parsed from response")
            activity?.runOnUiThread {
                showToast("Lỗi xử lý dữ liệu POI")
                loading.dismiss()
            }
            return
        }

        // Log total number of results for debugging
        Log.d(TAG, "processResults: Found a total of ${poiResults.size} POI results")
        
        // Save results in a local final copy to avoid thread safety issues
        val resultsToShow = ArrayList(poiResults)
        
        // Update the UI on main thread
        activity?.runOnUiThread {
            try {
                Log.d(TAG, "Updating UI on main thread with ${resultsToShow.size} POI results")
                
                // Double check we still have data
                if (resultsToShow.isEmpty()) {
                    Log.e(TAG, "ERROR: Results are empty on UI thread")
                    showToast("Không tìm thấy kết quả")
                    loading.dismiss()
                    return@runOnUiThread
                }

                // Ensure adapter is initialized before showing popup
                if (!::poiResultAdapter.isInitialized) {
                    Log.d(TAG, "Initializing adapter in processApiResponse")
                    initPoiResultsList()
                }

                // Log poiResults content before updating adapter
                for (i in 0 until min(5, resultsToShow.size)) {
                    Log.d(TAG, "Result ${i+1}: ${resultsToShow[i].name}, ${resultsToShow[i].address}")
                }

                // Update adapter with a fresh copy of the data
                poiResultAdapter.updateData(ArrayList(resultsToShow))
                poiResultAdapter.notifyDataSetChanged()
                
                Log.d(TAG, "Adapter updated, now showing popup with ${poiResultAdapter.itemCount} items")
                
                // Show popup with results - after a slight delay to ensure UI update completes
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    showPoiResultsPopup(category, resultsToShow.size)
                }, 100) // 100ms delay to ensure UI updates

                // Dismiss loading indicator
                loading.dismiss()

                // Log success
                Log.d(TAG, "Successfully updated UI with POI results")
            } catch (e: Exception) {
                Log.e(TAG, "Error in processResults UI update: ${e.message}", e)
                showToast("Lỗi hiển thị kết quả: ${e.message}")
                loading.dismiss()
            }
        }
    }

    /**
     * Helper để chuyển Drawable thành Bitmap cho marker icon
     * Được cải tiến để xử lý vấn đề tương thích với Android mới
     */
    private fun getBitmapFromDrawable(drawable: android.graphics.drawable.Drawable): android.graphics.Bitmap? {
        try {
            // Tạo một bản sao của drawable để đảm bảo nó có thể thay đổi
            val mutableDrawable = drawable.mutate()
            
            // Tạo bitmap với kích thước đầy đủ của drawable
            val bitmap = android.graphics.Bitmap.createBitmap(
                mutableDrawable.intrinsicWidth,
                mutableDrawable.intrinsicHeight,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            
            // Tạo canvas để vẽ
            val canvas = android.graphics.Canvas(bitmap)
            
            // Đặt kích thước cho drawable
            mutableDrawable.setBounds(0, 0, canvas.width, canvas.height)
            
            // Vẽ drawable lên canvas
            mutableDrawable.draw(canvas)
            
            return bitmap
        } catch (e: Exception) {
            Log.e("POI_SEARCH", "Error converting drawable to bitmap", e)
            return null
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
        }
    }

    /**
     * Helper method to get the night style URL based on the selected country.
     * Uses constants defined in the Constants class to provide country-specific night styling.
     *
     * @param idCountry The country identifier (e.g., "vn", "sg", "th")
     * @return The appropriate night style URL for the given country
     */
    private fun getNightStyleUrl(idCountry: String?): String {
        return when (idCountry) {
            "vn" -> Constants.urlStyleNightVN
            "sg" -> Constants.urlStyleNightSG
            "th" -> Constants.urlStyleNightTH
            "tw" -> Constants.urlStyleNightTW
            "my" -> Constants.urlStyleNightMI
            else -> Constants.urlStyleNightVN
        }
    }

    /**
     * Helper method to get the simple style URL.
     * This provides a simplified map style that's cleaner and less detailed.
     *
     * @return The URL for the simple style
     */
    private fun getSimpleStyleUrl(): String {
        return MapUtils(requireActivity()).urlStyleSimple(idCountry)
    }

    /**
     * Lấy tên hiển thị cho danh mục POI
     */
    private fun getCategoryName(category: String): String {
        return when (category) {
            "restaurant" -> "nhà hàng"
            "hotel" -> "khách sạn"
            "store" -> "cửa hàng"
            "pharmacy" -> "hiệu thuốc"
            "entertainment" -> "giải trí"
            "government" -> "cơ quan nhà nước"
            "education" -> "trường học"
            "bank" -> "ngân hàng"
            "camera" -> "camera"
            "traffic" -> "giao thông"
            "repair" -> "dịch vụ sửa chữa"
            else -> category
        }
    }

    /**
     * Xóa các marker POI hiện tại
     */
    private fun clearPoiMarkers() {
        val markersToRemove = trackasiaMap.markers.filter { marker ->
            marker.title?.startsWith("POI:") == true
        }

        markersToRemove.forEach { marker ->
            trackasiaMap.removeMarker(marker)
        }
    }

    /**
     * Hiển thị tất cả các marker POI trên bản đồ
     */
    private fun showAllPoiMarkersOnMap() {
        Log.d(TAG, "Showing all ${poiResults.size} POI markers on map")

        // Xóa các marker hiện tại
        clearPoiMarkers()

        // Nếu không có kết quả, thoát sớm
        if (poiResults.isEmpty()) {
            showToast("Không có địa điểm nào để hiển thị")
            return
        }

        // Thu thập tất cả vị trí marker
        val poiMarkers = mutableListOf<LatLng>()
        
        // Thêm marker cho mỗi POI với hiệu ứng đơn giản
        poiResults.forEachIndexed { index, poi ->
            poiMarkers.add(poi.location)
            
            // Tạo marker options
            val markerOptions = MarkerOptions()
                .position(poi.location)
                .title("POI: ${poi.name}")
                .snippet(poi.address)

            // Thêm icon phù hợp nếu có
            getPoiIcon(poi.category.orEmpty())?.let { iconId ->
                try {
                    val drawable = androidx.core.content.ContextCompat.getDrawable(requireContext(), iconId)
                    drawable?.let {
                        val bitmap = getBitmapFromDrawable(it)
                        bitmap?.let { bmp ->
                            val iconFactory = com.trackasia.android.annotations.IconFactory.getInstance(requireContext())
                            markerOptions.icon(iconFactory.fromBitmap(bmp))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting icon for marker: ${e.message}")
                }
            }

            // Thêm marker với hiệu ứng đơn giản
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    val marker = trackasiaMap.addMarker(markerOptions)
                    
                    // Tạo hiệu ứng bounce đơn giản
                    marker?.let { bounceMarker(it) }
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding marker: ${e.message}")
                }
            }, index * 50L) // 50ms delay giữa các marker
        }

        // Di chuyển camera để hiển thị tất cả các marker
        if (poiMarkers.isNotEmpty()) {
            try {
                val builder = com.trackasia.android.geometry.LatLngBounds.Builder()
                poiMarkers.forEach { builder.include(it) }
                val bounds = builder.build()

                // Thêm padding để các marker không nằm sát viền màn hình
                val padding = resources.displayMetrics.density * 64
                
                // Animate camera mượt mà
                trackasiaMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(bounds, padding.toInt()),
                    1000,
                    object : TrackAsiaMap.CancelableCallback {
                        override fun onCancel() { }
                        override fun onFinish() {
                            showToast("Đang hiển thị ${poiMarkers.size} địa điểm ${getCategoryName(currentPoiCategory)}")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error animating camera: ${e.message}")
                // Fallback to first point if bounds fail
                poiMarkers.firstOrNull()?.let {
                    trackasiaMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(it, 14.0),
                        1000
                    )
                }
            }
        }
    }
    
    /**
     * Tạo hiệu ứng bounce cho marker 
     */
    private fun bounceMarker(marker: com.trackasia.android.annotations.Marker) {
        val originalPosition = marker.position
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        
        // Tạo hiệu ứng nảy lên và rơi xuống dễ dàng hơn
        handler.postDelayed({ 
            try { marker.position = LatLng(originalPosition.latitude + 0.00008, originalPosition.longitude) }
            catch(e: Exception) {}
        }, 0)
        
        handler.postDelayed({ 
            try { marker.position = LatLng(originalPosition.latitude + 0.00004, originalPosition.longitude) } 
            catch(e: Exception) {}
        }, 75)
        
        handler.postDelayed({
            try { marker.position = originalPosition }
            catch(e: Exception) {}
        }, 150)
    }
    
    /**
     * Lấy icon ID cho danh mục POI
     */
    private fun getPoiIcon(category: String): Int? {
        val lowerCategory = category.lowercase()
        return when {
            lowerCategory.contains("restaurant") || lowerCategory.contains("food") || 
            lowerCategory.contains("drink") || lowerCategory.contains("cafe") -> R.drawable.ic_restaurant
            
            lowerCategory.contains("hotel") || lowerCategory.contains("lodging") || 
            lowerCategory.contains("accommodation") -> R.drawable.ic_hotel
            
            lowerCategory.contains("supermarket") || lowerCategory.contains("shop") || 
            lowerCategory.contains("store") || lowerCategory.contains("mall") -> R.drawable.ic_store
            
            lowerCategory.contains("pharmacy") || lowerCategory.contains("drugstore") || 
            lowerCategory.contains("hospital") || lowerCategory.contains("clinic") -> R.drawable.ic_pharmacy
            
            lowerCategory.contains("entertainment") -> R.drawable.ic_entertainment
            lowerCategory.contains("government") -> R.drawable.ic_government
            lowerCategory.contains("education") -> R.drawable.ic_education
            lowerCategory.contains("bank") -> R.drawable.ic_bank
            
            else -> R.drawable.ic_location
        }
    }

    /**
     * Hiển thị popup danh sách kết quả POI
     */
    private fun showPoiResultsPopup(category: String, count: Int) {
        Log.d(TAG, "Hiển thị popup kết quả POI cho danh mục: $category với $count kết quả")
        currentPoiCategory = category

        try {
            // Tìm và kiểm tra container
            val container = view?.findViewById<FrameLayout>(R.id.poiResultsContainer) ?: run {
                Log.e(TAG, "Không tìm thấy poiResultsContainer")
                showToast("Lỗi hiển thị kết quả")
                return
            }

            // Hiển thị container
            container.visibility = View.VISIBLE
            container.alpha = 1f
            container.bringToFront()

            // Lấy layout bên trong
            val poiResultsLayout = container.findViewById<View>(R.id.poiResultsLayout) ?: run {
                Log.e(TAG, "Không tìm thấy poiResultsLayout")
                return
            }

            // Cập nhật tiêu đề và số lượng
            updateHeaderInfo(poiResultsLayout, category, count)

            // Cập nhật RecyclerView
            setupRecyclerView(poiResultsLayout)

            // Thiết lập sự kiện cho các nút
            setupButtons(poiResultsLayout)

            // Hiển thị thông báo
            showToast("Đã tìm thấy $count ${getCategoryName(category)} gần đây")

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi hiển thị popup kết quả POI", e)
            showToast("Lỗi hiển thị kết quả: ${e.message}")
        }
    }

    /**
     * Cập nhật tiêu đề và số lượng kết quả
     */
    private fun updateHeaderInfo(layout: View, category: String, count: Int) {
        // Lấy tiêu đề dựa trên danh mục
        val titleText = when (category) {
            "restaurant" -> "Nhà hàng gần đây"
            "hotel" -> "Khách sạn gần đây"
            "store" -> "Cửa hàng gần đây"
            "pharmacy" -> "Hiệu thuốc gần đây"
            "entertainment" -> "Giải trí gần đây"
            "government" -> "Cơ quan gần đây"
            "education" -> "Giáo dục gần đây"
            "bank" -> "Ngân hàng gần đây"
            else -> "Kết quả tìm kiếm"
        }

        // Cập nhật tiêu đề và số lượng
        layout.findViewById<TextView>(R.id.tvPoiResultsTitle)?.text = titleText
        layout.findViewById<TextView>(R.id.tvPoiResultsCount)?.text = if (count > 0) "($count)" else ""
    }

    /**
     * Thiết lập RecyclerView
     */
    private fun setupRecyclerView(layout: View) {
        val recyclerView = layout.findViewById<RecyclerView>(R.id.rvPoiResults) ?: run {
            Log.e(TAG, "Không tìm thấy RecyclerView trong layout")
            return
        }

        // Đảm bảo recyclerView có layoutManager
        if (recyclerView.layoutManager == null) {
            recyclerView.layoutManager = LinearLayoutManager(context)
        }

        // Đảm bảo adapter đã được khởi tạo
        if (!::poiResultAdapter.isInitialized) {
            initPoiResultsList()
        }

        // Đảm bảo adapter được gắn vào RecyclerView 
        if (recyclerView.adapter == null || recyclerView.adapter != poiResultAdapter) {
            recyclerView.adapter = poiResultAdapter
        }

        // Nếu adapter không có dữ liệu nhưng poiResults có, cập nhật adapter
        if (poiResultAdapter.itemCount == 0 && poiResults.isNotEmpty()) {
            poiResultAdapter.updateData(ArrayList(poiResults))
            poiResultAdapter.notifyDataSetChanged()
        }
    }

    /**
     * Thiết lập sự kiện cho các nút
     */
    private fun setupButtons(layout: View) {
        // Nút đóng
        layout.findViewById<View>(R.id.btnClosePoiResults)?.setOnClickListener {
            hidePoiResultsPopup()
        }

        // Nút hiển thị tất cả trên bản đồ
        layout.findViewById<View>(R.id.btnShowAllMarkersOnMap)?.let { button ->
            button.setOnClickListener { animateAndShowAllMarkers(button) }
        }
    }

    /**
     * Ẩn popup danh sách kết quả POI
     */
    private fun hidePoiResultsPopup() {
        // Tìm container
        view?.findViewById<FrameLayout>(R.id.poiResultsContainer)?.let { container ->
            // Nếu đã ẩn, không cần animate
            if (container.visibility != View.VISIBLE) {
                return
            }
            
            // Animate fade out
            container.animate()
                .alpha(0f)
                .setDuration(250)
                .withEndAction {
                    container.visibility = View.GONE
                    
                    // Hiện lại các nút menu
                    showFabButtons()
                }
                .start()
        }
    }

    /**
     * Show the FAB buttons with animation
     */
    private fun showFabButtons() {
        Log.d(TAG, "Showing FAB buttons")

        // Show card view FAB
        binding.fabCardView.visibility = View.VISIBLE
        binding.fabCardView.alpha = 0f
        binding.fabCardView.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

        // Show POI search FAB
        binding.fabPoiSearch.visibility = View.VISIBLE
        binding.fabPoiSearch.alpha = 0f
        binding.fabPoiSearch.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    /**
     * Hiển thị marker tại vị trí đích đã chọn
     */
    private fun showDestinationMarker(location: LatLng) {
        try {
            // Xóa marker đích cũ nếu có
            trackasiaMap.markers.forEach { marker ->
                if (marker.title?.startsWith("Đích:") == true) {
                    trackasiaMap.removeMarker(marker)
                }
            }

            // Thêm marker mới
            val markerOptions = MarkerOptions()
                .position(location)
                .title("Đích: $targetName")
                .snippet(addressAt)

            // Thử sử dụng icon đích đặc biệt với cách xử lý tốt hơn
            try {
                val iconFactory = com.trackasia.android.annotations.IconFactory.getInstance(requireContext())
                val drawable = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker_destination)
                
                if (drawable != null) {
                    // Thay vì dùng tint attribute, tạo một bitmap đã được tô màu
                    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                    
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    
                    val canvas = android.graphics.Canvas(bitmap)
                    drawable.draw(canvas)
                    
                    // Sử dụng bitmap đã tạo để tạo icon
                    markerOptions.icon(iconFactory.fromBitmap(bitmap))
                }
            } catch (e: Exception) {
                Log.e("DESTINATION", "Error setting destination icon", e)
                // Tiếp tục mà không cần đặt icon tùy chỉnh
            }

            // Thêm marker vào bản đồ
            trackasiaMap.addMarker(markerOptions)

            // Hiển thị chỉ dẫn cho người dùng
            showToast("Đã đánh dấu điểm đích: $targetName")
        } catch (e: Exception) {
            Log.e("DESTINATION", "Error showing destination marker", e)
            showToast("Lỗi hiển thị điểm đích")
        }
    }

    /**
     * Tính toán tuyến đường từ vị trí hiện tại đến vị trí đích
     */
    private fun calculateRoute() {
        // Kiểm tra xem đã chọn điểm đích chưa
        if (!targetChoosen || targetLatLng == null) {
            showToast("Vui lòng chọn điểm đến trước")
            return
        }

        // Lấy vị trí hiện tại từ location component
        val currentLocation = trackasiaMap.locationComponent.lastKnownLocation
        if (currentLocation == null) {
            showToast("Không thể xác định vị trí hiện tại của bạn")
            return
        }

        // Hiển thị loading
        loading.show()

        // Hiển thị thông báo cho người dùng
        showToast("Đang tính toán tuyến đường đến $targetName...")

        // Tạo tọa độ điểm đầu và cuối
        val origin = LatLng(currentLocation.latitude, currentLocation.longitude)
        val destination = targetLatLng!!

        // Xây dựng URL cho API chỉ đường
        val url = "https://maps.track-asia.com/route/v1/car/" +
                "${origin.longitude},${origin.latitude};" +
                "${destination.longitude},${destination.latitude}" +
                ".json?geometries=polyline6&steps=true&overview=full&key=public_key"

        Log.d("NAVIGATION", "Requesting route: $url")

        // Thực hiện request API
        val client = OkHttpClient()
        client.newCall(
            okhttp3.Request.Builder()
                .header("User-Agent", "TrackAsia Android Navigation SDK Demo App")
                .url(url)
                .build()
        ).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                activity?.runOnUiThread {
                    loading.dismiss()
                    showToast("Không thể kết nối đến dịch vụ định tuyến: ${e.message}")
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) {
                        activity?.runOnUiThread {
                            loading.dismiss()
                            showToast("Lỗi từ máy chủ: ${response.code}")
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
    
    /**
     * Xử lý kết quả từ API chỉ đường
     */
    private fun processRouteResponse(responseJson: String) {
        Log.d("NAVIGATION", "Processing route response: ${responseJson.take(100)}...")
        
        try {
            val trackasiaResponse = com.trackasia.navigation.core.models.DirectionsResponse.fromJson(responseJson)
            
            if (trackasiaResponse.routes.isEmpty()) {
                activity?.runOnUiThread { 
                    loading.dismiss()
                    showToast("Không tìm thấy tuyến đường nào") 
                }
                return
            }
            
            val firstRoute = trackasiaResponse.routes.first()
            if (firstRoute.geometry.isEmpty()) {
                activity?.runOnUiThread { 
                    loading.dismiss()
                    showToast("Không tìm thấy hình dạng tuyến đường") 
                }
                return
            }
            
            Log.d("NAVIGATION", "Route found: Distance=${firstRoute.distance}m, Duration=${firstRoute.duration}s")
            
            activity?.runOnUiThread {
                loading.dismiss()
                
                // Hiển thị tuyến đường trên bản đồ
                if (navigationMapRoute == null) {
                    navigationMapRoute = com.trackasia.navigation.android.navigation.ui.v5.route.NavigationMapRoute(
                        null, binding.mapView, trackasiaMap
                    )
                }
                
                // Xóa tuyến đường cũ và thêm tuyến đường mới
                navigationMapRoute?.removeRoute()
                navigationMapRoute?.addRoute(firstRoute)
                
                // Hiển thị thông tin tuyến đường
                val distance = formatDistance(firstRoute.distance.toDouble())
                val duration = formatDuration(firstRoute.duration.toLong())
                
                showToast("Đã tìm thấy tuyến đường: $distance, $duration")
                
                // Hiển thị nút bắt đầu điều hướng
                // calculateDirectionButton.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Log.e("NAVIGATION", "Error processing route response", e)
            activity?.runOnUiThread {
                loading.dismiss()
                showToast("Lỗi xử lý dữ liệu tuyến đường: ${e.message}")
            }
        }
    }
    
    /**
     * Định dạng khoảng cách theo đơn vị phù hợp
     */
    private fun formatDistance(distanceInMeters: Double): String {
        return when {
            distanceInMeters < 1000 -> "${distanceInMeters.toInt()} m"
            else -> String.format("%.1f km", distanceInMeters / 1000)
        }
    }
    
    /**
     * Định dạng thời gian theo đơn vị phù hợp
     */
    private fun formatDuration(durationInSeconds: Long): String {
        val hours = durationInSeconds / 3600
        val minutes = (durationInSeconds % 3600) / 60
        val seconds = durationInSeconds % 60
        
        return when {
            hours > 0 -> String.format("%d giờ %d phút", hours, minutes)
            minutes > 0 -> String.format("%d phút", minutes)
            else -> String.format("%d giây", seconds)
        }
    }

    /**
     * Thêm nút tính toán tuyến đường nếu chưa có
     */
    private fun addCalculateDirectionButtonIfNeeded() {
        // Kiểm tra xem nút đã tồn tại chưa
        if (view?.findViewById<Button>(R.id.calculateDirectionButton) != null) {
            return
        }
        
        // Tạo button mới
        val button = Button(context).apply {
            id = View.generateViewId()
            text = "Tính toán tuyến đường"
            setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.colorBlue))
            setTextColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.white))
            visibility = View.GONE
            
            // Thiết lập layout params
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.BOTTOM
                setMargins(32, 0, 32, 32)
            }
            layoutParams = params
            
            // Thiết lập padding
            setPadding(16, 16, 16, 16)
            
            // Thiết lập click listener
            setOnClickListener {
                calculateRoute()
            }
        }
        
        // Đặt ID cho button để có thể tìm kiếm sau này
        button.id = R.id.calculateDirectionButton
        
        // Thêm button vào layout
        val rootView = view as? ViewGroup
        rootView?.addView(button)
    }

    /**
     * Khởi tạo RecyclerView cho danh sách kết quả POI
     */
    private fun initPoiResultsList() {
        Log.d(TAG, "Initializing POI results list")
        try {
            // Tìm các view cần thiết
            val container = view?.findViewById<FrameLayout>(R.id.poiResultsContainer) ?: run {
                Log.e(TAG, "poiResultsContainer not found")
                showToast("Lỗi khởi tạo danh sách: Container không tìm thấy")
                return
            }
            
            val poiResultsLayout = container.findViewById<View>(R.id.poiResultsLayout) ?: run {
                Log.e(TAG, "poiResultsLayout not found")
                showToast("Lỗi khởi tạo danh sách: Layout không tìm thấy")
                return
            }
            
            val recyclerView = poiResultsLayout.findViewById<RecyclerView>(R.id.rvPoiResults) ?: run {
                Log.e(TAG, "rvPoiResults not found")
                showToast("Lỗi khởi tạo danh sách: RecyclerView không tìm thấy")
                return
            }
            
            val btnClose = poiResultsLayout.findViewById<View>(R.id.btnClosePoiResults)
            val btnShowOnMap = poiResultsLayout.findViewById<View>(R.id.btnShowAllMarkersOnMap)

            // Khởi tạo adapter chỉ nếu chưa được khởi tạo
            if (!::poiResultAdapter.isInitialized) {
                poiResultAdapter = PoiResultAdapter(
                    items = ArrayList(poiResults),
                    onItemClick = { poi -> onPoiSelected(poi) },
                    onNavigateClick = { poi -> onPoiNavigate(poi) }
                )
                
                // Thiết lập RecyclerView
                if (recyclerView.layoutManager == null) {
                    recyclerView.layoutManager = LinearLayoutManager(context)
                }
                recyclerView.adapter = poiResultAdapter
                recyclerView.setHasFixedSize(true)
            } else if (poiResultAdapter.itemCount == 0 && poiResults.isNotEmpty()) {
                // Cập nhật dữ liệu nếu cần
                poiResultAdapter.updateData(ArrayList(poiResults))
            }

            // Thiết lập sự kiện click
            btnClose?.setOnClickListener { hidePoiResultsPopup() }
            btnShowOnMap?.setOnClickListener { animateAndShowAllMarkers(btnShowOnMap) }
            
            Log.d(TAG, "POI results list initialized with ${poiResults.size} items")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing POI results list: ${e.message}")
            showToast("Lỗi khởi tạo danh sách")
        }
    }
    
    /**
     * Tạo hiệu ứng cho nút và hiển thị tất cả marker
     */
    private fun animateAndShowAllMarkers(button: View) {
        button.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction {
                        showAllPoiMarkersOnMap()
                        hidePoiResultsPopup()
                    }
                    .start()
            }
            .start()
    }
    
    /**
     * Xử lý khi người dùng chọn một POI
     */
    private fun onPoiSelected(poi: PoiResult) {
        Log.d(TAG, "POI selected: ${poi.name}")
        trackasiaMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(poi.location, 16.0),
            1000
        )
        hidePoiResultsPopup()
    }
    
    /**
     * Xử lý khi người dùng muốn điều hướng đến một POI
     */
    private fun onPoiNavigate(poi: PoiResult) {
        Log.d(TAG, "Navigate to POI: ${poi.name}")
        
        // Lưu thông tin địa điểm đích
        targetLatLng = poi.location
        addressAt = poi.address
        targetName = poi.name
        targetChoosen = true
        
        // Hiển thị marker và di chuyển camera
        showDestinationMarker(poi.location)
        trackasiaMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(poi.location, 16.0),
            1000
        )
        
        // Đóng popup và hiển thị UI điều hướng
        hidePoiResultsPopup()
        showNavigationUI()
    }
    
    /**
     * Hiển thị UI điều hướng sau khi chọn điểm đích
     */
    private fun showNavigationUI() {
        view?.findViewById<Button>(R.id.calculateDirectionButton)?.let { button ->
            button.visibility = View.VISIBLE
            button.setOnClickListener { calculateRoute() }
        }
    }

}