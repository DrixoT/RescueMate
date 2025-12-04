package com.rescuemate.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.rescuemate.emergency.EmergencyConstants
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class FCMRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val prefs = context.getSharedPreferences(
        EmergencyConstants.PREF_NAME_EMERGENCY,
        Context.MODE_PRIVATE
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    companion object {
        private const val TAG = "FCMRepository"
    }

    /**
     * Get current FCM token
     */
    suspend fun getToken(): String? {
        return try {
            Log.d(TAG, "Requesting FCM token from Firebase")
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM token received: ${token.take(20)}...")
            token
        } catch (e: Exception) {
            Log.e(TAG, "Error getting FCM token", e)
            null
        }
    }

    /**
     * Register FCM token with backend
     */
    suspend fun registerToken(token: String? = null): Boolean {
        return try {
            Log.d(TAG, "Starting FCM token registration")
            
            val fcmToken = token ?: getToken()
            if (fcmToken == null) {
                Log.w(TAG, "FCM token is null, cannot register")
                return false
            }
            
            Log.d(TAG, "FCM token obtained: ${fcmToken.take(20)}...")

            val user = auth.currentUser
            if (user == null) {
                Log.w(TAG, "User not logged in, cannot register FCM token")
                return false
            }

            val email = user.email ?: ""
            val rawPhoneNumber = user.phoneNumber ?: prefs.getString("user_phone", "") ?: ""
            val normalizedPhone = normalizePhoneNumber(rawPhoneNumber)

            Log.d(TAG, "User info for FCM registration - userId: ${user.uid}, email: $email, rawPhone: $rawPhoneNumber, normalizedPhone: $normalizedPhone")

            val backendUrl = prefs.getString(EmergencyConstants.PREF_KEY_BACKEND_URL, "") ?: ""
            if (backendUrl.isEmpty()) {
                Log.w(TAG, "Backend URL not configured")
                return false
            }

            val url = if (backendUrl.endsWith("/")) {
                "${backendUrl}api/users/fcm-token"
            } else {
                "$backendUrl/api/users/fcm-token"
            }

            Log.d(TAG, "Registering FCM token with backend: $url")

            val requestBody = JSONObject().apply {
                put("fcmToken", fcmToken)
                put("userId", user.uid)
                put("email", email.lowercase())
                put("phoneNumber", normalizedPhone)
            }.toString()

            Log.d(TAG, "FCM registration request body: $requestBody")

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Emergency-Auth", getAuthToken())
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "No response body"

            if (response.isSuccessful) {
                Log.d(TAG, "FCM token registered successfully - userId: ${user.uid}, email: ${email.lowercase()}, phone: $normalizedPhone, response: $responseBody")
                prefs.edit().putString("fcm_token", fcmToken).apply()
                true
            } else {
                Log.e(TAG, "Failed to register FCM token - statusCode: ${response.code}, errorBody: $responseBody, userId: ${user.uid}, email: ${email.lowercase()}, phone: $normalizedPhone")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering FCM token", e)
            false
        }
    }

    /**
     * Normalize phone number to E.164 format
     * This must match the backend normalization in UserFCMToken.js and users.js
     */
    private fun normalizePhoneNumber(phone: String): String {
        if (phone.isEmpty()) {
            Log.d(TAG, "Phone number is empty, returning empty string")
            return ""
        }
        
        // Remove all non-digits first
        val cleaned = phone.replace(Regex("[^0-9]"), "")
        
        if (cleaned.isEmpty()) {
            Log.d(TAG, "Phone number has no digits after cleaning")
            return ""
        }
        
        val normalized = when {
            cleaned.length == 10 -> {
                // 10-digit US number: add +1 prefix
                "+1$cleaned"
            }
            cleaned.startsWith("1") && cleaned.length == 11 -> {
                // 11-digit number starting with 1: add + prefix
                "+$cleaned"
            }
            phone.startsWith("+") -> {
                // Already has + prefix: clean and return with +
                // This handles cases like "+1 (555) 123-4567" -> "+15551234567"
                "+$cleaned"
            }
            else -> {
                // Other format: add + prefix
                "+$cleaned"
            }
        }
        
        Log.d(TAG, "Phone normalization: '$phone' -> '$normalized'")
        return normalized
    }

    /**
     * Get authentication token
     */
    private fun getAuthToken(): String {
        return prefs.getString("emergency_auth_token", "") ?: ""
    }
}
