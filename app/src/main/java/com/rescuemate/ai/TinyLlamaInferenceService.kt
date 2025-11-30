package com.rescuemate.ai

import android.content.Context
import android.util.Log
import com.rescuemate.services.StreamingLLM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * TinyLlama Inference Service
 * Helper for managing the TinyLlama model file and running offline inference.
 * Uses StreamingLLM JNI backend for actual generation.
 */
class TinyLlamaInferenceService(private val context: Context) {

    companion object {
        private const val TAG = "TinyLlamaInference"
        private const val MODEL_NAME = "TinyLlama-1.1B-Chat-v0.4-Q4_K_M.gguf"
    }

    private var modelPath: String? = null
    private var isInitialized = false
    private var streamingLLM: StreamingLLM? = null

    /**
     * Initialize and ensure model file is ready.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isInitialized && modelPath != null && streamingLLM?.isReady() == true) {
                return@withContext true
            }

            // Check assets
            if (!modelExistsInAssets()) {
                Log.e(TAG, "Model file not found in assets/models/$MODEL_NAME")
                return@withContext false
            }

            // Copy to internal storage
            modelPath = copyModelFromAssets()
            
            if (modelPath != null && File(modelPath!!).exists()) {
                // Initialize StreamingLLM instance for offline analysis
                if (streamingLLM == null) {
                    streamingLLM = StreamingLLM(modelPath!!)
                }
                
                if (streamingLLM!!.initialize()) {
                    isInitialized = true
                    Log.d(TAG, "Model prepared and loaded at: $modelPath")
                    return@withContext true
                } else {
                    Log.e(TAG, "Failed to load model into memory")
                    return@withContext false
                }
            } else {
                Log.e(TAG, "Failed to prepare model file")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TinyLlama service", e)
            return@withContext false
        }
    }

    private fun modelExistsInAssets(): Boolean {
        return try {
            context.assets.open("models/$MODEL_NAME").use { true }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun copyModelFromAssets(): String? = withContext(Dispatchers.IO) {
        try {
            val internalDir = context.getDir("models", Context.MODE_PRIVATE)
            val modelFile = File(internalDir, MODEL_NAME)

            if (modelFile.exists() && modelFile.length() > 0) {
                return@withContext modelFile.absolutePath
            }

            Log.d(TAG, "Copying model from assets...")
            context.assets.open("models/$MODEL_NAME").use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Model copied successfully")
            return@withContext modelFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error copying model", e)
            return@withContext null
        }
    }

    fun getModelPath(): String? = modelPath

    /**
     * Generate health analysis using the offline model.
     * Returns the raw response string (expected to be JSON).
     */
    suspend fun generateHealthAnalysis(prompt: String, maxTokens: Int = 256): String? = withContext(Dispatchers.Default) {
        if (!isInitialized || streamingLLM == null) {
            Log.e(TAG, "Service not initialized")
            return@withContext null
        }

        try {
            val systemPrompt = """You are a medical AI assistant. Analyze the provided vital signs.
Respond ONLY with a valid JSON object in this exact format:
{
  "isAbnormal": boolean,
  "riskScore": float (0.0-1.0),
  "alertReason": "string",
  "recommendedAction": "string",
  "confidence": float (0.0-1.0),
  "trendAnalysis": "string"
}
Do not include markdown formatting or extra text."""

            return@withContext streamingLLM?.generateFullResponse(
                userInput = prompt,
                systemPrompt = systemPrompt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating health analysis", e)
            return@withContext null
        }
    }
    
    suspend fun generateConversation(
        systemPrompt: String,
        conversationHistory: List<Pair<String, String>>,
        userMessage: String
    ): String? {
        return "Please use LocalVoiceLLMService for conversation."
    }

    fun shutdown() {
        streamingLLM?.cleanup()
        streamingLLM = null
        isInitialized = false
    }
}
