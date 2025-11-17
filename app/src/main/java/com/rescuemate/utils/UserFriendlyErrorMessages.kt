package com.rescuemate.utils

/**
 * User-Friendly Error Messages
 * Maps technical errors to user-understandable messages
 */
object UserFriendlyErrorMessages {
    
    /**
     * Get user-friendly message for an error
     */
    fun getMessage(error: ErrorHandler.AppError): String {
        return when (error.category) {
            ErrorHandler.ErrorCategory.DATABASE -> getDatabaseErrorMessage(error)
            ErrorHandler.ErrorCategory.NETWORK -> getNetworkErrorMessage(error)
            ErrorHandler.ErrorCategory.PERMISSION -> getPermissionErrorMessage(error)
            ErrorHandler.ErrorCategory.VALIDATION -> getValidationErrorMessage(error)
            ErrorHandler.ErrorCategory.SENSOR -> getSensorErrorMessage(error)
            ErrorHandler.ErrorCategory.EMERGENCY -> getEmergencyErrorMessage(error)
            ErrorHandler.ErrorCategory.AUTHENTICATION -> getAuthenticationErrorMessage(error)
            ErrorHandler.ErrorCategory.UNKNOWN -> getGenericErrorMessage(error)
        }
    }
    
    private fun getDatabaseErrorMessage(error: ErrorHandler.AppError): String {
        return when (error.severity) {
            ErrorHandler.ErrorSeverity.CRITICAL -> 
                "Critical database error. Please restart the app. If the problem persists, reinstall the app."
            ErrorHandler.ErrorSeverity.HIGH -> 
                "Unable to save data. Please try again."
            else -> 
                "Failed to load data. Please refresh."
        }
    }
    
    private fun getNetworkErrorMessage(error: ErrorHandler.AppError): String {
        return when {
            error.technicalDetails?.contains("timeout", ignoreCase = true) == true ->
                "Connection timeout. Please check your internet and try again."
            error.technicalDetails?.contains("404", ignoreCase = true) == true ->
                "Service not found. Please update the app."
            error.technicalDetails?.contains("500", ignoreCase = true) == true ->
                "Server error. Please try again later."
            error.technicalDetails?.contains("401", ignoreCase = true) == true ||
            error.technicalDetails?.contains("403", ignoreCase = true) == true ->
                "Authentication failed. Please sign in again."
            else ->
                "No internet connection. Please check your network and try again."
        }
    }
    
    private fun getPermissionErrorMessage(error: ErrorHandler.AppError): String {
        val permission = error.context?.substringAfter("Permission: ") ?: "required permission"
        val friendlyPermission = mapPermissionToFriendlyName(permission)
        
        return if (error.severity == ErrorHandler.ErrorSeverity.HIGH) {
            "Please enable $friendlyPermission permission in app settings for this feature to work."
        } else {
            "This feature requires $friendlyPermission permission."
        }
    }
    
    private fun getValidationErrorMessage(error: ErrorHandler.AppError): String {
        return error.message // Validation messages are already user-friendly
    }
    
    private fun getSensorErrorMessage(error: ErrorHandler.AppError): String {
        return when (error.severity) {
            ErrorHandler.ErrorSeverity.CRITICAL -> 
                "Critical sensor error. Health monitoring unavailable."
            else -> 
                "Unable to read sensor data. Please check device sensors."
        }
    }
    
    private fun getEmergencyErrorMessage(error: ErrorHandler.AppError): String {
        return when (error.severity) {
            ErrorHandler.ErrorSeverity.CRITICAL -> 
                "Emergency system error! Please contact support immediately or call emergency services directly."
            ErrorHandler.ErrorSeverity.HIGH -> 
                "Emergency alert could not be sent. Please try manual emergency call."
            else -> 
                "Emergency system issue. Please check your emergency contacts."
        }
    }
    
    private fun getAuthenticationErrorMessage(error: ErrorHandler.AppError): String {
        return when (error.severity) {
            ErrorHandler.ErrorSeverity.HIGH -> 
                "Authentication failed. Please sign in again."
            else -> 
                "Session expired. Please log in."
        }
    }
    
    private fun getGenericErrorMessage(error: ErrorHandler.AppError): String {
        return when (error.severity) {
            ErrorHandler.ErrorSeverity.CRITICAL -> 
                "A critical error occurred. Please restart the app."
            ErrorHandler.ErrorSeverity.HIGH -> 
                "Something went wrong. Please try again."
            else -> 
                "An error occurred. Please try again."
        }
    }
    
    /**
     * Map Android permission to user-friendly name
     */
    private fun mapPermissionToFriendlyName(permission: String): String {
        return when {
            permission.contains("LOCATION", ignoreCase = true) -> "location"
            permission.contains("CAMERA", ignoreCase = true) -> "camera"
            permission.contains("MICROPHONE", ignoreCase = true) || 
            permission.contains("RECORD_AUDIO", ignoreCase = true) -> "microphone"
            permission.contains("CONTACTS", ignoreCase = true) -> "contacts"
            permission.contains("PHONE", ignoreCase = true) || 
            permission.contains("CALL", ignoreCase = true) -> "phone"
            permission.contains("SMS", ignoreCase = true) -> "SMS"
            permission.contains("NOTIFICATION", ignoreCase = true) -> "notifications"
            permission.contains("BLUETOOTH", ignoreCase = true) -> "Bluetooth"
            permission.contains("SENSOR", ignoreCase = true) || 
            permission.contains("BODY_SENSORS", ignoreCase = true) -> "health sensors"
            else -> "required"
        }
    }
    
    /**
     * Get recovery action message
     */
    fun getRecoveryActionMessage(error: ErrorHandler.AppError): String? {
        return error.recoveryAction
    }
    
    /**
     * Get detailed error message (for support/debugging)
     */
    fun getDetailedMessage(error: ErrorHandler.AppError): String {
        return buildString {
            append("Error: ${error.message}\n")
            error.context?.let { append("Context: $it\n") }
            error.technicalDetails?.let { append("Details: $it\n") }
            append("Category: ${error.category}\n")
            append("Severity: ${error.severity}\n")
            append("Recoverable: ${error.isRecoverable}\n")
            error.recoveryAction?.let { append("Recovery: $it\n") }
        }
    }
}

