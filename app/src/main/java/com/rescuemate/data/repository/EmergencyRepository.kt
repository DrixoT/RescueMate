package com.rescuemate.data.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.rescuemate.data.UserPreferences
import com.rescuemate.emergency.data.EmergencyContact
import com.rescuemate.emergency.data.MedicalInfo
import com.rescuemate.emergency.data.database.EmergencyDatabaseHelper

/**
 * Emergency Repository
 * Centralized data access for emergency contacts and medical information
 */
class EmergencyRepository(private val context: Context) {

    private val dbHelper = EmergencyDatabaseHelper(context)
    private val userPrefs = UserPreferences(context)

    companion object {
        private const val TAG = "EmergencyRepository"
    }

    // ==================== Emergency Contacts ====================

    fun addContact(contact: EmergencyContact): Boolean {
        Log.d(TAG, "Adding emergency contact: ${contact.name} (${contact.phoneNumber})")
        return try {
            // Format phone number for Twilio before saving
            val formattedPhone = formatPhoneNumberForTwilio(contact.phoneNumber)
            val formattedContact = contact.copy(phoneNumber = formattedPhone)
            
            Log.d(TAG, "📞 Formatted phone: ${contact.phoneNumber} -> $formattedPhone")
            
            val result = dbHelper.insertContact(formattedContact)
            if (result > 0) {
                Log.d(TAG, "✅ Contact added successfully: ${formattedContact.name}")
                
                // Verify the contact was saved correctly
                val savedContacts = dbHelper.getAllContacts()
                val savedContact = savedContacts.find { it.id == formattedContact.id || 
                    (it.name == formattedContact.name && it.phoneNumber == formattedPhone) }
                
                if (savedContact != null) {
                    Log.d(TAG, "✅ Verified contact persisted correctly: ${savedContact.name}")
                    showToast("Contact added: ${formattedContact.name}")
                    true
                } else {
                    Log.w(TAG, "⚠️ Contact saved but verification failed")
                    showToast("Contact added: ${formattedContact.name}")
                    true // Still return true as insert succeeded
                }
            } else {
                Log.e(TAG, "❌ Failed to add contact: ${formattedContact.name}")
                showToast("Failed to add contact")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception adding contact: ${e.message}", e)
            showToast("Error: ${e.message}")
            false
        }
    }

    fun getAllContacts(): List<EmergencyContact> {
        Log.d(TAG, " Retrieving all emergency contacts")
        return try {
            val contacts = dbHelper.getAllContacts()
            Log.d(TAG, " Retrieved ${contacts.size} contacts")
            contacts
        } catch (e: Exception) {
            Log.e(TAG, " Exception retrieving contacts: ${e.message}", e)
            emptyList()
        }
    }

    fun deleteContact(contactId: String): Boolean {
        Log.d(TAG, " Deleting contact: $contactId")
        return try {
            val result = dbHelper.deleteContact(contactId)
            if (result > 0) {
                Log.d(TAG, " Contact deleted successfully")
                showToast("Contact deleted")
                true
            } else {
                Log.e(TAG, " Failed to delete contact")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, " Exception deleting contact: ${e.message}", e)
            false
        }
    }

    fun getPrimaryContacts(): List<EmergencyContact> {
        val contacts = getAllContacts()
        val primaryContacts = contacts.filter { it.isPrimaryContact }
        Log.d(TAG, " Found ${primaryContacts.size} primary contacts")
        return primaryContacts
    }

    // ==================== Medical Information ====================

    fun saveMedicalInfo(medicalInfo: MedicalInfo): Boolean {
        Log.d(TAG, " Saving medical information for user: ${medicalInfo.userId}")
        return try {
            val result = dbHelper.insertOrUpdateMedicalInfo(medicalInfo)
            if (result > 0) {
                Log.d(TAG, "✅ Medical info saved successfully")
                showToast("Medical information saved")
                true
            } else {
                Log.e(TAG, " Failed to save medical info")
                showToast("Failed to save medical information")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception saving medical info: ${e.message}", e)
            showToast("Error: ${e.message}")
            false
        }
    }

    fun getMedicalInfo(): MedicalInfo? {
        val userId = userPrefs.getUserId()
        Log.d(TAG, "📖 Retrieving medical info for user: $userId")
        return try {
            val medicalInfo = dbHelper.getMedicalInfo(userId)
            if (medicalInfo != null) {
                Log.d(TAG, "✅ Medical info retrieved successfully")
            } else {
                Log.d(TAG, "ℹ️ No medical info found for user")
            }
            medicalInfo
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception retrieving medical info: ${e.message}", e)
            null
        }
    }

    // ==================== Validation ====================

    fun validateContact(name: String, phone: String, relationship: String): ValidationResult {
        Log.d(TAG, "🔍 Validating contact: name='$name', phone='$phone', relationship='$relationship'")

        return when {
            name.isBlank() -> {
                Log.w(TAG, "❌ Validation failed: Name is empty")
                ValidationResult(false, "Please enter contact name")
            }
            name.length < 2 -> {
                Log.w(TAG, "❌ Validation failed: Name too short")
                ValidationResult(false, "Name must be at least 2 characters")
            }
            name.length > 100 -> {
                Log.w(TAG, "❌ Validation failed: Name too long")
                ValidationResult(false, "Name must be less than 100 characters")
            }
            phone.isBlank() -> {
                Log.w(TAG, "❌ Validation failed: Phone is empty")
                ValidationResult(false, "Please enter phone number")
            }
            !isValidPhoneNumber(phone) -> {
                Log.w(TAG, "❌ Validation failed: Invalid phone format")
                ValidationResult(false, "Please enter valid phone number (e.g., +1234567890 or 1234567890)")
            }
            relationship.isBlank() -> {
                Log.w(TAG, "❌ Validation failed: Relationship is empty")
                ValidationResult(false, "Please select relationship")
            }
            else -> {
                Log.d(TAG, "✅ Contact validation passed")
                ValidationResult(true, "Valid")
            }
        }
    }
    
    /**
     * Validate phone number format (E.164 compatible)
     * Accepts formats like: +1234567890, 1234567890, (123) 456-7890, etc.
     */
    private fun isValidPhoneNumber(phone: String): Boolean {
        // Remove all non-digit characters except +
        val cleaned = phone.replace(Regex("[^+0-9]"), "")
        
        // Check if it's a valid E.164 format (optional +, then 1-15 digits)
        // Or standard format (10-15 digits without +)
        return when {
            cleaned.startsWith("+") -> {
                // E.164 format: + followed by 1-15 digits
                cleaned.length >= 2 && cleaned.length <= 16 && 
                cleaned.substring(1).all { it.isDigit() }
            }
            cleaned.all { it.isDigit() } -> {
                // Standard format: 10-15 digits
                cleaned.length >= 10 && cleaned.length <= 15
            }
            else -> false
        }
    }
    
    /**
     * Format phone number to E.164 format for Twilio
     * @param phone Raw phone number input
     * @return Formatted phone number in E.164 format (e.g., +1234567890)
     */
    fun formatPhoneNumberForTwilio(phone: String): String {
        // Remove all non-digit characters
        val digits = phone.replace(Regex("[^0-9]"), "")
        
        return when {
            digits.isEmpty() -> phone // Return original if no digits found
            digits.length == 10 -> {
                // US number without country code - add +1
                "+1$digits"
            }
            digits.length == 11 && digits.startsWith("1") -> {
                // US number with leading 1 - add +
                "+$digits"
            }
            digits.length >= 10 && digits.length <= 15 -> {
                // Already has country code - add +
                "+$digits"
            }
            else -> phone // Return original if format unclear
        }
    }

    fun validateUserProfile(name: String, age: String, gender: String, phone: String): ValidationResult {
        Log.d(TAG, "🔍 Validating user profile")

        return when {
            name.isBlank() -> {
                Log.w(TAG, "❌ Validation failed: Name is empty")
                ValidationResult(false, "Please enter your name")
            }
            age.isBlank() -> {
                Log.w(TAG, "❌ Validation failed: Age is empty")
                ValidationResult(false, "Please enter your age")
            }
            age.toIntOrNull() == null || age.toInt() < 1 || age.toInt() > 150 -> {
                Log.w(TAG, "❌ Validation failed: Invalid age")
                ValidationResult(false, "Please enter valid age (1-150)")
            }
            gender.isBlank() -> {
                Log.w(TAG, "❌ Validation failed: Gender is empty")
                ValidationResult(false, "Please select gender")
            }
            phone.isBlank() -> {
                Log.w(TAG, "❌ Validation failed: Phone is empty")
                ValidationResult(false, "Please enter phone number")
            }
            !isValidPhoneNumber(phone) -> {
                Log.w(TAG, "❌ Validation failed: Invalid phone format")
                ValidationResult(false, "Please enter valid phone number")
            }
            else -> {
                Log.d(TAG, "✅ User profile validation passed")
                ValidationResult(true, "Valid")
            }
        }
    }

    fun validateEmail(email: String): ValidationResult {
        Log.d(TAG, "🔍 Validating email: $email")

        return when {
            email.isBlank() -> {
                Log.w(TAG, "❌ Validation failed: Email is empty")
                ValidationResult(false, "Please enter email")
            }
            !email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) -> {
                Log.w(TAG, "❌ Validation failed: Invalid email format")
                ValidationResult(false, "Please enter valid email address")
            }
            else -> {
                Log.d(TAG, "✅ Email validation passed")
                ValidationResult(true, "Valid")
            }
        }
    }

    fun validatePassword(password: String): ValidationResult {
        Log.d(TAG, "🔍 Validating password (length: ${password.length})")

        return when {
            password.isBlank() -> {
                Log.w(TAG, "❌ Validation failed: Password is empty")
                ValidationResult(false, "Please enter password")
            }
            password.length < 6 -> {
                Log.w(TAG, "❌ Validation failed: Password too short")
                ValidationResult(false, "Password must be at least 6 characters")
            }
            else -> {
                Log.d(TAG, "✅ Password validation passed")
                ValidationResult(true, "Valid")
            }
        }
    }

    // ==================== Helper Methods ====================

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val message: String
)

