package com.rescuemate.emergency.data

import android.os.Parcelable
import com.rescuemate.emergency.EmergencyConstants
import kotlinx.parcelize.Parcelize

/**
 * Emergency Event Data Model
 * Represents a complete emergency occurrence from trigger to resolution
 */
@Parcelize
data class EmergencyEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String,
    val emergencyType: EmergencyConstants.EmergencyType,
    val status: EmergencyConstants.EmergencyStatus,
    val currentPhase: Int = 1, // 1, 2, or 3

    // Timing
    val triggeredTimestamp: Long = System.currentTimeMillis(),
    val phase1StartTime: Long = System.currentTimeMillis(),
    val phase2StartTime: Long? = null,
    val phase3StartTime: Long? = null,
    val resolvedTimestamp: Long? = null,

    // Context Data
    val healthData: HealthData,
    val locationData: LocationData,
    val userInfo: UserInfo,

    // Emergency Contact Tracking
    val emergencyContacts: List<EmergencyContact>,
    val contactAttempts: List<ContactAttempt> = emptyList(),
    val contactResponses: List<ContactResponse> = emptyList(),

    // User Response
    val userResponded: Boolean = false,
    val userResponseTime: Long? = null,
    val userCancelled: Boolean = false,
    val cancelReason: String? = null,

    // Backend Integration
    val backendNotified: Boolean = false,
    val twilioCallSid: String? = null,
    val emergencyServicesNotified: Boolean = false,

    // Metadata
    val notes: String? = null,
    val deviceInfo: String? = null,
    val batteryLevel: Int? = null
) : Parcelable {

    fun getDurationSeconds(): Long {
        val endTime = resolvedTimestamp ?: System.currentTimeMillis()
        return (endTime - triggeredTimestamp) / 1000
    }

    fun isActive(): Boolean {
        return status != EmergencyConstants.EmergencyStatus.CANCELLED_BY_USER &&
               status != EmergencyConstants.EmergencyStatus.RESOLVED_BY_CONTACT &&
               status != EmergencyConstants.EmergencyStatus.FAILED &&
               resolvedTimestamp == null
    }

    fun shouldEscalateToPhase2(): Boolean {
        return currentPhase == 1 &&
               !userResponded &&
               System.currentTimeMillis() - phase1StartTime >= EmergencyConstants.PHASE_1_DURATION_MS
    }

    fun shouldEscalateToPhase3(): Boolean {
        return currentPhase == 2 &&
               phase2StartTime != null &&
               !contactResponses.any { it.response == EmergencyConstants.ContactResponse.USER_FINE } &&
               System.currentTimeMillis() - phase2StartTime >= EmergencyConstants.PHASE_2_DURATION_MS
    }

    fun getSuccessfulContactResponses(): List<ContactResponse> {
        return contactResponses.filter {
            it.response == EmergencyConstants.ContactResponse.USER_FINE ||
            it.response == EmergencyConstants.ContactResponse.CHECKING_ON_USER
        }
    }
}

/**
 * Contact Attempt Tracking
 */
@Parcelize
data class ContactAttempt(
    val id: String = java.util.UUID.randomUUID().toString(),
    val contactId: String,
    val contactName: String,
    val contactPhone: String,
    val attemptType: AttemptType,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean,
    val failureReason: String? = null,
    val callSid: String? = null,
    val callDuration: Long? = null
) : Parcelable {

    enum class AttemptType {
        VOICE_CALL,
        SMS,
        EMAIL,
        PUSH_NOTIFICATION
    }
}

/**
 * Contact Response Tracking
 */
@Parcelize
data class ContactResponse(
    val id: String = java.util.UUID.randomUUID().toString(),
    val emergencyId: String,
    val contactId: String,
    val contactPhone: String,
    val response: EmergencyConstants.ContactResponse,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val responseSource: ResponseSource = ResponseSource.PHONE_CALL
) : Parcelable {

    enum class ResponseSource {
        PHONE_CALL,
        SMS_REPLY,
        WEB_INTERFACE,
        APP_NOTIFICATION
    }
}

/**
 * User Information for Emergency Context
 */
@Parcelize
data class UserInfo(
    val userId: String,
    val name: String,
    val age: Int,
    val phoneNumber: String,
    val medicalInfo: MedicalInfo,
    val profilePhotoUrl: String? = null
) : Parcelable

/**
 * Emergency API Request Models
 */

data class EmergencyContactAlertRequest(
    val userId: String,
    val emergencyType: String,
    val healthData: HealthData,
    val location: LocationData,
    val userInfo: UserInfo,
    val timestamp: String
)

data class EmergencyContactCallRequest(
    val userId: String,
    val emergencyId: String,
    val contactPhone: String,
    val contactName: String,
    val messageType: String, // "voice" or "sms"
    val healthSummary: String,
    val locationLink: String,
    val emergencyDetails: String
)

data class EmergencyContactResponseRequest(
    val emergencyId: String,
    val contactPhone: String,
    val response: String, // "user_fine", "need_help", "no_response"
    val timestamp: String,
    val notes: String?
)

/**
 * Emergency API Response Models
 */

data class EmergencyApiResponse(
    val success: Boolean,
    val message: String? = null,
    val emergencyId: String? = null,
    val callSid: String? = null,
    val estimatedResponse: String? = null,
    val error: String? = null
)

data class TwilioCallStatus(
    val callSid: String,
    val status: String, // queued, ringing, in-progress, completed, failed
    val duration: Int? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null
)

