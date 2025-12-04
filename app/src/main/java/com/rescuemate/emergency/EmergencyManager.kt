package com.rescuemate.emergency

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rescuemate.R
import com.rescuemate.data.UserPreferences
import com.rescuemate.emergency.data.*
import com.rescuemate.emergency.data.database.EmergencyDatabaseHelper
import com.rescuemate.emergency.health.HealthMonitoringService
import com.rescuemate.emergency.location.EmergencyLocationService
import com.rescuemate.emergency.twilio.TwilioEmergencyService
import com.rescuemate.utils.NetworkMonitor
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
    private val userPreferences = UserPreferences(context)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val networkMonitor = NetworkMonitor(context)

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentEmergency: EmergencyEvent? = null
    private var phase1Timer: Job? = null
    private var phase2Timer: Job? = null
    
    // Queue for emergency events when network is unavailable
    private val pendingEmergencyEvents = mutableListOf<EmergencyEvent>()

    // Callbacks
    private var onEmergencyTriggered: ((EmergencyEvent) -> Unit)? = null
    private var onEmergencyCancelled: ((EmergencyEvent) -> Unit)? = null
    private var onPhaseEscalation: ((EmergencyEvent, Int) -> Unit)? = null
    private var onContactAttemptComplete: ((ContactAttempt) -> Unit)? = null

    init {
        createNotificationChannels()
        networkMonitor.startMonitoring()
        
        // Monitor network and process queued events when connection restored
        scope.launch {
            networkMonitor.isConnected.collect { isConnected ->
                if (isConnected && pendingEmergencyEvents.isNotEmpty()) {
                    Log.d("EmergencyManager", "🌐 Network restored, processing ${pendingEmergencyEvents.size} queued events")
                    processQueuedEvents()
                }
            }
        }
    }
    
    /**
     * Process queued emergency events when network is restored
     */
    private suspend fun processQueuedEvents() {
        val eventsToProcess = pendingEmergencyEvents.toList()
        pendingEmergencyEvents.clear()
        
        for (event in eventsToProcess) {
            try {
                Log.d("EmergencyManager", "📤 Processing queued event: ${event.id}")
                notifyEmergencyContacts(event)
            } catch (e: Exception) {
                Log.e("EmergencyManager", "Error processing queued event", e)
                // Re-queue if processing fails
                pendingEmergencyEvents.add(event)
            }
        }
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
            Log.d("EmergencyManager", "🚨 Triggering health emergency for user: $userId")
            
            // Get current location
            val locationResult = locationService.getCurrentLocation()
            val locationData = locationResult.getOrNull() ?: LocationData(
                latitude = 0.0,
                longitude = 0.0,
                accuracy = 0f,
                address = "Location unavailable"
            )
            
            Log.d("EmergencyManager", "📍 Location: ${locationData.address} (${locationData.latitude}, ${locationData.longitude})")

            // Get emergency contacts
            val contactsResult = database.getAllContacts()
            if (contactsResult.isFailure) {
                Log.e("EmergencyManager", "❌ Failed to get emergency contacts", contactsResult.exceptionOrNull())
                return@withContext Result.failure(contactsResult.exceptionOrNull() ?: Exception("Failed to get contacts"))
            }
            
            val contacts = contactsResult.getOrNull() ?: emptyList()
            if (contacts.isEmpty()) {
                Log.e("EmergencyManager", "❌ No emergency contacts configured")
                return@withContext Result.failure(
                    Exception(EmergencyConstants.ERROR_NO_EMERGENCY_CONTACTS)
                )
            }
            
            Log.d("EmergencyManager", "📞 Found ${contacts.size} emergency contacts")

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
            val insertResult = database.insertEmergencyEvent(event)
            if (insertResult.isFailure) {
                Log.e("EmergencyManager", "❌ Failed to save emergency event to database", insertResult.exceptionOrNull())
                return@withContext Result.failure(insertResult.exceptionOrNull() ?: Exception("Failed to save emergency event"))
            }
            currentEmergency = event
            
            Log.d("EmergencyManager", "✅ Emergency event created: ${event.id}")

            // Start Phase 1
            startPhase1(event)

            Result.success(event)

        } catch (e: Exception) {
            Log.e("EmergencyManager", "❌ Error triggering health emergency", e)
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

            val contactsResult = database.getAllContacts()
            if (contactsResult.isFailure) {
                return@withContext Result.failure(contactsResult.exceptionOrNull() ?: Exception("Failed to get contacts"))
            }
            
            val contacts = contactsResult.getOrNull() ?: emptyList()
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

            val insertResult = database.insertEmergencyEvent(event)
            if (insertResult.isFailure) {
                return@withContext Result.failure(insertResult.exceptionOrNull() ?: Exception("Failed to save emergency event"))
            }
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
        Log.d("EmergencyManager", "⏱️ Starting Phase 1: User Response Check (60s)")
        
        val updatedEvent = event.copy(
            status = EmergencyConstants.EmergencyStatus.PHASE_1_USER_RESPONSE_CHECK,
            currentPhase = 1,
            phase1StartTime = System.currentTimeMillis()
        )
        database.insertEmergencyEvent(updatedEvent) // Result ignored here as event already saved
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
                Log.w("EmergencyManager", "⏰ Phase 1 timeout - User did not respond, escalating to Phase 2")
                // User didn't respond - escalate to Phase 2
                escalateToPhase2(current)
            } else {
                Log.d("EmergencyManager", "✅ User responded during Phase 1 - emergency cancelled")
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
        Log.d("EmergencyManager", "📞 Starting Phase 2: Emergency Contact Notification")
        
        val updatedEvent = event.copy(
            status = EmergencyConstants.EmergencyStatus.PHASE_2_CONTACT_NOTIFICATION,
            currentPhase = 2,
            phase2StartTime = System.currentTimeMillis()
        )
        database.insertEmergencyEvent(updatedEvent) // Result ignored here as event already saved
        currentEmergency = updatedEvent

        // Show notification
        showContactCallingNotification(updatedEvent)

        // Notify callbacks
        onPhaseEscalation?.invoke(updatedEvent, 2)

        // Start calling emergency contacts
        scope.launch {
            try {
                notifyEmergencyContacts(updatedEvent)
            } catch (e: Exception) {
                Log.e("EmergencyManager", "❌ Error notifying emergency contacts", e)
            }
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
                Log.w("EmergencyManager", "⏰ Phase 2 timeout - No contact confirmed user safe, escalating to Phase 3")
                // No confirmation - would escalate to Phase 3 (emergency services)
                // Phase 3 is reserved for future implementation
                escalateToPhase3(current)
            } else {
                Log.d("EmergencyManager", "✅ Contact confirmed user safe during Phase 2")
            }
        }
    }

    /**
     * Notify all emergency contacts
     */
    private suspend fun notifyEmergencyContacts(event: EmergencyEvent) = withContext(Dispatchers.IO) {
        Log.d("EmergencyManager", "📞 Notifying ${event.emergencyContacts.size} emergency contacts")

        // Check for Simulation Mode
        if (userPreferences.getSimulationMode()) {
            Log.i("EmergencyManager", "🎭 SIMULATION MODE ACTIVE: Sending FCM notifications first, then simulation calls/SMS")
            
            // Step 1: Send FCM notifications via backend (even in simulation mode)
            if (networkMonitor.checkConnection()) {
                Log.d("EmergencyManager", "🎭 Sim: Sending FCM notifications via backend")
                val alertResult = twilioService.sendEmergencyContactAlert(event)
                if (alertResult.isSuccess) {
                    Log.d("EmergencyManager", "🎭 Sim: FCM notifications sent successfully")
                    // Wait a moment for notifications to be delivered
                    delay(2000) // 2 second delay for FCM delivery
                } else {
                    Log.w("EmergencyManager", "🎭 Sim: Failed to send FCM notifications: ${alertResult.exceptionOrNull()?.message}")
                }
            } else {
                Log.w("EmergencyManager", "🎭 Sim: No network connection, skipping FCM notifications")
            }
            
            // Step 2: Proceed with simulation SMS and calls
            notifySimulationContacts(event)
            return@withContext
        }
        
        // Check network connectivity
        if (!networkMonitor.checkConnection()) {
            Log.w("EmergencyManager", "⚠️ No network connection, queueing emergency event")
            pendingEmergencyEvents.add(event)
            
            // Try fallback SMS immediately (may work even without internet for local SMS)
            sendFallbackSMS(event)
            return@withContext
        }
        
        // Send initial alert to backend
        val alertResult = twilioService.sendEmergencyContactAlert(event)
        if (alertResult.isFailure) {
            Log.w("EmergencyManager", "⚠️ Backend alert failed, using fallback SMS")
            // Fallback: try direct SMS
            sendFallbackSMS(event)
        } else {
            Log.d("EmergencyManager", "✅ Backend alert sent successfully")
        }

        // Call each contact sequentially
        val sortedContacts = event.emergencyContacts.sortedBy { it.priority }
        
        var successfulCalls = 0
        var failedCalls = 0

        for (contact in sortedContacts) {
            // Check if emergency was cancelled
            if (currentEmergency?.userCancelled == true) {
                Log.d("EmergencyManager", "🛑 Emergency cancelled, stopping contact notifications")
                break
            }
            
            Log.d("EmergencyManager", "📞 Contacting: ${contact.name} (${contact.phoneNumber})")

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

                if (callResult.isSuccess) {
                    successfulCalls++
                    Log.d("EmergencyManager", "✅ Voice call initiated to ${contact.name}")
                } else {
                    failedCalls++
                    Log.w("EmergencyManager", "❌ Voice call failed to ${contact.name}: ${callResult.exceptionOrNull()?.message}")
                }

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

                if (smsResult.isSuccess) {
                    Log.d("EmergencyManager", "✅ SMS sent to ${contact.name}")
                } else {
                    Log.w("EmergencyManager", "❌ SMS failed to ${contact.name}: ${smsResult.exceptionOrNull()?.message}")
                }

                recordContactAttempt(event.id, attempt)
            }
        }
        
        Log.d("EmergencyManager", "📊 Contact notification summary: $successfulCalls successful, $failedCalls failed")
    }

    /**
     * Handle simulation mode contacts (Direct SMS and Call)
     * Note: FCM notifications should be sent before calling this function
     */
    private suspend fun notifySimulationContacts(event: EmergencyEvent) = withContext(Dispatchers.IO) {
        Log.d("EmergencyManager", "🎭 Sim: Starting simulation SMS and calls")
        
        val smsManager = android.telephony.SmsManager.getDefault()
        val message = buildFallbackSMSMessage(event) // Reuse fallback message for sim
        
        // 1. Send SMS to all contacts
        event.emergencyContacts.forEach { contact ->
            try {
                Log.d("EmergencyManager", "🎭 Sim: Sending SMS to ${contact.name}")
                smsManager.sendTextMessage(
                    contact.phoneNumber,
                    null,
                    message,
                    null,
                    null
                )
            } catch (e: Exception) {
                Log.e("EmergencyManager", "🎭 Sim: Failed to send SMS to ${contact.name}", e)
            }
        }

        // 2. Wait a moment before making the call (to ensure notification was received)
        delay(1000) // 1 second delay after SMS

        // 3. Call Primary Contact directly (with improved error handling)
        val primaryContact = event.emergencyContacts.find { it.isPrimaryContact } 
            ?: event.emergencyContacts.firstOrNull()
            
        if (primaryContact != null) {
            try {
                Log.d("EmergencyManager", "🎭 Sim: Attempting to call ${primaryContact.name} at ${primaryContact.phoneNumber}")
                
                // Check if we have permission to make calls
                val hasCallPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
                
                withContext(Dispatchers.Main) {
                    try {
                        if (hasCallPermission) {
                            // Use ACTION_CALL if permission is granted (for real devices)
                    val intent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:${primaryContact.phoneNumber}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                            Log.d("EmergencyManager", "🎭 Sim: Call intent started")
                        } else {
                            // Use ACTION_DIAL as fallback (safer, opens dialer without requiring permission)
                            Log.w("EmergencyManager", "🎭 Sim: CALL_PHONE permission not granted, using ACTION_DIAL")
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${primaryContact.phoneNumber}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            Log.d("EmergencyManager", "🎭 Sim: Dialer opened")
                        }
                    } catch (e: SecurityException) {
                        Log.e("EmergencyManager", "🎭 Sim: SecurityException when starting call intent", e)
                        // Fallback to ACTION_DIAL if ACTION_CALL fails
                        try {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${primaryContact.phoneNumber}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            Log.d("EmergencyManager", "🎭 Sim: Fallback dialer opened")
                        } catch (e2: Exception) {
                            Log.e("EmergencyManager", "🎭 Sim: Failed to open dialer", e2)
                        }
                    } catch (e: Exception) {
                        Log.e("EmergencyManager", "🎭 Sim: Failed to initiate call/dial", e)
                        // Don't crash - just log the error
                    }
                }
            } catch (e: Exception) {
                Log.e("EmergencyManager", "🎭 Sim: Exception in call attempt", e)
                // Don't rethrow - prevent crash
            }
        }
        
        Log.d("EmergencyManager", "🎭 Sim: Simulation SMS and calls completed")
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
        database.insertEmergencyEvent(updatedEvent) // Result ignored here as event already saved
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

