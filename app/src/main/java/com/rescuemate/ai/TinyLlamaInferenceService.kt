package com.rescuemate.ai

import android.content.Context
import android.util.Log
// TODO: Fix LlamaBridge import - library may have different package structure
// import com.llamatik.LlamaBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * TinyLlama Inference Service
 * Primary LLM service for health analysis - runs locally on device
 * Uses Llamatik (llama.cpp Kotlin wrapper) for on-device inference
 */
class TinyLlamaInferenceService(private val context: Context) {

    companion object {
        private const val TAG = "TinyLlamaInference"
        private const val MODEL_NAME = "TinyLlama-1.1B-Chat-v0.4-Q4_K_M.gguf"
    }

    private var modelPath: String? = null
    private var isInitialized = false
    private var isModelLoaded = false

    /**
     * Initialize the service and load model
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isInitialized && isModelLoaded) {
                Log.d(TAG, "TinyLlama already initialized")
                return@withContext true
            }

            // Check if model exists in assets
            if (!modelExistsInAssets()) {
                Log.w(TAG, "Model file not found in assets/models/$MODEL_NAME")
                Log.w(TAG, "Please download TinyLlama GGUF model and place it in app/src/main/assets/models/")
                return@withContext false
            }

            // Use Llamatik to get model path (copies from assets to app files dir)
            try {
                // Note: getModelPath is @Composable in Llamatik, so we need to handle it carefully
                // For now, manually copy the model
                modelPath = copyModelFromAssets()
                
                if (modelPath == null) {
                    Log.e(TAG, "Failed to prepare model file")
                    return@withContext false
                }

                // Load model using Llamatik
                // TODO: Fix LlamaBridge API - library integration needs verification
                // For now, mark as loaded if model file exists
                if (modelPath != null && File(modelPath).exists()) {
                    isModelLoaded = true
                    isInitialized = true
                    Log.d(TAG, "TinyLlama model file prepared (LlamaBridge integration pending): $modelPath")
                    return@withContext true
                } else {
                    Log.e(TAG, "Model file not found at: $modelPath")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading model with Llamatik", e)
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TinyLlama", e)
            return@withContext false
        }
    }

    /**
     * Check if model exists in assets
     */
    private fun modelExistsInAssets(): Boolean {
        return try {
            context.assets.open("models/$MODEL_NAME").use { true }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Copy model from assets to internal storage
     */
    private suspend fun copyModelFromAssets(): String? = withContext(Dispatchers.IO) {
        try {
            val internalDir = context.getDir("models", Context.MODE_PRIVATE)
            val modelFile = File(internalDir, MODEL_NAME)

            // Check if model already exists
            if (modelFile.exists() && modelFile.length() > 0) {
                Log.d(TAG, "Model file already exists: ${modelFile.absolutePath}")
                return@withContext modelFile.absolutePath
            }

            // Copy from assets
            context.assets.open("models/$MODEL_NAME").use { input ->
                modelFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            
            Log.d(TAG, "Model copied from assets to: ${modelFile.absolutePath}")
            return@withContext modelFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error copying model from assets", e)
            return@withContext null
        }
    }

    /**
     * Generate health analysis response using TinyLlama via Llamatik
     * @param prompt The health analysis prompt (includes system, context, and user prompt)
     * @param maxTokens Maximum tokens to generate (default: 256) - Note: Llamatik may not support this parameter
     * @return Analysis result or null if failed
     */
    suspend fun generateHealthAnalysis(
        prompt: String,
        maxTokens: Int = 256
    ): String? = withContext(Dispatchers.Default) {
        if (!isModelLoaded || modelPath == null) {
            Log.w(TAG, "Model not loaded, cannot generate analysis")
            return@withContext null
        }

        try {
            // TODO: Implement LlamaBridge.generate() when library is properly integrated
            // For now, return a placeholder response
            Log.w(TAG, "LlamaBridge.generate() not available - returning placeholder")
            val response = "Health analysis: Model loaded but LlamaBridge integration pending. Please check library configuration."
            
            Log.d(TAG, "Generated placeholder response (${response.length} chars)")
            return@withContext response.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error generating health analysis", e)
            return@withContext null
        }
    }

    /**
     * Generate conversational response with context
     * @param systemPrompt System instructions for the AI
     * @param conversationHistory Previous conversation messages (user/assistant pairs)
     * @param userMessage Current user message
     * @return AI response or null if failed
     */
    suspend fun generateConversation(
        systemPrompt: String,
        conversationHistory: List<Pair<String, String>> = emptyList(), // (user, assistant) pairs
        userMessage: String
    ): String? = withContext(Dispatchers.Default) {
        if (!isModelLoaded || modelPath == null) {
            Log.w(TAG, "Model not loaded, cannot generate conversation")
            return@withContext null
        }

        try {
            // Build conversation prompt
            val prompt = buildConversationPrompt(systemPrompt, conversationHistory, userMessage)
            
            // TODO: Implement LlamaBridge.generateWithContext() when library is properly integrated
            // For now, return a placeholder response
            Log.w(TAG, "LlamaBridge.generateWithContext() not available - returning placeholder")
            val response = "I understand you said: $userMessage. However, the LlamaBridge library integration needs to be configured. Please check the library setup."
            
            Log.d(TAG, "Generated placeholder conversation response (${response.length} chars)")
            return@withContext response.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error generating conversation with Llamatik", e)
            return@withContext null
        }
    }

    /**
     * Build conversation prompt from system prompt, history, and current message
     */
    private fun buildConversationPrompt(
        systemPrompt: String,
        conversationHistory: List<Pair<String, String>>,
        userMessage: String
    ): String {
        val builder = StringBuilder()
        
        // System prompt
        builder.append("$systemPrompt\n\n")
        
        // Conversation history
        if (conversationHistory.isNotEmpty()) {
            builder.append("Previous conversation:\n")
            conversationHistory.forEach { (user, assistant) ->
                builder.append("User: $user\n")
                builder.append("Assistant: $assistant\n\n")
            }
        }
        
        // Current user message
        builder.append("User: $userMessage\n")
        builder.append("Assistant:")
        
        return builder.toString()
    }

    /**
     * Check if model is available and loaded
     */
    fun isAvailable(): Boolean {
        return isModelLoaded && modelPath != null
    }

    /**
     * Get model file path (for debugging)
     */
    fun getModelPath(): String? = modelPath

    /**
     * Shutdown and cleanup resources
     */
    fun shutdown() {
        if (isModelLoaded) {
            try {
                // TODO: Implement LlamaBridge.shutdown() when library is properly integrated
                // LlamaBridge.shutdown()
                isModelLoaded = false
                isInitialized = false
                Log.d(TAG, "TinyLlama cleaned up (LlamaBridge shutdown pending)")
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }
}

