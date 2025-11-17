package com.rescuemate.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * LocalSpeechToTextService
 * Uses Android's built-in SpeechRecognizer API for offline speech-to-text
 * Works without network connection (on-device recognition)
 * 
 * NOTE: Offline recognition requires language packs to be downloaded
 * User can download packs via: Settings > Language & Input > Voice Input
 */
class LocalSpeechToTextService(private val context: Context) {

    companion object {
        private const val TAG = "LocalSpeechToText"
    }
    
    /**
     * Exception thrown when offline speech recognition is not available
     */
    class OfflineSpeechNotAvailableException(message: String) : Exception(message)

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    /**
     * Check if speech recognition is available on the device
     */
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    /**
     * Check if offline speech recognition is supported
     * Note: This checks availability but doesn't guarantee offline packs are installed
     */
    fun isOfflineRecognitionSupported(): Boolean {
        if (!isAvailable()) {
            return false
        }
        
        // Android SpeechRecognizer supports offline if the device has it enabled
        // Most modern devices (Android 4.1+) support offline recognition
        // but require language pack downloads
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking offline recognition support", e)
            return false
        }
    }
    
    /**
     * Check if offline language pack is likely available
     * This is a best-effort check
     */
    fun hasOfflineLanguagePack(language: String = "en-US"): Boolean {
        // Unfortunately, Android doesn't provide a direct API to check this
        // We can only try and see if it works
        // Return true optimistically - will handle errors during actual use
        return isOfflineRecognitionSupported()
    }

    /**
     * Initialize the speech recognizer
     */
    fun initialize(): Boolean {
        if (!isAvailable()) {
            Log.e(TAG, "Speech recognition not available on this device")
            return false
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            Log.d(TAG, "Speech recognizer initialized")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize speech recognizer", e)
            return false
        }
    }

    /**
     * Start listening for speech input
     * @param language Language code (e.g., "en-US")
     * @param maxResults Maximum number of results to return
     * @return Recognized text or null if error/timeout
     */
    suspend fun startListening(
        language: String = "en-US",
        maxResults: Int = 1
    ): String? = suspendCancellableCoroutine { continuation ->
        if (!isAvailable() || speechRecognizer == null) {
            Log.e(TAG, "Speech recognizer not available or not initialized")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        if (isListening) {
            Log.w(TAG, "Already listening, stopping previous session")
            stopListening()
        }

        isListening = true

        val recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech input")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech detected, listening...")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Audio level change - can be used for visual feedback
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Partial results (not used in this implementation)
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech ended")
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error - offline language pack may not be available"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout - offline recognition unavailable"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error - offline recognition may be unavailable"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
                    else -> "Unknown error: $error"
                }
                Log.w(TAG, "Speech recognition error: $errorMessage (code: $error)")
                
                // Handle network errors in offline mode
                if (error == SpeechRecognizer.ERROR_NETWORK || 
                    error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
                    error == SpeechRecognizer.ERROR_SERVER) {
                    Log.e(TAG, "Offline speech recognition failed - language pack may not be installed")
                    Log.e(TAG, "Please download offline language pack in Android Settings > Language & Input")
                    continuation.resumeWithException(OfflineSpeechNotAvailableException(errorMessage))
                } else if (error == SpeechRecognizer.ERROR_NO_MATCH || 
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    // These are not critical errors - user might not have spoken
                    continuation.resume(null)
                } else {
                    continuation.resumeWithException(Exception(errorMessage))
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                
                if (matches != null && matches.isNotEmpty()) {
                    val recognizedText = matches[0]
                    Log.d(TAG, "Recognized text: $recognizedText")
                    continuation.resume(recognizedText)
                } else {
                    Log.w(TAG, "No recognition results")
                    continuation.resume(null)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Partial results - can be used for real-time feedback
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    Log.d(TAG, "Partial result: ${matches[0]}")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Additional events
            }
        }

        speechRecognizer?.setRecognitionListener(recognitionListener)

        // Create recognition intent with offline preference
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            
            // Force offline recognition (requires offline language packs)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            
            // Additional flags to ensure offline operation
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, language)
            
            Log.d(TAG, "Starting offline speech recognition for language: $language")
        }

        try {
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Started listening for speech (language: $language)")
        } catch (e: Exception) {
            isListening = false
            Log.e(TAG, "Failed to start listening", e)
            continuation.resumeWithException(e)
        }

        // Cancel listening if coroutine is cancelled
        continuation.invokeOnCancellation {
            stopListening()
        }
    }

    /**
     * Stop listening for speech input
     */
    fun stopListening() {
        if (isListening && speechRecognizer != null) {
            try {
                speechRecognizer?.stopListening()
                isListening = false
                Log.d(TAG, "Stopped listening")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping listening", e)
            }
        }
    }

    /**
     * Cancel current recognition session
     */
    fun cancel() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer?.cancel()
                isListening = false
                Log.d(TAG, "Cancelled recognition")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling recognition", e)
            }
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        Log.d(TAG, "Speech recognizer cleaned up")
    }
}

