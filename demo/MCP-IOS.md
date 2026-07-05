# Android Project Configuration Analysis and iOS Mapping

## 1. .cursorrules File Analysis

The `.cursorrules` file contains IDE configuration for the Cursor editor, specifying:

- **Code Style Rules**: Kotlin version (1.8), indentation (4 spaces), max line length (120), auto imports
- **Build Configuration**: Java compatibility (1.8)
- **File Templates**: Includes custom snippets for Kotlin activities, fragments, and map implementations
- **Exclusion Patterns**: Build directories, IDE files, and other unnecessary files
- **Quick Actions**: Format, organize imports, run current file

## 2. Android Configuration Files

### 2.1 Build Configuration (build.gradle)

- **Root build.gradle**:
  - Kotlin version: 1.9.10
  - Google Maps Services: 4.4.0
  - Secrets Gradle Plugin: 2.0.1

- **App build.gradle**:
  - Min SDK: 26
  - Target SDK: 35
  - Supported ABIs: armeabi-v7a, arm64-v8a, x86, x86_64
  - Key dependencies:
    - Network: Retrofit 2.9.0, OkHttp 4.9.3
    - Google Maps: play-services-maps 18.0.2
    - TrackAsia SDK components: android-sdk 2.0.2, navigation-ui-android 2.0.2

### 2.2 AndroidManifest.xml

- **Permissions**:
  - Internet
  - ACCESS_WIFI_STATE
  - POST_NOTIFICATIONS
  - READ_PHONE_STATE
  - ACCESS_NETWORK_STATE
  - WAKE_LOCK
  - FOREGROUND_SERVICE
  - CAMERA

- **Google API Key**: 
  - Map API key in meta-data: `AIzaSyA0T9_RVH4Jva3pKhenF5G059Pa7G0KxWE`

## 3. Mobile Configuration Profile (MCP)

### 3.1 API Endpoints (Constants.kt)

- **Base URLs** for different regions:
  - Vietnam: https://maps.track-asia.com/
  - Singapore: https://sg-maps.track-asia.com/
  - Thailand: https://th-maps.track-asia.com/
  - Taiwan: https://tw-maps.track-asia.com/
  - Malaysia: https://my-maps.track-asia.com/

- **Map Styles**:
  - Streets style
  - Satellite style
  - Night style
  - Simple style
  - Terrain style

- **API Access**: Most endpoints use a public key (`?key=public` or `?key=public_key`)

### 3.2 Network Configuration (RetrofitClient.kt)

- Uses OkHttp with 10-second read timeout and 5-second connect timeout
- Includes logging interceptor for debugging
- Uses Retrofit with Gson converter and RxJava2 adapter

### 3.3 API Services (APIService.kt)

- **Reverse Geocoding**: `/api/v1/reverse?key=public_key`
- **Autocomplete**: `/api/v1/autocomplete?lang=vi&key=public_key`

### 3.4 Firebase Configuration (google-services.json)

- **Project ID**: tracking-fcfd0
- **Firebase URL**: https://tracking-fcfd0.firebaseio.com
- **Storage Bucket**: tracking-fcfd0.appspot.com
- **API Key**: AIzaSyBwkQUArVSc1fh-xweGuiLjJQaf4TA-PTM
- **Multiple client apps** configured with different package names

### 3.5 MapBox Access Token

- Value: `YOUR_TRACKASIA_ACCESS_TOKEN`

## 4. iOS Mapping Recommendations

### 4.1 Project Configuration

| Android | iOS Equivalent |
|---------|----------------|
| build.gradle | Podfile & Project settings |
| AndroidManifest.xml | Info.plist |
| .cursorrules | .swiftlint.yml (for code style) |

### 4.2 Permissions Mapping

| Android Permission | iOS Permission Key |
|---------|----------------|
| INTERNET | No explicit permission needed |
| ACCESS_WIFI_STATE | No direct equivalent |
| POST_NOTIFICATIONS | NSUserNotificationsUsageDescription |
| READ_PHONE_STATE | No direct equivalent |
| ACCESS_NETWORK_STATE | No explicit permission needed |
| CAMERA | NSCameraUsageDescription |

### 4.3 Library Mapping

| Android Library | iOS Equivalent |
|---------|----------------|
| Retrofit + OkHttp | Alamofire |
| Gson | Swift Codable or SwiftyJSON |
| RxJava | RxSwift + RxCocoa |
| Google Maps | Google Maps iOS SDK or MapKit |
| TrackAsia SDK | MapBox iOS SDK or customize MapKit |
| Timber | CocoaLumberjack or os_log |

### 4.4 Configuration File Mapping

| Android File | iOS Equivalent |
|---------|----------------|
| google-services.json | GoogleService-Info.plist |
| developer-config.xml | Configuration.plist or settings bundle |
| colors.xml | UIColor extensions or Asset catalog color sets |
| strings.xml | Localizable.strings |
| dimens.xml | Design constants in Swift |

## 5. iOS Implementation

### 5.1 Alamofire Setup (equivalent to RetrofitClient)

```swift
import Alamofire

class NetworkManager {
    static let shared = NetworkManager()
    
    private init() {}
    
    private let session: Session = {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 10
        configuration.timeoutIntervalForResource = 10
        return Session(configuration: configuration)
    }()
    
    func request<T: Decodable>(_ urlConvertible: URLConvertible, 
                             method: HTTPMethod = .get, 
                             parameters: Parameters? = nil,
                             completion: @escaping (Result<T, Error>) -> Void) {
        session.request(urlConvertible, method: method, parameters: parameters)
            .validate()
            .responseDecodable(of: T.self) { response in
                switch response.result {
                case .success(let value):
                    completion(.success(value))
                case .failure(let error):
                    completion(.failure(error))
                }
            }
    }
}
```

### 5.2 API Constants (equivalent to Constants.kt)

```swift
struct APIConstants {
    // Base URLs
    static let baseUrlVN = "https://maps.track-asia.com/"
    static let baseUrlSG = "https://sg-maps.track-asia.com/"
    static let baseUrlTH = "https://th-maps.track-asia.com/"
    static let baseUrlTW = "https://tw-maps.track-asia.com/"
    static let baseUrlMI = "https://my-maps.track-asia.com/"
    
    // Style URLs
    static let styleUrlVN = "https://maps.track-asia.com/styles/v2/streets.json?key=public"
    static let styleUrlSG = "https://sg-maps.track-asia.com/styles/v2/streets.json?key=public"
    // Add other style URLs similarly
    
    // API Endpoints
    static let reverseGeocode = "/api/v1/reverse"
    static let autocomplete = "/api/v1/autocomplete"
}
```

### 5.3 API Services (equivalent to APIService.kt)

```swift
struct GeocodingService {
    static func reverseGeocode(longitude: Double, latitude: Double, language: String = "en", completion: @escaping (Result<GeocodingResponse, Error>) -> Void) {
        let url = APIConstants.baseUrlVN + APIConstants.reverseGeocode
        let parameters: [String: Any] = [
            "key": "public_key", 
            "lang": language,
            "point.lon": longitude,
            "point.lat": latitude
        ]
        
        NetworkManager.shared.request(url, parameters: parameters, completion: completion)
    }
    
    static func getSuggestions(text: String, completion: @escaping (Result<AutoSuggestionResponse, Error>) -> Void) {
        let url = APIConstants.baseUrlVN + APIConstants.autocomplete
        let parameters: [String: Any] = [
            "key": "public_key",
            "lang": "vi",
            "text": text
        ]
        
        NetworkManager.shared.request(url, parameters: parameters, completion: completion)
    }
}
```

### 5.4 Model Definitions (equivalent to GeocodingModel.kt)

```swift
struct GeocodingResponse: Codable {
    let type: String
    let features: [Feature]
}

struct Feature: Codable {
    let properties: Properties
    let geometry: Geometry
}

struct Properties: Codable {
    let name: String
    let street: String
    let distance: Double
    let country: String
    let region: String
    let county: String
    let locality: String
    let label: String
}

struct Geometry: Codable {
    let coordinates: [Double]
}

struct AutoSuggestionResponse: Codable {
    let features: [Feature]
}
```

### 5.5 Firebase Configuration (GoogleService-Info.plist)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>API_KEY</key>
    <string>AIzaSyBwkQUArVSc1fh-xweGuiLjJQaf4TA-PTM</string>
    <key>BUNDLE_ID</key>
    <string>com.trackasia.sample</string>
    <key>CLIENT_ID</key>
    <string>817120621812-2dl74jg778bnhfh7v17dt4rvj1jf6ui0.apps.googleusercontent.com</string>
    <key>GCM_SENDER_ID</key>
    <string>817120621812</string>
    <key>PLIST_VERSION</key>
    <string>1</string>
    <key>PROJECT_ID</key>
    <string>tracking-fcfd0</string>
    <key>STORAGE_BUCKET</key>
    <string>tracking-fcfd0.appspot.com</string>
</dict>
</plist>
```

### 5.6 Info.plist Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- App Transport Security -->
    <key>NSAppTransportSecurity</key>
    <dict>
        <key>NSAllowsArbitraryLoads</key>
        <true/>
    </dict>
    
    <!-- Permission descriptions -->
    <key>NSCameraUsageDescription</key>
    <string>This app needs camera access to take photos for your profile</string>
    <key>NSLocationWhenInUseUsageDescription</key>
    <string>This app needs access to your location to show it on the map</string>
    <key>NSUserNotificationsUsageDescription</key>
    <string>This app needs to send you notifications about map updates</string>
    
    <!-- Google Maps API Key -->
    <key>GMSAPIKey</key>
    <string>AIzaSyA0T9_RVH4Jva3pKhenF5G059Pa7G0KxWE</string>
    
    <!-- TrackAsia Configuration -->
    <key>MGLMapboxAccessToken</key>
    <string>YOUR_TRACKASIA_ACCESS_TOKEN</string>
</dict>
</plist>
```

### 5.7 Podfile

```ruby
platform :ios, '13.0'

target 'TrackAsiaSample' do
  use_frameworks!

  # Networking
  pod 'Alamofire', '~> 5.4'
  
  # Maps
  pod 'Mapbox-iOS-SDK', '~> 6.4.0'
  pod 'GoogleMaps', '~> 6.0.0'
  
  # Firebase
  pod 'Firebase/Core'
  pod 'Firebase/Analytics'
  
  # UI Components
  pod 'SnapKit', '~> 5.0.0'
  
  # Logging
  pod 'CocoaLumberjack/Swift', '~> 3.7.0'
  
  # Reactive
  pod 'RxSwift', '~> 6.0'
  pod 'RxCocoa', '~> 6.0'
end
```

## 6. Implementation Steps for iOS

1. **Setup Xcode Project**:
   - Create a new iOS project with Swift
   - Configure bundle identifier to match Android's "com.trackasia.sample"
   - Set minimum iOS version (equivalent to Android's API 26, roughly iOS 13)

2. **Dependency Management**:
   - Add Podfile with required dependencies
   - Run `pod install` to set up workspace

3. **Configuration Files**:
   - Create Info.plist with necessary permissions and settings
   - Add GoogleService-Info.plist for Firebase

4. **Network Layer**:
   - Implement NetworkManager class and API services
   - Create model structures using Codable

5. **Map Integration**:
   - Set up MapBox or Google Maps SDK
   - Configure map styles and initial region
   - Implement map features (clusters, waypoints, etc.)

6. **User Interface**:
   - Create equivalent view controllers for Android fragments
   - Implement navigation structure (tab bar or side menu)

7. **Testing**:
   - Test all API endpoints and mapping functions
   - Verify proper permission handling

## 7. Conclusion

The Android application is a mapping demo that uses the TrackAsia SDK with multiple features:
- Map visualization with different styles
- Location-based services (geocoding, reverse geocoding)
- Routing and navigation
- Points of interest and waypoints
- Map feature visualization and clustering

For iOS implementation, the best approach is to use MapBox iOS SDK with custom styling to match TrackAsia's functionality, while maintaining the same API endpoints and configuration parameters.

## 8. Screen-by-Screen Implementation Guide

### 8.1 Main Screen (MainActivity → MainViewController)

#### Android Implementation
The Android app uses `MainActivity.kt` as the entry point with fragments for different map features.

#### iOS Implementation
1. Create a `MainViewController` with a UITableView for feature selection
2. Implement navigation to different map feature screens
3. Use example code:
   ```swift
   class MainViewController: UIViewController, UITableViewDelegate, UITableViewDataSource {
       private var mapSelectionTableView: UITableView!
       private let mapFeatures = ["Single Point Map", "Multi Point Map", "Waypoint Map", "Cluster Map", "Animation Map", "Feature Map"]
       
       override func viewDidLoad() {
           super.viewDidLoad()
           setupUI()
       }
       
       // Configure UI and tableView
       
       func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
           // Navigate to appropriate view controller based on selection
       }
   }
   ```

### 8.2 Single Point Map (MapSinglePointFragment → SinglePointMapViewController)

#### Android Implementation
Displays a single marker on the map with info window, implemented in `MapSinglePointFragment.kt`.

#### iOS Implementation
1. Create a view controller with a MapBox map view
2. Add tap gesture recognition to place markers
3. Implement reverse geocoding
4. Display coordinate information

### 8.3 Multi Point Map (MapMultiPointFragment → MultiPointMapViewController)

#### Android Implementation
Displays multiple markers and calculates routes between them.

#### iOS Implementation
1. Create a view controller with map view
2. Add tap gesture to add multiple points
3. Implement route calculation using the TrackAsia routing API
4. Draw polylines between points

### 8.4 Cluster Map (MapClusterFragment → ClusterMapViewController)

#### Android Implementation
Groups multiple markers into clusters when zoomed out.

#### iOS Implementation
1. Use MapBox's built-in clustering capabilities
2. Create custom cluster layouts to match Android implementation
3. Handle cluster tap events

### 8.5 Waypoint Map (MapWayPointFragment → WaypointMapViewController)

#### Android Implementation
Allows adding and dragging waypoints to calculate routes.

#### iOS Implementation
1. Create draggable annotations for waypoints
2. Implement route calculation between waypoints
3. Add UI controls for route options

### 8.6 Animation Map (MapAnimationFragment → AnimationMapViewController)

#### Android Implementation
Demonstrates marker animations and camera movements.

#### iOS Implementation
1. Use CABasicAnimation for marker animations
2. Implement camera transitions for map movements
3. Add timing controls for animations

### 8.7 Feature Map (MapFeatureFragment → FeatureMapViewController)

#### Android Implementation
Shows GeoJSON features on the map.

#### iOS Implementation
1. Add GeoJSON feature support
2. Implement feature styling and interaction
3. Add feature selection capability

## 9. Common Components

### 9.1 RouteManager

```swift
class RouteManager {
    static let shared = RouteManager()
    
    func calculateRoute(waypoints: [CLLocationCoordinate2D], completion: @escaping (Result<[CLLocationCoordinate2D], Error>) -> Void) {
        // Build coordinates string
        let coordinatesString = waypoints.map { "\($0.longitude),\($0.latitude)" }.joined(separator: ";")
        
        // Implement the routing API request using the same endpoints as Android
    }
}
```

### 9.2 StyleManager

```swift
class StyleManager {
    enum MapStyle {
        case streets, satellite, night, simple, terrain
        
        var url: String {
            switch self {
            case .streets: return APIConstants.styleUrlVN
            case .satellite: return APIConstants.urlStyle3DVN
            // Other cases...
            }
        }
    }
    
    static func applyStyle(_ style: MapStyle, to mapView: MapView) {
        // Apply the selected style to the map
    }
}
```

### 9.3 LocationPermissionManager

```swift
class LocationPermissionManager {
    static let shared = LocationPermissionManager()
    
    func requestLocationPermission(completion: @escaping (Bool) -> Void) {
        // Handle iOS-specific location permissions
    }
}
```

## 10. Resources and Assets

For a complete iOS implementation, include:
1. Map marker icons in Assets.xcassets
2. Custom pins for different point types
3. Localization files
4. Gesture recognizers for map interactions

## 11. Phân tích chi tiết cấu trúc dự án

### 11.1 Cấu trúc thư mục chính

Dự án Android có cấu trúc thư mục sau:
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/trackasia/sample/
│   │   │   ├── adapter/
│   │   │   ├── api/
│   │   │   │   ├── Constants.kt
│   │   │   │   ├── model/
│   │   │   │   ├── repository/
│   │   │   │   ├── RetrofitClient.kt
│   │   │   │   └── service/
│   │   │   ├── custom/
│   │   │   ├── model/
│   │   │   ├── navigation/
│   │   │   ├── utils/
│   │   │   ├── MainActivity.kt
│   │   │   ├── MainViewModel.kt
│   │   │   ├── MapSinglePointFragment.kt
│   │   │   ├── MapMultiPointFragment.kt
│   │   │   ├── MapClusterFragment.kt
│   │   │   ├── MapAnimationFragment.kt
│   │   │   ├── MapFeatureFragment.kt
│   │   │   ├── MapFeatureSnapshotFragment.kt
│   │   │   ├── MapWayPointFragment.kt
│   │   │   └── MapDirectionPointFragment.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
```

### 11.2 Phân tích chi tiết các màn hình chính

#### 11.2.1 MainActivity

`MainActivity.kt` là điểm vào chính của ứng dụng, có trách nhiệm:
- Quản lý và điều hướng giữa các Fragment bản đồ
- Hiển thị thanh công cụ (toolbar) với chọn quốc gia (VN, Singapore, Thailand)
- Xử lý bottom navigation với 5 tùy chọn: Single Point, Multi-Point, Clusters, Animation, Features

Cấu trúc UI bao gồm:
1. AppBarLayout với thông tin quốc gia và tiêu đề
2. NavHostFragment để chứa các fragment bản đồ
3. BottomNavigationView với 5 tab điều hướng

**Triển khai iOS:**
```swift
import UIKit

class MainViewController: UIViewController {
    private var countryLabel: UILabel!
    private var titleLabel: UILabel!
    private var containerView: UIView!
    private var tabBar: UITabBar!
    private var childViewControllers: [UIViewController] = []
    private var currentViewController: UIViewController?
    private var preferences: UserDefaults!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Khởi tạo UserDefaults
        preferences = UserDefaults.standard
        let country = preferences.string(forKey: "country") ?? "vn"
        
        // Thiết lập UI
        setupUI(country: country)
        
        // Khởi tạo ViewControllers
        setupViewControllers()
        
        // Hiển thị màn hình đầu tiên (Single Point)
        showViewController(at: 0)
    }
    
    private func setupUI(country: String) {
        // Tạo phần header
        let headerView = UIView()
        headerView.backgroundColor = UIColor(named: "colorBlue")
        headerView.layer.cornerRadius = 12
        
        // Logo và tên app
        let logoImageView = UIImageView(image: UIImage(named: "ic_navigation_24"))
        logoImageView.tintColor = .white
        
        let appNameLabel = UILabel()
        appNameLabel.text = "TrackAsia"
        appNameLabel.textColor = .white
        appNameLabel.font = UIFont.boldSystemFont(ofSize: 16)
        
        titleLabel = UILabel()
        titleLabel.textColor = .white
        titleLabel.font = UIFont.systemFont(ofSize: 15)
        
        // Label hiển thị quốc gia
        countryLabel = UILabel()
        countryLabel.text = getCountryName(from: country)
        countryLabel.textColor = UIColor(named: "colorBlue")
        countryLabel.backgroundColor = .white
        countryLabel.font = UIFont.boldSystemFont(ofSize: 14)
        countryLabel.textAlignment = .center
        countryLabel.layer.cornerRadius = 8
        countryLabel.layer.masksToBounds = true
        
        // Thêm sự kiện cho countryLabel
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(showCountrySelector))
        countryLabel.isUserInteractionEnabled = true
        countryLabel.addGestureRecognizer(tapGesture)
        
        // Container cho các fragment
        containerView = UIView()
        
        // TabBar
        tabBar = UITabBar()
        tabBar.delegate = self
        
        let singlePointItem = UITabBarItem(title: "Single Point", image: UIImage(named: "ic_map_single"), tag: 0)
        let multiPointItem = UITabBarItem(title: "Multi-Point", image: UIImage(named: "ic_map_multi"), tag: 1)
        let clustersItem = UITabBarItem(title: "Clusters", image: UIImage(named: "ic_map_cluster"), tag: 2)
        let animationItem = UITabBarItem(title: "Animation", image: UIImage(named: "ic_map_animation"), tag: 3)
        let featuresItem = UITabBarItem(title: "Features", image: UIImage(named: "ic_feature"), tag: 4)
        
        tabBar.items = [singlePointItem, multiPointItem, clustersItem, animationItem, featuresItem]
        tabBar.selectedItem = singlePointItem
        
        // Add subviews and setup constraints
        // ...
    }
    
    private func setupViewControllers() {
        // Khởi tạo các view controller
        let singlePointVC = SinglePointMapViewController()
        let multiPointVC = MultiPointMapViewController()
        let clusterMapVC = ClusterMapViewController()
        let animationMapVC = AnimationMapViewController()
        let featureMapVC = FeatureMapViewController()
        
        childViewControllers = [singlePointVC, multiPointVC, clusterMapVC, animationMapVC, featureMapVC]
    }
    
    private func showViewController(at index: Int) {
        // Remove current view controller
        if let currentVC = currentViewController {
            currentVC.willMove(toParent: nil)
            currentVC.view.removeFromSuperview()
            currentVC.removeFromParent()
        }
        
        // Add new view controller
        let newVC = childViewControllers[index]
        addChild(newVC)
        containerView.addSubview(newVC.view)
        newVC.view.frame = containerView.bounds
        newVC.didMove(toParent: self)
        currentViewController = newVC
        
        // Update title
        titleLabel.text = tabBar.items?[index].title
    }
    
    @objc private func showCountrySelector() {
        let alertController = UIAlertController(title: "Chọn quốc gia", message: nil, preferredStyle: .actionSheet)
        
        let vietnamAction = UIAlertAction(title: "Việt Nam", style: .default) { [weak self] _ in
            self?.updateCountry("vn", name: "Việt Nam")
        }
        
        let singaporeAction = UIAlertAction(title: "Singapore", style: .default) { [weak self] _ in
            self?.updateCountry("sg", name: "Singapore")
        }
        
        let thailandAction = UIAlertAction(title: "Thailand", style: .default) { [weak self] _ in
            self?.updateCountry("th", name: "Thailand")
        }
        
        let cancelAction = UIAlertAction(title: "Hủy", style: .cancel)
        
        alertController.addAction(vietnamAction)
        alertController.addAction(singaporeAction)
        alertController.addAction(thailandAction)
        alertController.addAction(cancelAction)
        
        present(alertController, animated: true)
    }
    
    private func updateCountry(_ code: String, name: String) {
        preferences.set(code, forKey: "country")
        countryLabel.text = name
        
        // Refresh current view controller
        if let index = tabBar.items?.firstIndex(of: tabBar.selectedItem!) {
            // Tạo mới view controller với cấu hình quốc gia mới
            setupViewControllers()
            showViewController(at: index)
        }
    }
    
    private func getCountryName(from code: String) -> String {
        switch code {
        case "vn": return "Việt Nam"
        case "sg": return "Singapore"
        case "th": return "Thailand"
        default: return "Việt Nam"
        }
    }
}

// MARK: - UITabBarDelegate
extension MainViewController: UITabBarDelegate {
    func tabBar(_ tabBar: UITabBar, didSelect item: UITabBarItem) {
        showViewController(at: item.tag)
    }
}
```

#### 11.2.2 MapSinglePointFragment

`MapSinglePointFragment.kt` hiển thị một điểm trên bản đồ với các tính năng:
- Hiển thị điểm hiện tại của người dùng
- Đặt marker khi chạm vào bản đồ
- Tìm kiếm địa điểm và geocoding ngược
- Hiển thị thông tin địa điểm khi chọn

**Triển khai iOS chi tiết:**
```swift
import UIKit
import MapboxMaps

class SinglePointMapViewController: UIViewController {
    private var mapView: MapView!
    private var searchTextField: UITextField!
    private var myLocationButton: UIButton!
    private var coordinateLabel: UILabel!
    private var searchResultsTableView: UITableView!
    private var calculateDirectionButton: UIButton!
    
    private var suggestions: [Feature] = []
    private var marker: PointAnnotation?
    private var pointAnnotationManager: PointAnnotationManager!
    private var currentLocation: CLLocationCoordinate2D?
    private var targetLocation: CLLocationCoordinate2D?
    private var targetName: String = "Điểm đến"
    
    private var preferences: UserDefaults!
    private var countryCode: String = "vn"
    private var initialCenter: CLLocationCoordinate2D = CLLocationCoordinate2D(latitude: 10.728073, longitude: 106.624054)
    private var zoomLevel: Double = 16.0
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Khởi tạo preferences
        preferences = UserDefaults.standard
        countryCode = preferences.string(forKey: "country") ?? "vn"
        
        // Cấu hình vị trí ban đầu dựa trên quốc gia
        setupInitialLocation()
        
        // Thiết lập UI
        setupUI()
        
        // Khởi tạo bản đồ
        setupMapView()
        
        // Thiết lập gesture recognizers
        setupGestureRecognizers()
        
        // Thiết lập các listener
        setupListeners()
    }
    
    private func setupMapView() {
        // Khởi tạo MapboxMaps với style URL phù hợp
        let styleURL = getStyleURL(for: countryCode)
        
        let options = MapInitOptions(
            resourceOptions: ResourceOptions(accessToken: APIConstants.mapboxAccessToken),
            cameraOptions: CameraOptions(center: initialCenter, zoom: zoomLevel),
            styleURI: StyleURI(url: URL(string: styleURL)!)
        )
        
        mapView = MapView(frame: view.bounds, mapInitOptions: options)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(mapView)
        view.sendSubviewToBack(mapView)
        
        // Khởi tạo annotation manager
        pointAnnotationManager = mapView.annotations.makePointAnnotationManager()
        
        // Yêu cầu quyền vị trí và hiển thị vị trí người dùng
        requestLocationPermission()
    }
    
    private func setupUI() {
        // Tạo SearchTextField
        searchTextField = UITextField()
        searchTextField.placeholder = "Tìm địa điểm..."
        searchTextField.backgroundColor = .white
        searchTextField.layer.cornerRadius = 8
        searchTextField.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 10, height: searchTextField.frame.height))
        searchTextField.leftViewMode = .always
        searchTextField.clearButtonMode = .whileEditing
        
        // Tạo nút vị trí
        myLocationButton = UIButton(type: .system)
        myLocationButton.setImage(UIImage(systemName: "location.fill"), for: .normal)
        myLocationButton.backgroundColor = .white
        myLocationButton.layer.cornerRadius = 20
        
        // Label hiển thị thông tin tọa độ
        coordinateLabel = UILabel()
        coordinateLabel.backgroundColor = UIColor.black.withAlphaComponent(0.7)
        coordinateLabel.textColor = .white
        coordinateLabel.textAlignment = .center
        coordinateLabel.layer.cornerRadius = 8
        coordinateLabel.layer.masksToBounds = true
        coordinateLabel.numberOfLines = 0
        coordinateLabel.isHidden = true
        
        // TableView kết quả tìm kiếm
        searchResultsTableView = UITableView()
        searchResultsTableView.backgroundColor = .white
        searchResultsTableView.layer.cornerRadius = 8
        searchResultsTableView.isHidden = true
        searchResultsTableView.register(UITableViewCell.self, forCellReuseIdentifier: "Cell")
        
        // Nút tính toán tuyến đường
        calculateDirectionButton = UIButton(type: .system)
        calculateDirectionButton.setTitle("Tính tuyến đường", for: .normal)
        calculateDirectionButton.backgroundColor = UIColor(named: "colorBlue")
        calculateDirectionButton.setTitleColor(.white, for: .normal)
        calculateDirectionButton.layer.cornerRadius = 8
        calculateDirectionButton.isHidden = true
        
        // Thêm các view vào view chính
        view.addSubview(searchTextField)
        view.addSubview(myLocationButton)
        view.addSubview(coordinateLabel)
        view.addSubview(searchResultsTableView)
        view.addSubview(calculateDirectionButton)
        
        // Thiết lập constraints
        // ...
    }
    
    @objc private func handleMapTap(_ gesture: UITapGestureRecognizer) {
        // Ẩn bàn phím nếu đang hiển thị
        searchTextField.resignFirstResponder()
        
        // Lấy vị trí tap
        let point = gesture.location(in: mapView)
        let coordinate = mapView.mapboxMap.coordinate(for: point)
        
        // Đặt marker và hiển thị thông tin
        addMarker(at: coordinate)
        performReverseGeocoding(at: coordinate)
    }
    
    private func addMarker(at coordinate: CLLocationCoordinate2D) {
        // Xóa marker hiện tại nếu có
        pointAnnotationManager.annotations = []
        
        // Tạo marker mới
        var annotation = PointAnnotation(coordinate: coordinate)
        annotation.image = .init(image: UIImage(named: "map_marker")!, name: "marker")
        
        pointAnnotationManager.annotations = [annotation]
        marker = annotation
        targetLocation = coordinate
        
        // Hiển thị nút chỉ đường
        calculateDirectionButton.isHidden = false
    }
    
    private func performReverseGeocoding(at coordinate: CLLocationCoordinate2D) {
        GeocodingService.reverseGeocode(
            longitude: coordinate.longitude,
            latitude: coordinate.latitude,
            language: "vi"
        ) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let response):
                    if let feature = response.features.first {
                        self?.targetName = feature.properties.name
                        self?.coordinateLabel.text = """
                        \(feature.properties.name)
                        \(feature.properties.street), \(feature.properties.locality)
                        Lat: \(coordinate.latitude.rounded(toPlaces: 6)), Lng: \(coordinate.longitude.rounded(toPlaces: 6))
                        """
                        self?.coordinateLabel.isHidden = false
                    }
                case .failure(let error):
                    self?.coordinateLabel.text = """
                    Vị trí không xác định
                    Lat: \(coordinate.latitude.rounded(toPlaces: 6)), Lng: \(coordinate.longitude.rounded(toPlaces: 6))
                    """
                    self?.coordinateLabel.isHidden = false
                    print("Geocoding error: \(error.localizedDescription)")
                }
            }
        }
    }
    
    @objc private func myLocationButtonTapped() {
        if let userLocation = currentLocation {
            // Di chuyển camera đến vị trí người dùng
            mapView.camera.ease(
                to: CameraOptions(center: userLocation, zoom: zoomLevel),
                duration: 0.5
            )
            
            // Đặt marker tại vị trí người dùng
            addMarker(at: userLocation)
            performReverseGeocoding(at: userLocation)
        } else {
            // Yêu cầu quyền vị trí nếu chưa có
            requestLocationPermission()
        }
    }
    
    @objc private func searchTextChanged() {
        // Xử lý thay đổi text tìm kiếm
        guard let query = searchTextField.text, query.count >= 2 else {
            searchResultsTableView.isHidden = true
            return
        }
        
        // Gọi API autocomplete
        GeocodingService.getSuggestions(text: query) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let response):
                    self?.suggestions = response.features
                    self?.searchResultsTableView.reloadData()
                    self?.searchResultsTableView.isHidden = false
                case .failure:
                    self?.suggestions = []
                    self?.searchResultsTableView.isHidden = true
                }
            }
        }
    }
    
    @objc private func calculateDirectionButtonTapped() {
        guard let userLocation = currentLocation, let destination = targetLocation else {
            showAlert(title: "Lỗi", message: "Không thể tính toán tuyến đường. Vui lòng chọn điểm đến.")
            return
        }
        
        // Chuyển sang màn hình chỉ đường
        let routeVC = RouteViewController()
        routeVC.startLocation = userLocation
        routeVC.endLocation = destination
        routeVC.destinationName = targetName
        
        present(routeVC, animated: true)
    }
    
    private func getStyleURL(for countryCode: String) -> String {
        switch countryCode {
        case "sg": return APIConstants.styleUrlSG
        case "th": return APIConstants.styleUrlTH
        default: return APIConstants.styleUrlVN
        }
    }
    
    private func setupInitialLocation() {
        switch countryCode {
        case "sg":
            initialCenter = CLLocationCoordinate2D(latitude: 1.352083, longitude: 103.819839)
            zoomLevel = 12.0
        case "th":
            initialCenter = CLLocationCoordinate2D(latitude: 13.7563, longitude: 100.5018)
            zoomLevel = 11.0
        default: // vn
            initialCenter = CLLocationCoordinate2D(latitude: 10.728073, longitude: 106.624054)
            zoomLevel = 10.0
        }
    }
    
    private func requestLocationPermission() {
        // Implement location permission request
    }
}

// MARK: - UITableViewDataSource, UITableViewDelegate
extension SinglePointMapViewController: UITableViewDataSource, UITableViewDelegate {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return suggestions.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "Cell", for: indexPath)
        let feature = suggestions[indexPath.row]
        cell.textLabel?.text = feature.properties.name
        return cell
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        
        let feature = suggestions[indexPath.row]
        let coordinates = feature.geometry.coordinates
        let location = CLLocationCoordinate2D(latitude: coordinates[1], longitude: coordinates[0])
        
        // Cập nhật map và marker
        mapView.camera.ease(to: CameraOptions(center: location, zoom: zoomLevel), duration: 0.5)
        addMarker(at: location)
        
        // Cập nhật thông tin địa điểm
        targetName = feature.properties.name
        coordinateLabel.text = """
        \(feature.properties.name)
        \(feature.properties.street), \(feature.properties.locality)
        Lat: \(location.latitude.rounded(toPlaces: 6)), Lng: \(location.longitude.rounded(toPlaces: 6))
        """
        coordinateLabel.isHidden = false
        
        // Ẩn bảng kết quả và bàn phím
        searchResultsTableView.isHidden = true
        searchTextField.text = feature.properties.name
        searchTextField.resignFirstResponder()
    }
}
```

### 11.3 Cấu trúc dữ liệu chung

Các thành phần dữ liệu chung cần được triển khai trong iOS:

1. **Model địa điểm**:
```swift
struct Feature: Codable {
    let properties: Properties
    let geometry: Geometry
}

struct Properties: Codable {
    let name: String
    let street: String
    let distance: Double
    let country: String
    let region: String
    let county: String
    let locality: String
    let label: String
}

struct Geometry: Codable {
    let coordinates: [Double]
}
```

2. **MapUtils**:
```swift
class MapUtils {
    static func getCountryName(from code: String) -> String {
        switch code {
        case "sg": return "Singapore"
        case "th": return "Thailand"
        case "tw": return "Taiwan"
        case "my": return "Malaysia"
        default: return "Việt Nam"
        }
    }
    
    static func getInitialCoordinates(for countryCode: String) -> CLLocationCoordinate2D {
        switch countryCode {
        case "sg": return CLLocationCoordinate2D(latitude: 1.352083, longitude: 103.819839)
        case "th": return CLLocationCoordinate2D(latitude: 13.7563, longitude: 100.5018)
        case "tw": return CLLocationCoordinate2D(latitude: 25.033, longitude: 121.565)
        case "my": return CLLocationCoordinate2D(latitude: 3.140, longitude: 101.693)
        default: return CLLocationCoordinate2D(latitude: 10.728073, longitude: 106.624054)
        }
    }
    
    static func getZoomLevel(for countryCode: String) -> Double {
        switch countryCode {
        case "sg": return 11.0
        case "th": return 10.0
        case "tw": return 10.0
        case "my": return 10.0
        default: return 10.0
        }
    }
    
    static func decodePolyline(_ polyline: String) -> [CLLocationCoordinate2D] {
        // Implement polyline decoding algorithm
        // ...
        return []
    }
}
```

### 11.4 Mô tả chi tiết các trường hợp sử dụng

Dựa trên phân tích mã nguồn, các trường hợp sử dụng chính của ứng dụng bao gồm:

1. **Hiển thị và thay đổi quốc gia**: Người dùng có thể chọn giữa Việt Nam, Singapore và Thái Lan, thay đổi vị trí trung tâm bản đồ và phong cách hiển thị.

2. **Single Point Map**: 
   - Tìm kiếm địa điểm
   - Đặt marker tại vị trí chạm
   - Hiển thị thông tin địa điểm
   - Tính toán tuyến đường từ vị trí hiện tại đến điểm đã chọn

3. **Multi Point Map**:
   - Đặt nhiều điểm trên bản đồ
   - Vẽ tuyến đường nối các điểm
   - Tính toán và hiển thị thông tin tuyến đường (khoảng cách, thời gian)

4. **Cluster Map**:
   - Hiển thị nhiều điểm dưới dạng cụm khi zoom out
   - Hiển thị các điểm riêng lẻ khi zoom in
   - Lọc và hiển thị các loại điểm khác nhau (POI, dịch bệnh, etc.)

5. **Animation Map**:
   - Hiệu ứng di chuyển marker
   - Hiệu ứng camera (zoom, pan, tilt)
   - Thay đổi style bản đồ (standard, night, satellite)

6. **Feature Map**:
   - Hiển thị các đối tượng GeoJSON
   - Styling các đối tượng trên bản đồ
   - Tương tác với các đối tượng (tap, long press)

Mỗi trường hợp sử dụng này cần được triển khai tương ứng trong iOS với các controller riêng biệt, chia sẻ các thành phần chung như cách xử lý bản đồ, API, và utilities.

## 12. Triển khai chi tiết MultiPointMapViewController

`MapMultiPointFragment.kt` trong dự án Android có chức năng:
- Thêm nhiều điểm trên bản đồ
- Tính toán và hiển thị tuyến đường giữa các điểm
- Xử lý sự kiện tap, kéo điểm

### 12.1 Triển khai iOS

```swift
import UIKit
import MapboxMaps

class MultiPointMapViewController: UIViewController {
    private var mapView: MapView!
    private var pointAnnotationManager: PointAnnotationManager!
    private var polylineAnnotationManager: PolylineAnnotationManager!
    private var points: [CLLocationCoordinate2D] = []
    private var markerLabels: [String] = ["Điểm A", "Điểm B", "Điểm C", "Điểm D", "Điểm E"]
    
    private var calculateRouteButton: UIButton!
    private var clearMapButton: UIButton!
    private var myLocationButton: UIButton!
    private var instructionsLabel: UILabel!
    
    private var preferences: UserDefaults!
    private var countryCode: String = "vn"
    private var initialCenter: CLLocationCoordinate2D = CLLocationCoordinate2D(latitude: 10.728073, longitude: 106.624054)
    private var zoomLevel: Double = 14.0
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Khởi tạo preferences
        preferences = UserDefaults.standard
        countryCode = preferences.string(forKey: "country") ?? "vn"
        
        // Thiết lập vị trí ban đầu
        setupInitialLocation()
        
        // Thiết lập UI
        setupUI()
        
        // Khởi tạo bản đồ
        setupMapView()
    }
    
    private func setupUI() {
        // Nút tính toán tuyến đường
        calculateRouteButton = UIButton(type: .system)
        calculateRouteButton.setTitle("Tính tuyến đường", for: .normal)
        calculateRouteButton.backgroundColor = UIColor(named: "colorBlue")
        calculateRouteButton.setTitleColor(.white, for: .normal)
        calculateRouteButton.layer.cornerRadius = 8
        calculateRouteButton.isEnabled = false
        
        // Nút xóa bản đồ
        clearMapButton = UIButton(type: .system)
        clearMapButton.setTitle("Xóa", for: .normal)
        clearMapButton.backgroundColor = .systemRed
        clearMapButton.setTitleColor(.white, for: .normal)
        clearMapButton.layer.cornerRadius = 8
        
        // Nút vị trí
        myLocationButton = UIButton(type: .system)
        myLocationButton.setImage(UIImage(systemName: "location.fill"), for: .normal)
        myLocationButton.backgroundColor = .white
        myLocationButton.tintColor = UIColor(named: "colorBlue")
        myLocationButton.layer.cornerRadius = 20
        
        // Label hướng dẫn
        instructionsLabel = UILabel()
        instructionsLabel.text = "Chạm vào bản đồ để đặt điểm"
        instructionsLabel.backgroundColor = UIColor.black.withAlphaComponent(0.7)
        instructionsLabel.textColor = .white
        instructionsLabel.textAlignment = .center
        instructionsLabel.layer.cornerRadius = 8
        instructionsLabel.layer.masksToBounds = true
        
        // Thêm các view vào view chính
        view.addSubview(calculateRouteButton)
        view.addSubview(clearMapButton)
        view.addSubview(myLocationButton)
        view.addSubview(instructionsLabel)
        
        // Thiết lập constraints
        calculateRouteButton.translatesAutoresizingMaskIntoConstraints = false
        clearMapButton.translatesAutoresizingMaskIntoConstraints = false
        myLocationButton.translatesAutoresizingMaskIntoConstraints = false
        instructionsLabel.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            // calculateRouteButton constraints - đặt ở dưới cùng giữa màn hình
            calculateRouteButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            calculateRouteButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -20),
            calculateRouteButton.widthAnchor.constraint(equalToConstant: 200),
            calculateRouteButton.heightAnchor.constraint(equalToConstant: 50),
            
            // clearMapButton constraints - đặt ở góc trên bên phải
            clearMapButton.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 20),
            clearMapButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            clearMapButton.widthAnchor.constraint(equalToConstant: 60),
            clearMapButton.heightAnchor.constraint(equalToConstant: 40),
            
            // myLocationButton constraints - đặt ở góc dưới bên phải
            myLocationButton.bottomAnchor.constraint(equalTo: calculateRouteButton.topAnchor, constant: -20),
            myLocationButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            myLocationButton.widthAnchor.constraint(equalToConstant: 40),
            myLocationButton.heightAnchor.constraint(equalToConstant: 40),
            
            // instructionsLabel constraints - đặt ở trên cùng giữa màn hình
            instructionsLabel.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 20),
            instructionsLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            instructionsLabel.trailingAnchor.constraint(equalTo: clearMapButton.leadingAnchor, constant: -20),
            instructionsLabel.heightAnchor.constraint(equalToConstant: 40)
        ])
        
        // Thiết lập sự kiện
        calculateRouteButton.addTarget(self, action: #selector(calculateRouteButtonTapped), for: .touchUpInside)
        clearMapButton.addTarget(self, action: #selector(clearMapButtonTapped), for: .touchUpInside)
        myLocationButton.addTarget(self, action: #selector(myLocationButtonTapped), for: .touchUpInside)
    }
    
    private func setupMapView() {
        // Khởi tạo MapboxMaps với style URL phù hợp
        let styleURL = getStyleURL(for: countryCode)
        
        let options = MapInitOptions(
            resourceOptions: ResourceOptions(accessToken: APIConstants.mapboxAccessToken),
            cameraOptions: CameraOptions(center: initialCenter, zoom: zoomLevel),
            styleURI: StyleURI(url: URL(string: styleURL)!)
        )
        
        mapView = MapView(frame: view.bounds, mapInitOptions: options)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(mapView)
        view.sendSubviewToBack(mapView)
        
        // Thêm gesture recognizer cho tap
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap(_:)))
        mapView.addGestureRecognizer(tapGesture)
        
        // Khởi tạo annotation managers
        pointAnnotationManager = mapView.annotations.makePointAnnotationManager()
        polylineAnnotationManager = mapView.annotations.makePolylineAnnotationManager()
        
        // Thiết lập delegate cho pointAnnotationManager để xử lý sự kiện drag
        setupAnnotationInteraction()
    }
    
    private func setupAnnotationInteraction() {
        // Cho phép kéo điểm
        pointAnnotationManager.delegate = self
    }
    
    @objc private func handleTap(_ gesture: UITapGestureRecognizer) {
        // Kiểm tra số lượng điểm (giới hạn 5 điểm)
        if points.count >= 5 {
            showAlert(title: "Thông báo", message: "Bạn đã thêm tối đa 5 điểm. Xóa bản đồ để bắt đầu lại.")
            return
        }
        
        // Lấy vị trí tap
        let point = gesture.location(in: mapView)
        let coordinate = mapView.mapboxMap.coordinate(for: point)
        
        // Thêm điểm vào mảng
        points.append(coordinate)
        
        // Thêm marker tại vị trí tap
        addMarker(at: coordinate, index: points.count - 1)
        
        // Cập nhật trạng thái nút tính toán tuyến đường
        calculateRouteButton.isEnabled = points.count >= 2
        
        // Cập nhật hướng dẫn
        updateInstructionsLabel()
    }
    
    private func addMarker(at coordinate: CLLocationCoordinate2D, index: Int) {
        var annotation = PointAnnotation(coordinate: coordinate)
        
        // Sử dụng các marker khác nhau tùy thuộc vào vị trí trong danh sách
        let markerName = index == 0 ? "marker_start" : (index == points.count - 1 ? "marker_end" : "marker_waypoint")
        annotation.image = .init(image: UIImage(named: markerName)!, name: markerName)
        
        // Thêm text
        if index < markerLabels.count {
            annotation.textField = markerLabels[index]
            annotation.textOffset = [0, -2]
            annotation.textColor = .init(.black)
            annotation.textHaloColor = .init(.white)
            annotation.textHaloWidth = 1.0
        }
        
        // Thêm marker mới vào danh sách
        var currentAnnotations = pointAnnotationManager.annotations
        currentAnnotations.append(annotation)
        pointAnnotationManager.annotations = currentAnnotations
    }
    
    @objc private func calculateRouteButtonTapped() {
        guard let userLocation = currentLocation, let destination = targetLocation else {
            showAlert(title: "Lỗi", message: "Không thể tính toán tuyến đường. Vui lòng chọn điểm đến.")
            return
        }
        
        // Hiển thị loading indicator
        instructionsLabel.text = "Đang tính toán tuyến đường..."
        
        // Tính toán tuyến đường
        calculateRoute()
    }
    
    private func calculateRoute() {
        // Tạo chuỗi tọa độ cho API request
        let coordinatesString = points.map { "\($0.longitude),\($0.latitude)" }.joined(separator: ";")
        
        // Tạo URL request
        let baseUrl = APIConstants.baseUrlVN + "route/v1/car"
        let url = "\(baseUrl)/\(coordinatesString).json?geometries=polyline6&steps=true&overview=full&key=public_key"
        
        guard let requestURL = URL(string: url) else {
            instructionsLabel.text = "Lỗi: URL không hợp lệ"
            return
        }
        
        // Gọi API routing
        URLSession.shared.dataTask(with: requestURL) { [weak self] data, response, error in
            DispatchQueue.main.async {
                guard let self = self else { return }
                
                if let error = error {
                    self.instructionsLabel.text = "Lỗi kết nối: \(error.localizedDescription)"
                    return
                }
                
                guard let data = data else {
                    self.instructionsLabel.text = "Không nhận được dữ liệu"
                    return
                }
                
                do {
                    let routeResponse = try JSONDecoder().decode(RouteResponse.self, from: data)
                    
                    if let route = routeResponse.routes.first, let geometry = route.geometry {
                        // Giải mã polyline và vẽ tuyến đường
                        let coordinates = self.decodePolyline(geometry)
                        self.drawRoute(coordinates: coordinates)
                        
                        // Hiển thị thông tin tuyến đường
                        let distance = String(format: "%.1f", route.distance / 1000)
                        let duration = self.formatDuration(seconds: Int(route.duration))
                        self.instructionsLabel.text = "Quãng đường: \(distance) km | Thời gian: \(duration)"
                    } else {
                        self.instructionsLabel.text = "Không tìm thấy tuyến đường"
                    }
                } catch {
                    self.instructionsLabel.text = "Lỗi xử lý dữ liệu: \(error.localizedDescription)"
                }
            }
        }.resume()
    }
    
    private func drawRoute(coordinates: [CLLocationCoordinate2D]) {
        // Xóa polyline hiện có
        polylineAnnotationManager.annotations = []
        
        // Tạo polyline mới
        var polyline = PolylineAnnotation(coordinates: coordinates)
        polyline.lineColor = StyleColor(.systemBlue)
        polyline.lineWidth = 4.0
        
        // Hiển thị polyline
        polylineAnnotationManager.annotations = [polyline]
    }
    
    @objc private func clearMapButtonTapped() {
        // Xóa tất cả các điểm và tuyến đường
        points.removeAll()
        pointAnnotationManager.annotations = []
        polylineAnnotationManager.annotations = []
        
        // Cập nhật UI
        calculateRouteButton.isEnabled = false
        instructionsLabel.text = "Chạm vào bản đồ để đặt điểm"
    }
    
    @objc private func myLocationButtonTapped() {
        // Yêu cầu và lấy vị trí người dùng
        LocationManager.shared.getCurrentLocation { [weak self] location in
            guard let self = self, let location = location else { return }
            
            // Di chuyển camera đến vị trí người dùng
            self.mapView.camera.ease(
                to: CameraOptions(center: location, zoom: self.zoomLevel),
                duration: 0.5
            )
        }
    }
    
    private func updateInstructionsLabel() {
        switch points.count {
        case 0:
            instructionsLabel.text = "Chạm vào bản đồ để đặt điểm"
        case 1:
            instructionsLabel.text = "Đã đặt điểm đầu. Chạm để đặt điểm tiếp theo."
        default:
            instructionsLabel.text = "Đã đặt \(points.count) điểm. Nhấn 'Tính tuyến đường' để tiếp tục."
        }
    }
    
    private func decodePolyline(_ polyline: String) -> [CLLocationCoordinate2D] {
        // Triển khai thuật toán giải mã polyline
        // ...
        return []
    }
    
    private func formatDuration(seconds: Int) -> String {
        let hours = seconds / 3600
        let minutes = (seconds % 3600) / 60
        
        if hours > 0 {
            return "\(hours) giờ \(minutes) phút"
        } else {
            return "\(minutes) phút"
        }
    }
    
    private func getStyleURL(for countryCode: String) -> String {
        switch countryCode {
        case "sg": return APIConstants.styleUrlSG
        case "th": return APIConstants.styleUrlTH
        default: return APIConstants.styleUrlVN
        }
    }
    
    private func setupInitialLocation() {
        switch countryCode {
        case "sg":
            initialCenter = CLLocationCoordinate2D(latitude: 1.352083, longitude: 103.819839)
        case "th":
            initialCenter = CLLocationCoordinate2D(latitude: 13.7563, longitude: 100.5018)
        default: // vn
            initialCenter = CLLocationCoordinate2D(latitude: 10.728073, longitude: 106.624054)
        }
    }
    
    private func showAlert(title: String, message: String) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }
}

// MARK: - PointAnnotationManagerDelegate
extension MultiPointMapViewController: AnnotationInteractionDelegate {
    func annotationManager(_ manager: AnnotationManager, didDetectTappedAnnotations annotations: [Annotation]) {
        // Xử lý sự kiện chạm vào annotation
    }
}

// MARK: - Model cho Route Response
struct RouteResponse: Codable {
    let routes: [Route]
}

struct Route: Codable {
    let geometry: String?
    let distance: Double
    let duration: Double
}

### 12.2 Triển khai LocationManager

```swift
class LocationManager {
    static let shared = LocationManager()
    
    private let locationManager = CLLocationManager()
    
    private init() {
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }
    
    func requestLocationPermission(completion: @escaping (Bool) -> Void) {
        let status = CLLocationManager.authorizationStatus()
        
        switch status {
        case .authorizedWhenInUse, .authorizedAlways:
            completion(true)
        case .notDetermined:
            locationManager.requestWhenInUseAuthorization()
            completion(false)
        case .restricted, .denied:
            completion(false)
        @unknown default:
            completion(false)
        }
    }
    
    func getCurrentLocation(completion: @escaping (CLLocationCoordinate2D?) -> Void) {
        let status = CLLocationManager.authorizationStatus()
        
        guard status == .authorizedWhenInUse || status == .authorizedAlways else {
            requestLocationPermission { granted in
                if granted {
                    self.getCurrentLocation(completion: completion)
                } else {
                    completion(nil)
                }
            }
            return
        }
        
        if let location = locationManager.location {
            completion(location.coordinate)
        } else {
            completion(nil)
        }
    }
}
```

## 13. Triển khai ClusterMapViewController

`MapClusterFragment.kt` trong dự án Android có chức năng:
- Hiển thị nhiều điểm dữ liệu trên bản đồ
- Tự động nhóm các điểm thành cluster khi zoom out
- Hỗ trợ nhiều loại dữ liệu khác nhau (POI, dịch bệnh)

### 13.1 Triển khai iOS

Triển khai này sử dụng khả năng clustering của MapBox để hiển thị nhóm điểm:

```swift
import UIKit
import MapboxMaps

class ClusterMapViewController: UIViewController {
    private var mapView: MapView!
    private var styleManager: StyleManager!
    private var myLocationButton: UIButton!
    private var styleSwitchButton: UIButton!
    private var loadDataButton: UIButton!
    private var loadingIndicator: UIActivityIndicatorView!
    private var infoLabel: UILabel!
    
    private var preferences: UserDefaults!
    private var countryCode: String = "vn"
    private var clusterSource: GeoJSONSource?
    private var clusterLayer: CircleLayer?
    private var clusterCountLayer: SymbolLayer?
    private var unclusteredPointLayer: CircleLayer?
    
    private enum MapMode {
        case normal, busCluster, pestDisease, poi
    }
    
    private var currentMapMode: MapMode = .normal
    
    // Các hằng số
    private let SOURCE_ID = "cluster-source"
    private let CLUSTER_CIRCLE_LAYER_ID = "clusters"
    private let CLUSTER_COUNT_LAYER_ID = "cluster-count"
    private let UNCLUSTERED_POINT_LAYER_ID = "unclustered-points"
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Khởi tạo preferences
        preferences = UserDefaults.standard
        countryCode = preferences.string(forKey: "country") ?? "vn"
        
        // Thiết lập UI
        setupUI()
        
        // Khởi tạo bản đồ
        setupMapView()
    }
    
    private func setupUI() {
        // Nút vị trí
        myLocationButton = UIButton(type: .system)
        myLocationButton.setImage(UIImage(systemName: "location.fill"), for: .normal)
        myLocationButton.backgroundColor = .white
        myLocationButton.tintColor = UIColor(named: "colorBlue")
        myLocationButton.layer.cornerRadius = 20
        
        // Nút chuyển style
        styleSwitchButton = UIButton(type: .system)
        styleSwitchButton.setImage(UIImage(systemName: "map.fill"), for: .normal)
        styleSwitchButton.backgroundColor = .white
        styleSwitchButton.tintColor = UIColor(named: "colorBlue")
        styleSwitchButton.layer.cornerRadius = 20
        
        // Nút tải dữ liệu
        loadDataButton = UIButton(type: .system)
        loadDataButton.setTitle("Tải dữ liệu", for: .normal)
        loadDataButton.backgroundColor = UIColor(named: "colorBlue")
        loadDataButton.setTitleColor(.white, for: .normal)
        loadDataButton.layer.cornerRadius = 8
        
        // Loading indicator
        loadingIndicator = UIActivityIndicatorView(style: .medium)
        loadingIndicator.hidesWhenStopped = true
        loadingIndicator.color = .white
        
        // Info label
        infoLabel = UILabel()
        infoLabel.backgroundColor = UIColor.black.withAlphaComponent(0.7)
        infoLabel.textColor = .white
        infoLabel.textAlignment = .center
        infoLabel.layer.cornerRadius = 8
        infoLabel.layer.masksToBounds = true
        infoLabel.text = "Nhấn 'Tải dữ liệu' để hiển thị clusters"
        
        // Thêm các view vào view chính
        view.addSubview(myLocationButton)
        view.addSubview(styleSwitchButton)
        view.addSubview(loadDataButton)
        view.addSubview(loadingIndicator)
        view.addSubview(infoLabel)
        
        // Thiết lập constraints
        calculateRouteButton.translatesAutoresizingMaskIntoConstraints = false
        clearMapButton.translatesAutoresizingMaskIntoConstraints = false
        myLocationButton.translatesAutoresizingMaskIntoConstraints = false
        instructionsLabel.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            // calculateRouteButton constraints - đặt ở dưới cùng giữa màn hình
            calculateRouteButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            calculateRouteButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -20),
            calculateRouteButton.widthAnchor.constraint(equalToConstant: 200),
            calculateRouteButton.heightAnchor.constraint(equalToConstant: 50),
            
            // clearMapButton constraints - đặt ở góc trên bên phải
            clearMapButton.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 20),
            clearMapButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            clearMapButton.widthAnchor.constraint(equalToConstant: 60),
            clearMapButton.heightAnchor.constraint(equalToConstant: 40),
            
            // myLocationButton constraints - đặt ở góc dưới bên phải
            myLocationButton.bottomAnchor.constraint(equalTo: calculateRouteButton.topAnchor, constant: -20),
            myLocationButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            myLocationButton.widthAnchor.constraint(equalToConstant: 40),
            myLocationButton.heightAnchor.constraint(equalToConstant: 40),
            
            // instructionsLabel constraints - đặt ở trên cùng giữa màn hình
            instructionsLabel.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 20),
            instructionsLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            instructionsLabel.trailingAnchor.constraint(equalTo: clearMapButton.leadingAnchor, constant: -20),
            instructionsLabel.heightAnchor.constraint(equalToConstant: 40)
        ])
        
        // Thiết lập sự kiện
        myLocationButton.addTarget(self, action: #selector(myLocationButtonTapped), for: .touchUpInside)
        styleSwitchButton.addTarget(self, action: #selector(styleSwitchButtonTapped), for: .touchUpInside)
        loadDataButton.addTarget(self, action: #selector(loadDataButtonTapped), for: .touchUpInside)
    }
    
    private func setupMapView() {
        // Khởi tạo MapboxMaps với style URL phù hợp
        let styleURL = StyleManager.getStyleURL(for: countryCode, styleType: .streets)
        
        let options = MapInitOptions(
            resourceOptions: ResourceOptions(accessToken: APIConstants.mapboxAccessToken),
            cameraOptions: CameraOptions(
                center: MapUtils.getInitialCoordinates(for: countryCode),
                zoom: MapUtils.getZoomLevel(for: countryCode)
            ),
            styleURI: StyleURI(url: URL(string: styleURL)!)
        )
        
        mapView = MapView(frame: view.bounds, mapInitOptions: options)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(mapView)
        view.sendSubviewToBack(mapView)
        
        // Thiết lập style manager
        styleManager = StyleManager(mapView: mapView)
        
        // Thiết lập tap gesture để xử lý sự kiện chạm vào cluster
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleMapTap(_:)))
        mapView.addGestureRecognizer(tapGesture)
    }
    
    @objc private func loadDataButtonTapped() {
        switch currentMapMode {
        case .normal:
            loadBusStopData()
        case .busCluster:
            loadPestDiseaseData()
        case .pestDisease:
            loadPOIData()
        case .poi:
            resetMapData()
        }
    }
    
    private func loadBusStopData() {
        // Hiển thị loading
        loadingIndicator.startAnimating()
        infoLabel.text = "Đang tải dữ liệu trạm xe buýt..."
        
        // Cập nhật mode
        currentMapMode = .busCluster
        
        // Tải dữ liệu từ API hoặc từ file JSON
        DispatchQueue.global().async { [weak self] in
            guard let self = self else { return }
            
            // Tải dữ liệu (giả lập)
            Thread.sleep(forTimeInterval: 1.0)
            
            // Tạo feature collection
            let features = self.createSampleBusStopFeatures()
            
            DispatchQueue.main.async {
                // Cập nhật UI
                self.loadingIndicator.stopAnimating()
                self.infoLabel.text = "Đã tải \(features.count) trạm xe buýt"
                self.loadDataButton.setTitle("Tải dữ liệu dịch bệnh", for: .normal)
                
                // Thêm dữ liệu vào bản đồ
                self.addClusteredDataToMap(features: features)
            }
        }
    }
    
    private func loadPestDiseaseData() {
        // Tương tự như loadBusStopData nhưng với dữ liệu khác
        // ...
        currentMapMode = .pestDisease
    }
    
    private func loadPOIData() {
        // Tương tự như loadBusStopData nhưng với dữ liệu POI
        // ...
        currentMapMode = .poi
    }
    
    private func resetMapData() {
        // Xóa tất cả dữ liệu trên bản đồ
        if let style = mapView.mapboxMap.style {
            if style.sourceExists(withId: SOURCE_ID) {
                try? style.removeLayer(withId: CLUSTER_CIRCLE_LAYER_ID)
                try? style.removeLayer(withId: CLUSTER_COUNT_LAYER_ID)
                try? style.removeLayer(withId: UNCLUSTERED_POINT_LAYER_ID)
                try? style.removeSource(withId: SOURCE_ID)
            }
        }
        
        // Cập nhật UI
        infoLabel.text = "Nhấn 'Tải dữ liệu' để hiển thị clusters"
        loadDataButton.setTitle("Tải dữ liệu", for: .normal)
        currentMapMode = .normal
    }
    
    private func addClusteredDataToMap(features: [Feature]) {
        // Tạo Feature Collection
        let featureCollection = FeatureCollection(features: features)
        
        mapView.mapboxMap.style.styleJSON { [weak self] result in
            guard let self = self, let style = try? result.get() else { return }
            
            // Xóa dữ liệu cũ nếu có
            if style.sourceExists(withId: self.SOURCE_ID) {
                try? self.mapView.mapboxMap.style.removeLayer(withId: self.CLUSTER_CIRCLE_LAYER_ID)
                try? self.mapView.mapboxMap.style.removeLayer(withId: self.CLUSTER_COUNT_LAYER_ID)
                try? self.mapView.mapboxMap.style.removeLayer(withId: self.UNCLUSTERED_POINT_LAYER_ID)
                try? self.mapView.mapboxMap.style.removeSource(withId: self.SOURCE_ID)
            }
            
            // Tạo source với clustering
            let source = GeoJSONSource(id: self.SOURCE_ID)
            source.data = .featureCollection(featureCollection)
            source.cluster = true
            source.clusterMaxZoom = 14
            source.clusterRadius = 50
            
            // Thêm source vào style
            try? self.mapView.mapboxMap.style.addSource(source)
            
            // Thêm layer cho các cluster
            try? self.addClusterLayers()
        }
    }
    
    private func addClusterLayers() throws {
        // Layer hiển thị các cluster dưới dạng hình tròn
        var circleLayer = CircleLayer(id: CLUSTER_CIRCLE_LAYER_ID, source: SOURCE_ID)
        circleLayer.filter = Exp(.has) {
            "point_count"
        }
        circleLayer.circleColor = .constant(.init(.systemBlue))
        circleLayer.circleOpacity = .constant(0.8)
        circleLayer.circleStrokeWidth = .constant(1)
        circleLayer.circleStrokeColor = .constant(.init(.white))
        circleLayer.circleRadius = .expression(
            Exp(.step) {
                Exp(.get) {
                    "point_count"
                }
                20   // Kích thước mặc định
                10   // Ngưỡng số điểm
                30   // Kích thước khi số điểm >= 10
                50   // Ngưỡng số điểm
                40   // Kích thước khi số điểm >= 50
            }
        )
        
        // Layer hiển thị số lượng điểm trong cluster
        var countLayer = SymbolLayer(id: CLUSTER_COUNT_LAYER_ID, source: SOURCE_ID)
        countLayer.filter = Exp(.has) {
            "point_count"
        }
        countLayer.textField = .expression(
            Exp(.toString) {
                Exp(.get) {
                    "point_count"
                }
            }
        )
        countLayer.textSize = .constant(12)
        countLayer.textColor = .constant(.init(.white))
        
        // Layer hiển thị điểm đơn lẻ
        var pointLayer = CircleLayer(id: UNCLUSTERED_POINT_LAYER_ID, source: SOURCE_ID)
        pointLayer.filter = Exp(.not) {
            Exp(.has) {
                "point_count"
            }
        }
        pointLayer.circleRadius = .constant(8)
        pointLayer.circleColor = .constant(.init(.systemGreen))
        pointLayer.circleStrokeWidth = .constant(1)
        pointLayer.circleStrokeColor = .constant(.init(.white))
        
        // Thêm các layer vào style
        try mapView.mapboxMap.style.addLayer(circleLayer)
        try mapView.mapboxMap.style.addLayer(countLayer)
        try mapView.mapboxMap.style.addLayer(pointLayer)
    }
    
    @objc private func handleMapTap(_ gesture: UITapGestureRecognizer) {
        let point = gesture.location(in: mapView)
        
        // Kiểm tra xem người dùng có tap vào cluster không
        mapView.mapboxMap.queryRenderedFeatures(
            at: point,
            options: RenderedQueryOptions(layerIds: [CLUSTER_CIRCLE_LAYER_ID], filter: nil)
        ) { [weak self] result in
            guard let self = self else { return }
            
            switch result {
            case .success(let features):
                guard let feature = features.first else { return }
                
                // Kiểm tra xem feature có phải là cluster không
                if let clusterID = feature.feature.properties?["cluster_id"]?.jsonValue as? Int {
                    // Xử lý tap vào cluster
                    self.handleClusterTap(clusterID: clusterID)
                }
            case .failure:
                break
            }
        }
    }
    
    private func handleClusterTap(clusterID: Int) {
        // Lấy tọa độ của các điểm trong cluster
        // Trong Mapbox iOS, thông thường phải gọi thêm một API để lấy các điểm
        // Ở đây chúng ta sẽ zoom vào cluster để hiển thị các điểm chi tiết hơn
        
        // Lấy tọa độ của cluster
        mapView.mapboxMap.querySourceFeatures(
            sourceId: SOURCE_ID,
            options: SourceQueryOptions(
                sourceLayerId: nil,
                filter: Exp(.eq) {
                    Exp(.get) {
                        "cluster_id"
                    }
                    .constant(NSNumber(value: clusterID))
                }
            )
        ) { [weak self] result in
            guard let self = self else { return }
            
            switch result {
            case .success(let features):
                guard let feature = features.first,
                      let geometry = feature.feature.geometry,
                      case .point(let point) = geometry else { return }
                
                // Zoom vào cluster
                let coordinate = CLLocationCoordinate2D(latitude: point.coordinates.latitude, longitude: point.coordinates.longitude)
                let currentZoom = self.mapView.cameraState.zoom
                
                self.mapView.camera.ease(
                    to: CameraOptions(center: coordinate, zoom: currentZoom + 2),
                    duration: 0.5
                )
            case .failure:
                break
            }
        }
    }
    
    @objc private func myLocationButtonTapped() {
        LocationManager.shared.getCurrentLocation { [weak self] location in
            guard let self = self, let location = location else { return }
            
            self.mapView.camera.ease(
                to: CameraOptions(center: location, zoom: 15),
                duration: 0.5
            )
        }
    }
    
    @objc private func styleSwitchButtonTapped() {
        // Hiển thị menu chọn style
        let alertController = UIAlertController(title: "Chọn style bản đồ", message: nil, preferredStyle: .actionSheet)
        
        let streetsAction = UIAlertAction(title: "Streets", style: .default) { [weak self] _ in
            self?.styleManager.applyStyle(.streets)
        }
        
        let satelliteAction = UIAlertAction(title: "Satellite", style: .default) { [weak self] _ in
            self?.styleManager.applyStyle(.satellite)
        }
        
        let nightAction = UIAlertAction(title: "Night", style: .default) { [weak self] _ in
            self?.styleManager.applyStyle(.night)
        }
        
        let cancelAction = UIAlertAction(title: "Hủy", style: .cancel)
        
        alertController.addAction(streetsAction)
        alertController.addAction(satelliteAction)
        alertController.addAction(nightAction)
        alertController.addAction(cancelAction)
        
        present(alertController, animated: true)
    }
    
    // Tạo dữ liệu mẫu cho bus stops
    private func createSampleBusStopFeatures() -> [Feature] {
        var features: [Feature] = []
        
        // Tạo điểm ngẫu nhiên dựa vào vị trí trung tâm của quốc gia
        let center = MapUtils.getInitialCoordinates(for: countryCode)
        
        for i in 0..<100 {
            // Tạo vị trí ngẫu nhiên trong phạm vi 0.1 độ từ trung tâm
            let lat = center.latitude + Double.random(in: -0.1...0.1)
            let lon = center.longitude + Double.random(in: -0.1...0.1)
            
            // Tạo feature
            let geometry = Geometry.point(Point(CLLocationCoordinate2D(latitude: lat, longitude: lon)))
            let properties: [String: Any] = [
                "id": i,
                "name": "Bus Stop \(i)",
                "type": "bus_stop"
            ]
            
            let feature = Feature(geometry: geometry, properties: properties)
            features.append(feature)
        }
        
        return features
    }
}
```

### 13.2 StyleManager

```swift
class StyleManager {
    enum MapStyle {
        case streets, satellite, night, simple, terrain
    }
    
    private weak var mapView: MapView?
    
    init(mapView: MapView) {
        self.mapView = mapView
    }
    
    static func getStyleURL(for countryCode: String, styleType: MapStyle) -> String {
        switch (countryCode, styleType) {
        case (_, .streets):
            switch countryCode {
            case "sg": return APIConstants.styleUrlSG
            case "th": return APIConstants.styleUrlTH
            default: return APIConstants.styleUrlVN
            }
        case (_, .satellite):
            switch countryCode {
            case "sg": return APIConstants.urlStyle3DSG
            case "th": return APIConstants.urlStyle3DTH
            default: return APIConstants.urlStyle3DVN
            }
        case (_, .night):
            switch countryCode {
            case "sg": return APIConstants.urlStyleNightSG
            case "th": return APIConstants.urlStyleNightTH
            default: return APIConstants.urlStyleNightVN
            }
        case (_, .simple):
            switch countryCode {
            case "sg": return APIConstants.urlStyleSimpleSG
            case "th": return APIConstants.urlStyleSimpleTH
            default: return APIConstants.urlStyleSimpleVN
            }
        case (_, .terrain):
            return APIConstants.urlStyle3DTerrainVN
        }
    }
    
    func applyStyle(_ style: MapStyle) {
        guard let mapView = mapView else { return }
        
        let countryCode = UserDefaults.standard.string(forKey: "country") ?? "vn"
        let styleURL = StyleManager.getStyleURL(for: countryCode, styleType: style)
        
        if let url = URL(string: styleURL) {
            mapView.mapboxMap.loadStyleURI(StyleURI(url: url))
        }
    }
}
```

### 13.3 LocationPermissionManager

```swift
class LocationPermissionManager {
    static let shared = LocationPermissionManager()
    
    func requestLocationPermission(completion: @escaping (Bool) -> Void) {
        // Handle iOS-specific location permissions
    }
}
```

## 14. Resources and Assets

For a complete iOS implementation, include:
1. Map marker icons in Assets.xcassets
2. Custom pins for different point types
3. Localization files
4. Gesture recognizers for map interactions

## 15. Phân tích chi tiết cấu trúc dự án

### 15.1 Cấu trúc thư mục chính

Dự án Android có cấu trúc thư mục sau:
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/trackasia/sample/
│   │   │   ├── adapter/
│   │   │   ├── api/
│   │   │   │   ├── Constants.kt
│   │   │   │   ├── model/
│   │   │   │   ├── repository/
│   │   │   │   ├── RetrofitClient.kt
│   │   │   │   └── service/
│   │   │   ├── custom/
│   │   │   ├── model/
│   │   │   ├── navigation/
│   │   │   ├── utils/
│   │   │   ├── MainActivity.kt
│   │   │   ├── MainViewModel.kt
│   │   │   ├── MapSinglePointFragment.kt
│   │   │   ├── MapMultiPointFragment.kt
│   │   │   ├── MapClusterFragment.kt
│   │   │   ├── MapAnimationFragment.kt
│   │   │   ├── MapFeatureFragment.kt
│   │   │   ├── MapFeatureSnapshotFragment.kt
│   │   │   ├── MapWayPointFragment.kt
│   │   │   └── MapDirectionPointFragment.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
```

### 15.2 Phân tích chi tiết các màn hình chính

#### 15.2.1 MainActivity

`MainActivity.kt` là điểm vào chính của ứng dụng, có trách nhiệm:
- Quản lý và điều hướng giữa các Fragment bản đồ
- Hiển thị thanh công cụ (toolbar) với chọn quốc gia (VN, Singapore, Thailand)
- Xử lý bottom navigation với 5 tùy chọn: Single Point, Multi-Point, Clusters, Animation, Features

Cấu trúc UI bao gồm:
1. AppBarLayout với thông tin quốc gia và tiêu đề
2. NavHostFragment để chứa các fragment bản đồ
3. BottomNavigationView với 5 tab điều hướng

**Triển khai iOS:**
```swift
import UIKit

class MainViewController: UIViewController {
    private var countryLabel: UILabel!
    private var titleLabel: UILabel!
    private var containerView: UIView!
    private var tabBar: UITabBar!
    private var childViewControllers: [UIViewController] = []
    private var currentViewController: UIViewController?
    private var preferences: UserDefaults!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Khởi tạo UserDefaults
        preferences = UserDefaults.standard
        let country = preferences.string(forKey: "country") ?? "vn"
        
        // Thiết lập UI
        setupUI(country: country)
        
        // Khởi tạo ViewControllers
        setupViewControllers()
        
        // Hiển thị màn hình đầu tiên (Single Point)
        showViewController(at: 0)
    }
    
    private func setupUI(country: String) {
        // Tạo phần header
        let headerView = UIView()
        headerView.backgroundColor = UIColor(named: "colorBlue")
        headerView.layer.cornerRadius = 12
        
        // Logo và tên app
        let logoImageView = UIImageView(image: UIImage(named: "ic_navigation_24"))
        logoImageView.tintColor = .white
        
        let appNameLabel = UILabel()
        appNameLabel.text = "TrackAsia"
        appNameLabel.textColor = .white
        appNameLabel.font = UIFont.boldSystemFont(ofSize: 16)
        
        titleLabel = UILabel()
        titleLabel.textColor = .white
        titleLabel.font = UIFont.systemFont(ofSize: 15)
        
        // Label hiển thị quốc gia
        countryLabel = UILabel()
        countryLabel.text = getCountryName(from: country)
        countryLabel.textColor = UIColor(named: "colorBlue")
        countryLabel.backgroundColor = .white
        countryLabel.font = UIFont.boldSystemFont(ofSize: 14)
        countryLabel.textAlignment = .center
        countryLabel.layer.cornerRadius = 8
        countryLabel.layer.masksToBounds = true
        
        // Thêm sự kiện cho countryLabel
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(showCountrySelector))
        countryLabel.isUserInteractionEnabled = true
        countryLabel.addGestureRecognizer(tapGesture)
        
        // Container cho các fragment
        containerView = UIView()
        
        // TabBar
        tabBar = UITabBar()
        tabBar.delegate = self
        
        let singlePointItem = UITabBarItem(title: "Single Point", image: UIImage(named: "ic_map_single"), tag: 0)
        let multiPointItem = UITabBarItem(title: "Multi-Point", image: UIImage(named: "ic_map_multi"), tag: 1)
        let clustersItem = UITabBarItem(title: "Clusters", image: UIImage(named: "ic_map_cluster"), tag: 2)
        let animationItem = UITabBarItem(title: "Animation", image: UIImage(named: "ic_map_animation"), tag: 3)
        let featuresItem = UITabBarItem(title: "Features", image: UIImage(named: "ic_feature"), tag: 4)
        
        tabBar.items = [singlePointItem, multiPointItem, clustersItem, animationItem, featuresItem]
        tabBar.selectedItem = singlePointItem
        
        // Add subviews and setup constraints
        // ...
    }
    
    private func setupViewControllers() {
        // Khởi tạo các view controller
        let singlePointVC = SinglePointMapViewController()
        let multiPointVC = MultiPointMapViewController()
        let clusterMapVC = ClusterMapViewController()
        let animationMapVC = AnimationMapViewController()
        let featureMapVC = FeatureMapViewController()
        
        childViewControllers = [singlePointVC, multiPointVC, clusterMapVC, animationMapVC, featureMapVC]
    }
    
    private func showViewController(at index: Int) {
        // Remove current view controller
        if let currentVC = currentViewController {
            currentVC.willMove(toParent: nil)
            currentVC.view.removeFromSuperview()
            currentVC.removeFromParent()
        }
        
        // Add new view controller
        let newVC = childViewControllers[index]
        addChild(newVC)
        containerView.addSubview(newVC.view)
        newVC.view.frame = containerView.bounds
        newVC.didMove(toParent: self)
        currentViewController = newVC
        
        // Update title
        titleLabel.text = tabBar.items?[index].title
    }
    
    @objc private func showCountrySelector() {
        let alertController = UIAlertController(title: "Chọn quốc gia", message: nil, preferredStyle: .actionSheet)
        
        let vietnamAction = UIAlertAction(title: "Việt Nam", style: .default) { [weak self] _ in
            self?.updateCountry("vn", name: "Việt Nam")
        }
        
        let singaporeAction = UIAlertAction(title: "Singapore", style: .default) { [weak self] _ in
            self?.updateCountry("sg", name: "Singapore")
        }
        
        let thailandAction = UIAlertAction(title: "Thailand", style: .default) { [weak self] _ in
            self?.updateCountry("th", name: "Thailand")
        }
        
        let cancelAction = UIAlertAction(title: "Hủy", style: .cancel)
        
        alertController.addAction(vietnamAction)
        alertController.addAction(singaporeAction)
        alertController.addAction(thailandAction)
        alertController.addAction(cancelAction)
        
        present(alertController, animated: true)
    }
    
    private func updateCountry(_ code: String, name: String) {
        preferences.set(code, forKey: "country")
        countryLabel.text = name
        
        // Refresh current view controller
        if let index = tabBar.items?.firstIndex(of: tabBar.selectedItem!) {
            // Tạo mới view controller với cấu hình quốc gia mới
            setupViewControllers()
            showViewController(at: index)
        }
    }
    
    private func getCountryName(from code: String) -> String {
        switch code {
        case "vn": return "Việt Nam"
        case "sg": return "Singapore"
        case "th": return "Thailand"
        default: return "Việt Nam"
        }
    }
}

// MARK: - UITabBarDelegate
extension MainViewController: UITabBarDelegate {
    func tabBar(_ tabBar: UITabBar, didSelect item: UITabBarItem) {
        showViewController(at: item.tag)
    }
}
```

#### 15.2.2 MapSinglePointFragment

`MapSinglePointFragment.kt` hiển thị một điểm trên bản đồ với các tính năng:
- Hiển thị điểm hiện tại của người dùng
- Đặt marker khi chạm vào bản đồ
- Tìm kiếm địa điểm và geocoding ngược
- Hiển thị thông tin địa điểm khi chọn

**Triển khai iOS chi tiết:**
```swift
import UIKit
import MapboxMaps

class SinglePointMapViewController: UIViewController {
    private var mapView: MapView!
    private var searchTextField: UITextField!
    private var myLocationButton: UIButton!
    private var coordinateLabel: UILabel!
    private var searchResultsTableView: UITableView!
    private var calculateDirectionButton: UIButton!
    
    private var suggestions: [Feature] = []
    private var marker: PointAnnotation?
    private var pointAnnotationManager: PointAnnotationManager!
    private var currentLocation: CLLocationCoordinate2D?
    private var targetLocation: CLLocationCoordinate2D?
    private var targetName: String = "Điểm đến"
    
    private var preferences: UserDefaults!
    private var countryCode: String = "vn"
    private var initialCenter: CLLocationCoordinate2D = CLLocationCoordinate2D(latitude: 10.728073, longitude: 106.624054)
    private var zoomLevel: Double = 16.0
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Khởi tạo preferences
        preferences = UserDefaults.standard
        countryCode = preferences.string(forKey: "country") ?? "vn"
        
        // Cấu hình vị trí ban đầu dựa trên quốc gia
        setupInitialLocation()
        
        // Thiết lập UI
        setupUI()
        
        // Khởi tạo bản đồ
        setupMapView()
        
        // Thiết lập gesture recognizers
        setupGestureRecognizers()
        
        // Thiết lập các listener
        setupListeners()
    }
    
    private func setupMapView() {
        // Khởi tạo MapboxMaps với style URL phù hợp
        let styleURL = getStyleURL(for: countryCode)
        
        let options = MapInitOptions(
            resourceOptions: ResourceOptions(accessToken: APIConstants.mapboxAccessToken),
            cameraOptions: CameraOptions(center: initialCenter, zoom: zoomLevel),
            styleURI: StyleURI(url: URL(string: styleURL)!)
        )
        
        mapView = MapView(frame: view.bounds, mapInitOptions: options)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(mapView)
        view.sendSubviewToBack(mapView)
        
        // Khởi tạo annotation manager
        pointAnnotationManager = mapView.annotations.makePointAnnotationManager()
        
        // Yêu cầu quyền vị trí và hiển thị vị trí người dùng
        requestLocationPermission()
    }
    
    private func setupUI() {
        // Tạo SearchTextField
        searchTextField = UITextField()
        searchTextField.placeholder = "Tìm địa điểm..."
        searchTextField.backgroundColor = .white
        searchTextField.layer.cornerRadius = 8
        searchTextField.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 10, height: searchTextField.frame.height))
        searchTextField.leftViewMode = .always
        searchTextField.clearButtonMode = .whileEditing
        
        // Tạo nút vị trí
        myLocationButton = UIButton(type: .system)
        myLocationButton.setImage(UIImage(systemName: "location.fill"), for: .normal)
        myLocationButton.backgroundColor = .white
        myLocationButton.layer.cornerRadius = 20
        
        // Label hiển thị thông tin tọa độ
        coordinateLabel = UILabel()
        coordinateLabel.backgroundColor = UIColor.black.withAlphaComponent(0.7)
        coordinateLabel.textColor = .white
        coordinateLabel.textAlignment = .center
        coordinateLabel.layer.cornerRadius = 8
        coordinateLabel.layer.masksToBounds = true
        coordinateLabel.numberOfLines = 0
        coordinateLabel.isHidden = true
        
        // TableView kết quả tìm kiếm
        searchResultsTableView = UITableView()
        searchResultsTableView.backgroundColor = .white
        searchResultsTableView.layer.cornerRadius = 8
        searchResultsTableView.isHidden = true
        searchResultsTableView.register(UITableViewCell.self, forCellReuseIdentifier: "Cell")
        
        // Nút tính toán tuyến đường
        calculateDirectionButton = UIButton(type: .system)
        calculateDirectionButton.setTitle("Tính tuyến đường", for: .normal)
        calculateDirectionButton.backgroundColor = UIColor(named: "colorBlue")
        calculateDirectionButton.setTitleColor(.white, for: .normal)
        calculateDirectionButton.layer.cornerRadius = 8
        calculateDirectionButton.isHidden = true
        
        // Thêm các view vào view chính
        view.addSubview(searchTextField)
        view.addSubview(myLocationButton)
        view.addSubview(coordinateLabel)
        view.addSubview(searchResultsTableView)
        view.addSubview(calculateDirectionButton)
        
        // Thiết lập constraints
        // ...
    }
    
    @objc private func handleMapTap(_ gesture: UITapGestureRecognizer) {
        // Ẩn bàn phím nếu đang hiển thị
        searchTextField.resignFirstResponder()
        
        // Lấy vị trí tap
        let point = gesture.location(in: mapView)
        let coordinate = mapView.mapboxMap.coordinate(for: point)
        
        // Đặt marker và hiển thị thông tin
        addMarker(at: coordinate)
        performReverseGeocoding(at: coordinate)
    }
    
    private func addMarker(at coordinate: CLLocationCoordinate2D) {
        // Xóa marker hiện tại nếu có
        pointAnnotationManager.annotations = []
        
        // Tạo marker mới
        var annotation = PointAnnotation(coordinate: coordinate)
        annotation.image = .init(image: UIImage(named: "map_marker")!, name: "marker")
        
        pointAnnotationManager.annotations = [annotation]
        marker = annotation
        targetLocation = coordinate
        
        // Hiển thị nút chỉ đường
        calculateDirectionButton.isHidden = false
    }
    
    private func performReverseGeocoding(at coordinate: CLLocationCoordinate2D) {
        GeocodingService.reverseGeocode(
            longitude: coordinate.longitude,
            latitude: coordinate.latitude,
            language: "vi"
        ) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let response):
                    if let feature = response.features.first {
                        self?.targetName = feature.properties.name
                        self?.coordinateLabel.text = """
                        \(feature.properties.name)
                        \(feature.properties.street), \(feature.properties.locality)
                        Lat: \(coordinate.latitude.rounded(toPlaces: 6)), Lng: \(coordinate.longitude.rounded(toPlaces: 6))
                        """
                        self?.coordinateLabel.isHidden = false
                    }
                case .failure(let error):
                    self?.coordinateLabel.text = """
                    Vị trí không xác định
                    Lat: \(coordinate.latitude.rounded(toPlaces: 6)), Lng: \(coordinate.longitude.rounded(toPlaces: 6))
                    """
                    self?.coordinateLabel.isHidden = false
                    print("Geocoding error: \(error.localizedDescription)")
                }
            }
        }
    }
    
    @objc private func myLocationButtonTapped() {
        if let userLocation = currentLocation {
            // Di chuyển camera đến vị trí người dùng
            mapView.camera.ease(
                to: CameraOptions(center: userLocation, zoom: zoomLevel),
                duration: 0.5
            )
            
            // Đặt marker tại vị trí người dùng
            addMarker(at: userLocation)
            performReverseGeocoding(at: userLocation)
        } else {
            // Yêu cầu quyền vị trí nếu chưa có
            requestLocationPermission()
        }
    }
    
    @objc private func searchTextChanged() {
        // Xử lý thay đổi text tìm kiếm
        guard let query = searchTextField.text, query.count >= 2 else {
            searchResultsTableView.isHidden = true
            return
        }
        
        // Gọi API autocomplete
        GeocodingService.getSuggestions(text: query) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let response):
                    self?.suggestions = response.features
                    self?.searchResultsTableView.reloadData()
                    self?.searchResultsTableView.isHidden = false
                case .failure:
                    self?.suggestions = []
                    self?.searchResultsTableView.isHidden = true
                }
            }
        }
    }
    
    @objc private func calculateDirectionButtonTapped() {
        guard let userLocation = currentLocation, let destination = targetLocation else {
            showAlert(title: "Lỗi", message: "Không thể tính toán tuyến đường. Vui lòng chọn điểm đến.")
            return
        }
        
        // Chuyển sang màn hình chỉ đường
        let routeVC = RouteViewController()
        routeVC.startLocation = userLocation
        routeVC.endLocation = destination
        routeVC.destinationName = targetName
        
        present(routeVC, animated: true)
    }
    
    private func getStyleURL(for countryCode: String) -> String {
        switch countryCode {
        case "sg": return APIConstants.styleUrlSG
        case "th": return APIConstants.styleUrlTH
        default: return APIConstants.styleUrlVN
        }
    }
    
    private func setupInitialLocation() {
        switch countryCode {
        case "sg":
            initialCenter = CLLocationCoordinate2D(latitude: 1.352083, longitude: 103.819839)
            zoomLevel = 12.0
        case "th":
            initialCenter = CLLocationCoordinate2D(latitude: 13.7563, longitude: 100.5018)
            zoomLevel = 11.0
        default: // vn
            initialCenter = CLLocationCoordinate2D(latitude: 10.728073, longitude: 106.624054)
            zoomLevel = 10.0
        }
    }
    
    private func requestLocationPermission() {
        // Implement location permission request
    }
}

// MARK: - UITableViewDataSource, UITableViewDelegate
extension SinglePointMapViewController: UITableViewDataSource, UITableViewDelegate {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return suggestions.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "Cell", for: indexPath)
        let feature = suggestions[indexPath.row]
        cell.textLabel?.text = feature.properties.name
        return cell
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        
        let feature = suggestions[indexPath.row]
        let coordinates = feature.geometry.coordinates
        let location = CLLocationCoordinate2D(latitude: coordinates[1], longitude: coordinates[0])
        
        // Cập nhật map và marker
        mapView.camera.ease(to: CameraOptions(center: location, zoom: zoomLevel), duration: 0.5)
        addMarker(at: location)
        
        // Cập nhật thông tin địa điểm
        targetName = feature.properties.name
        coordinateLabel.text = """
        \(feature.properties.name)
        \(feature.properties.street), \(feature.properties.locality)
        Lat: \(location.latitude.rounded(toPlaces: 6)), Lng: \(location.longitude.rounded(toPlaces: 6))
        """
        coordinateLabel.isHidden = false
        
        // Ẩn bảng kết quả và bàn phím
        searchResultsTableView.isHidden = true
        searchTextField.text = feature.properties.name
        searchTextField.resignFirstResponder()
    }
}
```

### 15.3 Cấu trúc dữ liệu chung

Các thành phần dữ liệu chung cần được triển khai trong iOS:

1. **Model địa điểm**:
```swift
struct Feature: Codable {
    let properties: Properties
    let geometry: Geometry
}

struct Properties: Codable {
    let name: String
    let street: String
    let distance: Double
    let country: String
    let region: String
    let county: String
    let locality: String
    let label: String
}

struct Geometry: Codable {
    let coordinates: [Double]
}
```

2. **MapUtils**:
```swift
class MapUtils {
    static func getCountryName(from code: String) -> String {
        switch code {
        case "sg": return "Singapore"
        case "th": return "Thailand"
        case "tw": return "Taiwan"
        case "my": return "Malaysia"
        default: return "Việt Nam"
        }
    }
    
    static func getInitialCoordinates(for countryCode: String) -> CLLocationCoordinate2D {
        switch countryCode {
        case "sg": return CLLocationCoordinate2D(latitude: 1.352083, longitude: 103.819839)
        case "th": return CLLocationCoordinate2D(latitude: 13.7563, longitude: 100.5018)
        case "tw": return CLLocationCoordinate2D(latitude: 25.033, longitude: 121.565)
        case "my": return CLLocationCoordinate2D(latitude: 3.140, longitude: 101.693)
        default: return CLLocationCoordinate2D(latitude: 10.728073, longitude: 106.624054)
        }
    }
    
    static func getZoomLevel(for countryCode: String) -> Double {
        switch countryCode {
        case "sg": return 11.0
        case "th": return 10.0
        case "tw": return 10.0
        case "my": return 10.0
        default: return 10.0
        }
    }
    
    static func decodePolyline(_ polyline: String) -> [CLLocationCoordinate2D] {
        // Implement polyline decoding algorithm
        // ...
        return []
    }
}
```

### 15.4 Mô tả chi tiết các trường hợp sử dụng

Dựa trên phân tích mã nguồn, các trường hợp sử dụng chính của ứng dụng bao gồm:

1. **Hiển thị và thay đổi quốc gia**: Người dùng có thể chọn giữa Việt Nam, Singapore và Thái Lan, thay đổi vị trí trung tâm bản đồ và phong cách hiển thị.

2. **Single Point Map**: 
   - Tìm kiếm địa điểm
   - Đặt marker tại vị trí chạm
   - Hiển thị thông tin địa điểm
   - Tính toán tuyến đường từ vị trí hiện tại đến điểm đã chọn

3. **Multi Point Map**:
   - Đặt nhiều điểm trên bản đồ
   - Vẽ tuyến đường nối các điểm
   - Tính toán và hiển thị thông tin tuyến đường (khoảng cách, thời gian)

4. **Cluster Map**:
   - Hiển thị nhiều điểm dưới dạng cụm khi zoom out
   - Hiển thị các điểm riêng lẻ khi zoom in
   - Lọc và hiển thị các loại điểm khác nhau (POI, dịch bệnh, etc.)

5. **Animation Map**:
   - Hiệu ứng di chuyển marker
   - Hiệu ứng camera (zoom, pan, tilt)
   - Thay đổi style bản đồ (standard, night, satellite)

6. **Feature Map**:
   - Hiển thị các đối tượng GeoJSON
   - Styling các đối tượng trên bản đồ
   - Tương tác với các đối tượng (tap, long press)

Mỗi trường hợp sử dụng này cần được triển khai tương ứng trong iOS với các controller riêng biệt, chia sẻ các thành phần chung như cách xử lý bản đồ, API, và utilities.

## 16. Triển khai chi tiết ClusterMapViewController

`MapClusterFragment.kt` trong dự án Android có chức năng:
- Hiển thị nhiều điểm dữ liệu trên bản đồ
- Tự động nhóm các điểm thành cluster khi zoom out
- Hỗ trợ nhiều loại dữ liệu khác nhau (POI, dịch bệnh)

### 16.1 Triển khai iOS

Triển khai này sử dụng khả năng clustering của MapBox để hiển thị nhóm điểm:

```swift
import UIKit
import MapboxMaps

class ClusterMapViewController: UIViewController {
    private var mapView: MapView!
    private var styleManager: StyleManager!
    private var myLocationButton: UIButton!
    private var styleSwitchButton: UIButton!
    private var loadDataButton: UIButton!
    private var loadingIndicator: UIActivityIndicatorView!
    private var infoLabel: UILabel!
    
    private var preferences: UserDefaults!
    private var countryCode: String = "vn"
    private var clusterSource: GeoJSONSource?
    private var clusterLayer: CircleLayer?
    private var clusterCountLayer: SymbolLayer?
    private var unclusteredPointLayer: CircleLayer?
    
    private enum MapMode {
        case normal, busCluster, pestDisease, poi
    }
    
    private var currentMapMode: MapMode = .normal
    
    // Các hằng số
    private let SOURCE_ID = "cluster-source"
    private let CLUSTER_CIRCLE_LAYER_ID = "clusters"
    private let CLUSTER_COUNT_LAYER_ID = "cluster-count"
    private let UNCLUSTERED_POINT_LAYER_ID = "unclustered-points"
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Khởi tạo preferences
        preferences = UserDefaults.standard
        countryCode = preferences.string(forKey: "country") ?? "vn"
        
        // Thiết lập UI
        setupUI()
        
        // Khởi tạo bản đồ
        setupMapView()
    }
    
    private func setupUI() {
        // Nút vị trí
        myLocationButton = UIButton(type: .system)
        myLocationButton.setImage(UIImage(systemName: "location.fill"), for: .normal)
        myLocationButton.backgroundColor = .white
        myLocationButton.tintColor = UIColor(named: "colorBlue")
        myLocationButton.layer.cornerRadius = 20
        
        // Nút chuyển style
        styleSwitchButton = UIButton(type: .system)
        styleSwitchButton.setImage(UIImage(systemName: "map.fill"), for: .normal)
        styleSwitchButton.backgroundColor = .white
        styleSwitchButton.tintColor = UIColor(named: "colorBlue")
        styleSwitchButton.layer.cornerRadius = 20
        
        // Nút tải dữ liệu
        loadDataButton = UIButton(type: .system)
        loadDataButton.setTitle("Tải dữ liệu", for: .normal)
        loadDataButton.backgroundColor = UIColor(named: "colorBlue")
        loadDataButton.setTitleColor(.white, for: .normal)
        loadDataButton.layer.cornerRadius = 8
        
        // Loading indicator
        loadingIndicator = UIActivityIndicatorView(style: .medium)
        loadingIndicator.hidesWhenStopped = true
        loadingIndicator.color = .white
        
        // Info label
        infoLabel = UILabel()
        infoLabel.backgroundColor = UIColor.black.withAlphaComponent(0.7)
        infoLabel.textColor = .white
        infoLabel.textAlignment = .center
        infoLabel.layer.cornerRadius = 8
        infoLabel.layer.masksToBounds = true
        infoLabel.text = "Nhấn 'Tải dữ liệu' để hiển thị clusters"
        
        // Thêm các view vào view chính
        view.addSubview(myLocationButton)
        view.addSubview(styleSwitchButton)
        view.addSubview(loadDataButton)
        view.addSubview(loadingIndicator)
        view.addSubview(infoLabel)
        
        // Thiết lập constraints
        calculateRouteButton.translatesAutoresizingMaskIntoConstraints = false
        clearMapButton.translatesAutoresizingMaskIntoConstraints = false
        myLocationButton.translatesAutoresizingMaskIntoConstraints = false
        instructionsLabel.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            // calculateRouteButton constraints - đặt ở dưới cùng giữa màn hình
            calculateRouteButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            calculateRouteButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -20),
            calculateRouteButton.widthAnchor.constraint(equalToConstant: 200),
            calculateRouteButton.heightAnchor.constraint(equalToConstant: 50),
            
            // clearMapButton constraints - đặt ở góc trên bên phải
            clearMapButton.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 20),
            clearMapButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            clearMapButton.widthAnchor.constraint(equalToConstant: 60),
            clearMapButton.heightAnchor.constraint(equalToConstant: 40),
            
            // myLocationButton constraints - đặt ở góc dưới bên phải
            myLocationButton.bottomAnchor.constraint(equalTo: calculateRouteButton.topAnchor, constant: -20),
            myLocationButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            myLocationButton.widthAnchor.constraint(equalToConstant: 40),
            myLocationButton.heightAnchor.constraint(equalToConstant: 40),
            
            // instructionsLabel constraints - đặt ở trên cùng giữa màn hình
            instructionsLabel.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 20),
            instructionsLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            instructionsLabel.trailingAnchor.constraint(equalTo: clearMapButton.leadingAnchor, constant: -20),
            instructionsLabel.heightAnchor.constraint(equalToConstant: 40)
        ])
        
        // Thiết lập sự kiện
        myLocationButton.addTarget(self, action: #selector(myLocationButtonTapped), for: .touchUpInside)
        styleSwitchButton.addTarget(self, action: #selector(styleSwitchButtonTapped), for: .touchUpInside)
        loadDataButton.addTarget(self, action: #selector(loadDataButtonTapped), for: .touchUpInside)
    }
    
    private func setupMapView() {
        // Khởi tạo MapboxMaps với style URL phù hợp
        let styleURL = StyleManager.getStyleURL(for: countryCode, styleType: .streets)
        
        let options = MapInitOptions(
            resourceOptions: ResourceOptions(accessToken: APIConstants.mapboxAccessToken),
            cameraOptions: CameraOptions(center: initialCenter, zoom: zoomLevel),
            styleURI: StyleURI(url: URL(string: styleURL)!)
        )
        
        mapView = MapView(frame: view.bounds, mapInitOptions: options)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(mapView)
        view.sendSubviewToBack(mapView)
        
        // Thiết lập style manager
        styleManager = StyleManager(mapView: mapView)
        
        // Thiết lập tap gesture để xử lý sự kiện chạm vào cluster
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleMapTap(_:)))
        mapView.addGestureRecognizer(tapGesture)
    }
    
    @objc private func loadDataButtonTapped() {
        switch currentMapMode {
        case .normal:
            loadBusStopData()
        case .busCluster:
            loadPestDiseaseData()
        case .pestDisease:
            loadPOIData()
        case .poi:
            resetMapData()
        }
    }
    
    private func loadBusStopData() {
        // Hiển thị loading
        loadingIndicator.startAnimating()
        infoLabel.text = "Đang tải dữ liệu trạm xe buýt..."
        
        // Cập nhật mode
        currentMapMode = .busCluster
        
        // Tải dữ liệu từ API hoặc từ file JSON
        DispatchQueue.global().async { [weak self] in
            guard let self = self else { return }
            
            // Tải dữ liệu (giả lập)
            Thread.sleep(forTimeInterval: 1.0)
            
            // Tạo feature collection
            let features = self.createSampleBusStopFeatures()
            
            DispatchQueue.main.async {
                // Cập nhật UI
                self.loadingIndicator.stopAnimating()
                self.infoLabel.text = "Đã tải \(features.count) trạm xe buýt"
                self.loadDataButton.setTitle("Tải dữ liệu dịch bệnh", for: .normal)
                
                // Thêm dữ liệu vào bản đồ
                self.addClusteredDataToMap(features: features)
            }
        }
    }
    
    private func loadPestDiseaseData() {
        // Tương tự như loadBusStopData nhưng với dữ liệu khác
        // ...
        currentMapMode = .pestDisease
    }
    
    private func loadPOIData() {
        // Tương tự như loadBusStopData nhưng với dữ liệu POI
        // ...
        currentMapMode = .poi
    }
    
    private func resetMapData() {
        // Xóa tất cả dữ liệu trên bản đồ
        if let style = mapView.mapboxMap.style {
            if style.sourceExists(withId: SOURCE_ID) {
                try? style.removeLayer(withId: CLUSTER_CIRCLE_LAYER_ID)
                try? style.removeLayer(withId: CLUSTER_COUNT_LAYER_ID)
                try? style.removeLayer(withId: UNCLUSTERED_POINT_LAYER_ID)
                try? style.removeSource(withId: SOURCE_ID)
            }
        }
        
        // Cập nhật UI
        infoLabel.text = "Nhấn 'Tải dữ liệu' để hiển thị clusters"
        loadDataButton.setTitle("Tải dữ liệu", for: .normal)
        currentMapMode = .normal
    }
    
    private func addClusteredDataToMap(features: [Feature]) {
        // Tạo Feature Collection
        let featureCollection = FeatureCollection(features: features)
        
        mapView.mapboxMap.style.styleJSON { [weak self] result in
            guard let self = self, let style = try? result.get() else { return }
            
            // Xóa dữ liệu cũ nếu có
            if style.sourceExists(withId: self.SOURCE_ID) {
                try? self.mapView.mapboxMap.style.removeLayer(withId: self.CLUSTER_CIRCLE_LAYER_ID)
                try? self.mapView.mapboxMap.style.removeLayer(withId: self.CLUSTER_COUNT_LAYER_ID)
                try? self.mapView.mapboxMap.style.removeLayer(withId: self.UNCLUSTERED_POINT_LAYER_ID)
                try? self.mapView.mapboxMap.style.removeSource(withId: self.SOURCE_ID)
            }
            
            // Tạo source với clustering
            let source = GeoJSONSource(id: self.SOURCE_ID)
            source.data = .featureCollection(featureCollection)
            source.cluster = true
            source.clusterMaxZoom = 14
            source.clusterRadius = 50
            
            // Thêm source vào style
            try? self.mapView.mapboxMap.style.addSource(source)
            
            // Thêm layer cho các cluster
            try? self.addClusterLayers()
        }
    }
    
    private func addClusterLayers() throws {
        // Layer hiển thị các cluster dưới dạng hình tròn
        var circleLayer = CircleLayer(id: CLUSTER_CIRCLE_LAYER_ID, source: SOURCE_ID)
        circleLayer.filter = Exp(.has) {
            "point_count"
        }
        circleLayer.circleColor = .constant(.init(.systemBlue))
        circleLayer.circleOpacity = .constant(0.8)
        circleLayer.circleStrokeWidth = .constant(1)
        circleLayer.circleStrokeColor = .constant(.init(.white))
        circleLayer.circleRadius = .expression(
            Exp(.step) {
                Exp(.get) {
                    "point_count"
                }
                20   // Kích thước mặc định
                10   // Ngưỡng số điểm
                30   // Kích thước khi số điểm >= 10
                50   // Ngưỡng số điểm
                40   // Kích thước khi số điểm >= 50
            }
        )
        
        // Layer hiển thị số lượng điểm trong cluster
        var countLayer = SymbolLayer(id: CLUSTER_COUNT_LAYER_ID, source: SOURCE_ID)
        countLayer.filter = Exp(.has) {
            "point_count"
        }
        countLayer.textField = .expression(
            Exp(.toString) {
                Exp(.get) {
                    "point_count"
                }
            }
        )
        countLayer.textSize = .constant(12)
        countLayer.textColor = .constant(.init(.white))
        
        // Layer hiển thị điểm đơn lẻ
        var pointLayer = CircleLayer(id: UNCLUSTERED_POINT_LAYER_ID, source: SOURCE_ID)
        pointLayer.filter = Exp(.not) {
            Exp(.has) {
                "point_count"
            }
        }
        pointLayer.circleRadius = .constant(8)
        pointLayer.circleColor = .constant(.init(.systemGreen))
        pointLayer.circleStrokeWidth = .constant(1)
        pointLayer.circleStrokeColor = .constant(.init(.white))
        
        // Thêm các layer vào style
        try mapView.mapboxMap.style.addLayer(circleLayer)
        try mapView.mapboxMap.style.addLayer(countLayer)
        try mapView.mapboxMap.style.addLayer(pointLayer)
    }
    
    @objc private func handleMapTap(_ gesture: UITapGestureRecognizer) {
        let point = gesture.location(in: mapView)
        
        // Kiểm tra xem người dùng có tap vào cluster không
        mapView.mapboxMap.queryRenderedFeatures(
            at: point,
            options: RenderedQueryOptions(layerIds: [CLUSTER_CIRCLE_LAYER_ID], filter: nil)
        ) { [weak self] result in
            guard let self = self else { return }
            
            switch result {
            case .success(let features):
                guard let feature = features.first else { return }
                
                // Kiểm tra xem feature có phải là cluster không
                if let clusterID = feature.feature.properties?["cluster_id"]?.jsonValue as? Int {
                    // Xử lý tap vào cluster
                    self.handleClusterTap(clusterID: clusterID)
                }
            case .failure:
                break
            }
        }
    }
    
    private func handleClusterTap(clusterID: Int) {
        // Lấy tọa độ của các điểm trong cluster
        // Trong Mapbox iOS, thông thường phải gọi thêm một API để lấy các điểm
        // Ở đây chúng ta sẽ zoom vào cluster để hiển thị các điểm chi tiết hơn
        
        // Lấy tọa độ của cluster
        mapView.mapboxMap.querySourceFeatures(
            sourceId: SOURCE_ID,
            options: SourceQueryOptions(
                sourceLayerId: nil,
                filter: Exp(.eq) {
                    Exp(.get) {
                        "cluster_id"
                    }
                    .constant(NSNumber(value: clusterID))
                }
            )
        ) { [weak self] result in
            guard let self = self else { return }
            
            switch result {
            case .success(let features):
                guard let feature = features.first,
                      let geometry = feature.feature.geometry,
                      case .point(let point) = geometry else { return }
                
                // Zoom vào cluster
                let coordinate = CLLocationCoordinate2D(latitude: point.coordinates.latitude, longitude: point.coordinates.longitude)
                let currentZoom = self.mapView.cameraState.zoom
                
                self.mapView.camera.ease(
                    to: CameraOptions(center: coordinate, zoom: currentZoom + 2),
                    duration: 0.5
                )
            case .failure:
                break
            }
        }
    }
    
    @objc private func myLocationButtonTapped() {
        LocationManager.shared.getCurrentLocation { [weak self] location in
            guard let self = self, let location = location else { return }
            
            self.mapView.camera.ease(
                to: CameraOptions(center: location, zoom: 15),
                duration: 0.5
            )
        }
    }
    
    @objc private func styleSwitchButtonTapped() {
        // Hiển thị menu chọn style
        let alertController = UIAlertController(title: "Chọn style bản đồ", message: nil, preferredStyle: .actionSheet)
        
        let streetsAction = UIAlertAction(title: "Streets", style: .default) { [weak self] _ in
            self?.styleManager.applyStyle(.streets)
        }
        
        let satelliteAction = UIAlertAction(title: "Satellite", style: .default) { [weak self] _ in
            self?.styleManager.applyStyle(.satellite)
        }
        
        let nightAction = UIAlertAction(title: "Night", style: .default) { [weak self] _ in
            self?.styleManager.applyStyle(.night)
        }
        
        let cancelAction = UIAlertAction(title: "Hủy", style: .cancel)
        
        alertController.addAction(streetsAction)
        alertController.addAction(satelliteAction)
        alertController.addAction(nightAction)
        alertController.addAction(cancelAction)
        
        present(alertController, animated: true)
    }
    
    // Tạo dữ liệu mẫu cho bus stops
    private func createSampleBusStopFeatures() -> [Feature] {
        var features: [Feature] = []
        
        // Tạo điểm ngẫu nhiên dựa vào vị trí trung tâm của quốc gia
        let center = MapUtils.getInitialCoordinates(for: countryCode)
        
        for i in 0..<100 {
            // Tạo vị trí ngẫu nhiên trong phạm vi 0.1 độ từ trung tâm
            let lat = center.latitude + Double.random(in: -0.1...0.1)
            let lon = center.longitude + Double.random(in: -0.1...0.1)
            
            // Tạo feature
            let geometry = Geometry.point(Point(CLLocationCoordinate2D(latitude: lat, longitude: lon)))
            let properties: [String: Any] = [
                "id": i,
                "name": "Bus Stop \(i)",
                "type": "bus_stop"
            ]
            
            let feature = Feature(geometry: geometry, properties: properties)
            features.append(feature)
        }
        
        return features
    }
}
```

### 16.2 StyleManager

```swift
class StyleManager {
    enum MapStyle {
        case streets, satellite, night, simple, terrain
    }
    
    private weak var mapView: MapView?
    
    init(mapView: MapView) {
        self.mapView = mapView
    }
    
    static func getStyleURL(for countryCode: String, styleType: MapStyle) -> String {
        switch (countryCode, styleType) {
        case (_, .streets):
            switch countryCode {
            case "sg": return APIConstants.styleUrlSG
            case "th": return APIConstants.styleUrlTH
            default: return APIConstants.styleUrlVN
            }
        case (_, .satellite):
            switch countryCode {
            case "sg": return APIConstants.urlStyle3DSG
            case "th": return APIConstants.urlStyle3DTH
            default: return APIConstants.urlStyle3DVN
            }
        case (_, .night):
            switch countryCode {
            case "sg": return APIConstants.urlStyleNightSG
            case "th": return APIConstants.urlStyleNightTH
            default: return APIConstants.urlStyleNightVN
            }
        case (_, .simple):
            switch countryCode {
            case "sg": return APIConstants.urlStyleSimpleSG
            case "th": return APIConstants.urlStyleSimpleTH
            default: return APIConstants.urlStyleSimpleVN
            }
        case (_, .terrain):
            return APIConstants.urlStyle3DTerrainVN
        }
    }
    
    func applyStyle(_ style: MapStyle) {
        guard let mapView = mapView else { return }
        
        let countryCode = UserDefaults.standard.string(forKey: "country") ?? "vn"
        let styleURL = StyleManager.getStyleURL(for: countryCode, styleType: style)
        
        if let url = URL(string: styleURL) {
            mapView.mapboxMap.loadStyleURI(StyleURI(url: url))
        }
    }
}
```

### 16.3 LocationPermissionManager

```swift
class LocationPermissionManager {
    static let shared = LocationPermissionManager()
    
    func requestLocationPermission(completion: @escaping (Bool) -> Void) {
        // Handle iOS-specific location permissions
    }
}
```

## 17. Resources and Assets

For a complete iOS implementation, include:
1. Map marker icons in Assets.xcassets
2. Custom pins for different point types
3. Localization files
4. Gesture recognizers for map interactions

## 18. Phân tích chi tiết cấu trúc dự án

### 18.1 Cấu trúc thư mục chính

Dự án Android có cấu trúc thư mục sau:
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/trackasia/sample/
│   │   │   ├── adapter/
│   │   │   ├── api/
│   │   │   │   ├── Constants.kt
│   │   │   │   ├── model/
│   │   │   │   ├── repository/
│   │   │   │   ├── RetrofitClient.kt
│   │   │   │   └── service/
│   │   │   ├── custom/
│   │   │   ├── model/
│   │   │   ├── navigation/
│   │   │   ├── utils/
│   │   │   ├── MainActivity.kt
│   │   │   ├── MainViewModel.kt
│   │   │   ├── MapSinglePointFragment.kt
│   │   │   ├── MapMultiPointFragment.kt
│   │   │   ├── MapClusterFragment.kt
│   │   │   ├── MapAnimationFragment.kt
│   │   │   ├── MapFeatureFragment.kt
│   │   │   ├── MapFeatureSnapshotFragment.kt
│   │   │   ├── MapWayPointFragment.kt
│   │   │   └── MapDirectionPointFragment.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
```

### 18.2 Phân tích chi tiết các màn hình chính

#### 18.2.1 MainActivity

`MainActivity.kt` là điểm vào chính của ứng dụng, có trách nhiệm:
- Quản lý và điều hướng giữa các Fragment bản đồ
- Hiển thị thanh công cụ (toolbar) với chọn quốc gia (VN, Singapore, Thailand)
- Xử lý bottom navigation với 5 tùy chọn: Single Point, Multi-Point, Clusters, Animation, Features

Cấu trúc UI bao gồm:
1. AppBarLayout với thông tin quốc gia và tiêu đề
2. NavHostFragment để chứa các fragment bản đồ
3. BottomNavigationView với 5 tab điều hướng

**Triển khai iOS:**
```swift
import UIKit

class MainViewController: UIViewController {
    private var countryLabel: UILabel!
    private var titleLabel: UILabel!
    private var containerView: UIView!
    private var tabBar: UITabBar!
    private var childViewControllers: [UIViewController] = []
    private var currentViewController: UIViewController?
    private var preferences: UserDefaults!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Khởi tạo UserDefaults
        preferences = UserDefaults.standard
        let country = preferences.string(forKey: "country") ?? "vn"
        
        // Thiết lập UI
        setupUI(country: country)
        
        // Khởi tạo ViewControllers
        setupViewControllers()
        
        // Hiển thị màn hình đầu tiên (Single Point)
        showViewController(at: 0)
    }
    
    private func setupUI(country: String) {
        // Tạo phần header
        let headerView = UIView()
        headerView.backgroundColor = UIColor(named: "colorBlue")
        headerView.layer.cornerRadius = 12
        
        // Logo và tên app
        let logoImageView = UIImageView(image: UIImage(named: "ic_navigation_24"))
        logoImageView.tintColor = .white
        
        let appNameLabel = UILabel()
        appNameLabel.text = "TrackAsia"
        appNameLabel.textColor = .white
        appNameLabel.font = UIFont.boldSystemFont(ofSize: 16)
        
        titleLabel = UILabel()
        titleLabel.textColor = .white
        titleLabel.font = UIFont.systemFont(ofSize: 15)
        
        // Label hiển thị quốc gia
        countryLabel = UILabel()
        countryLabel.text = getCountryName(from: country)
        countryLabel.textColor = UIColor(named: "colorBlue")
        countryLabel.backgroundColor = .white
        countryLabel.font = UIFont.boldSystemFont(ofSize: 14)
        countryLabel.textAlignment = .center
        countryLabel.layer.cornerRadius = 8
        countryLabel.layer.masksToBounds = true
        
        // Thêm sự kiện cho countryLabel
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(showCountrySelector))
        countryLabel.isUserInteractionEnabled = true
        countryLabel.addGestureRecognizer(tapGesture)
        
        // Container cho các fragment
        containerView = UIView()
        
        // TabBar
        tabBar = UITabBar()
        tabBar.delegate = self
        
        let singlePointItem = UITabBarItem(title: "Single Point", image: UIImage(named: "ic_map_single"), tag: 0)
        let multiPointItem = UITabBarItem(title: "Multi-Point", image: UIImage(named: "ic_map_multi"), tag: 1)
        let clustersItem = UITabBarItem(title: "Clusters", image: UIImage(named: "ic_map_cluster"), tag: 2)
        let animationItem = UITabBarItem(title: "Animation", image: UIImage(named: "ic_map_animation"), tag: 3)
        let featuresItem = UITabBarItem(title: "Features", image: UIImage(named: "ic_feature"), tag: 4)
        
        tabBar.items = [singlePointItem, multiPointItem, clustersItem, animationItem, featuresItem]
        tabBar.selectedItem = singlePointItem
        
        // Add subviews and setup constraints
        // ...
    }
    
    private func setupViewControllers() {
        // Khởi tạo các view controller
        let singlePointVC = SinglePointMapViewController()
        let multiPointVC = MultiPointMapViewController()
        let clusterMapVC = ClusterMapViewController()
        let animationMapVC = AnimationMapViewController()
        let featureMapVC = FeatureMapViewController()
        
        childViewControllers = [singlePointVC, multiPointVC, clusterMapVC, animationMapVC, featureMapVC]
    }
    
    private func showViewController(at index: Int) {
        // Remove current view controller
        if let currentVC = currentViewController {
            currentVC.willMove(toParent: nil)
            currentVC.view.removeFromSuperview()
            currentVC.removeFromParent()
        }
        
        // Add new view controller
        let newVC = childViewControllers[index]
        addChild(newVC)
        containerView.addSubview(newVC.view)
        newVC.view.frame = containerView.bounds
        newVC.didMove(toParent: self)
        currentViewController = newVC
        
        // Update title
        titleLabel.text = tabBar.items?[index].title
    }
    
    @objc private func showCountrySelector() {
        let alertController = UIAlertController(title: "Chọn quốc gia", message: nil, preferredStyle: .actionSheet)
        
        let vietnamAction = UIAlertAction(title: "Việt Nam", style: .default) { [weak self] _ in
            self?.updateCountry("vn", name: "Việt Nam")
        }
        
        let singaporeAction = UIAlertAction(title: "Singapore", style: .default) { [weak self] _ in
            self?.updateCountry("sg", name: "Singapore")
        }
        
        let thailandAction = UIAlertAction(title: "Thailand", style: .default) { [weak self] _ in
            self?.updateCountry("th", name: "Thailand")
        }
        
        let cancelAction = UIAlertAction(title: "Hủy", style: .cancel)
        
        alertController.addAction(vietnamAction)
        alertController.addAction(singaporeAction)
        alertController.addAction(thailandAction)
        alertController.addAction(cancelAction)
        
        present(alertController, animated: true)
    }
    
    private func updateCountry(_ code: String, name: String) {
        preferences.set(code, forKey: "country")
        countryLabel.text = name
        
        // Refresh current view controller
        if let index = tabBar.items?.firstIndex(of: tabBar.selectedItem!) {
            // Tạo mới view controller với cấu hình quốc gia mới
            setupViewControllers()
            showViewController(at: index)
        }
    }
    
    private func getCountryName(from code: String) -> String {
        switch code {
        case "vn": return "Việt Nam"
        case "sg": return "Singapore"
        case "th": return "Thailand"
        default: return "Việt Nam"
        }
    }
}

// MARK: - UITabBarDelegate
extension MainViewController: UITabBarDelegate {
    func tabBar(_ tabBar: UITabBar, didSelect item: UITabBarItem) {
        showViewController(at: item.tag)
    }
}
```

#### 18.2.2 MapSinglePointFragment

`MapSinglePointFragment.kt` hiển thị một điểm trên bản đồ với các tính năng:
- Hiển thị điểm hiện tại của người dùng
- Đặt marker khi chạm vào bản đồ
- Tìm kiếm địa điểm và geocoding ngược
- Hiển thị thông tin địa điểm khi chọn

**Triển khai iOS chi tiết:**
```swift
import UIKit
import MapboxMaps

class SinglePointMapViewController: UIViewController {
    private var mapView: MapView!
    private var searchTextField: UITextField!
    private var myLocationButton: UIButton!
    private var coordinateLabel: UILabel!
    private var searchResultsTableView: UITableView!
    private var calculateDirectionButton: UIButton!
    
    private var suggestions: [Feature] = []
    private var marker: PointAnnotation?
    private var pointAnnotationManager: PointAnnotationManager!
    private var currentLocation: CLLocationCoordinate2D?
    private var targetLocation: CLLocationCoordinate2D?
    private var targetName: String = "Điểm đến"
    
    private var preferences: UserDefaults!
    private var countryCode: String = "vn"
    private var initialCenter: CLLocationCoordinate2D = CLLocationCoordinate2D(latitude: 10.728073, longitude: 106.624054)
    private var zoomLevel: Double = 16.0
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Khởi tạo preferences
        preferences = UserDefaults.standard
        countryCode = preferences.string(forKey: "country") ?? "vn"
        
        // Cấu hình vị trí ban đầu dựa trên quốc gia
        setupInitialLocation()
        
        // Thiết lập UI
        setupUI()
        
        // Khởi tạo bản đồ
        setupMapView()
        
        // Thiết lập gesture recognizers
        setupGestureRecognizers()
        
        // Thiết lập các listener
        setupListeners()
    }
    
    private func setupMapView() {
        // Khởi tạo MapboxMaps với style URL phù hợp
        let styleURL = getStyleURL(for: countryCode)
        
        let options = MapInitOptions(
            resourceOptions: ResourceOptions(accessToken: APIConstants.mapboxAccessToken),
            cameraOptions: CameraOptions(center: initialCenter, zoom: zoomLevel),
            styleURI: StyleURI(url: URL(string: styleURL)!)
        )
        
        mapView = MapView(frame: view.bounds, mapInitOptions: options)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(mapView)
        view.sendSubviewToBack(mapView)
        
        // Khởi tạo annotation manager
        pointAnnotationManager = mapView.annotations.makePointAnnotationManager()
        
        // Yêu cầu quyền vị trí và hiển thị vị trí người dùng
        requestLocationPermission()
    }
    
    private func setupUI() {
        // Tạo SearchTextField
        searchTextField = UITextField()
        searchTextField.placeholder = "Tìm địa điểm..."
        searchTextField.backgroundColor = .white
        searchTextField.layer.cornerRadius = 8
        searchTextField.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 10, height: searchTextField.frame.height))
        searchTextField.leftViewMode = .always
        searchTextField.clearButtonMode = .whileEditing
        
        // Tạo nút vị trí
        myLocationButton = UIButton(type: .system)
        myLocationButton.setImage(UIImage(systemName: "location.fill"), for: .normal)
        myLocationButton.backgroundColor = .white
        myLocationButton.layer.cornerRadius = 20
        
        // Label hiển thị thông tin tọa độ
        coordinateLabel = UILabel()
        coordinateLabel.backgroundColor = UIColor.black.withAlphaComponent(0.7)
        coordinateLabel.textColor = .white
        coordinateLabel.textAlignment = .center
        coordinateLabel.layer.cornerRadius = 8
        coordinateLabel.layer.masksToBounds = true
        coordinateLabel.numberOfLines = 0
        coordinateLabel.isHidden = true
        
        // TableView kết quả tìm kiếm
        searchResultsTableView = UITableView()
        searchResultsTableView.backgroundColor = .white
        searchResultsTableView.layer.cornerRadius = 8
        searchResultsTableView.isHidden = true
        searchResultsTableView.register(UITableViewCell.self, forCellReuseIdentifier: "Cell")
        
        // Nút tính toán tuyến đường
        calculateDirectionButton = UIButton(type: .system)
        calculateDirectionButton.setTitle("Tính tuyến đường", for: .normal)
        calculateDirectionButton.backgroundColor = UIColor(named: "colorBlue")
        calculateDirectionButton.setTitleColor(.white, for: .normal)
        calculateDirectionButton.layer.cornerRadius = 8
        calculateDirectionButton.isHidden = true
        
        // Thêm các view vào view chính
        view.addSubview(searchTextField)
        view.addSubview(myLocationButton)
        view.addSubview(coordinateLabel)
        view.addSubview(searchResultsTableView)
        view.addSubview(calculateDirectionButton)
        
        // Thiết lập constraints
        // ...
    }
    
    @objc private func handleMapTap(_ gesture: UITapGestureRecognizer) {
        // Ẩn bàn phím nếu đang hiển thị
        searchTextField.resignFirstResponder()
        
        // Lấy vị trí tap
        let point = gesture.location(in: mapView)
        let coordinate = mapView.mapboxMap.coordinate(for: point)
        
        // Đặt marker và hiển thị thông tin
        addMarker(at: coordinate)
        performReverseGeocoding(at: coordinate)
    }
    
    private func addMarker(at coordinate: CLLocationCoordinate2D) {
        // Xóa marker hiện tại nếu có
        pointAnnotationManager.annotations = []
        
        // Tạo marker mới
        var annotation = PointAnnotation(coordinate: coordinate)
        annotation.image = .init(image: UIImage(named: "map_marker")!, name: "marker")
        
        pointAnnotationManager.annotations = [annotation]
        marker = annotation
        targetLocation = coordinate
        
        // Hiển thị nút chỉ đường
        calculateDirectionButton.isHidden = false
    }
    
    private func performReverseGeocoding(at coordinate: CLLocationCoordinate2D) {
        GeocodingService.reverseGeocode(
            longitude: coordinate.longitude,
            latitude: coordinate.latitude,
            language: "vi"
        ) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let response):
                    if let feature = response.features.first {
                        self?.targetName = feature.properties.name
                        self?.coordinateLabel.text = """
                        \(feature.properties.name)
                        \(feature.properties.street), \(feature.properties.locality)
                        Lat: \(coordinate.latitude.rounded(toPlaces: 6)), Lng: \(coordinate.longitude.rounded(toPlaces: 6))
                        """
                        self?.coordinateLabel.isHidden = false
                    }
                case .failure(let error):
                    self?.coordinateLabel.text = """
                    Vị trí không xác định
                    Lat: \(coordinate.latitude.rounded(toPlaces: 6)), Lng: \(coordinate.longitude.rounded(toPlaces: 6))
                    """
                    self?.coordinateLabel.isHidden = false
                    print("Geocoding error: \(error.localizedDescription)")
                }
            }
        }
    }
    
    @objc private func myLocationButtonTapped() {
        if let userLocation = currentLocation {
            // Di chuyển camera đến vị trí người dùng
            mapView.camera.ease(
                to: CameraOptions(center: userLocation, zoom: zoomLevel),
                duration: 0.5
            )
            
            // Đặt marker tại vị trí người dùng
            addMarker(at: userLocation)
            performReverseGeocoding(at: userLocation)
        } else {
            // Yêu cầu quyền vị trí nếu chưa có
            requestLocationPermission()
        }
    }
    
    @objc private func searchTextChanged() {
        // Xử lý thay đổi text tìm kiếm
        guard let query = searchTextField.text, query.count >= 2 else {
            searchResultsTableView.isHidden = true
            return
        }
        
        // Gọi API autocomplete
        GeocodingService.getSuggestions(text: query) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let response):
                    self?.suggestions = response.features
                    self?.searchResultsTableView.reloadData()
                    self?.searchResultsTableView.isHidden = false
                case .failure:
                    self?.suggestions = []
                    self?.searchResultsTableView.isHidden = true
                }
            }
        }
    }
    
    @objc private func calculateDirectionButtonTapped() {
        guard let userLocation = currentLocation, let destination = targetLocation else {
            showAlert(title: "Lỗi", message: "Không thể tính toán tuyến đường. Vui lòng chọn điểm đến.")
            return
        }
        
        // Chuyển sang màn hình chỉ đường
        let routeVC = RouteViewController()
        routeVC.startLocation = userLocation
        routeVC.endLocation = destination
        routeVC.destinationName = targetName
        
        present(routeVC, animated: true)
    }
    
    private func getStyleURL(for countryCode: String) -> String {
        switch countryCode {
        case "sg": return APIConstants.styleUrlSG
        case "th": return APIConstants.styleUrlTH
        default: return APIConstants.styleUrlVN
        }
    }
    
    private func setupInitialLocation() {
        switch countryCode {
        case "sg":
            initialCenter = CLLocationCoordinate2D(latitude: 1.352083, longitude: 103.819839)
            zoomLevel = 12.0
        case "th":
            initialCenter = CLLocationCoordinate2D(latitude: 13.7563, longitude: 100.5018)
            zoomLevel = 11.0
        default: // vn
            initialCenter = CLLocationCoordinate2D(latitude: 10.728073, longitude: 106.624054)
            zoomLevel = 10.0
        }
    }
    
    private func requestLocationPermission() {
        // Implement location permission request
    }
}

// MARK: - UITableViewDataSource, UITableViewDelegate
extension SinglePointMapViewController: UITableViewDataSource, UITableViewDelegate {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return suggestions.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "Cell", for: indexPath)
        let feature = suggestions[indexPath.row]
        cell.textLabel?.text = feature.properties.name
        return cell
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        
        let feature = suggestions[indexPath.row]
        let coordinates = feature.geometry.coordinates
        let location = CLLocationCoordinate2D(latitude: coordinates[1], longitude: coordinates[0])
        
        // Cập nhật map và marker
        mapView.camera.ease(to: CameraOptions(center: location, zoom: zoomLevel), duration: 0.5)
        addMarker(at: location)
        
        // Cập nhật thông tin địa điểm
        targetName = feature.properties.