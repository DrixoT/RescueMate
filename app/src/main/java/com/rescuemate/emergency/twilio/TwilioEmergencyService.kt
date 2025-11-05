package com.rescuemate.emergency.twilio

import android.content.Context
import com.rescuemate.emergency.EmergencyConstants
import com.rescuemate.emergency.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Twilio Emergency Contact Service
 * Handles emergency contact calling via Twilio API
 * Phase 1: Emergency contacts only (NOT emergency services - that costs $75)
 */
class TwilioEmergencyService(private val context: Context) {

    private val prefs = context.getSharedPreferences(
        EmergencyConstants.PREF_NAME_EMERGENCY,
        Context.MODE_PRIVATE
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "TwilioEmergencyService"
    }

    /**
     * Send emergency alert to backend (which triggers Twilio)
     */
    suspend fun sendEmergencyContactAlert(
        event: EmergencyEvent
    ): Result<EmergencyApiResponse> = withContext(Dispatchers.IO) {
        try {
            val backendUrl = getBackendUrl()
            if (backendUrl.isEmpty()) {
                return@withContext Result.failure(
                    Exception(EmergencyConstants.ERROR_BACKEND_UNREACHABLE)
                )
            }

            val requestBody = buildContactAlertRequest(event)
            val url = "$backendUrl${EmergencyConstants.API_EMERGENCY_CONTACT_ALERT}"

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Emergency-Auth", getAuthToken())
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Backend request failed: ${response.code} - ${response.message}")
                )
            }

            val responseBody = response.body?.string() ?: ""
            val apiResponse = parseApiResponse(responseBody)

            Result.success(apiResponse)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Initiate Twilio voice call to emergency contact
     */
    suspend fun callEmergencyContact(
        event: EmergencyEvent,
        contact: EmergencyContact
    ): Result<EmergencyApiResponse> = withContext(Dispatchers.IO) {
        try {
            val backendUrl = getBackendUrl()
            if (backendUrl.isEmpty()) {
                return@withContext Result.failure(
                    Exception(EmergencyConstants.ERROR_BACKEND_UNREACHABLE)
                )
            }

            val requestBody = buildContactCallRequest(event, contact)
            val url = "$backendUrl${EmergencyConstants.API_EMERGENCY_CONTACT_CALL}"

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Emergency-Auth", getAuthToken())
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Call request failed: ${response.code}")
                )
            }

            val responseBody = response.body?.string() ?: ""
            val apiResponse = parseApiResponse(responseBody)

            Result.success(apiResponse)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Send SMS to emergency contact
     */
    suspend fun sendEmergencySMS(
        event: EmergencyEvent,
        contact: EmergencyContact
    ): Result<EmergencyApiResponse> = withContext(Dispatchers.IO) {
        try {
            val backendUrl = getBackendUrl()
            if (backendUrl.isEmpty()) {
                return@withContext Result.failure(
                    Exception(EmergencyConstants.ERROR_BACKEND_UNREACHABLE)
                )
            }

            val requestBody = buildContactCallRequest(event, contact, "sms")
            val url = "$backendUrl${EmergencyConstants.API_EMERGENCY_CONTACT_CALL}"

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Emergency-Auth", getAuthToken())
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("SMS request failed: ${response.code}")
                )
            }

            val responseBody = response.body?.string() ?: ""
            val apiResponse = parseApiResponse(responseBody)

            Result.success(apiResponse)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Submit contact response (user safe/not safe)
     */
    suspend fun submitContactResponse(
        emergencyId: String,
        contactPhone: String,
        response: EmergencyConstants.ContactResponse,
        notes: String? = null
    ): Result<EmergencyApiResponse> = withContext(Dispatchers.IO) {
        try {
            val backendUrl = getBackendUrl()
            if (backendUrl.isEmpty()) {
                return@withContext Result.failure(
                    Exception(EmergencyConstants.ERROR_BACKEND_UNREACHABLE)
                )
            }

            val requestData = EmergencyContactResponseRequest(
                emergencyId = emergencyId,
                contactPhone = contactPhone,
                response = response.name.lowercase(),
                timestamp = java.time.Instant.now().toString(),
                notes = notes
            )

            val requestBody = JSONObject().apply {
                put("emergencyId", requestData.emergencyId)
                put("contactPhone", requestData.contactPhone)
                put("response", requestData.response)
                put("timestamp", requestData.timestamp)
                put("notes", requestData.notes)
            }.toString()

            val url = "$backendUrl${EmergencyConstants.API_EMERGENCY_CONTACT_RESPONSE}"

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Emergency-Auth", getAuthToken())
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Response submission failed: ${response.code}")
                )
            }

            val responseBody = response.body?.string() ?: ""
            val apiResponse = parseApiResponse(responseBody)

            Result.success(apiResponse)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Check Twilio call status
     */
    suspend fun getCallStatus(callSid: String): Result<TwilioCallStatus> = withContext(Dispatchers.IO) {
        try {
            val backendUrl = getBackendUrl()
            if (backendUrl.isEmpty()) {
                return@withContext Result.failure(
                    Exception(EmergencyConstants.ERROR_BACKEND_UNREACHABLE)
                )
            }

            val url = "$backendUrl/api/emergency/call-status/$callSid"

            val request = Request.Builder()
                .url(url)
                .addHeader("X-Emergency-Auth", getAuthToken())
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Status check failed: ${response.code}")
                )
            }

            val responseBody = response.body?.string() ?: ""
            val status = parseCallStatus(responseBody)

            Result.success(status)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // Helper Methods

    private fun buildContactAlertRequest(event: EmergencyEvent): String {
        val request = EmergencyContactAlertRequest(
            userId = event.userId,
            emergencyType = event.emergencyType.name,
            healthData = event.healthData,
            location = event.locationData,
            userInfo = event.userInfo,
            timestamp = java.time.Instant.now().toString()
        )

        return JSONObject().apply {
            put("userId", request.userId)
            put("emergencyType", request.emergencyType)
            put("healthData", JSONObject().apply {
                put("currentHeartRate", request.healthData.currentHeartRate)
                put("normalHeartRate", request.healthData.normalHeartRate)
                put("riskScore", request.healthData.riskScore)
                put("alertReason", request.healthData.alertReason)
                put("activityLevel", request.healthData.activityLevel.name)
            })
            put("location", JSONObject().apply {
                put("latitude", request.location.latitude)
                put("longitude", request.location.longitude)
                put("address", request.location.address)
                put("accuracy", request.location.accuracy)
                put("googleMapsLink", request.location.getGoogleMapsLink())
            })
            put("userInfo", JSONObject().apply {
                put("name", request.userInfo.name)
                put("age", request.userInfo.age)
                put("phoneNumber", request.userInfo.phoneNumber)
                put("medicalInfo", JSONObject().apply {
                    put("bloodType", request.userInfo.medicalInfo.bloodType)
                    put("knownConditions", request.userInfo.medicalInfo.knownConditions.joinToString(", "))
                    put("currentMedications", request.userInfo.medicalInfo.currentMedications.joinToString(", ") { it.name })
                    put("allergies", request.userInfo.medicalInfo.allergies.joinToString(", "))
                })
            })
            put("timestamp", request.timestamp)
        }.toString()
    }

    private fun buildContactCallRequest(
        event: EmergencyEvent,
        contact: EmergencyContact,
        messageType: String = "voice"
    ): String {
        val healthSummary = buildHealthSummary(event)
        val emergencyDetails = buildEmergencyDetails(event)

        val request = EmergencyContactCallRequest(
            userId = event.userId,
            emergencyId = event.id,
            contactPhone = contact.phoneNumber,
            contactName = contact.name,
            messageType = messageType,
            healthSummary = healthSummary,
            locationLink = event.locationData.getGoogleMapsLink(),
            emergencyDetails = emergencyDetails
        )

        return JSONObject().apply {
            put("userId", request.userId)
            put("emergencyId", request.emergencyId)
            put("contactPhone", request.contactPhone)
            put("contactName", request.contactName)
            put("messageType", request.messageType)
            put("healthSummary", request.healthSummary)
            put("locationLink", request.locationLink)
            put("emergencyDetails", request.emergencyDetails)
        }.toString()
    }

    private fun buildHealthSummary(event: EmergencyEvent): String {
        return buildString {
            append("${event.userInfo.name} (age ${event.userInfo.age}) is experiencing a ${event.emergencyType.displayName}. ")
            append("Heart rate: ${event.healthData.currentHeartRate} BPM (normal: ${event.healthData.normalHeartRate} BPM). ")
            append("Risk level: ${(event.healthData.riskScore * 100).toInt()}%. ")
            append("Location: ${event.locationData.address ?: "Unknown"}. ")
        }
    }

    private fun buildEmergencyDetails(event: EmergencyEvent): String {
        return buildString {
            appendLine("EMERGENCY ALERT - RescueMate")
            appendLine()
            appendLine("User: ${event.userInfo.name}")
            appendLine("Age: ${event.userInfo.age}")
            appendLine("Emergency Type: ${event.emergencyType.displayName}")
            appendLine()
            appendLine("Health Status:")
            appendLine("- Heart Rate: ${event.healthData.currentHeartRate} BPM (Normal: ${event.healthData.normalHeartRate} BPM)")
            appendLine("- Alert Reason: ${event.healthData.alertReason}")
            appendLine("- Risk Score: ${(event.healthData.riskScore * 100).toInt()}%")
            appendLine()
            appendLine("Location:")
            appendLine("- Address: ${event.locationData.address ?: "Unknown"}")
            appendLine("- Coordinates: ${event.locationData.latitude}, ${event.locationData.longitude}")
            appendLine("- Google Maps: ${event.locationData.getGoogleMapsLink()}")
            appendLine("- Accuracy: ${event.locationData.accuracy.toInt()}m")
            appendLine()
            appendLine("Medical Information:")
            event.userInfo.medicalInfo.bloodType?.let { appendLine("- Blood Type: $it") }
            if (event.userInfo.medicalInfo.knownConditions.isNotEmpty()) {
                appendLine("- Conditions: ${event.userInfo.medicalInfo.knownConditions.joinToString(", ")}")
            }
            if (event.userInfo.medicalInfo.currentMedications.isNotEmpty()) {
                appendLine("- Medications: ${event.userInfo.medicalInfo.currentMedications.joinToString(", ") { it.name }}")
            }
            if (event.userInfo.medicalInfo.allergies.isNotEmpty()) {
                appendLine("- Allergies: ${event.userInfo.medicalInfo.allergies.joinToString(", ")}")
            }
            appendLine()
            appendLine("Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
            appendLine()
            appendLine("If ${event.userInfo.name} is safe, please respond immediately to cancel escalation.")
        }
    }

    private fun parseApiResponse(json: String): EmergencyApiResponse {
        return try {
            val obj = JSONObject(json)
            EmergencyApiResponse(
                success = obj.optBoolean("success", false),
                message = obj.optString("message"),
                emergencyId = obj.optString("emergencyId"),
                callSid = obj.optString("callSid"),
                estimatedResponse = obj.optString("estimatedResponse"),
                error = obj.optString("error")
            )
        } catch (e: Exception) {
            EmergencyApiResponse(
                success = false,
                error = "Failed to parse response: ${e.message}"
            )
        }
    }

    private fun parseCallStatus(json: String): TwilioCallStatus {
        return try {
            val obj = JSONObject(json)
            TwilioCallStatus(
                callSid = obj.getString("callSid"),
                status = obj.getString("status"),
                duration = obj.optInt("duration"),
                errorCode = obj.optString("errorCode"),
                errorMessage = obj.optString("errorMessage")
            )
        } catch (e: Exception) {
            TwilioCallStatus(
                callSid = "",
                status = "unknown",
                errorMessage = "Failed to parse status: ${e.message}"
            )
        }
    }

    private fun getBackendUrl(): String {
        return prefs.getString(EmergencyConstants.PREF_KEY_BACKEND_URL, "") ?: ""
    }

    private fun getAuthToken(): String {
        // In production, use secure token storage
        return prefs.getString("emergency_auth_token", "") ?: ""
    }

    /**
     * Set backend URL
     */
    fun setBackendUrl(url: String) {
        prefs.edit()
            .putString(EmergencyConstants.PREF_KEY_BACKEND_URL, url)
            .apply()
    }

    /**
     * Set authentication token
     */
    fun setAuthToken(token: String) {
        prefs.edit()
            .putString("emergency_auth_token", token)
            .apply()
    }

    /**
     * Check if Twilio is configured
     */
    fun isTwilioConfigured(): Boolean {
        return getBackendUrl().isNotEmpty()
    }
}

