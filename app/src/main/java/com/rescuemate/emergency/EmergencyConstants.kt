package com.rescuemate.emergency
/**
 * Emergency SOS Module Constants
 * Centralized configuration for emergency system
 */
object EmergencyConstants {

    // Phase Timing Configuration
    const val PHASE_1_DURATION_MS = 60_000L // 60 seconds - User response check
    const val PHASE_2_DURATION_MS = 300_000L // 5 minutes - Emergency contact escalation
    const val CONTACT_CALL_DURATION_MS = 45_000L // 45 seconds per contact call

    // Emergency Detection
    const val SHAKE_DETECTION_THRESHOLD = 3
    const val SHAKE_DETECTION_WINDOW_MS = 2_000L
    const val VOLUME_BUTTON_PRESS_COUNT = 5
    const val VOLUME_BUTTON_WINDOW_MS = 3_000L

    // Health Monitoring
    const val HEART_RATE_SAMPLE_INTERVAL_MS = 5_000L // 5 seconds
    const val HEART_RATE_HIGH_THRESHOLD_MULTIPLIER = 1.5 // 150% of normal
    const val HEART_RATE_LOW_THRESHOLD = 40 // BPM
    const val HEART_RATE_CRITICAL_HIGH = 180 // BPM
    const val HEALTH_ALERT_WINDOW_MINUTES = 5 // Consecutive anomaly duration

    // Location Services
    const val LOCATION_UPDATE_INTERVAL_MS = 10_000L // 10 seconds
    const val LOCATION_FASTEST_INTERVAL_MS = 5_000L // 5 seconds
    const val LOCATION_ACCURACY_THRESHOLD_METERS = 50f

    // Emergency Contact Configuration
    const val MAX_EMERGENCY_CONTACTS = 10
    const val MIN_EMERGENCY_CONTACTS = 1
    const val CONTACT_NOTIFICATION_RETRY_ATTEMPTS = 3
    const val CONTACT_NOTIFICATION_RETRY_DELAY_MS = 5_000L

    // Notification IDs
    const val NOTIFICATION_ID_EMERGENCY_ACTIVE = 1001
    const val NOTIFICATION_ID_HEALTH_ALERT = 1002
    const val NOTIFICATION_ID_USER_RESPONSE_CHECK = 1003
    const val NOTIFICATION_ID_CONTACT_CALLING = 1004
    const val NOTIFICATION_CHANNEL_ID_EMERGENCY = "emergency_sos_channel"
    const val NOTIFICATION_CHANNEL_ID_HEALTH = "health_monitoring_channel"

    // Emergency Types
    enum class EmergencyType(val displayName: String, val priority: Int) {
        CARDIAC_ALERT("Cardiac Alert", 5),
        MEDICAL_EMERGENCY("Medical Emergency", 4),
        UNRESPONSIVE("User Unresponsive", 5),
        MANUAL_TRIGGER("Manual Emergency", 3),
        SCHEDULED_CHECKIN_MISSED("Check-in Missed", 2),
        FALL_DETECTED("Fall Detected", 4),
        ABNORMAL_VITALS("Abnormal Vitals", 3)
    }

    // Emergency Response Status
    enum class EmergencyStatus {
        INITIATED,
        PHASE_1_USER_RESPONSE_CHECK,
        PHASE_2_CONTACT_NOTIFICATION,
        PHASE_3_RESERVED_FUTURE, // Reserved for emergency services
        CANCELLED_BY_USER,
        RESOLVED_BY_CONTACT,
        ESCALATED_TO_SERVICES,
        FAILED
    }

    // Contact Response Types
    enum class ContactResponse {
        USER_FINE,
        CHECKING_ON_USER,
        NEED_HELP,
        NO_RESPONSE
    }

    // Shared Preferences Keys
    const val PREF_NAME_EMERGENCY = "emergency_sos_prefs"
    const val PREF_KEY_TWILIO_ENABLED = "twilio_enabled"
    const val PREF_KEY_BACKEND_URL = "backend_url"
    const val PREF_KEY_USER_BASELINE_HEART_RATE = "user_baseline_heart_rate"
    const val PREF_KEY_HEALTH_MONITORING_ENABLED = "health_monitoring_enabled"
    const val PREF_KEY_SHAKE_DETECTION_ENABLED = "shake_detection_enabled"
    const val PREF_KEY_VOLUME_BUTTON_ENABLED = "volume_button_enabled"
    const val PREF_KEY_SCHEDULED_CHECKIN_ENABLED = "scheduled_checkin_enabled"
    const val PREF_KEY_CHECKIN_INTERVAL_MINUTES = "checkin_interval_minutes"
    const val PREF_KEY_SMARTWATCH_CONNECTED = "smartwatch_connected"
    const val PREF_KEY_SMARTWATCH_NAME = "smartwatch_name"

    // API Endpoints (Relative to base URL)
    const val API_EMERGENCY_CONTACT_ALERT = "/api/emergency/contact-alert"
    const val API_EMERGENCY_CONTACT_CALL = "/api/emergency/contact-call"
    const val API_EMERGENCY_CONTACT_RESPONSE = "/api/emergency/contact-response"
    const val API_EMERGENCY_SERVICES = "/api/emergency/services" // RESERVED - Future use

    // Intent Actions
    const val ACTION_EMERGENCY_TRIGGERED = "com.rescuemate.emergency.TRIGGERED"
    const val ACTION_EMERGENCY_CANCELLED = "com.rescuemate.emergency.CANCELLED"
    const val ACTION_USER_RESPONSE_TIMEOUT = "com.rescuemate.emergency.USER_RESPONSE_TIMEOUT"
    const val ACTION_CONTACT_CONFIRMED_SAFE = "com.rescuemate.emergency.CONTACT_CONFIRMED_SAFE"
    const val ACTION_PHASE_ESCALATION = "com.rescuemate.emergency.PHASE_ESCALATION"

    // Intent Extras
    const val EXTRA_EMERGENCY_ID = "emergency_id"
    const val EXTRA_EMERGENCY_TYPE = "emergency_type"
    const val EXTRA_EMERGENCY_PHASE = "emergency_phase"
    const val EXTRA_HEALTH_DATA = "health_data"
    const val EXTRA_LOCATION_DATA = "location_data"

    // Database
    const val DATABASE_NAME = "emergency_sos_db"
    const val DATABASE_VERSION = 1

    // Error Messages
    const val ERROR_NO_EMERGENCY_CONTACTS = "No emergency contacts configured"
    const val ERROR_LOCATION_UNAVAILABLE = "Location unavailable"
    const val ERROR_BACKEND_UNREACHABLE = "Emergency backend unreachable"
    const val ERROR_TWILIO_NOT_CONFIGURED = "Twilio not configured"

    // Success Messages
    const val SUCCESS_EMERGENCY_CANCELLED = "Emergency cancelled successfully"
    const val SUCCESS_CONTACT_NOTIFIED = "Emergency contacts notified"

    // Twilio Configuration
    const val TWILIO_TTS_VOICE = "Polly.Matthew" // Emergency voice
    const val TWILIO_TTS_LANGUAGE = "en-US"
    const val TWILIO_SMS_SERVICE_NAME = "RescueMate Emergency"
}



