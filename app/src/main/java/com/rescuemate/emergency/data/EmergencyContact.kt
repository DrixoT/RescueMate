package com.rescuemate.emergency.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Emergency Contact Data Model
 */
@Parcelize
data class EmergencyContact(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val priority: Int = 1, // 1 = highest priority
    val isVerified: Boolean = false,
    val email: String? = null,
    val isPrimaryContact: Boolean = false,
    val notificationPreference: NotificationPreference = NotificationPreference.ALL,
    val medicalKnowledge: MedicalKnowledge = MedicalKnowledge.BASIC,
    val lastContactedTimestamp: Long = 0L,
    val responseHistory: List<String> = emptyList()
) : Parcelable {

    enum class NotificationPreference {
        ALL,           // SMS + Voice + Email
        VOICE_SMS,     // Voice calls and SMS only
        SMS_ONLY,      // SMS only
        VOICE_ONLY     // Voice calls only
    }

    enum class MedicalKnowledge {
        NONE,          // No medical training
        BASIC,         // Basic first aid
        CERTIFIED,     // CPR/First Aid certified
        MEDICAL_PROFESSIONAL // Doctor, Nurse, EMT
    }

    fun canReceiveVoiceCall(): Boolean {
        return notificationPreference == NotificationPreference.ALL ||
               notificationPreference == NotificationPreference.VOICE_SMS ||
               notificationPreference == NotificationPreference.VOICE_ONLY
    }

    fun canReceiveSMS(): Boolean {
        return notificationPreference == NotificationPreference.ALL ||
               notificationPreference == NotificationPreference.VOICE_SMS ||
               notificationPreference == NotificationPreference.SMS_ONLY
    }
}

/**
 * User Medical Information
 */
@Parcelize
data class MedicalInfo(
    val userId: String,
    val dateOfBirth: String? = null,
    val bloodType: String? = null,
    val knownConditions: List<String> = emptyList(),
    val currentMedications: List<Medication> = emptyList(),
    val allergies: List<String> = emptyList(),
    val baselineHeartRate: Int = 70,
    val baselineBloodPressure: String? = null,
    val emergencyNotes: String? = null,
    val preferredHospital: String? = null,
    val doctorName: String? = null,
    val doctorPhone: String? = null,
    val insuranceInfo: String? = null,
    val dnrStatus: Boolean = false,
    val organDonor: Boolean = false
) : Parcelable

/**
 * Medication Information
 */
@Parcelize
data class Medication(
    val name: String,
    val dosage: String,
    val frequency: String,
    val affectsHeartRate: Boolean = false,
    val criticalMedication: Boolean = false
) : Parcelable

/**
 * Health Data Snapshot
 */
@Parcelize
data class HealthData(
    val timestamp: Long = System.currentTimeMillis(),
    val currentHeartRate: Int,
    val normalHeartRate: Int,
    val heartRateTrend: List<Int> = emptyList(),
    val riskScore: Float = 0f,
    val alertReason: String,
    val activityLevel: ActivityLevel = ActivityLevel.UNKNOWN,
    val isExercising: Boolean = false,
    val stressLevel: StressLevel = StressLevel.NORMAL
) : Parcelable {

    enum class ActivityLevel {
        STATIONARY,
        WALKING,
        RUNNING,
        EXERCISING,
        UNKNOWN
    }

    enum class StressLevel {
        LOW,
        NORMAL,
        ELEVATED,
        HIGH,
        CRITICAL
    }

    fun getHeartRateDeviation(): Float {
        return (currentHeartRate - normalHeartRate).toFloat() / normalHeartRate
    }

    fun isAbnormal(): Boolean {
        return riskScore > 0.7f || getHeartRateDeviation() > 0.5f
    }
}

/**
 * Location Data
 */
@Parcelize
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val accuracy: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val altitude: Double? = null,
    val speed: Float? = null,
    val isIndoor: Boolean = false,
    val locationContext: LocationContext = LocationContext.UNKNOWN
) : Parcelable {

    enum class LocationContext {
        HOME,
        WORK,
        FAMILIAR_LOCATION,
        UNFAMILIAR_LOCATION,
        UNKNOWN
    }

    fun getGoogleMapsLink(): String {
        return "https://www.google.com/maps?q=$latitude,$longitude"
    }

    fun isAccurate(): Boolean {
        return accuracy < 50f // Less than 50 meters
    }
}

