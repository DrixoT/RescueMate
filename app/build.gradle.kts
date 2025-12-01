plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
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
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rescuemate"
        minSdk = 26
        targetSdk = 35
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
        externalNativeBuild {
            cmake {
                arguments("-DANDROID_STL=c++_shared")
            }
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }
    buildToolsVersion = "36.1.0"
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Google Maps
    implementation("com.google.maps.android:maps-compose:6.2.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // HTTP Client for WebSocket support & Emergency Backend
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // ElevenLabs Official SDK for Conversational AI
    implementation("io.elevenlabs:elevenlabs-android:0.5.4")

    // JSON parsing
    implementation("org.json:json:20240303")

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Image Loading (Coil)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Vosk Offline Speech Recognition
    implementation("com.alphacephei:vosk-android:0.3.47")

    // Biometric Authentication
    implementation("androidx.biometric:biometric:1.1.0")

    // QR Code Scanning & Generation
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
