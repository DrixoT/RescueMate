package com.rescuemate.emergency.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.rescuemate.emergency.EmergencyConstants
import com.rescuemate.emergency.data.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume

/**
 * Emergency Location Service
 * Provides high-accuracy location tracking for emergencies
 */
class EmergencyLocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    private var locationCallback: LocationCallback? = null
    private var lastKnownLocation: LocationData? = null

    companion object {
        private const val MAX_GEOCODING_RESULTS = 1
        private const val GEOCODING_TIMEOUT_MS = 5000L
    }

    /**
     * Get current high-accuracy location
     */
    suspend fun getCurrentLocation(): Result<LocationData> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext Result.failure(SecurityException("Location permission not granted"))
        }

        try {
            val location = getLastKnownLocationAsync() ?: return@withContext Result.failure(
                Exception(EmergencyConstants.ERROR_LOCATION_UNAVAILABLE)
            )

            val locationData = convertToLocationData(location)
            lastKnownLocation = locationData

            Result.success(locationData)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Get last known location asynchronously
     */
    private suspend fun getLastKnownLocationAsync(): Location? = suspendCancellableCoroutine { continuation ->
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        // Try to get last known location first
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                continuation.resume(location)
            } else {
                // Request fresh location if no last known location
                requestFreshLocation { freshLocation ->
                    continuation.resume(freshLocation)
                }
            }
        }.addOnFailureListener {
            continuation.resume(null)
        }
    }

    /**
     * Request fresh high-accuracy location
     */
    private fun requestFreshLocation(callback: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null)
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            EmergencyConstants.LOCATION_UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(EmergencyConstants.LOCATION_FASTEST_INTERVAL_MS)
            setMaxUpdateDelayMillis(10000L)
            setWaitForAccurateLocation(true)
        }.build()

        val tempCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                fusedLocationClient.removeLocationUpdates(this)
                callback(location)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                tempCallback,
                Looper.getMainLooper()
            )

            // Timeout after 10 seconds
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                fusedLocationClient.removeLocationUpdates(tempCallback)
                callback(null)
            }, 10000L)

        } catch (e: SecurityException) {
            callback(null)
        }
    }

    /**
     * Convert Android Location to LocationData
     */
    private suspend fun convertToLocationData(location: Location): LocationData = withContext(Dispatchers.IO) {
        val address = try {
            geocodeLocation(location.latitude, location.longitude)
        } catch (e: Exception) {
            null
        }

        LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            address = address,
            accuracy = location.accuracy,
            timestamp = location.time,
            altitude = if (location.hasAltitude()) location.altitude else null,
            speed = if (location.hasSpeed()) location.speed else null,
            isIndoor = location.accuracy > 50f, // Rough estimate
            locationContext = determineLocationContext(location)
        )
    }

    /**
     * Geocode coordinates to address
     */
    private suspend fun geocodeLocation(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocation(
                latitude,
                longitude,
                MAX_GEOCODING_RESULTS
            )

            addresses?.firstOrNull()?.let { address ->
                buildAddressString(address)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Build human-readable address string
     */
    private fun buildAddressString(address: Address): String {
        val parts = mutableListOf<String>()

        // Street address
        if (address.maxAddressLineIndex >= 0) {
            parts.add(address.getAddressLine(0))
        } else {
            address.thoroughfare?.let { parts.add(it) }
            address.subThoroughfare?.let { parts.add(it) }
        }

        // City
        address.locality?.let { parts.add(it) }

        // State
        address.adminArea?.let { parts.add(it) }

        // Postal code
        address.postalCode?.let { parts.add(it) }

        // Country
        address.countryName?.let { parts.add(it) }

        return parts.joinToString(", ")
    }

    /**
     * Determine location context (home, work, etc.)
     */
    private fun determineLocationContext(location: Location): LocationData.LocationContext {
        // This would integrate with saved locations in the app
        // For now, return unknown
        return LocationData.LocationContext.UNKNOWN
    }

    /**
     * Start continuous location updates
     */
    fun startLocationUpdates(onLocationUpdate: (LocationData) -> Unit) {
        if (!hasLocationPermission()) return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            EmergencyConstants.LOCATION_UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(EmergencyConstants.LOCATION_FASTEST_INTERVAL_MS)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        val locationData = convertToLocationData(location)
                        lastKnownLocation = locationData
                        withContext(Dispatchers.Main) {
                            onLocationUpdate(locationData)
                        }
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Stop location updates
     */
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    /**
     * Get last known cached location
     */
    fun getLastKnownLocationData(): LocationData? = lastKnownLocation

    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Calculate distance between two locations (in meters)
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    /**
     * Check if location is accurate enough for emergency
     */
    fun isLocationAccurate(locationData: LocationData): Boolean {
        return locationData.accuracy < EmergencyConstants.LOCATION_ACCURACY_THRESHOLD_METERS
    }
}


