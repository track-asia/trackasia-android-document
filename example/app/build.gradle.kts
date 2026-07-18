plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.trackasia.trackasiademotest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.trackasia.trackasiademotest"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xskip-metadata-version-check")
    }

    viewBinding { enable = true }

    buildFeatures {  viewBinding = true }
}

dependencies {
    // TrackAsia SDK components (versions match demo project)
    implementation("io.github.track-asia:android-sdk:2.0.2")
    implementation("io.github.track-asia:geojson:2.0.2")
    implementation("io.github.track-asia:turf:2.0.2")
    implementation("io.github.track-asia:android-plugin-annotation-v9:2.0.1")
    implementation("io.github.track-asia:navigation-core:2.0.2")
    implementation("io.github.track-asia:navigation-ui-android:2.0.2")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.basement)
    implementation("com.google.android.gms:play-services-location:21.0.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Force consistent versions across all dependencies
configurations.all {
    resolutionStrategy {
        force("io.github.track-asia:android-sdk:2.0.2")
        force("io.github.track-asia:geojson:2.0.2")
        force("io.github.track-asia:turf:2.0.2")
        force("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:1.9.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.10")
    }
    exclude(group = "io.github.track-asia", module = "android-sdk-geojson")
    exclude(group = "io.github.track-asia", module = "android-sdk-turf")
}