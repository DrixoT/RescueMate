# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# ===============================
# RESCUEMATE SECURITY CONFIGURATION
# ===============================

# Obfuscate API keys and sensitive strings
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep BuildConfig for API keys (will be obfuscated)
-keep class com.rescuemate.BuildConfig { *; }

# ===============================
# DATA MODELS (PARCELIZE)
# ===============================

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep data classes
-keep class com.rescuemate.emergency.data.** { *; }

# ===============================
# GOOGLE SERVICES
# ===============================

# Google Maps
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**

# ===============================
# KOTLIN
# ===============================

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ===============================
# JETPACK COMPOSE
# ===============================

-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ===============================
# ELEVENLABS SDK
# ===============================

-keep class io.elevenlabs.** { *; }
-keepclassmembers class io.elevenlabs.** { *; }
-dontwarn io.elevenlabs.**

# ===============================
# OKHTTP & NETWORKING
# ===============================

-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ===============================
# JSON PARSING
# ===============================

-keep class org.json.** { *; }
-keepclassmembers class org.json.** { *; }

# ===============================
# LLAMATIK (TINYLLAMA)
# ===============================

-keep class com.llamatik.** { *; }
-keepclassmembers class com.llamatik.** { *; }
-dontwarn com.llamatik.**

# ===============================
# ANDROIDX LIBRARIES
# ===============================

-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# Keep all classes with @Keep annotation
-keep class * {
    @androidx.annotation.Keep *;
}

# ===============================
# REFLECTION
# ===============================

-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ===============================
# NATIVE METHODS
# ===============================

-keepclasseswithmembernames class * {
    native <methods>;
}

# ===============================
# ENUMS
# ===============================

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===============================
# REMOVE LOGGING IN RELEASE
# ===============================

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# Keep source file names and line numbers for stack traces
-keepattributes SourceFile,LineNumberTable

# Rename source file attribute to hide original source file name
-renamesourcefileattribute SourceFile

