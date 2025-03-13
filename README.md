# **TRACK ASIA TÍCH HỢP**

# Tích hợp TrackAsia Map vào Android

## 1. Cấu hình Gradle

### Root `build.gradle`
Thêm các dependencies và repository cần thiết:

```gradle
buildscript {
    ext.kotlin_version = "1.9.10"
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.4.2'
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version'
    }
}
```

### `gradle-wrapper.properties`
Đảm bảo sử dụng Gradle phiên bản 8:

```
distributionUrl=https\://services.gradle.org/distributions/gradle-8.6-bin.zip
```

### Module `build.gradle`
Thêm dependencies cho TrackAsia SDK và các dịch vụ liên quan:

```gradle
dependencies {
    implementation('io.github.track-asia:android-sdk:2.0.1')
    implementation('io.github.track-asia:android-sdk-geojson:2.0.1')
    implementation('io.github.track-asia:android-sdk-turf:2.0.1')
    implementation('io.github.track-asia:android-plugin-annotation-v9:2.0.1')
    implementation('io.github.track-asia:libandroid-navigation:2.0.0')
    implementation('io.github.track-asia:libandroid-navigation-ui:2.0.0')
}
```

## 2. Triển khai `MapView` trong XML
Thêm `MapView` vào file layout XML:

```xml
<com.trackasia.android.maps.MapView
    android:id="@+id/mapView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:trackasia_cameraZoom="12"
    app:trackasia_enableTilePrefetch="true"
    app:trackasia_enableZMediaOverlay="true"
    app:trackasia_renderTextureMode="true"
    app:trackasia_renderTextureTranslucentSurface="true"
    app:trackasia_uiAttribution="true"
    app:trackasia_uiCompass="true"
    app:trackasia_uiDoubleTapGestures="true"
    app:trackasia_uiLogo="true"
    app:trackasia_uiRotateGestures="true"
    app:trackasia_uiScrollGestures="true"
    app:trackasia_uiTiltGestures="true"
    app:trackasia_uiZoomGestures="true" />
```

## 3. Khởi tạo TrackAsia Map trong Activity/Fragment

### Import thư viện cần thiết:

```kotlin
import com.trackasia.android.maps.MapView
import com.trackasia.android.maps.TrackAsiaMap
import com.trackasia.android.maps.CameraPosition
import com.trackasia.android.maps.Style
import com.trackasia.android.geometry.LatLng
import com.trackasia.android.TrackAsia
import com.trackasia.android.navigation.ui.NavigationMapRoute
```

### Khai báo biến:

```kotlin
private lateinit var mapboxMap: TrackAsiaMap
private var styleUrl = "https://maps.track-asia.com/styles/v1/streets.json?key=public_key"
private lateinit var navigationMapRoute: NavigationMapRoute
```

### Khởi tạo TrackAsia trong `onCreateView()`:

```kotlin
TrackAsia.getInstance(requireActivity())
```

### Thiết lập bản đồ trong `onViewCreated()`:

```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    
    mapView.onCreate(savedInstanceState)
    mapView.getMapAsync { map ->
        this.mapboxMap = map
        
        map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
            enableLocationComponent(style)
        }
        
        navigationMapRoute = NavigationMapRoute(mapView, map)
        
        val latlng = LatLng(10.728073, 106.624054)
        map.cameraPosition = CameraPosition.Builder().target(latlng).zoom(12.0).build()
    }
}
```

## 4. Quản lý vòng đời của `MapView`
Để tránh lỗi bộ nhớ, cần gọi các phương thức vòng đời tương ứng:

```kotlin
override fun onStart() {
    super.onStart()
    mapView.onStart()
}

override fun onResume() {
    super.onResume()
    mapView.onResume()
}

override fun onPause() {
    super.onPause()
    mapView.onPause()
}

override fun onStop() {
    super.onStop()
    mapView.onStop()
}

override fun onDestroy() {
    super.onDestroy()
    mapView.onDestroy()
}

override fun onLowMemory() {
    super.onLowMemory()
    mapView.onLowMemory()
}

override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    mapView.onSaveInstanceState(outState)
}
```

## 5. Hình ảnh Sample

<p align="center">
  <img src="https://git.advn.vn/sangnguyen/trackasia-document/-/raw/master/images/android_1.JPEG" alt="Android" width="18%">   
  <img src="https://git.advn.vn/sangnguyen/trackasia-document/-/raw/master/images/android_2.JPEG" alt="Android" width="18%">
  <img src="https://git.advn.vn/sangnguyen/trackasia-document/-/raw/master/images/android_3.JPEG" alt="Android" width="18%">
  <img src="https://git.advn.vn/sangnguyen/trackasia-document/-/raw/master/images/android_4.JPEG" alt="Android" width="18%">
  <img src="https://git.advn.vn/sangnguyen/trackasia-document/-/raw/master/images/android_5.JPEG" alt="Android" width="18%">
  <img src="https://git.advn.vn/sangnguyen/trackasia-document/-/raw/master/images/android_6.JPEG" alt="Android" width="18%">
  <img src="https://git.advn.vn/sangnguyen/trackasia-document/-/raw/master/images/android_7.JPEG" alt="Android" width="18%">
</p>


## 6. Link Github Core
```kotlin

[⭐️ TrackAsia Java - Chứa các thư viện hỗ trợ Map](https://github.com/track-asia/trackasia-java)

[⭐️ TrackAsia Native - Chứa các thư viện core deploy chính của Map Chọn Android](https://github.com/track-asia/trackasia-native)

[⭐️ TrackAsia Navigation - Chứa các thư viện Navigation, Directions của Map](https://github.com/track-asia/trackasia-navigation-android)

```

## 7. Kết luận
```kotlin
Với hướng dẫn trên, bạn đã có thể tích hợp TrackAsia Map vào ứng dụng Android, thiết lập bản đồ với giao diện tuỳ chỉnh, và quản lý vòng đời của `MapView` đúng cách. Bạn có thể mở rộng tính năng như hiển thị marker, vẽ tuyến đường, và sử dụng navigation bằng cách tích hợp thêm các API của TrackAsia.
```