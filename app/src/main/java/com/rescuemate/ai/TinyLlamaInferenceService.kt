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
    
    // Mutex to prevent concurrent native calls which crash llama.cpp
    private val inferenceMutex = Mutex()

    /**
     * Initialize and ensure model file is ready.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized && modelPath != null && streamingLLM?.isReady() == true) {
            Log.d(TAG, "Already initialized, returning true")
            return@withContext true
        }

        // Use withLock but with a check to ensure we don't deadlock if previously crashed
        inferenceMutex.withLock {
            try {
                Log.d(TAG, "Starting TinyLlama initialization...")
                
                // Double check after acquiring lock
                if (isInitialized && modelPath != null && streamingLLM?.isReady() == true) {
                    return@withLock true
                }

                // Check assets
                Log.d(TAG, "Checking if model exists in assets...")
                // Skip asset list check if we can't list it, but rely on copy failing
                // if (!modelExistsInAssets()) {
                //    Log.e(TAG, "Model file not found in assets/models/$MODEL_NAME")
                //    return@withLock false
                // }
                // Log.d(TAG, "Model found in assets")

                // Copy to internal storage
                Log.d(TAG, "Copying model to internal storage (this may take a while for large models)...")
                modelPath = copyModelFromAssets()
                Log.d(TAG, "Copy complete, modelPath = $modelPath")
                
                if (modelPath != null && File(modelPath!!).exists()) {
                    val fileSize = File(modelPath!!).length()
                    Log.d(TAG, "Model file exists at $modelPath (size: ${fileSize / 1024 / 1024} MB)")
                    
                    // Initialize StreamingLLM instance for offline analysis
                    if (streamingLLM == null) {
                        Log.d(TAG, "Creating StreamingLLM instance...")
                        streamingLLM = StreamingLLM(modelPath!!)
                    }
                    
                    // Initialize native model if not already done
                    if (!streamingLLM!!.isReady()) {
                        Log.d(TAG, "Loading model into native memory (this may take 10-30 seconds)...")
                        if (streamingLLM!!.initialize()) {
                            isInitialized = true
                            Log.d(TAG, "SUCCESS: Model prepared and loaded at: $modelPath")
                            return@withLock true
                        } else {
                            Log.e(TAG, "FAILED: Native model initialization failed - check native library loading")
                            return@withLock false
                        }
                    }
                    Log.d(TAG, "Model already loaded, returning true")
                    isInitialized = true
                    return@withLock true
                } else {
                    Log.e(TAG, "Failed to prepare model file - modelPath=$modelPath, exists=${modelPath?.let { File(it).exists() }}")
                    return@withLock false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing TinyLlama service: ${e.message}", e)
                return@withLock false
            }
        }
    }

    private fun modelExistsInAssets(): Boolean {
        return try {
            context.assets.list("models")?.contains(MODEL_NAME) == true
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
     * This should essentially NEVER be called during app runtime unless memory pressure is extreme.
     * The crash happened because we were cleaning up while a request was pending or active.
     */
    suspend fun shutdown() {
        // We prevent shutdown if ANY inference is running by waiting for lock
        // If this hangs, it means inference is stuck, but better than crashing
        try {
            if (inferenceMutex.isLocked) {
                Log.w(TAG, "Attempting shutdown while mutex locked - waiting...")
            }
            
            inferenceMutex.withLock {
                try {
                    streamingLLM?.cleanup()
                    streamingLLM = null
                    isInitialized = false
                    Log.d(TAG, "TinyLlama service shutdown complete")
                } catch (e: Exception) {
                    Log.e(TAG, "Error shutting down model", e)
                }
            }
        } catch (e: Exception) {
             Log.e(TAG, "Error acquiring lock for shutdown", e)
        }
    }
    
    /**
     * Force reset state (use with caution, only if stuck)
     */
    fun forceReset() {
        isInitialized = false
        // We don't clear streamingLLM here to avoid NPEs in running threads, 
        // but we mark it as uninitialized to force re-init
    }
}
