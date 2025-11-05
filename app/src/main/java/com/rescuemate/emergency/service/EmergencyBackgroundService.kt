package com.rescuemate.emergency.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rescuemate.emergency.EmergencyConstants
import com.rescuemate.emergency.EmergencyManager
import com.rescuemate.emergency.data.HealthData
import com.rescuemate.emergency.data.MedicalInfo
import com.rescuemate.emergency.data.UserInfo
import com.rescuemate.emergency.detection.*
import com.rescuemate.emergency.health.HealthMonitoringService
import kotlinx.coroutines.*

/**
 * Emergency Background Service
 * Continuously monitors for emergency triggers (health, shake, volume buttons, check-in)
 */
class EmergencyBackgroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var emergencyManager: EmergencyManager
    private lateinit var healthMonitoring: HealthMonitoringService

    // Detection services
    private var shakeDetection: ShakeDetectionService? = null
    private var volumeButtonDetector: VolumeButtonEmergencyDetector? = null
    private var checkInService: ScheduledCheckInService? = null
    private var fallDetection: FallDetectionService? = null

    // Health monitoring
    private var healthMonitoringJob: Job? = null
    private var checkInMonitoringJob: Job? = null

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
        emergencyManager = EmergencyManager(this)
        healthMonitoring = HealthMonitoringService(this)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                val userId = intent.getStringExtra(EXTRA_USER_ID) ?: return START_NOT_STICKY
                val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: "User"
                val userAge = intent.getIntExtra(EXTRA_USER_AGE, 0)
                val userPhone = intent.getStringExtra(EXTRA_USER_PHONE) ?: ""

                val enableShake = intent.getBooleanExtra(EXTRA_ENABLE_SHAKE, true)
                val enableVolume = intent.getBooleanExtra(EXTRA_ENABLE_VOLUME, true)
                val enableCheckIn = intent.getBooleanExtra(EXTRA_ENABLE_CHECKIN, false)
                val enableHealth = intent.getBooleanExtra(EXTRA_ENABLE_HEALTH, true)
                val llmApiKey = intent.getStringExtra(EXTRA_LLM_API_KEY)

                startMonitoring(
                    userId, userName, userAge, userPhone,
                    enableShake, enableVolume, enableCheckIn, enableHealth, llmApiKey
                )
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
        // Start foreground service
        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Initialize shake detection
        if (enableShake) {
            shakeDetection = ShakeDetectionService(this) {
                onEmergencyTrigger(userId, userName, userAge, userPhone, "Shake detected")
            }.also { it.startListening() }
        }

        // Initialize volume button detection
        if (enableVolume) {
            volumeButtonDetector = VolumeButtonEmergencyDetector {
                onEmergencyTrigger(userId, userName, userAge, userPhone, "Volume button sequence")
            }
        }

        // Initialize check-in service
        if (enableCheckIn) {
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
        }

        // Initialize health monitoring
        if (enableHealth) {
            startHealthMonitoring(userId, userName, userAge, userPhone, llmApiKey)
        }

        // Initialize fall detection (future)
        fallDetection = FallDetectionService(this) {
            onEmergencyTrigger(userId, userName, userAge, userPhone, "Fall detected")
        }
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
                    // Simulate heart rate monitoring (would integrate with smartwatch)
                    val currentHeartRate = simulateHeartRateReading()

                    // Record reading
                    healthMonitoring.recordHeartRate(currentHeartRate)

                    // Analyze health status
                    val analysis = healthMonitoring.analyzeHealthStatus(
                        currentHeartRate = currentHeartRate,
                        activityLevel = HealthData.ActivityLevel.UNKNOWN,
                        isExercising = false,
                        llmApiKey = llmApiKey
                    )

                    // Check if emergency should be triggered
                    if (healthMonitoring.shouldTriggerEmergency(analysis)) {
                        val healthData = healthMonitoring.getCurrentHealthData(
                            currentHeartRate,
                            analysis
                        )

                        triggerHealthEmergency(userId, userName, userAge, userPhone, healthData)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Wait for next reading
                delay(EmergencyConstants.HEART_RATE_SAMPLE_INTERVAL_MS)
            }
        }
    }

    private fun simulateHeartRateReading(): Int {
        // This would integrate with actual smartwatch data
        // For now, return baseline + random variation
        val baseline = healthMonitoring.getBaselineHeartRate()
        val variation = (-5..5).random()
        return (baseline + variation).coerceIn(40, 200)
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
        emergencyManager.cleanup()
        scope.cancel()
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
        val intent = Intent(this, EmergencyBackgroundService::class.java).apply {
            action = ACTION_CANCEL_EMERGENCY
        }

        val pendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("RescueMate Emergency Monitoring")
            .setContentText("Monitoring for emergencies in background")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "I'm Safe",
                pendingIntent
            )
            .build()
    }
}

