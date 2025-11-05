package com.rescuemate.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Permission Manager
 * Centralized permission handling for emergency features
 */
class PermissionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "PermissionManager"
        
        // Permission request codes
        const val REQUEST_LOCATION = 1001
        const val REQUEST_SMS = 1002
        const val REQUEST_PHONE = 1003
        const val REQUEST_ALL = 1004
    }
    
    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return fineLocation || coarseLocation
    }
    
    /**
     * Check if SMS permission is granted
     */
    fun hasSMSPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if phone call permission is granted
     */
    fun hasPhonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if body sensors permission is granted (for health monitoring)
     */
    fun hasBodySensorsPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BODY_SENSORS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true // Not required before Android Q
    }
    
    /**
     * Check if all essential permissions are granted
     */
    fun hasAllEssentialPermissions(): Boolean {
        return hasLocationPermission() && hasSMSPermission()
    }
    
    /**
     * Request location permission
     */
    fun requestLocationPermission(activity: Activity) {
        if (hasLocationPermission()) {
            Log.d(TAG, "Location permission already granted")
            return
        }
        
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            REQUEST_LOCATION
        )
    }
    
    /**
     * Request SMS permission
     */
    fun requestSMSPermission(activity: Activity) {
        if (hasSMSPermission()) {
            Log.d(TAG, "SMS permission already granted")
            return
        }
        
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.SEND_SMS),
            REQUEST_SMS
        )
    }
    
    /**
     * Request phone call permission
     */
    fun requestPhonePermission(activity: Activity) {
        if (hasPhonePermission()) {
            Log.d(TAG, "Phone permission already granted")
            return
        }
        
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CALL_PHONE),
            REQUEST_PHONE
        )
    }
    
    /**
     * Request all essential permissions at once
     */
    fun requestAllEssentialPermissions(activity: Activity) {
        val permissionsToRequest = mutableListOf<String>()
        
        if (!hasLocationPermission()) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        
        if (!hasSMSPermission()) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS)
        }
        
        if (!hasPhonePermission()) {
            permissionsToRequest.add(Manifest.permission.CALL_PHONE)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                REQUEST_ALL
            )
        } else {
            Log.d(TAG, "All essential permissions already granted")
        }
    }
    
    /**
     * Check if permission was granted based on request result
     */
    fun isPermissionGranted(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (grantResults.isEmpty()) {
            return false
        }
        
        return grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    }
    
    /**
     * Get missing permissions list
     */
    fun getMissingPermissions(): List<String> {
        val missing = mutableListOf<String>()
        
        if (!hasLocationPermission()) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (!hasSMSPermission()) {
            missing.add(Manifest.permission.SEND_SMS)
        }
        
        if (!hasPhonePermission()) {
            missing.add(Manifest.permission.CALL_PHONE)
        }
        
        return missing
    }
    
    /**
     * Check if we should show rationale for permission
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
    
    /**
     * Get permission explanation message
     */
    fun getPermissionExplanation(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> 
                "Location access is required to send your location to emergency contacts during emergencies."
            Manifest.permission.SEND_SMS -> 
                "SMS permission is required to send emergency alerts to contacts when backend is unavailable."
            Manifest.permission.CALL_PHONE -> 
                "Phone permission is required for emergency calling features."
            Manifest.permission.BODY_SENSORS -> 
                "Body sensors permission is required to monitor your heart rate and health data."
            else -> 
                "This permission is required for emergency features to work properly."
        }
    }
}

