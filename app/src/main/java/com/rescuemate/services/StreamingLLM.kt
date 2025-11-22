package com.rescuemate.services

import android.util.Log
import java.io.File

/**
 * StreamingLLM
 * JNI Wrapper for llama.cpp to support streaming token generation.
 * Requires 'libllama-android.so' to be loaded.
 */
class StreamingLLM(private val modelPath: String) {

    companion object {
        private const val TAG = "StreamingLLM"
        
        init {
            try {
                System.loadLibrary("llama-android")
                Log.d(TAG, "Native library 'llama-android' loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library 'llama-android'", e)
            }
        }
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

    /**
     * Initialize the LLM model.
     */
    fun initialize(): Boolean {
        if (isInitialized) return true
        
        val file = File(modelPath)
        if (!file.exists()) {
            Log.e(TAG, "Model file not found at $modelPath")
            return false
        }
        
        try {
            modelContext = initModel(modelPath)
            if (modelContext != 0L) {
                isInitialized = true
                Log.d(TAG, "Model initialized successfully")
                return true
            }
        } catch (e: UnsatisfiedLinkError) {
             Log.e(TAG, "Native method initModel not found. Make sure JNI bindings are correct.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing model", e)
        }
        return false
    }
    
    /**
     * Generate a response using streaming.
     */
    fun generateResponse(userInput: String, onToken: (String) -> Unit) {
        if (!isInitialized) {
            Log.e(TAG, "Model not initialized")
            return
        }

        val prompt = buildPrompt(userInput)
        
        try {
            generateTokenStream(modelContext, prompt) { token ->
                onToken(token)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during generation", e)
        }
    }
    
    /**
     * Build the prompt in TinyLlama-Chat format.
     */
    private fun buildPrompt(userInput: String): String {
        // TinyLlama format:
        // <|system|>
        // System prompt</s>
        // <|user|>
        // User input</s>
        // <|assistant|>
        
        return """<|system|>
You are an emergency assistant. Provide calm, clear, and concise guidance.</s>
<|user|>
$userInput</s>
<|assistant|>
"""
    }
    
    fun cleanup() {
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
    
    fun isReady(): Boolean = isInitialized
}

