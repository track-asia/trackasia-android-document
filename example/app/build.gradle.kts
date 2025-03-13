plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.trackasia.trackasiademotest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.trackasia.trackasiademotest"
        minSdk = 24
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
    }

    viewBinding { enable = true }

    buildFeatures {  viewBinding = true }
}

dependencies {
    implementation("io.github.track-asia:android-sdk:2.0.1")
    implementation("io.github.track-asia:android-sdk-geojson:2.0.1")
    implementation("io.github.track-asia:android-sdk-turf:2.0.1")
    implementation("io.github.track-asia:android-plugin-annotation-v9:2.0.1")
    implementation("io.github.track-asia:libandroid-navigation:2.0.0")
    implementation("io.github.track-asia:libandroid-navigation-ui:2.0.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.basement)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}