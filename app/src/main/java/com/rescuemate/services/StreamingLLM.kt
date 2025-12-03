package com.rescuemate.services

import android.util.Log
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * StreamingLLM
 * JNI Wrapper for llama.cpp to support streaming token generation.
 * Requires 'libllama-android.so' to be loaded.
 */
class StreamingLLM(private val modelPath: String) {

    companion object {
        private const val TAG = "StreamingLLM"
        private var nativeLibraryLoaded = false
        
        init {
            try {
                Log.d(TAG, "Loading native library 'llama-android'...")
                System.loadLibrary("llama-android")
                nativeLibraryLoaded = true
                Log.d(TAG, "SUCCESS: Native library 'llama-android' loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                nativeLibraryLoaded = false
                Log.e(TAG, "FAILED: Could not load native library 'llama-android': ${e.message}", e)
            }
        }
        
        fun isNativeLibraryLoaded(): Boolean = nativeLibraryLoaded
    }

    // Native methods that must be implemented in the C++ library
    private external fun initModel(modelPath: String): Long
    private external fun generateTokenStream(
        contextPtr: Long, 
        prompt: String,
        callback: (String) -> Unit
    )
    private external fun freeModel(contextPtr: Long)
    
    private var modelContext: Long = 0
    private var isInitialized = false
    private val modelLock = Any() // Lock for native context access

    /**
     * Initialize the LLM model.
     */
    fun initialize(): Boolean {
        Log.d(TAG, "Starting model initialization...")
        
        synchronized(modelLock) {
            if (isInitialized) {
                Log.d(TAG, "Model already initialized, returning true")
                return true
            }
            
            if (!nativeLibraryLoaded) {
                Log.e(TAG, "FAILED: Cannot initialize - native library was not loaded")
                return false
            }
            
            val file = File(modelPath)
            if (!file.exists()) {
                Log.e(TAG, "FAILED: Model file not found at $modelPath")
                return false
            }
            Log.d(TAG, "Model file exists: $modelPath (${file.length() / 1024 / 1024} MB)")

            try {
                Log.d(TAG, "Calling native initModel (this may take 10-30 seconds)...")
                modelContext = initModel(modelPath)
                Log.d(TAG, "Native initModel returned: $modelContext")
                
                if (modelContext != 0L) {
                    isInitialized = true
                    Log.d(TAG, "SUCCESS: Model initialized successfully, context=$modelContext")
                    return true
                } else {
                    Log.e(TAG, "FAILED: Native initModel returned null context (0)")
                }
            } catch (e: UnsatisfiedLinkError) {
                 Log.e(TAG, "FAILED: Native method initModel not found - JNI bindings issue: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(TAG, "FAILED: Error initializing model: ${e.message}", e)
            }
            return false
        }
    }
    
    /**
     * Generate a response using streaming.
     * Supports custom system prompt.
     */
    fun generateResponse(
        userInput: String, 
        systemPrompt: String? = null,
        onToken: (String) -> Unit
    ) {
        synchronized(modelLock) {
            if (!isInitialized || modelContext == 0L) {
                Log.e(TAG, "Model not initialized")
                return
            }

            val prompt = buildPrompt(userInput, systemPrompt)

            try {
                generateTokenStream(modelContext, prompt) { token ->
                    onToken(token)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during generation", e)
            }
        }
    }
    
    /**
     * Generate a full response (blocking/synchronous like behavior).
     * Useful for non-streaming tasks like health analysis.
     */
    fun generateFullResponse(
        userInput: String,
        systemPrompt: String? = null,
        maxWaitMs: Long = 30000
    ): String {
        synchronized(modelLock) {
            if (!isInitialized || modelContext == 0L) {
                Log.e(TAG, "Model not initialized")
                return ""
            }

            val sb = StringBuilder()
            
            try {
                val prompt = buildPrompt(userInput, systemPrompt)
                
                generateTokenStream(modelContext, prompt) { token ->
                    sb.append(token)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during full generation", e)
            }
            
            return sb.toString().replace("</s>", "").trim()
        }
    }

    /**
     * Build the prompt in TinyLlama-Chat format.
     */
    private fun buildPrompt(userInput: String, systemPrompt: String? = null): String {
        // TinyLlama format:
        // <|system|>
        // System prompt</s>
        // <|user|>
        // User input</s>
        // <|assistant|>
        
        val sys = systemPrompt ?: "You are an emergency assistant. Provide calm, clear, and concise guidance."
        
        return """<|system|>
$sys</s>
<|user|>
$userInput</s>
<|assistant|>
"""
    }
    
    fun cleanup() {
        synchronized(modelLock) {
            if (modelContext != 0L) {
                try {
                    freeModel(modelContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Error freeing model", e)
                }
                modelContext = 0
                isInitialized = false
            }
        }
    }
    
    fun isReady(): Boolean {
        synchronized(modelLock) {
            return isInitialized && modelContext != 0L
        }
    }
}
