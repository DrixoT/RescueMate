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
 * Handles model loading, continuous listening, partial result callbacks, and confidence scoring.
 */
class VoskSTT(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onPartialResult: ((String) -> Unit)? = null,
    private val onError: ((String) -> Unit)? = null,
    private val onResultWithConfidence: ((String, Float) -> Unit)? = null
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
        StorageService.unpack(context, "models/$MODEL_NAME", "model",
            { result ->
                try {
                    // StorageService.unpack might return the Model object directly or a path
                    if (result is Model) {
                        Log.d(TAG, "Vosk model loaded successfully (Direct Model object)")
                        model = result
                        onInitialized(true)
                        return@unpack
                    }

                    // If we got here, it wasn't a Model object, so try to treat it as a path
                    val pathString = when (result) {
                        is String -> result
                        is File -> result.absolutePath
                        else -> result.toString()
                    }
                    
                    // Check if the returned path ends with the model name, if not append it
                    val fullModelPath = if (pathString.endsWith(MODEL_NAME)) {
                        pathString
                    } else {
                        // Look for the specific model folder inside the unpacked path
                        val unpackedDir = File(pathString)
                        val modelDir = File(unpackedDir, "models/$MODEL_NAME")
                        if (modelDir.exists()) {
                            modelDir.absolutePath
                        } else {
                             // Fallback: try without "models/" prefix if structure is flat
                            val flatModelDir = File(unpackedDir, MODEL_NAME)
                            if (flatModelDir.exists()) {
                                flatModelDir.absolutePath
                            } else {
                                pathString // Hope for the best
                            }
                        }
                    }

                    Log.d(TAG, "Loading Vosk model from: $fullModelPath")
                    model = Model(fullModelPath)
                    Log.d(TAG, "Vosk model loaded successfully")
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
                    hypothesis?.let { processResult(it, isFinal = false) }
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
                     hypothesis?.let { processResult(it, isFinal = true) }
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

    private fun processResult(jsonResult: String, isFinal: Boolean) {
        try {
            val jsonObj = JSONObject(jsonResult)
            val text = jsonObj.optString("text", "")
            
            if (text.isNotEmpty()) {
                // Parse confidence if available (usually in "result" array)
                var confidence = 1.0f
                if (jsonObj.has("result")) {
                    val results = jsonObj.getJSONArray("result")
                    if (results.length() > 0) {
                        // Average confidence of words
                        var totalConf = 0.0
                        for (i in 0 until results.length()) {
                            totalConf += results.getJSONObject(i).optDouble("conf", 1.0)
                        }
                        confidence = (totalConf / results.length()).toFloat()
                    }
                }

                Log.d(TAG, "Result: $text (Conf: $confidence)")
                
                onResult(text)
                onResultWithConfidence?.invoke(text, confidence)
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse error: ${e.message}")
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
