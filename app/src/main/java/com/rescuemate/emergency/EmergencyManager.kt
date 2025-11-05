package com.rescuemate.emergency

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rescuemate.R
import com.rescuemate.emergency.data.*
import com.rescuemate.emergency.data.database.EmergencyDatabaseHelper
import com.rescuemate.emergency.health.HealthMonitoringService
import com.rescuemate.emergency.location.EmergencyLocationService
import com.rescuemate.emergency.twilio.TwilioEmergencyService
import kotlinx.coroutines.*

/**
 * Emergency Manager - Core orchestrator for emergency workflow
 * Manages the 3-phase emergency escalation system
 */
class EmergencyManager(private val context: Context) {

    val database = EmergencyDatabaseHelper(context)  // Made public for external access
    private val healthMonitoring = HealthMonitoringService(context)
    private val locationService = EmergencyLocationService(context)
    private val twilioService = TwilioEmergencyService(context)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentEmergency: EmergencyEvent? = null
    private var phase1Timer: Job? = null
    private var phase2Timer: Job? = null

    // Callbacks
    private var onEmergencyTriggered: ((EmergencyEvent) -> Unit)? = null
    private var onEmergencyCancelled: ((EmergencyEvent) -> Unit)? = null
    private var onPhaseEscalation: ((EmergencyEvent, Int) -> Unit)? = null
    private var onContactAttemptComplete: ((ContactAttempt) -> Unit)? = null

    init {
        createNotificationChannels()
    }

    /**
     * Trigger emergency from health alert
     */
    suspend fun triggerHealthEmergency(
        userId: String,
        healthData: HealthData,
        userInfo: UserInfo
    ): Result<EmergencyEvent> = withContext(Dispatchers.IO) {
        try {
            // Get current location
            val locationResult = locationService.getCurrentLocation()
            val locationData = locationResult.getOrNull() ?: LocationData(
                latitude = 0.0,
                longitude = 0.0,
                accuracy = 0f,
                address = "Location unavailable"
            )

            // Get emergency contacts
            val contacts = database.getAllContacts()
            if (contacts.isEmpty()) {
                return@withContext Result.failure(
                    Exception(EmergencyConstants.ERROR_NO_EMERGENCY_CONTACTS)
                )
            }

            // Create emergency event
            val event = EmergencyEvent(
                userId = userId,
                emergencyType = EmergencyConstants.EmergencyType.CARDIAC_ALERT,
                status = EmergencyConstants.EmergencyStatus.INITIATED,
                currentPhase = 1,
                healthData = healthData,
                locationData = locationData,
                userInfo = userInfo,
                emergencyContacts = contacts
            )

            // Save to database
            database.insertEmergencyEvent(event)
            currentEmergency = event

            // Start Phase 1
            startPhase1(event)

            Result.success(event)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Trigger manual emergency (panic button)
     */
    suspend fun triggerManualEmergency(
        userId: String,
        userInfo: UserInfo,
        emergencyType: EmergencyConstants.EmergencyType = EmergencyConstants.EmergencyType.MANUAL_TRIGGER
    ): Result<EmergencyEvent> = withContext(Dispatchers.IO) {
        try {
            val locationResult = locationService.getCurrentLocation()
            val locationData = locationResult.getOrNull() ?: LocationData(
                latitude = 0.0,
                longitude = 0.0,
                accuracy = 0f,
                address = "Location unavailable"
            )

            val contacts = database.getAllContacts()
            if (contacts.isEmpty()) {
                return@withContext Result.failure(
                    Exception(EmergencyConstants.ERROR_NO_EMERGENCY_CONTACTS)
                )
            }

            // Create basic health data
            val healthData = HealthData(
                currentHeartRate = healthMonitoring.getBaselineHeartRate(),
                normalHeartRate = healthMonitoring.getBaselineHeartRate(),
                alertReason = "Manual emergency triggered by user",
                riskScore = 1.0f
            )

            val event = EmergencyEvent(
                userId = userId,
                emergencyType = emergencyType,
                status = EmergencyConstants.EmergencyStatus.INITIATED,
                currentPhase = 1,
                healthData = healthData,
                locationData = locationData,
                userInfo = userInfo,
                emergencyContacts = contacts
            )

            database.insertEmergencyEvent(event)
            currentEmergency = event

            // For manual trigger, skip Phase 1 and go straight to Phase 2
            startPhase2(event)

            Result.success(event)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Phase 1: User Response Check (60 seconds)
     */
    private fun startPhase1(event: EmergencyEvent) {
        val updatedEvent = event.copy(
            status = EmergencyConstants.EmergencyStatus.PHASE_1_USER_RESPONSE_CHECK,
            currentPhase = 1
        )
        database.insertEmergencyEvent(updatedEvent)
        currentEmergency = updatedEvent

        // Show user response notification
        showUserResponseNotification(updatedEvent)

        // Notify callbacks
        onEmergencyTriggered?.invoke(updatedEvent)

        // Start Phase 1 timer
        phase1Timer = scope.launch {
            delay(EmergencyConstants.PHASE_1_DURATION_MS)

            // Check if user responded
            val current = currentEmergency
            if (current != null && current.id == event.id && !current.userResponded) {
                // User didn't respond - escalate to Phase 2
                escalateToPhase2(current)
            }
        }
    }

    /**
     * Escalate to Phase 2: Emergency Contact Notification
     */
    private fun escalateToPhase2(event: EmergencyEvent) {
        phase1Timer?.cancel()
        startPhase2(event)
    }

    /**
     * Phase 2: Emergency Contact Notification (5 minutes)
     */
    private fun startPhase2(event: EmergencyEvent) {
        val updatedEvent = event.copy(
            status = EmergencyConstants.EmergencyStatus.PHASE_2_CONTACT_NOTIFICATION,
            currentPhase = 2,
            phase2StartTime = System.currentTimeMillis()
        )
        database.insertEmergencyEvent(updatedEvent)
        currentEmergency = updatedEvent

        // Show notification
        showContactCallingNotification(updatedEvent)

        // Notify callbacks
        onPhaseEscalation?.invoke(updatedEvent, 2)

        // Start calling emergency contacts
        scope.launch {
            notifyEmergencyContacts(updatedEvent)
        }

        // Start Phase 2 timer for Phase 3 escalation
        phase2Timer = scope.launch {
            delay(EmergencyConstants.PHASE_2_DURATION_MS)

            // Check if any contact confirmed user is fine
            val current = currentEmergency
            if (current != null && current.id == event.id &&
                !current.getSuccessfulContactResponses().any {
                    it.response == EmergencyConstants.ContactResponse.USER_FINE
                }) {
                // No confirmation - would escalate to Phase 3 (emergency services)
                // Phase 3 is reserved for future implementation
                escalateToPhase3(current)
            }
        }
    }

    /**
     * Notify all emergency contacts
     */
    private suspend fun notifyEmergencyContacts(event: EmergencyEvent) = withContext(Dispatchers.IO) {
        // Send initial alert to backend
        val alertResult = twilioService.sendEmergencyContactAlert(event)
        if (alertResult.isFailure) {
            // Fallback: try direct SMS
            sendFallbackSMS(event)
        }

        // Call each contact sequentially
        val sortedContacts = event.emergencyContacts.sortedBy { it.priority }

        for (contact in sortedContacts) {
            // Voice call if preference allows
            if (contact.canReceiveVoiceCall()) {
                val callResult = twilioService.callEmergencyContact(event, contact)

                val attempt = ContactAttempt(
                    contactId = contact.id,
                    contactName = contact.name,
                    contactPhone = contact.phoneNumber,
                    attemptType = ContactAttempt.AttemptType.VOICE_CALL,
                    success = callResult.isSuccess,
                    failureReason = callResult.exceptionOrNull()?.message,
                    callSid = callResult.getOrNull()?.callSid
                )

                // Record attempt
                recordContactAttempt(event.id, attempt)

                // Wait for call duration before next call
                delay(EmergencyConstants.CONTACT_CALL_DURATION_MS)
            }

            // SMS if preference allows
            if (contact.canReceiveSMS()) {
                val smsResult = twilioService.sendEmergencySMS(event, contact)

                val attempt = ContactAttempt(
                    contactId = contact.id,
                    contactName = contact.name,
                    contactPhone = contact.phoneNumber,
                    attemptType = ContactAttempt.AttemptType.SMS,
                    success = smsResult.isSuccess,
                    failureReason = smsResult.exceptionOrNull()?.message
                )

                recordContactAttempt(event.id, attempt)
            }

            // Check if emergency was cancelled
            if (currentEmergency?.userCancelled == true) {
                break
            }
        }
    }

    /**
     * Fallback SMS sending (direct Android SMS as backup)
     */
    private fun sendFallbackSMS(event: EmergencyEvent) {
        try {
            val smsManager = android.telephony.SmsManager.getDefault()
            val message = buildFallbackSMSMessage(event)

            event.emergencyContacts
                .filter { it.canReceiveSMS() }
                .forEach { contact ->
                    try {
                        smsManager.sendTextMessage(
                            contact.phoneNumber,
                            null,
                            message,
                            null,
                            null
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildFallbackSMSMessage(event: EmergencyEvent): String {
        return "EMERGENCY: ${event.userInfo.name} needs help! " +
               "Type: ${event.emergencyType.displayName}. " +
               "Location: ${event.locationData.getGoogleMapsLink()}. " +
               "Reply if they are safe."
    }

    /**
     * Phase 3: Reserved for Emergency Services (Future)
     */
    private fun escalateToPhase3(event: EmergencyEvent) {
        phase2Timer?.cancel()

        val updatedEvent = event.copy(
            status = EmergencyConstants.EmergencyStatus.PHASE_3_RESERVED_FUTURE,
            currentPhase = 3,
            phase3StartTime = System.currentTimeMillis()
        )
        database.insertEmergencyEvent(updatedEvent)
        currentEmergency = updatedEvent

        // Show notification about Phase 3
        showPhase3Notification(updatedEvent)

        // Notify callbacks
        onPhaseEscalation?.invoke(updatedEvent, 3)

        // Phase 3 implementation reserved for future
        // Would call emergency services via Twilio here
        // twilioService.callEmergencyServices(updatedEvent)
    }

    /**
     * User confirms they are okay - cancel emergency
     */
    fun userConfirmSafe() {
        currentEmergency?.let { event ->
            val updatedEvent = event.copy(
                status = EmergencyConstants.EmergencyStatus.CANCELLED_BY_USER,
                userResponded = true,
                userResponseTime = System.currentTimeMillis(),
                userCancelled = true,
                cancelReason = "User confirmed safe",
                resolvedTimestamp = System.currentTimeMillis()
            )

            database.updateEventStatus(event.id, EmergencyConstants.EmergencyStatus.CANCELLED_BY_USER)
            currentEmergency = null

            // Cancel timers
            phase1Timer?.cancel()
            phase2Timer?.cancel()

            // Notify all contacts of cancellation
            scope.launch {
                notifyContactsOfCancellation(updatedEvent)
            }

            // Clear notification
            notificationManager.cancel(EmergencyConstants.NOTIFICATION_ID_EMERGENCY_ACTIVE)

            // Notify callbacks
            onEmergencyCancelled?.invoke(updatedEvent)
        }
    }

    /**
     * Emergency contact confirms user is safe
     */
    fun contactConfirmedSafe(contactPhone: String, notes: String? = null) {
        currentEmergency?.let { event ->
            val response = ContactResponse(
                emergencyId = event.id,
                contactId = event.emergencyContacts.find { it.phoneNumber == contactPhone }?.id ?: "",
                contactPhone = contactPhone,
                response = EmergencyConstants.ContactResponse.USER_FINE,
                notes = notes
            )

            // Record response
            scope.launch {
                twilioService.submitContactResponse(
                    event.id,
                    contactPhone,
                    EmergencyConstants.ContactResponse.USER_FINE,
                    notes
                )
            }

            // Resolve emergency
            val updatedEvent = event.copy(
                status = EmergencyConstants.EmergencyStatus.RESOLVED_BY_CONTACT,
                resolvedTimestamp = System.currentTimeMillis(),
                contactResponses = event.contactResponses + response
            )

            database.updateEventStatus(event.id, EmergencyConstants.EmergencyStatus.RESOLVED_BY_CONTACT)
            currentEmergency = null

            // Cancel timers
            phase1Timer?.cancel()
            phase2Timer?.cancel()

            // Clear notification
            notificationManager.cancel(EmergencyConstants.NOTIFICATION_ID_EMERGENCY_ACTIVE)

            // Notify callbacks
            onEmergencyCancelled?.invoke(updatedEvent)
        }
    }

    /**
     * Get current active emergency
     */
    fun getCurrentEmergency(): EmergencyEvent? = currentEmergency

    /**
     * Check if emergency is active
     */
    fun isEmergencyActive(): Boolean = currentEmergency != null

    /**
     * Record contact attempt
     */
    private fun recordContactAttempt(emergencyId: String, attempt: ContactAttempt) {
        // Would save to database
        onContactAttemptComplete?.invoke(attempt)
    }

    /**
     * Notify contacts of cancellation
     */
    private suspend fun notifyContactsOfCancellation(event: EmergencyEvent) = withContext(Dispatchers.IO) {
        // Send SMS to all contacted contacts
        try {
            val smsManager = android.telephony.SmsManager.getDefault()
            val message = "EMERGENCY CANCELLED: ${event.userInfo.name} is safe. False alarm."

            event.emergencyContacts.forEach { contact ->
                try {
                    smsManager.sendTextMessage(
                        contact.phoneNumber,
                        null,
                        message,
                        null,
                        null
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Notification Methods

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val emergencyChannel = NotificationChannel(
                EmergencyConstants.NOTIFICATION_CHANNEL_ID_EMERGENCY,
                "Emergency SOS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency notifications and alerts"
                enableVibration(true)
                enableLights(true)
            }

            val healthChannel = NotificationChannel(
                EmergencyConstants.NOTIFICATION_CHANNEL_ID_HEALTH,
                "Health Monitoring",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Health monitoring notifications"
            }

            notificationManager.createNotificationChannel(emergencyChannel)
            notificationManager.createNotificationChannel(healthChannel)
        }
    }

    private fun showUserResponseNotification(event: EmergencyEvent) {
        val notification = NotificationCompat.Builder(context, EmergencyConstants.NOTIFICATION_CHANNEL_ID_EMERGENCY)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Emergency Alert Detected")
            .setContentText("Are you okay? Tap to respond or emergency contacts will be notified in 60 seconds.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        notificationManager.notify(
            EmergencyConstants.NOTIFICATION_ID_USER_RESPONSE_CHECK,
            notification
        )
    }

    private fun showContactCallingNotification(event: EmergencyEvent) {
        val notification = NotificationCompat.Builder(context, EmergencyConstants.NOTIFICATION_CHANNEL_ID_EMERGENCY)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Notifying Emergency Contacts")
            .setContentText("Calling ${event.emergencyContacts.size} emergency contacts...")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        notificationManager.notify(
            EmergencyConstants.NOTIFICATION_ID_CONTACT_CALLING,
            notification
        )
    }

    private fun showPhase3Notification(event: EmergencyEvent) {
        val notification = NotificationCompat.Builder(context, EmergencyConstants.NOTIFICATION_CHANNEL_ID_EMERGENCY)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Phase 3: Emergency Services")
            .setContentText("Emergency services integration reserved for future implementation")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        notificationManager.notify(
            EmergencyConstants.NOTIFICATION_ID_EMERGENCY_ACTIVE,
            notification
        )
    }

    // Callback Setters

    fun setOnEmergencyTriggered(callback: (EmergencyEvent) -> Unit) {
        onEmergencyTriggered = callback
    }

    fun setOnEmergencyCancelled(callback: (EmergencyEvent) -> Unit) {
        onEmergencyCancelled = callback
    }

    fun setOnPhaseEscalation(callback: (EmergencyEvent, Int) -> Unit) {
        onPhaseEscalation = callback
    }

    fun setOnContactAttemptComplete(callback: (ContactAttempt) -> Unit) {
        onContactAttemptComplete = callback
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        phase1Timer?.cancel()
        phase2Timer?.cancel()
        scope.cancel()
    }
}

