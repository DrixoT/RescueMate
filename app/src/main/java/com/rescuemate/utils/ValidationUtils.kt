package com.rescuemate.utils

import android.util.Patterns
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Validation Utilities
 * Comprehensive input validation for the entire application
 */
object ValidationUtils {
    
    /**
     * Validation Result
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    ) {
        companion object {
            fun success() = ValidationResult(true)
            fun error(message: String) = ValidationResult(false, message)
        }
    }
    
    // ============================================
    // CONTACT VALIDATION
    // ============================================
    
    /**
     * Validate contact name
     */
    fun validateContactName(name: String?): ValidationResult {
        return when {
            name.isNullOrBlank() -> ValidationResult.error("Name cannot be empty")
            name.length < 2 -> ValidationResult.error("Name must be at least 2 characters")
            name.length > 50 -> ValidationResult.error("Name must be less than 50 characters")
            !name.matches(Regex("^[a-zA-Z\\s'-]+$")) -> 
                ValidationResult.error("Name contains invalid characters")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate phone number (international format support)
     */
    fun validatePhoneNumber(phone: String?): ValidationResult {
        return when {
            phone.isNullOrBlank() -> ValidationResult.error("Phone number cannot be empty")
            phone.length < 10 -> ValidationResult.error("Phone number too short")
            phone.length > 15 -> ValidationResult.error("Phone number too long")
            !phone.matches(Regex("^\\+?[1-9]\\d{1,14}$")) -> {
                // Also accept formats with spaces, dashes, parentheses
                val cleaned = phone.replace(Regex("[\\s()\\-]"), "")
                if (!cleaned.matches(Regex("^\\+?[1-9]\\d{1,14}$"))) {
                    ValidationResult.error("Invalid phone number format")
                } else {
                    ValidationResult.success()
                }
            }
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Clean/normalize phone number
     */
    fun normalizePhoneNumber(phone: String): String {
        // Remove all non-digit characters except leading +
        var cleaned = phone.trim()
        val hasPlus = cleaned.startsWith("+")
        cleaned = cleaned.replace(Regex("[^0-9]"), "")
        return if (hasPlus) "+$cleaned" else cleaned
    }
    
    /**
     * Validate email address
     */
    fun validateEmail(email: String?): ValidationResult {
        return when {
            email.isNullOrBlank() -> ValidationResult.success() // Email is optional
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> 
                ValidationResult.error("Invalid email address format")
            email.length > 100 -> ValidationResult.error("Email address too long")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate relationship field
     */
    fun validateRelationship(relationship: String?): ValidationResult {
        return when {
            relationship.isNullOrBlank() -> ValidationResult.error("Relationship cannot be empty")
            relationship.length < 2 -> ValidationResult.error("Relationship must be at least 2 characters")
            relationship.length > 30 -> ValidationResult.error("Relationship must be less than 30 characters")
            else -> ValidationResult.success()
        }
    }
    
    // ============================================
    // MEDICAL INFO VALIDATION
    // ============================================
    
    /**
     * Validate date of birth
     */
    fun validateDateOfBirth(dob: String?): ValidationResult {
        if (dob.isNullOrBlank()) {
            return ValidationResult.success() // DOB is optional
        }
        
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            dateFormat.isLenient = false
            val date = dateFormat.parse(dob) ?: return ValidationResult.error("Invalid date format")
            
            val currentTime = System.currentTimeMillis()
            val minAge = currentTime - (150L * 365 * 24 * 60 * 60 * 1000) // 150 years ago
            val maxAge = currentTime - (18L * 365 * 24 * 60 * 60 * 1000) // 18 years ago
            
            return when {
                date.time > currentTime -> ValidationResult.error("Date of birth cannot be in the future")
                date.time < minAge -> ValidationResult.error("Invalid date of birth")
                date.time > maxAge -> ValidationResult.error("Must be at least 18 years old")
                else -> ValidationResult.success()
            }
        } catch (e: Exception) {
            return ValidationResult.error("Invalid date format. Use YYYY-MM-DD")
        }
    }
    
    /**
     * Validate blood type
     */
    fun validateBloodType(bloodType: String?): ValidationResult {
        if (bloodType.isNullOrBlank()) {
            return ValidationResult.success() // Blood type is optional
        }
        
        val validBloodTypes = setOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        return if (bloodType in validBloodTypes) {
            ValidationResult.success()
        } else {
            ValidationResult.error("Invalid blood type. Must be one of: ${validBloodTypes.joinToString()}")
        }
    }
    
    /**
     * Validate blood pressure
     */
    fun validateBloodPressure(bloodPressure: String?): ValidationResult {
        if (bloodPressure.isNullOrBlank()) {
            return ValidationResult.success() // Blood pressure is optional
        }
        
        // Format: "120/80"
        if (!bloodPressure.matches(Regex("^\\d{2,3}/\\d{2,3}$"))) {
            return ValidationResult.error("Invalid blood pressure format. Use format: 120/80")
        }
        
        val parts = bloodPressure.split("/")
        val systolic = parts[0].toIntOrNull() ?: return ValidationResult.error("Invalid systolic value")
        val diastolic = parts[1].toIntOrNull() ?: return ValidationResult.error("Invalid diastolic value")
        
        return when {
            systolic < 70 || systolic > 250 -> 
                ValidationResult.error("Systolic pressure must be between 70 and 250")
            diastolic < 40 || diastolic > 150 -> 
                ValidationResult.error("Diastolic pressure must be between 40 and 150")
            systolic <= diastolic -> 
                ValidationResult.error("Systolic must be greater than diastolic")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate heart rate
     */
    fun validateHeartRate(heartRate: Int?): ValidationResult {
        return when {
            heartRate == null -> ValidationResult.success() // Optional
            heartRate < 30 -> ValidationResult.error("Heart rate too low (minimum 30 BPM)")
            heartRate > 250 -> ValidationResult.error("Heart rate too high (maximum 250 BPM)")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate medication name
     */
    fun validateMedicationName(name: String?): ValidationResult {
        return when {
            name.isNullOrBlank() -> ValidationResult.error("Medication name cannot be empty")
            name.length < 2 -> ValidationResult.error("Medication name too short")
            name.length > 100 -> ValidationResult.error("Medication name too long")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate medication dosage
     */
    fun validateMedicationDosage(dosage: String?): ValidationResult {
        return when {
            dosage.isNullOrBlank() -> ValidationResult.error("Dosage cannot be empty")
            dosage.length > 50 -> ValidationResult.error("Dosage description too long")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate medical condition
     */
    fun validateMedicalCondition(condition: String?): ValidationResult {
        return when {
            condition.isNullOrBlank() -> ValidationResult.error("Condition cannot be empty")
            condition.length < 2 -> ValidationResult.error("Condition description too short")
            condition.length > 100 -> ValidationResult.error("Condition description too long")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate allergy
     */
    fun validateAllergy(allergy: String?): ValidationResult {
        return when {
            allergy.isNullOrBlank() -> ValidationResult.error("Allergy cannot be empty")
            allergy.length < 2 -> ValidationResult.error("Allergy description too short")
            allergy.length > 100 -> ValidationResult.error("Allergy description too long")
            else -> ValidationResult.success()
        }
    }
    
    // ============================================
    // USER PROFILE VALIDATION
    // ============================================
    
    /**
     * Validate full name
     */
    fun validateFullName(name: String?): ValidationResult {
        return when {
            name.isNullOrBlank() -> ValidationResult.error("Full name cannot be empty")
            name.length < 2 -> ValidationResult.error("Full name must be at least 2 characters")
            name.length > 100 -> ValidationResult.error("Full name must be less than 100 characters")
            !name.matches(Regex("^[a-zA-Z\\s'-]+$")) -> 
                ValidationResult.error("Name contains invalid characters")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate password
     */
    fun validatePassword(password: String?): ValidationResult {
        return when {
            password.isNullOrBlank() -> ValidationResult.error("Password cannot be empty")
            password.length < 8 -> ValidationResult.error("Password must be at least 8 characters")
            password.length > 128 -> ValidationResult.error("Password too long")
            !password.matches(Regex(".*[A-Z].*")) -> 
                ValidationResult.error("Password must contain at least one uppercase letter")
            !password.matches(Regex(".*[a-z].*")) -> 
                ValidationResult.error("Password must contain at least one lowercase letter")
            !password.matches(Regex(".*\\d.*")) -> 
                ValidationResult.error("Password must contain at least one number")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate password confirmation
     */
    fun validatePasswordConfirmation(password: String?, confirmation: String?): ValidationResult {
        return when {
            confirmation.isNullOrBlank() -> ValidationResult.error("Please confirm your password")
            password != confirmation -> ValidationResult.error("Passwords do not match")
            else -> ValidationResult.success()
        }
    }
    
    // ============================================
    // GENERAL VALIDATION
    // ============================================
    
    /**
     * Validate text field (general purpose)
     */
    fun validateTextField(
        text: String?,
        fieldName: String,
        minLength: Int = 1,
        maxLength: Int = 500,
        required: Boolean = true
    ): ValidationResult {
        return when {
            text.isNullOrBlank() && required -> 
                ValidationResult.error("$fieldName cannot be empty")
            text != null && text.length < minLength -> 
                ValidationResult.error("$fieldName must be at least $minLength characters")
            text != null && text.length > maxLength -> 
                ValidationResult.error("$fieldName must be less than $maxLength characters")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate numeric field
     */
    fun validateNumericField(
        value: String?,
        fieldName: String,
        min: Double? = null,
        max: Double? = null,
        required: Boolean = true
    ): ValidationResult {
        if (value.isNullOrBlank()) {
            return if (required) {
                ValidationResult.error("$fieldName cannot be empty")
            } else {
                ValidationResult.success()
            }
        }
        
        val numericValue = value.toDoubleOrNull() 
            ?: return ValidationResult.error("$fieldName must be a valid number")
        
        return when {
            min != null && numericValue < min -> 
                ValidationResult.error("$fieldName must be at least $min")
            max != null && numericValue > max -> 
                ValidationResult.error("$fieldName must be at most $max")
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validate multiple fields at once
     */
    fun validateAll(vararg validations: ValidationResult): ValidationResult {
        val firstError = validations.firstOrNull { !it.isValid }
        return firstError ?: ValidationResult.success()
    }
    
    /**
     * Get all validation errors
     */
    fun getAllErrors(vararg validations: ValidationResult): List<String> {
        return validations.mapNotNull { if (!it.isValid) it.errorMessage else null }
    }
}

