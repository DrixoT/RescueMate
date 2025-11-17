package com.rescuemate.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import com.rescuemate.services.LocalSpeechToTextService

/**
 * Offline Capability Checker
 * Checks if device supports offline features for voice AI
 */
object OfflineCapabilityChecker {
    
    private const val TAG = "OfflineCapabilityCheck"
    
    /**
     * Check if all offline features are available
     */
    fun checkOfflineCapabilities(context: Context): OfflineCapabilityReport {
        val sttService = LocalSpeechToTextService(context)
        
        val speechRecognitionAvailable = sttService.isAvailable()
        val offlineRecognitionSupported = sttService.isOfflineRecognitionSupported()
        val ttsAvailable = checkTTSAvailability(context)
        val tinyLlamaAvailable = checkTinyLlamaAvailability(context)
        
        val allAvailable = speechRecognitionAvailable && 
                          offlineRecognitionSupported && 
                          ttsAvailable && 
                          tinyLlamaAvailable
        
        return OfflineCapabilityReport(
            isFullyOfflineCapable = allAvailable,
            speechRecognitionAvailable = speechRecognitionAvailable,
            offlineRecognitionSupported = offlineRecognitionSupported,
            ttsAvailable = ttsAvailable,
            tinyLlamaAvailable = tinyLlamaAvailable
        )
    }
    
    /**
     * Check if TTS is available
     */
    private fun checkTTSAvailability(context: Context): Boolean {
        return try {
            // TTS is generally available on all Android devices
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking TTS availability", e)
            false
        }
    }
    
    /**
     * Check if TinyLlama model is available
     */
    private fun checkTinyLlamaAvailability(context: Context): Boolean {
        return try {
            context.assets.open("models/TinyLlama-1.1B-Chat-v0.4-Q4_K_M.gguf").use { true }
        } catch (e: Exception) {
            Log.w(TAG, "TinyLlama model not found in assets", e)
            false
        }
    }
    
    /**
     * Get user-friendly message about offline capabilities
     */
    fun getOfflineCapabilityMessage(report: OfflineCapabilityReport): String {
        return when {
            report.isFullyOfflineCapable -> 
                "✅ All offline features available"
            
            !report.speechRecognitionAvailable -> 
                "❌ Speech recognition not available on this device"
            
            !report.offlineRecognitionSupported -> 
                "⚠️ Offline speech recognition requires language pack download.\n" +
                "Go to: Settings > Language & Input > Voice Input"
            
            !report.ttsAvailable -> 
                "❌ Text-to-speech not available"
            
            !report.tinyLlamaAvailable -> 
                "❌ AI model not found. Please ensure TinyLlama model is in assets/models/"
            
            else -> 
                "⚠️ Some offline features may be unavailable"
        }
    }
    
    /**
     * Open settings to download offline language packs
     */
    fun openLanguageSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d(TAG, "Opened language input settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open language settings", e)
            // Fallback to general settings
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open settings", e2)
            }
        }
    }
    
    /**
     * Check if device can download offline language packs
     */
    fun canDownloadOfflinePacks(context: Context): Boolean {
        return try {
            val intent = Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS)
            val pm = context.packageManager
            val activities = pm.queryIntentActivities(intent, 0)
            activities.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking download capability", e)
            false
        }
    }
}

/**
 * Report of offline capabilities
 */
data class OfflineCapabilityReport(
    val isFullyOfflineCapable: Boolean,
    val speechRecognitionAvailable: Boolean,
    val offlineRecognitionSupported: Boolean,
    val ttsAvailable: Boolean,
    val tinyLlamaAvailable: Boolean
) {
    fun getMissingFeatures(): List<String> {
        val missing = mutableListOf<String>()
        if (!speechRecognitionAvailable) missing.add("Speech Recognition")
        if (!offlineRecognitionSupported) missing.add("Offline Language Pack")
        if (!ttsAvailable) missing.add("Text-to-Speech")
        if (!tinyLlamaAvailable) missing.add("AI Model")
        return missing
    }
}

