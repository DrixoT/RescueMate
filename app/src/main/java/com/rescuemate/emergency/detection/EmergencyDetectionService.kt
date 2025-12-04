package com.rescuemate.emergency.detection

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log
import kotlin.math.sqrt

/**
 * Shake Detection Service
 * Detects emergency shake pattern (3+ vigorous shakes in 2 seconds)
 */
class ShakeDetectionService(
    private val context: Context,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val shakeTimestamps = mutableListOf<Long>()
    private var lastShakeTime = 0L

    companion object {
        private const val SHAKE_THRESHOLD = 15.0 // m/s^2
        private const val SHAKE_COUNT_THRESHOLD = 3
        private const val SHAKE_WINDOW_MS = 2000L
        private const val SHAKE_COOLDOWN_MS = 1000L // Prevent duplicate detections
    }

    private var isEnabled = false

    /**
     * Start listening for shake gestures
     */
    fun startListening() {
        if (!isEnabled && accelerometer != null) {
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            isEnabled = true
        }
    }

    /**
     * Stop listening for shake gestures
     */
    fun stopListening() {
        if (isEnabled) {
            sensorManager.unregisterListener(this)
            isEnabled = false
            shakeTimestamps.clear()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate acceleration magnitude
        val acceleration = sqrt((x * x + y * y + z * z).toDouble()) - SensorManager.GRAVITY_EARTH

        if (acceleration > SHAKE_THRESHOLD) {
            val currentTime = System.currentTimeMillis()

            // Check cooldown period
            if (currentTime - lastShakeTime < SHAKE_COOLDOWN_MS) {
                return
            }

            // Add shake timestamp
            shakeTimestamps.add(currentTime)
            lastShakeTime = currentTime

            // Remove old timestamps outside window
            shakeTimestamps.removeAll { currentTime - it > SHAKE_WINDOW_MS }

            // Check if threshold met
            if (shakeTimestamps.size >= SHAKE_COUNT_THRESHOLD) {
                onShakeDetected()
                shakeTimestamps.clear()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    fun isListening(): Boolean = isEnabled
}

/**
 * Volume Button Emergency Detection
 * Detects volume up + volume down pressed simultaneously for 2 seconds
 */
class VolumeButtonEmergencyDetector(
    private val onEmergencyDetected: () -> Unit
) {

    private var isVolumeUpPressed = false
    private var isVolumeDownPressed = false
    private var bothPressedStartTime = 0L

    private val scope = CoroutineScope(Dispatchers.Main)
    private var checkJob: Job? = null

    fun onKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        // Only handle volume keys
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
             return false
        }

        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> isVolumeUpPressed = true
                KeyEvent.KEYCODE_VOLUME_DOWN -> isVolumeDownPressed = true
            }
        } else if (event.action == KeyEvent.ACTION_UP) {
             when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> isVolumeUpPressed = false
                KeyEvent.KEYCODE_VOLUME_DOWN -> isVolumeDownPressed = false
            }
        }

        checkState()
        return false // Don't consume, let system handle volume
    }

    private fun checkState() {
        if (isVolumeUpPressed && isVolumeDownPressed) {
            if (bothPressedStartTime == 0L) {
                bothPressedStartTime = System.currentTimeMillis()
                Log.d("VolumeDetector", "Both buttons pressed, starting timer...")
                
                checkJob?.cancel()
                checkJob = scope.launch {
                    delay(2000) // 2 seconds hold
                    if (isVolumeUpPressed && isVolumeDownPressed) {
                        Log.d("VolumeDetector", "Panic! Triggering emergency.")
                        onEmergencyDetected()
                        reset()
                    }
                }
            }
        } else {
            if (bothPressedStartTime != 0L) {
                 Log.d("VolumeDetector", "Buttons released, cancelling timer.")
            }
            bothPressedStartTime = 0L
            checkJob?.cancel()
        }
    }
    
    fun reset() {
        isVolumeUpPressed = false
        isVolumeDownPressed = false
        bothPressedStartTime = 0L
        checkJob?.cancel()
    }
}

/**
 * Scheduled Check-in Service
 * Monitors user check-ins and triggers emergency if missed
 */
class ScheduledCheckInService(
    private val context: Context,
    private val onCheckInMissed: () -> Unit
) {

    private val prefs = context.getSharedPreferences("emergency_checkin", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_LAST_CHECKIN_TIME = "last_checkin_time"
        private const val PREF_CHECKIN_INTERVAL_MINUTES = "checkin_interval_minutes"
        private const val PREF_CHECKIN_ENABLED = "checkin_enabled"
        private const val DEFAULT_CHECKIN_INTERVAL_MINUTES = 60 // 1 hour
    }

    /**
     * Record user check-in
     */
    fun recordCheckIn() {
        prefs.edit()
            .putLong(PREF_LAST_CHECKIN_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * Set check-in interval
     */
    fun setCheckInInterval(minutes: Int) {
        prefs.edit()
            .putInt(PREF_CHECKIN_INTERVAL_MINUTES, minutes)
            .apply()
    }

    /**
     * Get check-in interval
     */
    fun getCheckInInterval(): Int {
        return prefs.getInt(PREF_CHECKIN_INTERVAL_MINUTES, DEFAULT_CHECKIN_INTERVAL_MINUTES)
    }

    /**
     * Enable/disable scheduled check-ins
     */
    fun setCheckInEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(PREF_CHECKIN_ENABLED, enabled)
            .apply()

        if (enabled) {
            recordCheckIn() // Reset timer
        }
    }

    /**
     * Check if check-in is enabled
     */
    fun isCheckInEnabled(): Boolean {
        return prefs.getBoolean(PREF_CHECKIN_ENABLED, false)
    }

    /**
     * Check if check-in is overdue
     */
    fun isCheckInOverdue(): Boolean {
        if (!isCheckInEnabled()) return false

        val lastCheckIn = prefs.getLong(PREF_LAST_CHECKIN_TIME, 0L)
        if (lastCheckIn == 0L) return false

        val intervalMs = getCheckInInterval() * 60 * 1000L
        val timeSinceLastCheckIn = System.currentTimeMillis() - lastCheckIn

        return timeSinceLastCheckIn > intervalMs
    }

    /**
     * Get time until next check-in (milliseconds)
     */
    fun getTimeUntilNextCheckIn(): Long {
        if (!isCheckInEnabled()) return Long.MAX_VALUE

        val lastCheckIn = prefs.getLong(PREF_LAST_CHECKIN_TIME, System.currentTimeMillis())
        val intervalMs = getCheckInInterval() * 60 * 1000L
        val nextCheckInTime = lastCheckIn + intervalMs

        return (nextCheckInTime - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    /**
     * Get last check-in time
     */
    fun getLastCheckInTime(): Long {
        return prefs.getLong(PREF_LAST_CHECKIN_TIME, 0L)
    }

    /**
     * Perform check-in verification
     * Call this periodically to check if check-in is overdue
     */
    fun verifyCheckIn() {
        if (isCheckInOverdue()) {
            onCheckInMissed()
        }
    }
}

/**
 * Fall Detection Service (Future Enhancement)
 * Placeholder for future accelerometer-based fall detection
 */
class FallDetectionService(
    private val context: Context,
    private val onFallDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    companion object {
        private const val FALL_THRESHOLD = 25.0 // m/s^2 (sudden acceleration)
        private const val STATIONARY_THRESHOLD = 2.0 // m/s^2 (little movement after fall)
        private const val STATIONARY_DURATION_MS = 3000L // 3 seconds stationary
    }

    private var isEnabled = false
    private var possibleFallTime = 0L
    private var isStationaryAfterFall = false

    fun startListening() {
        if (!isEnabled && accelerometer != null) {
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            isEnabled = true
        }
    }

    fun stopListening() {
        if (isEnabled) {
            sensorManager.unregisterListener(this)
            isEnabled = false
            possibleFallTime = 0L
            isStationaryAfterFall = false
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = sqrt((x * x + y * y + z * z).toDouble())
        val currentTime = System.currentTimeMillis()

        // Detect sudden acceleration (possible fall)
        if (acceleration > FALL_THRESHOLD && possibleFallTime == 0L) {
            possibleFallTime = currentTime
            isStationaryAfterFall = false
        }

        // Check if stationary after possible fall
        if (possibleFallTime > 0L) {
            val timeSinceFall = currentTime - possibleFallTime

            if (acceleration < STATIONARY_THRESHOLD) {
                if (timeSinceFall >= STATIONARY_DURATION_MS) {
                    // Fall detected: sudden acceleration followed by prolonged stillness
                    onFallDetected()
                    possibleFallTime = 0L
                    isStationaryAfterFall = false
                }
            } else {
                // Reset if user is moving normally
                if (timeSinceFall > STATIONARY_DURATION_MS) {
                    possibleFallTime = 0L
                    isStationaryAfterFall = false
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    fun isListening(): Boolean = isEnabled
}

