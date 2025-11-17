package com.rescuemate.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Error Notification Manager
 * Unified system for displaying errors to users via Snackbars, Toasts, and Dialogs
 */
class ErrorNotificationManager(private val context: Context) {
    
    /**
     * Notification Type
     */
    enum class NotificationType {
        SNACKBAR,
        TOAST,
        DIALOG
    }
    
    /**
     * Show an error using the appropriate notification type
     */
    fun showError(
        error: ErrorHandler.AppError,
        snackbarHostState: SnackbarHostState? = null,
        coroutineScope: CoroutineScope? = null,
        notificationType: NotificationType = NotificationType.SNACKBAR
    ) {
        when (notificationType) {
            NotificationType.SNACKBAR -> {
                if (snackbarHostState != null && coroutineScope != null) {
                    showSnackbar(error, snackbarHostState, coroutineScope)
                } else {
                    // Fallback to toast if snackbar not available
                    showToast(error)
                }
            }
            NotificationType.TOAST -> showToast(error)
            NotificationType.DIALOG -> {
                // Dialog would be handled by UI layer
                // This just ensures the error is available
            }
        }
    }
    
    /**
     * Show error as Snackbar
     */
    private fun showSnackbar(
        error: ErrorHandler.AppError,
        snackbarHostState: SnackbarHostState,
        coroutineScope: CoroutineScope
    ) {
        val message = UserFriendlyErrorMessages.getMessage(error)
        val actionLabel = if (error.isRecoverable && error.recoveryAction != null) "Retry" else null
        
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = when (error.severity) {
                    ErrorHandler.ErrorSeverity.CRITICAL -> SnackbarDuration.Indefinite
                    ErrorHandler.ErrorSeverity.HIGH -> SnackbarDuration.Long
                    else -> SnackbarDuration.Short
                },
                withDismissAction = true
            )
            
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    // Handle retry action
                    handleRetryAction(error)
                }
                SnackbarResult.Dismissed -> {
                    // Snackbar dismissed
                }
            }
        }
    }
    
    /**
     * Show error as Toast
     */
    private fun showToast(error: ErrorHandler.AppError) {
        val message = UserFriendlyErrorMessages.getMessage(error)
        val duration = when (error.severity) {
            ErrorHandler.ErrorSeverity.CRITICAL, ErrorHandler.ErrorSeverity.HIGH -> Toast.LENGTH_LONG
            else -> Toast.LENGTH_SHORT
        }
        Toast.makeText(context, message, duration).show()
    }
    
    /**
     * Handle retry action for recoverable errors
     */
    private fun handleRetryAction(error: ErrorHandler.AppError) {
        when (error.category) {
            ErrorHandler.ErrorCategory.NETWORK -> {
                // Could trigger a network check or retry
            }
            ErrorHandler.ErrorCategory.PERMISSION -> {
                // Open app settings
                openAppSettings()
            }
            ErrorHandler.ErrorCategory.DATABASE -> {
                // Database retry would be handled by the calling code
            }
            else -> {
                // Generic retry
            }
        }
    }
    
    /**
     * Open app settings
     */
    fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Show a simple success message
     */
    fun showSuccess(
        message: String,
        snackbarHostState: SnackbarHostState? = null,
        coroutineScope: CoroutineScope? = null
    ) {
        if (snackbarHostState != null && coroutineScope != null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        } else {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Show a simple info message
     */
    fun showInfo(
        message: String,
        snackbarHostState: SnackbarHostState? = null,
        coroutineScope: CoroutineScope? = null
    ) {
        if (snackbarHostState != null && coroutineScope != null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        } else {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}

