package com.rescuemate.emergency.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rescuemate.emergency.EmergencyConstants
import com.rescuemate.emergency.EmergencyManager
import com.rescuemate.emergency.data.HealthData
import com.rescuemate.emergency.data.MedicalInfo
import com.rescuemate.emergency.data.UserInfo
import com.rescuemate.emergency.detection.*
import com.rescuemate.emergency.health.HealthMonitoringService
import com.rescuemate.emergency.health.MockSensorDataService
import kotlinx.coroutines.*

/**
 * Emergency Background Service
 * Continuously monitors for emergency triggers (health, shake, volume buttons, check-in)
 */
class EmergencyBackgroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var emergencyManager: EmergencyManager
    private lateinit var healthMonitoring: HealthMonitoringService
    private val mockSensorService = MockSensorDataService()

    // Detection services
    private var shakeDetection: ShakeDetectionService? = null
    private var volumeButtonDetector: VolumeButtonEmergencyDetector? = null
    private var checkInService: ScheduledCheckInService? = null
    private var fallDetection: FallDetectionService? = null

    // Health monitoring
    private var healthMonitoringJob: Job? = null
    private var checkInMonitoringJob: Job? = null
    
    // Mock sensor configuration
    private var mockAnomalyProbability: Float = 0.05f // 5% default
    private var simulateExercise: Boolean = false
    
    // Wake lock to keep service running
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val NOTIFICATION_ID = 9001
        private const val CHANNEL_ID = "emergency_service_channel"

        const val ACTION_START_MONITORING = "com.rescuemate.emergency.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.rescuemate.emergency.STOP_MONITORING"
        const val ACTION_TRIGGER_EMERGENCY = "com.rescuemate.emergency.TRIGGER_EMERGENCY"
        const val ACTION_CANCEL_EMERGENCY = "com.rescuemate.emergency.CANCEL_EMERGENCY"

        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_USER_NAME = "user_name"
        const val EXTRA_USER_AGE = "user_age"
        const val EXTRA_USER_PHONE = "user_phone"
        const val EXTRA_ENABLE_SHAKE = "enable_shake"
        const val EXTRA_ENABLE_VOLUME = "enable_volume"
        const val EXTRA_ENABLE_CHECKIN = "enable_checkin"
        const val EXTRA_ENABLE_HEALTH = "enable_health"
        const val EXTRA_LLM_API_KEY = "llm_api_key"
    }

    override fun onCreate() {
        super.onCreate()

        android.util.Log.d("EmergencyBackgroundService", "onCreate() called")

        try {
            // CRITICAL: Create notification channel FIRST (required for foreground service)
            createNotificationChannel()
            android.util.Log.d("EmergencyBackgroundService", "Notification channel created")

            // Initialize managers
            emergencyManager = EmergencyManager(this)
            healthMonitoring = HealthMonitoringService(this)
            android.util.Log.d("EmergencyBackgroundService", "Managers initialized")

            // Initialize mock sensor with baseline heart rate
            try {
                val baselineHR = healthMonitoring.getBaselineHeartRate()
                mockSensorService.setBaselineHeartRate(baselineHR)
                android.util.Log.d("EmergencyBackgroundService", "Mock sensor initialized with baseline: $baselineHR")
            } catch (e: Exception) {
                android.util.Log.e("EmergencyBackgroundService", "Failed to init mock sensor", e)
                // Use default baseline
                mockSensorService.setBaselineHeartRate(70)
            }

            // Acquire wake lock to keep service running
            acquireWakeLock()

            android.util.Log.d("EmergencyBackgroundService", "✅ onCreate() completed successfully")

        } catch (e: Exception) {
            android.util.Log.e("EmergencyBackgroundService", "❌ CRITICAL ERROR in onCreate()", e)
            // Don't throw - let the service continue with minimal functionality
        }
    }
    
    /**
     * Acquire wake lock to prevent device from sleeping
     */
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RescueMate::EmergencyServiceWakeLock"
            ).apply {
                acquire(10 * 60 * 60 * 1000L /*10 hours*/)
            }
            android.util.Log.d("EmergencyBackgroundService", "Wake lock acquired")
        } catch (e: Exception) {
            android.util.Log.e("EmergencyBackgroundService", "Failed to acquire wake lock", e)
        }
    }
    
    /**
     * Release wake lock
     */
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    android.util.Log.d("EmergencyBackgroundService", "Wake lock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            android.util.Log.e("EmergencyBackgroundService", "Failed to release wake lock", e)
        }
    }
    
    /**
     * Check if required permissions are granted
     */
    private fun checkPermissions(): Boolean {
        val requiredPermissions = mutableListOf<String>()
        
        // Location permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        // SMS permission (optional but recommended)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.w("EmergencyBackgroundService", 
                "SMS permission not granted - fallback SMS may not work")
        }
        
        if (requiredPermissions.isNotEmpty()) {
            android.util.Log.w("EmergencyBackgroundService", 
                "Missing permissions: ${requiredPermissions.joinToString()}")
            return false
        }
        
        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("EmergencyBackgroundService", "onStartCommand called with action: ${intent?.action}")

        // CRITICAL: For foreground services, call startForeground IMMEDIATELY
        // This MUST happen within 5 seconds on Android 8+
        if (intent?.action == ACTION_START_MONITORING) {
            try {
                android.util.Log.d("EmergencyBackgroundService", "Creating foreground notification...")
                val notification = createForegroundNotification()
                startForeground(NOTIFICATION_ID, notification)
                android.util.Log.d("EmergencyBackgroundService", "✅ startForeground() called successfully!")
            } catch (e: Exception) {
                android.util.Log.e("EmergencyBackgroundService", "❌ Failed to start foreground", e)
                // Try minimal notification as fallback
                try {
                    val minimalNotification = createMinimalNotification()
                    startForeground(NOTIFICATION_ID, minimalNotification)
                    android.util.Log.d("EmergencyBackgroundService", "✅ Started with minimal notification")
                } catch (e2: Exception) {
                    android.util.Log.e("EmergencyBackgroundService", "❌ FATAL: Cannot start foreground service", e2)
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }

        // NOW handle the actions (after foreground is started)
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                val userId = intent.getStringExtra(EXTRA_USER_ID) ?: "user_${System.currentTimeMillis()}"
                val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: "User"
                val userAge = intent.getIntExtra(EXTRA_USER_AGE, 0)
                val userPhone = intent.getStringExtra(EXTRA_USER_PHONE) ?: ""

                val enableShake = intent.getBooleanExtra(EXTRA_ENABLE_SHAKE, true)
                val enableVolume = intent.getBooleanExtra(EXTRA_ENABLE_VOLUME, true)
                val enableCheckIn = intent.getBooleanExtra(EXTRA_ENABLE_CHECKIN, false)
                val enableHealth = intent.getBooleanExtra(EXTRA_ENABLE_HEALTH, true)
                val llmApiKey = intent.getStringExtra(EXTRA_LLM_API_KEY)

                // Start monitoring in background (don't block)
                scope.launch {
                    try {
                        startMonitoring(
                            userId, userName, userAge, userPhone,
                            enableShake, enableVolume, enableCheckIn, enableHealth, llmApiKey
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("EmergencyBackgroundService", "Error in startMonitoring", e)
                    }
                }
            }
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                stopSelf()
            }
            ACTION_TRIGGER_EMERGENCY -> {
                val userId = intent.getStringExtra(EXTRA_USER_ID) ?: return START_NOT_STICKY
                triggerManualEmergency(userId)
            }
            ACTION_CANCEL_EMERGENCY -> {
                emergencyManager.userConfirmSafe()
            }
        }

        return START_STICKY
    }

    private fun startMonitoring(
        userId: String,
        userName: String,
        userAge: Int,
        userPhone: String,
        enableShake: Boolean,
        enableVolume: Boolean,
        enableCheckIn: Boolean,
        enableHealth: Boolean,
        llmApiKey: String?
    ) {
        android.util.Log.d("EmergencyBackgroundService", "Starting monitoring for user: $userId")

        // Check permissions (non-blocking)
        if (!checkPermissions()) {
            android.util.Log.w("EmergencyBackgroundService",
                "Some permissions missing - some features may not work")
        }

        // Ensure wake lock is acquired
        try {
            if (wakeLock == null || !wakeLock!!.isHeld) {
                acquireWakeLock()
            }
        } catch (e: Exception) {
            android.util.Log.e("EmergencyBackgroundService", "Failed to acquire wake lock", e)
        }

        // Initialize shake detection
        if (enableShake) {
            try {
                shakeDetection = ShakeDetectionService(this) {
                    onEmergencyTrigger(userId, userName, userAge, userPhone, "Shake detected")
                }.also { it.startListening() }
                android.util.Log.d("EmergencyBackgroundService", "Shake detection initialized")
            } catch (e: Exception) {
                android.util.Log.e("EmergencyBackgroundService", "Failed to initialize shake detection", e)
            }
        }

        // Initialize volume button detection
        if (enableVolume) {
            try {
                volumeButtonDetector = VolumeButtonEmergencyDetector {
                    onEmergencyTrigger(userId, userName, userAge, userPhone, "Volume button sequence")
                }
                android.util.Log.d("EmergencyBackgroundService", "Volume button detection initialized")
            } catch (e: Exception) {
                android.util.Log.e("EmergencyBackgroundService", "Failed to initialize volume button detection", e)
            }
        }

        // Initialize check-in service
        if (enableCheckIn) {
            try {
                checkInService = ScheduledCheckInService(this) {
                    onEmergencyTrigger(userId, userName, userAge, userPhone, "Check-in missed")
                }

                // Monitor check-ins
                checkInMonitoringJob = scope.launch {
                    while (isActive) {
                        checkInService?.verifyCheckIn()
                        delay(60_000L) // Check every minute
                    }
                }
                android.util.Log.d("EmergencyBackgroundService", "Check-in service initialized")
            } catch (e: Exception) {
                android.util.Log.e("EmergencyBackgroundService", "Failed to initialize check-in service", e)
            }
        }

        // Initialize health monitoring
        if (enableHealth) {
            try {
                startHealthMonitoring(userId, userName, userAge, userPhone, llmApiKey)
                android.util.Log.d("EmergencyBackgroundService", "Health monitoring initialized")
            } catch (e: Exception) {
                android.util.Log.e("EmergencyBackgroundService", "Failed to initialize health monitoring", e)
            }
        }

        // Initialize fall detection (future)
        try {
            fallDetection = FallDetectionService(this) {
                onEmergencyTrigger(userId, userName, userAge, userPhone, "Fall detected")
            }
            android.util.Log.d("EmergencyBackgroundService", "Fall detection initialized")
        } catch (e: Exception) {
            android.util.Log.e("EmergencyBackgroundService", "Failed to initialize fall detection", e)
        }

        android.util.Log.d("EmergencyBackgroundService", "✅ Monitoring started successfully for user: $userId")
    }

    private fun startHealthMonitoring(
        userId: String,
        userName: String,
        userAge: Int,
        userPhone: String,
        llmApiKey: String?
    ) {
        healthMonitoringJob = scope.launch {
            while (isActive) {
                try {
                    // Generate mock heart rate reading
                    val reading = mockSensorService.generateHeartRate(
                        variationLevel = 0.5f,
                        anomalyProbability = mockAnomalyProbability,
                        simulateExercise = simulateExercise
                    )
                    
                    val currentHeartRate = reading.heartRate
                    android.util.Log.d("EmergencyBackgroundService", 
                        "📊 Heart rate reading: $currentHeartRate BPM " +
                        "(baseline: ${mockSensorService.getBaselineHeartRate()}, " +
                        "anomaly: ${reading.isAnomaly})")

                    // Save current heart rate to SharedPreferences for UI display
                    val prefs = getSharedPreferences(EmergencyConstants.PREF_NAME_EMERGENCY, Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putInt("current_heart_rate", currentHeartRate)
                        putLong("last_heart_rate_update", System.currentTimeMillis())
                        apply()
                    }

                    // Record reading
                    healthMonitoring.recordHeartRate(
                        currentHeartRate,
                        reading.activityLevel.toHealthDataActivityLevel(),
                        reading.isExercising
                    )

                    // Analyze health status
                    val analysis = healthMonitoring.analyzeHealthStatus(
                        currentHeartRate = currentHeartRate,
                        activityLevel = reading.activityLevel.toHealthDataActivityLevel(),
                        isExercising = reading.isExercising,
                        llmApiKey = llmApiKey
                    )

                    // Log analysis result
                    android.util.Log.d("EmergencyBackgroundService",
                        "🔍 Health analysis: abnormal=${analysis.isAbnormal}, " +
                        "riskScore=${analysis.riskScore}, " +
                        "reason=${analysis.alertReason}")

                    // Check if emergency should be triggered
                    if (healthMonitoring.shouldTriggerEmergency(analysis)) {
                        android.util.Log.w("EmergencyBackgroundService",
                            "🚨 EMERGENCY TRIGGERED: ${analysis.alertReason}")
                        
                        val healthData = healthMonitoring.getCurrentHealthData(
                            currentHeartRate,
                            analysis,
                            reading.activityLevel.toHealthDataActivityLevel(),
                            reading.isExercising
                        )

                        triggerHealthEmergency(userId, userName, userAge, userPhone, healthData)
                    }

                } catch (e: Exception) {
                    android.util.Log.e("EmergencyBackgroundService", "Error in health monitoring", e)
                }

                // Wait for next reading
                delay(EmergencyConstants.HEART_RATE_SAMPLE_INTERVAL_MS)
            }
        }
    }
    
    /**
     * Set mock sensor anomaly probability (for testing)
     */
    fun setMockAnomalyProbability(probability: Float) {
        mockAnomalyProbability = probability.coerceIn(0f, 1f)
        android.util.Log.d("EmergencyBackgroundService", 
            "Mock anomaly probability set to: $mockAnomalyProbability")
    }
    
    /**
     * Set exercise simulation mode
     */
    fun setSimulateExercise(enabled: Boolean) {
        simulateExercise = enabled
        android.util.Log.d("EmergencyBackgroundService", 
            "Exercise simulation: $enabled")
    }
    
    /**
     * Generate specific heart rate for testing
     */
    fun simulateSpecificHeartRate(bpm: Int, isAnomaly: Boolean = false) {
        val reading = mockSensorService.generateSpecificHeartRate(bpm, isAnomaly)
        android.util.Log.d("EmergencyBackgroundService", 
            "Simulated HR: ${reading.heartRate} BPM (anomaly: ${reading.isAnomaly})")
    }
    
    /**
     * Convert MockSensorDataService.ActivityLevel to HealthData.ActivityLevel
     */
    private fun MockSensorDataService.ActivityLevel.toHealthDataActivityLevel(): HealthData.ActivityLevel {
        return when (this) {
            MockSensorDataService.ActivityLevel.STATIONARY -> HealthData.ActivityLevel.STATIONARY
            MockSensorDataService.ActivityLevel.WALKING -> HealthData.ActivityLevel.WALKING
            MockSensorDataService.ActivityLevel.RUNNING -> HealthData.ActivityLevel.RUNNING
            MockSensorDataService.ActivityLevel.EXERCISING -> HealthData.ActivityLevel.EXERCISING
            MockSensorDataService.ActivityLevel.UNKNOWN -> HealthData.ActivityLevel.UNKNOWN
        }
    }

    private fun onEmergencyTrigger(
        userId: String,
        userName: String,
        userAge: Int,
        userPhone: String,
        reason: String
    ) {
        scope.launch {
            val userInfo = createUserInfo(userId, userName, userAge, userPhone)
            val emergencyType = when {
                reason.contains("Shake") -> EmergencyConstants.EmergencyType.MANUAL_TRIGGER
                reason.contains("Volume") -> EmergencyConstants.EmergencyType.MANUAL_TRIGGER
                reason.contains("Check-in") -> EmergencyConstants.EmergencyType.SCHEDULED_CHECKIN_MISSED
                reason.contains("Fall") -> EmergencyConstants.EmergencyType.FALL_DETECTED
                else -> EmergencyConstants.EmergencyType.MANUAL_TRIGGER
            }

            emergencyManager.triggerManualEmergency(userId, userInfo, emergencyType)
        }
    }

    private fun triggerManualEmergency(userId: String) {
        // Get user info from preferences or database
        val prefs = getSharedPreferences(EmergencyConstants.PREF_NAME_EMERGENCY, MODE_PRIVATE)
        val userName = prefs.getString("user_name", "User") ?: "User"
        val userAge = prefs.getInt("user_age", 0)
        val userPhone = prefs.getString("user_phone", "") ?: ""

        scope.launch {
            val userInfo = createUserInfo(userId, userName, userAge, userPhone)
            emergencyManager.triggerManualEmergency(userId, userInfo)
        }
    }

    private fun triggerHealthEmergency(
        userId: String,
        userName: String,
        userAge: Int,
        userPhone: String,
        healthData: HealthData
    ) {
        if (emergencyManager.isEmergencyActive()) {
            return // Don't trigger if already active
        }

        scope.launch {
            val userInfo = createUserInfo(userId, userName, userAge, userPhone)
            emergencyManager.triggerHealthEmergency(userId, healthData, userInfo)
        }
    }

    private fun createUserInfo(
        userId: String,
        userName: String,
        userAge: Int,
        userPhone: String
    ): UserInfo {
        // Load medical info from database
        val medicalInfo = emergencyManager.database.getMedicalInfo(userId) ?: MedicalInfo(
            userId = userId,
            baselineHeartRate = healthMonitoring.getBaselineHeartRate()
        )

        return UserInfo(
            userId = userId,
            name = userName,
            age = userAge,
            phoneNumber = userPhone,
            medicalInfo = medicalInfo
        )
    }

    private fun stopMonitoring() {
        shakeDetection?.stopListening()
        fallDetection?.stopListening()
        healthMonitoringJob?.cancel()
        checkInMonitoringJob?.cancel()

        shakeDetection = null
        volumeButtonDetector = null
        checkInService = null
        fallDetection = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        releaseWakeLock()
        emergencyManager.cleanup()
        scope.cancel()
        android.util.Log.d("EmergencyBackgroundService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Emergency Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors for emergency situations"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        // Main notification intent
        val mainIntent = Intent(this, com.rescuemate.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Cancel/Safe intent
        val cancelIntent = Intent(this, EmergencyBackgroundService::class.java).apply {
            action = ACTION_CANCEL_EMERGENCY
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Stop monitoring intent
        val stopIntent = Intent(this, EmergencyBackgroundService::class.java).apply {
            action = ACTION_STOP_MONITORING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("RescueMate Emergency Monitoring")
            .setContentText("Monitoring for emergencies in background")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "I'm Safe",
                cancelPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Monitoring",
                stopPendingIntent
            )
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("RescueMate is actively monitoring your health data and sensor inputs. " +
                        "If an emergency is detected, you will be notified immediately."))
            .build()
    }

    /**
     * Create a minimal notification as fallback if main notification fails
     */
    private fun createMinimalNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("RescueMate Monitoring")
            .setContentText("Emergency monitoring active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
    }
}

