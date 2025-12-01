package com.rescuemate.ai

import android.content.Context
import android.util.Log
import com.rescuemate.services.StreamingLLM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * TinyLlama Inference Service
 * Helper for managing the TinyLlama model file and running offline inference.
 * Uses StreamingLLM JNI backend for actual generation.
 * 
 * Implements Singleton pattern to manage single native model instance safely.
 */
class TinyLlamaInferenceService private constructor(private val context: Context) {

    companion object {
        private const val TAG = "TinyLlamaInference"
        private const val MODEL_NAME = "TinyLlama-1.1B-Chat-v0.4-Q4_K_M.gguf"

        @Volatile
        private var instance: TinyLlamaInferenceService? = null

        fun getInstance(context: Context): TinyLlamaInferenceService {
            return instance ?: synchronized(this) {
                instance ?: TinyLlamaInferenceService(context.applicationContext).also { instance = it }
            }
        }
    }

    private var modelPath: String? = null
    private var isInitialized = false
    private var streamingLLM: StreamingLLM? = null
    private val inferenceMutex = Mutex()

    /**
     * Initialize and ensure model file is ready.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        inferenceMutex.withLock {
            try {
                if (isInitialized && modelPath != null && streamingLLM?.isReady() == true) {
                    return@withLock true
                }

                // Check assets
                if (!modelExistsInAssets()) {
                    Log.e(TAG, "Model file not found in assets/models/$MODEL_NAME")
                    return@withLock false
                }

                // Copy to internal storage
                modelPath = copyModelFromAssets()
                
                if (modelPath != null && File(modelPath!!).exists()) {
                    // Initialize StreamingLLM instance for offline analysis
                    if (streamingLLM == null) {
                        streamingLLM = StreamingLLM(modelPath!!)
                    }
                    
                    // Initialize native model if not already done
                    if (!streamingLLM!!.isReady()) {
                        if (streamingLLM!!.initialize()) {
                            isInitialized = true
                            Log.d(TAG, "Model prepared and loaded at: $modelPath")
                            return@withLock true
                        } else {
                            Log.e(TAG, "Failed to load model into memory")
                            return@withLock false
                        }
                    }
                    return@withLock true
                } else {
                    Log.e(TAG, "Failed to prepare model file")
                    return@withLock false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing TinyLlama service", e)
                return@withLock false
            }
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
        inferenceMutex.withLock {
            if (!isInitialized || streamingLLM == null) {
                Log.e(TAG, "Service not initialized")
                return@withLock null
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

                return@withLock streamingLLM?.generateFullResponse(
                    userInput = prompt,
                    systemPrompt = systemPrompt
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error generating health analysis", e)
                return@withLock null
            }
        }
    }

    /**
     * Generate a streaming response for conversation.
     * Thread-safe execution ensuring only one inference runs at a time.
     */
    suspend fun generateResponseStream(
        userInput: String,
        systemPrompt: String? = null,
        onToken: (String) -> Unit
    ) = withContext(Dispatchers.Default) {
        inferenceMutex.withLock {
            if (!isInitialized || streamingLLM == null) {
                Log.e(TAG, "Service not initialized")
                return@withLock
            }
            try {
                streamingLLM?.generateResponse(userInput, systemPrompt, onToken)
            } catch (e: Exception) {
                Log.e(TAG, "Error streaming response", e)
            }
        }
    }
    
    /**
     * Generate a simple text response for summarization or general queries.
     */
    suspend fun generateSimpleResponse(
        prompt: String, 
        systemPrompt: String = "You are a helpful assistant. Be concise."
    ): String? = withContext(Dispatchers.Default) {
        inferenceMutex.withLock {
            if (!isInitialized || streamingLLM == null) {
                Log.e(TAG, "Service not initialized")
                return@withLock null
            }
            try {
                return@withLock streamingLLM?.generateFullResponse(prompt, systemPrompt)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating simple response", e)
                return@withLock null
            }
        }
    }

    suspend fun generateConversation(
        systemPrompt: String,
        conversationHistory: List<Pair<String, String>>,
        userMessage: String
    ): String? {
        return "Please use LocalVoiceLLMService for conversation."
    }

    /**
     * Shutdown the model.
     * WARNING: This affects all users of the service.
     */
    suspend fun shutdown() {
        inferenceMutex.withLock {
            streamingLLM?.cleanup()
            streamingLLM = null
            isInitialized = false
        }
    }
}
