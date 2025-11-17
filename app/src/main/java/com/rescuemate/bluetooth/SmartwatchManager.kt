package com.rescuemate.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.rescuemate.emergency.data.HealthData
import com.rescuemate.emergency.health.HealthMonitoringService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Smartwatch Manager
 * Coordinates BLE health device connections and integrates with health monitoring
 */
class SmartwatchManager(
    private val context: Context,
    private val healthMonitoringService: HealthMonitoringService
) {

    companion object {
        private const val TAG = "SmartwatchManager"
    }

    private val bleHealthService = BLEHealthService(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var currentHeartRate: Int? = null
    private var currentBloodPressure: Pair<Int, Int>? = null
    private var deviceBatteryLevel: Int? = null

    init {
        setupBLECallbacks()
    }

    /**
     * Setup BLE service callbacks
     */
    private fun setupBLECallbacks() {
        bleHealthService.onHeartRateUpdate = { heartRate ->
            currentHeartRate = heartRate
            Log.d(TAG, "Heart rate received: $heartRate BPM")
            
            // Record heart rate in health monitoring service
            scope.launch {
                healthMonitoringService.recordHeartRate(
                    heartRate = heartRate,
                    activityLevel = HealthData.ActivityLevel.UNKNOWN,
                    isExercising = false
                )
            }
        }

        bleHealthService.onBloodPressureUpdate = { systolic, diastolic ->
            currentBloodPressure = Pair(systolic, diastolic)
            Log.d(TAG, "Blood pressure received: $systolic/$diastolic mmHg")
        }

        bleHealthService.onBatteryUpdate = { batteryLevel ->
            deviceBatteryLevel = batteryLevel
            Log.d(TAG, "Device battery: $batteryLevel%")
        }

        bleHealthService.onConnectionStateChange = { isConnected ->
            Log.d(TAG, "Connection state changed: $isConnected")
            if (!isConnected) {
                currentHeartRate = null
                currentBloodPressure = null
                deviceBatteryLevel = null
            }
        }

        bleHealthService.onDeviceFound = { device ->
            Log.d(TAG, "Health device found: ${device.name} (${device.address})")
        }
    }

    /**
     * Start scanning for BLE health devices
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun startScanning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bleHealthService.startScanning()
        } else {
            Log.w(TAG, "BLE scanning requires Android 5.0+")
        }
    }

    /**
     * Stop scanning for devices
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun stopScanning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bleHealthService.stopScanning()
        }
    }

    /**
     * Connect to a smartwatch device
     */
    fun connectToDevice(device: BluetoothDevice): Boolean {
        return bleHealthService.connect(device)
    }

    /**
     * Disconnect from current device
     */
    fun disconnect() {
        bleHealthService.disconnect()
    }

    /**
     * Check if connected to a device
     */
    fun isConnected(): Boolean {
        return bleHealthService.isConnected()
    }

    /**
     * Get currently connected device
     */
    fun getConnectedDevice(): BluetoothDevice? {
        return bleHealthService.getConnectedDevice()
    }

    /**
     * Get current heart rate from smartwatch
     */
    fun getCurrentHeartRate(): Int? = currentHeartRate

    /**
     * Get current blood pressure from smartwatch
     */
    fun getCurrentBloodPressure(): Pair<Int, Int>? = currentBloodPressure

    /**
     * Get device battery level
     */
    fun getDeviceBatteryLevel(): Int? = deviceBatteryLevel

    /**
     * Get health data snapshot from smartwatch
     */
    fun getHealthDataSnapshot(): HealthData? {
        val heartRate = currentHeartRate ?: return null

        return HealthData(
            timestamp = System.currentTimeMillis(),
            currentHeartRate = heartRate,
            normalHeartRate = healthMonitoringService.getBaselineHeartRate(),
            heartRateTrend = listOf(heartRate), // Single reading for now
            riskScore = 0f, // Will be calculated by health monitoring service
            alertReason = "Monitoring active",
            activityLevel = HealthData.ActivityLevel.UNKNOWN,
            isExercising = false
        )
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        bleHealthService.cleanup()
        scope.cancel()
    }
}

