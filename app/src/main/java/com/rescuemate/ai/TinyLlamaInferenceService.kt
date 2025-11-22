package com.rescuemate.ai

import android.content.Context
import android.util.Log
import com.rescuemate.services.StreamingLLM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * TinyLlama Inference Service
 * Helper for managing the TinyLlama model file.
 * Actual inference is now handled by StreamingLLM.
 */
class TinyLlamaInferenceService(private val context: Context) {

    companion object {
        private const val TAG = "TinyLlamaInference"
        private const val MODEL_NAME = "TinyLlama-1.1B-Chat-v0.4.Q4_K_M.gguf"
    }

    private var modelPath: String? = null
    private var isInitialized = false

    /**
     * Initialize and ensure model file is ready.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isInitialized && modelPath != null) {
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
                isInitialized = true
                Log.d(TAG, "Model prepared at: $modelPath")
                return@withContext true
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

    // Deprecated methods maintained for compatibility if referenced elsewhere
    suspend fun generateHealthAnalysis(prompt: String, maxTokens: Int = 256): String? {
        // This would need to be implemented via StreamingLLM non-streaming mode if needed
        return "Health analysis requires streaming implementation update."
    }
    
    suspend fun generateConversation(
        systemPrompt: String,
        conversationHistory: List<Pair<String, String>>,
        userMessage: String
    ): String? {
        return "Please use LocalVoiceLLMService for conversation."
    }

    fun shutdown() {
        // Nothing to clean up here, model file remains
    }
}
