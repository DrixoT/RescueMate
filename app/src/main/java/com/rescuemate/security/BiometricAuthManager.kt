package com.rescuemate.security

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Biometric Authentication Manager
 * Handles fingerprint/face authentication for sensitive operations
 */
class BiometricAuthManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BiometricAuthManager"
    }
    
    private val biometricManager = BiometricManager.from(context)
    
    /**
     * Check if biometric authentication is available
     */
    fun isBiometricAvailable(): BiometricAvailability {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                BiometricAvailability.AVAILABLE
                
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                BiometricAvailability.NO_HARDWARE
                
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                BiometricAvailability.HARDWARE_UNAVAILABLE
                
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                BiometricAvailability.NOT_ENROLLED
                
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                BiometricAvailability.SECURITY_UPDATE_REQUIRED
                
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
                BiometricAvailability.UNSUPPORTED
                
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
                BiometricAvailability.UNKNOWN
                
            else -> BiometricAvailability.UNKNOWN
        }
    }
    
    /**
     * Authenticate user with biometrics
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Biometric Authentication",
        subtitle: String = "Verify your identity",
        description: String = "Use your fingerprint or face to authenticate",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        val availability = isBiometricAvailable()
        
        if (availability != BiometricAvailability.AVAILABLE) {
            onError("Biometric authentication not available: ${availability.message}")
            return
        }
        
        val executor = ContextCompat.getMainExecutor(context)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText("Cancel")
            .setConfirmationRequired(true)
            .build()
        
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, "Authentication error: $errString (code: $errorCode)")
                    
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            onCancel()
                        }
                        else -> {
                            onError(errString.toString())
                        }
                    }
                }
                
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d(TAG, "Authentication succeeded")
                    onSuccess()
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "Authentication failed")
                    // Don't call error callback here - user can retry
                }
            })
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    /**
     * Biometric availability status
     */
    enum class BiometricAvailability(val message: String) {
        AVAILABLE("Biometric authentication is available"),
        NO_HARDWARE("No biometric hardware available"),
        HARDWARE_UNAVAILABLE("Biometric hardware is currently unavailable"),
        NOT_ENROLLED("No biometric credentials enrolled"),
        SECURITY_UPDATE_REQUIRED("Security update required"),
        UNSUPPORTED("Biometric authentication is not supported"),
        UNKNOWN("Unknown biometric availability status")
    }
    
    /**
     * Get user-friendly message for biometric availability
     */
    fun getAvailabilityMessage(): String {
        return isBiometricAvailable().message
    }
    
    /**
     * Check if device supports biometric authentication
     */
    fun isSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                isBiometricAvailable() != BiometricAvailability.NO_HARDWARE
    }
}

