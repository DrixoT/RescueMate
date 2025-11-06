plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
}

// Function to load .env file
fun loadEnvVariables(): Map<String, String> {
    val envFile = file("../.env")
    val envMap = mutableMapOf<String, String>()

    if (envFile.exists()) {
        envFile.readLines().forEach { line ->
            if (line.isNotBlank() && !line.startsWith("#")) {
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim().removeSurrounding("'").removeSurrounding("\"")
                    envMap[key] = value
                }
            }
        }
    }

    return envMap
}

val envVariables = loadEnvVariables()

android {
    namespace = "com.rescuemate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rescuemate"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Add BuildConfig fields from .env
        buildConfigField("String", "ELEVEN_API_KEY", "\"${envVariables["ELEVEN_API_KEY"] ?: ""}\"")
        buildConfigField("String", "ELEVEN_AGENT_ID", "\"${envVariables["ELEVEN_AGENT_ID"] ?: ""}\"")
        buildConfigField("String", "OPENAI_API_KEY", "\"${envVariables["OPENAI_API_KEY"] ?: ""}\"")
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"${envVariables["GOOGLE_MAPS_API_KEY"] ?: ""}\"")
        buildConfigField("String", "TWILIO_ACCOUNT_SID", "\"${envVariables["TWILIO_ACCOUNT_SID"] ?: ""}\"")
        buildConfigField("String", "TWILIO_AUTH_TOKEN", "\"${envVariables["TWILIO_AUTH_TOKEN"] ?: ""}\"")
        buildConfigField("String", "TWILIO_PHONE_NUMBER", "\"${envVariables["TWILIO_PHONE_NUMBER"] ?: ""}\"")

        // Add manifest placeholders for API keys
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = envVariables["GOOGLE_MAPS_API_KEY"] ?: ""
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    
    // Google Maps
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // HTTP Client for ElevenLabs API & Emergency Backend
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // ElevenLabs Conversational AI SDK
    implementation("io.elevenlabs:elevenlabs-android:0.1.1")

    // JSON parsing
    implementation("org.json:json:20231013")

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Image Loading (Coil)
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

