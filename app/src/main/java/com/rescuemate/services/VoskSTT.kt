package com.rescuemate.services

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.File

/**
 * VoskSTT
 * Implements offline Speech-to-Text using Vosk-Android.
 * Handles model loading, continuous listening, and partial result callbacks.
 */
class VoskSTT(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onPartialResult: ((String) -> Unit)? = null,
    private val onError: ((String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "VoskSTT"
        private const val MODEL_NAME = "vosk-model-small-en-us-0.15"
        private const val SAMPLE_RATE = 16000.0f
    }

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var isListening = false

    /**
     * Initialize the Vosk model.
     * Unpacks the model from assets to internal storage if needed.
     */
    fun initialize(onInitialized: (Boolean) -> Unit) {
        StorageService.unpack(context, MODEL_NAME, "model",
            { modelPath ->
                try {
                    // modelPath can be either String or File, handle both
                    val pathString = when (modelPath) {
                        is String -> modelPath
                        is File -> modelPath.absolutePath
                        else -> modelPath.toString()
                    }
                    model = Model(pathString)
                    Log.d(TAG, "Vosk model loaded successfully from $pathString")
                    onInitialized(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load Vosk model: ${e.message}")
                    onError?.invoke("Failed to load speech model: ${e.message}")
                    onInitialized(false)
                }
            },
            { error ->
                Log.e(TAG, "Failed to unpack Vosk model: ${error.message}")
                onError?.invoke("Failed to unpack speech model: ${error.message}")
                onInitialized(false)
            }
        )
    }

    /**
     * Start listening for speech.
     */
    fun startListening() {
        if (model == null) {
            Log.e(TAG, "Model not initialized, cannot start listening")
            onError?.invoke("Speech model not initialized")
            return
        }

        if (isListening) return

        try {
            val recognizer = Recognizer(model, SAMPLE_RATE)
            speechService = SpeechService(recognizer, SAMPLE_RATE)
            
            speechService?.startListening(object : RecognitionListener {
                override fun onResult(hypothesis: String?) {
                    hypothesis?.let {
                        try {
                            val jsonObj = JSONObject(it)
                            val text = jsonObj.optString("text", "")
                            if (text.isNotEmpty()) {
                                Log.d(TAG, "Result: $text")
                                onResult(text)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON parse error: ${e.message}")
                        }
                    }
                }

                override fun onPartialResult(hypothesis: String?) {
                    hypothesis?.let {
                        try {
                            val jsonObj = JSONObject(it)
                            val partial = jsonObj.optString("partial", "")
                            if (partial.isNotEmpty()) {
                                onPartialResult?.invoke(partial)
                            }
                        } catch (e: Exception) {
                            // Ignore parse errors for partial results
                        }
                    }
                }

                override fun onFinalResult(hypothesis: String?) {
                     hypothesis?.let {
                        try {
                            val jsonObj = JSONObject(it)
                            val text = jsonObj.optString("text", "")
                            if (text.isNotEmpty()) {
                                Log.d(TAG, "Final Result: $text")
                                onResult(text)
                            }
                        } catch (e: Exception) {
                             Log.e(TAG, "JSON parse error: ${e.message}")
                        }
                    }
                }

                override fun onError(exception: Exception?) {
                    Log.e(TAG, "Recognition error: ${exception?.message}")
                    onError?.invoke(exception?.message ?: "Unknown recognition error")
                    isListening = false
                }

                override fun onTimeout() {
                    Log.d(TAG, "Recognition timeout")
                    isListening = false
                }
            })
            
            isListening = true
            Log.d(TAG, "Started listening")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening: ${e.message}")
            onError?.invoke("Failed to start listening: ${e.message}")
            isListening = false
        }
    }

    /**
     * Stop listening.
     */
    fun stopListening() {
        if (!isListening) return
        
        speechService?.stop()
        speechService = null
        isListening = false
        Log.d(TAG, "Stopped listening")
    }

    /**
     * Clean up resources.
     */
    fun cleanup() {
        stopListening()
        speechService?.shutdown()
        model?.close()
        model = null
    }
    
    fun isInitialized(): Boolean = model != null
}

