package com.rescuemate.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Centralized Error Handler
 * Provides unified error processing, logging, and reporting
 */
object ErrorHandler {
    
    private const val TAG = "ErrorHandler"
    
    /**
     * Error Categories
     */
    enum class ErrorCategory {
        DATABASE,
        NETWORK,
        PERMISSION,
        VALIDATION,
        SENSOR,
        EMERGENCY,
        AUTHENTICATION,
        UNKNOWN
    }
    
    /**
     * Error Severity Levels
     */
    enum class ErrorSeverity {
        LOW,      // Non-critical, can be ignored
        MEDIUM,   // Important but not blocking
        HIGH,     // Critical, needs attention
        CRITICAL  // App-breaking, immediate action required
    }
    
    /**
     * Standardized Error Data Class
     */
    data class AppError(
        val category: ErrorCategory,
        val severity: ErrorSeverity,
        val message: String,
        val technicalDetails: String? = null,
        val exception: Throwable? = null,
        val context: String? = null, // Where the error occurred
        val timestamp: Long = System.currentTimeMillis(),
        val isRecoverable: Boolean = true,
        val recoveryAction: String? = null
    )
    
    /**
     * Error Listener Interface
     */
    interface ErrorListener {
        fun onError(error: AppError)
    }
    
    private val errorListeners = mutableListOf<ErrorListener>()
    
    /**
     * Register an error listener
     */
    fun registerListener(listener: ErrorListener) {
        if (!errorListeners.contains(listener)) {
            errorListeners.add(listener)
        }
    }
    
    /**
     * Unregister an error listener
     */
    fun unregisterListener(listener: ErrorListener) {
        errorListeners.remove(listener)
    }
    
    /**
     * Handle an error
     */
    fun handle(error: AppError) {
        // Log the error
        logError(error)
        
        // Notify listeners
        errorListeners.forEach { listener ->
            try {
                listener.onError(error)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying listener", e)
            }
        }
    }
    
    /**
     * Handle an exception
     */
    fun handle(
        exception: Throwable,
        category: ErrorCategory = ErrorCategory.UNKNOWN,
        severity: ErrorSeverity = ErrorSeverity.MEDIUM,
        context: String? = null,
        userMessage: String? = null
    ) {
        val error = AppError(
            category = category,
            severity = severity,
            message = userMessage ?: exception.message ?: "An error occurred",
            technicalDetails = exception.stackTraceToString(),
            exception = exception,
            context = context
        )
        handle(error)
    }
    
    /**
     * Handle a database error
     */
    fun handleDatabaseError(
        exception: Throwable,
        operation: String,
        isRecoverable: Boolean = true
    ) {
        val error = AppError(
            category = ErrorCategory.DATABASE,
            severity = if (isRecoverable) ErrorSeverity.MEDIUM else ErrorSeverity.HIGH,
            message = "Database error occurred",
            technicalDetails = exception.message,
            exception = exception,
            context = "Database operation: $operation",
            isRecoverable = isRecoverable,
            recoveryAction = if (isRecoverable) "Retry operation" else "Restart app"
        )
        handle(error)
    }
    
    /**
     * Handle a network error
     */
    fun handleNetworkError(
        exception: Throwable,
        endpoint: String? = null
    ) {
        val error = AppError(
            category = ErrorCategory.NETWORK,
            severity = ErrorSeverity.MEDIUM,
            message = "Network connection error",
            technicalDetails = exception.message,
            exception = exception,
            context = endpoint?.let { "API endpoint: $it" },
            isRecoverable = true,
            recoveryAction = "Check internet connection and retry"
        )
        handle(error)
    }
    
    /**
     * Handle a permission error
     */
    fun handlePermissionError(
        permission: String,
        isPermanentlyDenied: Boolean = false
    ) {
        val error = AppError(
            category = ErrorCategory.PERMISSION,
            severity = if (isPermanentlyDenied) ErrorSeverity.HIGH else ErrorSeverity.MEDIUM,
            message = "Permission required: $permission",
            context = "Permission: $permission",
            isRecoverable = true,
            recoveryAction = if (isPermanentlyDenied) 
                "Grant permission in app settings" 
            else 
                "Grant permission when prompted"
        )
        handle(error)
    }
    
    /**
     * Handle a validation error
     */
    fun handleValidationError(
        field: String,
        reason: String
    ) {
        val error = AppError(
            category = ErrorCategory.VALIDATION,
            severity = ErrorSeverity.LOW,
            message = "Invalid $field: $reason",
            context = "Field validation: $field",
            isRecoverable = true,
            recoveryAction = "Correct the input and try again"
        )
        handle(error)
    }
    
    /**
     * Handle an emergency system error
     */
    fun handleEmergencyError(
        exception: Throwable,
        operation: String
    ) {
        val error = AppError(
            category = ErrorCategory.EMERGENCY,
            severity = ErrorSeverity.CRITICAL,
            message = "Emergency system error",
            technicalDetails = exception.message,
            exception = exception,
            context = "Emergency operation: $operation",
            isRecoverable = false,
            recoveryAction = "Contact support immediately"
        )
        handle(error)
    }
    
    /**
     * Log an error
     */
    private fun logError(error: AppError) {
        val logMessage = buildString {
            append("[${ error.category}]")
            append("[${error.severity}] ")
            append(error.message)
            error.context?.let { append(" | Context: $it") }
        }
        
        when (error.severity) {
            ErrorSeverity.LOW -> Log.i(TAG, logMessage, error.exception)
            ErrorSeverity.MEDIUM -> Log.w(TAG, logMessage, error.exception)
            ErrorSeverity.HIGH, ErrorSeverity.CRITICAL -> Log.e(TAG, logMessage, error.exception)
        }
    }
    
    /**
     * Create a coroutine exception handler
     */
    fun createCoroutineExceptionHandler(
        context: String,
        category: ErrorCategory = ErrorCategory.UNKNOWN,
        onError: ((Throwable) -> Unit)? = null
    ): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            handle(
                exception = throwable,
                category = category,
                severity = ErrorSeverity.HIGH,
                context = context
            )
            onError?.invoke(throwable)
        }
    }
    
    /**
     * Execute a block with error handling
     */
    inline fun <T> safely(
        context: String,
        category: ErrorCategory = ErrorCategory.UNKNOWN,
        severity: ErrorSeverity = ErrorSeverity.MEDIUM,
        noinline onError: ((Throwable) -> T)? = null,
        block: () -> T
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            handle(
                exception = e,
                category = category,
                severity = severity,
                context = context
            )
            onError?.invoke(e)
        }
    }
    
    /**
     * Execute a suspend block with error handling
     */
    suspend inline fun <T> safelySuspend(
        context: String,
        category: ErrorCategory = ErrorCategory.UNKNOWN,
        severity: ErrorSeverity = ErrorSeverity.MEDIUM,
        noinline onError: (suspend (Throwable) -> T)? = null,
        crossinline block: suspend () -> T
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            handle(
                exception = e,
                category = category,
                severity = severity,
                context = context
            )
            onError?.invoke(e)
        }
    }
}

