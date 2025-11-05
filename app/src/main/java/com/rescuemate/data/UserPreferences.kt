package com.rescuemate.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * User Preferences Manager
 * Handles SharedPreferences for user data
 */
class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "UserPreferences"
        private const val PREFS_NAME = "rescuemate_user_prefs"

        // Keys
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_AGE = "user_age"
        private const val KEY_DATE_OF_BIRTH = "date_of_birth"
        private const val KEY_USER_GENDER = "user_gender"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PASSWORD_HASH = "user_password_hash"
        private const val KEY_MEDICAL_HISTORY = "medical_history"
        private const val KEY_CURRENT_MEDICATION = "current_medication"
        private const val KEY_ALLERGIES = "allergies"
        private const val KEY_BLOOD_TYPE = "blood_type"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_USER_ID = "user_id"
    }

    // User Authentication
    fun saveUserCredentials(email: String, passwordHash: String) {
        Log.d(TAG, "💾 Saving user credentials for: $email")
        prefs.edit().apply {
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_PASSWORD_HASH, passwordHash)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
        Log.d(TAG, "✅ User credentials saved successfully")
    }

    fun isLoggedIn(): Boolean {
        val loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        Log.d(TAG, "🔍 Checking login status: $loggedIn")
        return loggedIn
    }

    fun logout() {
        Log.d(TAG, "🚪 Logging out user")
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
        Log.d(TAG, "✅ User logged out successfully")
    }

    // User Profile Data
    fun saveUserProfile(name: String, age: String, gender: String, phone: String) {
        Log.d(TAG, "💾 Saving user profile - Name: $name, Age: $age, Gender: $gender, Phone: $phone")
        prefs.edit().apply {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_AGE, age)
            putString(KEY_USER_GENDER, gender)
            putString(KEY_USER_PHONE, phone)
            apply()
        }
        Log.d(TAG, "✅ User profile saved successfully")
    }

    fun getUserName(): String? {
        val name = prefs.getString(KEY_USER_NAME, null)
        Log.d(TAG, "📖 Retrieved user name: $name")
        return name
    }

    fun getUserAge(): String? = prefs.getString(KEY_USER_AGE, null)
    fun getUserGender(): String? = prefs.getString(KEY_USER_GENDER, null)
    fun getUserPhone(): String? = prefs.getString(KEY_USER_PHONE, null)
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    fun saveDateOfBirth(dateOfBirth: String) {
        Log.d(TAG, "Saving date of birth: $dateOfBirth")
        prefs.edit().putString(KEY_DATE_OF_BIRTH, dateOfBirth).apply()
    }

    fun getDateOfBirth(): String? = prefs.getString(KEY_DATE_OF_BIRTH, null)

    // Medical Information
    fun saveMedicalInfo(medicalHistory: String, currentMedication: String, allergies: String, bloodType: String = "") {
        Log.d(TAG, "💾 Saving medical info - History: ${medicalHistory.take(50)}..., Medication: ${currentMedication.take(30)}...")
        prefs.edit().apply {
            putString(KEY_MEDICAL_HISTORY, medicalHistory)
            putString(KEY_CURRENT_MEDICATION, currentMedication)
            putString(KEY_ALLERGIES, allergies)
            putString(KEY_BLOOD_TYPE, bloodType)
            apply()
        }
        Log.d(TAG, "✅ Medical info saved successfully")
    }

    fun getMedicalHistory(): String? = prefs.getString(KEY_MEDICAL_HISTORY, null)
    fun getCurrentMedication(): String? = prefs.getString(KEY_CURRENT_MEDICATION, null)
    fun getAllergies(): String? = prefs.getString(KEY_ALLERGIES, null)
    fun getBloodType(): String? = prefs.getString(KEY_BLOOD_TYPE, null)

    // User ID
    fun setUserId(userId: String) {
        Log.d(TAG, "💾 Setting user ID: $userId")
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String {
        val userId = prefs.getString(KEY_USER_ID, null) ?: generateUserId()
        if (prefs.getString(KEY_USER_ID, null) == null) {
            setUserId(userId)
        }
        return userId
    }

    private fun generateUserId(): String {
        val userId = "user_${System.currentTimeMillis()}"
        Log.d(TAG, "🆕 Generated new user ID: $userId")
        return userId
    }

    // Onboarding
    fun setOnboardingComplete(complete: Boolean) {
        Log.d(TAG, "💾 Setting onboarding complete: $complete")
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, complete).apply()
    }

    fun isOnboardingComplete(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    // Clear all data
    fun clearAllData() {
        Log.w(TAG, "⚠️ CLEARING ALL USER DATA")
        prefs.edit().clear().apply()
        Log.d(TAG, "✅ All data cleared")
    }
}

