import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY")?.trim() ?: ""
val baseUrl: String = localProperties.getProperty("BASE_URL")?.trim() ?: ""
val razorpayKeyId: String = localProperties.getProperty("RAZORPAY_KEY_ID")?.trim() ?: ""
// Release always targets HTTPS; override via RELEASE_BASE_URL in local.properties/CI if needed.
val releaseBaseUrl: String = localProperties.getProperty("RELEASE_BASE_URL")?.trim()
    ?.takeIf { it.isNotEmpty() } ?: "https://api.plugsy.in/"

android {
    namespace = "com.ganesh.ev"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ganesh.ev"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        // Cleartext allowed by default (debug); release overrides to false below.
        manifestPlaceholders["usesCleartextTraffic"] = "true"
        buildConfigField("String", "BASE_URL", "\"${baseUrl}\"")
        buildConfigField("String", "RAZORPAY_KEY_ID", "\"${razorpayKeyId}\"")
        // Exposed for the Directions REST call in route planning (E2).
        buildConfigField("String", "MAPS_API_KEY", "\"${mapsApiKey}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Production: HTTPS only, no cleartext.
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            buildConfigField("String", "BASE_URL", "\"${releaseBaseUrl}\"")
        }
        debug {
            buildConfigField("String", "BASE_URL", "\"${baseUrl}\"")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Coil for images
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Google Maps
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    // PolyUtil for decoding the Directions overview polyline (E2).
    implementation("com.google.maps.android:android-maps-utils:3.8.2")
    
    // Razorpay
    implementation("com.razorpay:checkout:1.6.38")
    
    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended")

    // Firebase (Crashlytics + Analytics + Cloud Messaging)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.auth)

    // SMS User Consent API — auto-read the incoming OTP (one-tap, no READ_SMS).
    implementation(libs.play.services.auth.api.phone)

    // WorkManager — durable, retrying background work (A2).
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Room (offline cache)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt (dependency injection)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
