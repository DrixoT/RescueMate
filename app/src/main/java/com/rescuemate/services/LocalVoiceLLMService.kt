package com.rescuemate.services

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.rescuemate.ai.TinyLlamaInferenceService
import com.rescuemate.emergency.EmergencyConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * LocalVoiceLLMService
 * Provides local voice conversation using streaming TinyLlama and Vosk STT.
 * Implements streaming pipeline: Voice -> STT -> LLM Stream -> TTS Stream
 * 
 * Features:
 * - Real-time Vitals Integration (HR, SpO2)
 * - Safety Protocol Enforcement
 * - Confidence-based fallback
 */
class LocalVoiceLLMService(private val context: Context) {

    companion object {
        private const val TAG = "LocalVoiceLLM"
        private const val LOW_CONFIDENCE_THRESHOLD = 0.7f
    }

    // Components
    private val tinyLlamaService = TinyLlamaInferenceService(context) // Helper for model file management
    private var voskSTT: VoskSTT? = null
    private var streamingLLM: StreamingLLM? = null
    private var streamingTTS: StreamingTTS? = null
    private val emergencyAssistant = EmergencyAssistantService()
    
    // State
    private var isInitialized = false
    private var isActive = false
    private var conversationId: String? = null
    private var isProcessing = false
    
    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Callbacks for conversation events
     */
    interface ConversationCallbacks {
        fun onConnect(conversationId: String)
        fun onModeChange(mode: String) // "listening", "speaking", "processing"
        fun onStatusChange(status: String)
        fun onMessage(source: String, messageJson: String)
        fun onError(error: String)
        fun onDisconnect()
        fun onCanSendFeedback(canSend: Boolean)
        fun onAudioLevelChange(level: Float)
    }

    private var callbacks: ConversationCallbacks? = null

    init {
        Log.d(TAG, "LocalVoiceLLMService instantiated")
    }

    /**
     * Initialize all required services
     */
    suspend fun initialize(): Boolean {
        if (isInitialized) return true

        return try {
            Log.d(TAG, "Initializing components...")
            
            // 1. Initialize TTS (needs time)
            streamingTTS = StreamingTTS(context)
            
            // 2. Prepare LLM Model file (using existing service logic)
            // This copies the asset to internal storage if needed
            val modelReady = tinyLlamaService.initialize()
            if (!modelReady) {
                Log.e(TAG, "Failed to prepare TinyLlama model file")
                return false
            }
            
            val modelPath = tinyLlamaService.getModelPath()
            if (modelPath == null) {
                Log.e(TAG, "Model path is null despite initialization")
                return false
            }
            
            // 3. Initialize Streaming LLM (JNI)
            streamingLLM = StreamingLLM(modelPath)
            if (!streamingLLM!!.initialize()) {
                Log.w(TAG, "StreamingLLM JNI init failed - check log for native errors")
                // We continue only if we want to support text fallback, but for voice mode this is critical
                // Return false if strict, or true to allow fallback
                return false 
            }

            // 4. Initialize Vosk STT
            // Use suspendCancellableCoroutine to wait for initialization
            val voskInitialized = suspendCancellableCoroutine<Boolean> { continuation ->
                voskSTT = VoskSTT(
                    context = context,
                    onResult = { text ->
                        // Standard callback (legacy support)
                    },
                    onPartialResult = { partial ->
                        handlePartialSpeech(partial)
                    },
                    onError = { error ->
                        Log.e(TAG, "STT Error: $error")
                        callbacks?.onError("Speech Error: $error")
                    },
                    onResultWithConfidence = { text, confidence ->
                        handleUserSpeech(text, confidence)
                    }
                )
                
                voskSTT?.initialize { success ->
                    if (continuation.isActive) {
                        continuation.resume(success)
                    }
                }
            }
            
            if (!voskInitialized) {
                Log.e(TAG, "Vosk initialization failed")
                return false
            } else {
                Log.d(TAG, "Vosk initialized successfully")
            }
            
            isInitialized = true
            Log.d(TAG, "LocalVoiceLLMService initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing LocalVoiceLLMService", e)
            false
        }
    }

    /**
     * Start a conversation
     */
    fun startConversation(
        systemPrompt: String? = null,
        callbacks: ConversationCallbacks
    ) {
        if (isActive) {
            callbacks.onError("Conversation already active")
            return
        }

        this.callbacks = callbacks
        isActive = true
        conversationId = "local_stream_${System.currentTimeMillis()}"

        scope.launch {
            if (!isInitialized) {
                val success = initialize()
                if (!success) {
                    callbacks.onError("Failed to initialize voice AI components")
                    isActive = false
                    return@launch
                }
            }

            callbacks.onConnect(conversationId!!)
            callbacks.onStatusChange("connected")
            
            // Initial greeting
            val greeting = emergencyAssistant.getGreeting()
            callbacks.onMessage("agent", greeting)
            callbacks.onModeChange("speaking")
            
            // Speak greeting
            streamingTTS?.speakToken(greeting)
            streamingTTS?.speakFinal()
            
            // Wait for greeting to finish (estimate) or just start listening
            delay(2000) 
            
            startListening()
        }
    }
    
    private fun startListening() {
        if (!isActive) return
        
        Log.d(TAG, "Starting listening...")
        callbacks?.onModeChange("listening")
        voskSTT?.startListening()
    }
    
    private fun stopListening() {
        voskSTT?.stopListening()
    }

    /**
     * Handle partial speech (interruption logic)
     */
    private fun handlePartialSpeech(partial: String) {
        // If user speaks while assistant is speaking, interrupt!
        if (streamingTTS?.isSpeaking() == true && partial.length > 2) {
            Log.d(TAG, "Interruption detected: $partial")
            streamingTTS?.stop()
            callbacks?.onModeChange("listening")
        }
    }

    /**
     * Handle complete user speech with confidence score
     */
    private fun handleUserSpeech(text: String, confidence: Float = 1.0f) {
        if (!isActive || isProcessing) return
        if (text.isBlank()) return

        Log.d(TAG, "User said: $text (Conf: $confidence)")
        callbacks?.onMessage("user", text)
        
        // Low confidence check
        if (confidence < LOW_CONFIDENCE_THRESHOLD) {
            val clarificationMsg = "I didn't quite catch that. Could you please repeat?"
            callbacks?.onMessage("agent", clarificationMsg)
            callbacks?.onModeChange("speaking")
            streamingTTS?.speakToken(clarificationMsg)
            streamingTTS?.speakFinal()
            return
        }
        
        // Stop listening while processing/speaking
        stopListening()
        isProcessing = true
        callbacks?.onModeChange("processing")

        scope.launch(Dispatchers.IO) {
            try {
                // 1. Fetch latest vitals
                val prefs = context.getSharedPreferences(EmergencyConstants.PREF_NAME_EMERGENCY, Context.MODE_PRIVATE)
                val currentHeartRate = prefs.getInt("current_heart_rate", 0).takeIf { it > 0 }
                // Assuming SpO2 might be stored similarly or unavailable
                val currentSpO2: Int? = null // Placeholder until SpO2 integration is confirmed

                // 2. Construct Context-Aware Prompt
                val systemPrompt = emergencyAssistant.getSystemPrompt(currentHeartRate, currentSpO2)
                
                // Generate response stream
                val responseBuilder = StringBuilder()
                
                // Notify UI we are about to speak
                withContext(Dispatchers.Main) {
                    callbacks?.onModeChange("speaking")
                }
                
                // Stream tokens from LLM -> TTS
                streamingLLM?.generateResponse(text, systemPrompt) { token ->
                    responseBuilder.append(token)
                    streamingTTS?.speakToken(token)
                }
                
                // Flush TTS buffer
                streamingTTS?.speakFinal()
                
                val fullResponse = responseBuilder.toString()
                Log.d(TAG, "Full response: $fullResponse")
                
                // 3. Safety Fallback Check
                if (emergencyAssistant.shouldTriggerFallback(text)) {
                    // If user mentioned critical keywords, ensure safety message is present
                    if (!fullResponse.lowercase().contains("911") && !fullResponse.lowercase().contains("emergency")) {
                        val safetyMsg = " Please call 911 immediately."
                        responseBuilder.append(safetyMsg)
                        streamingTTS?.speakToken(safetyMsg)
                        streamingTTS?.speakFinal()
                    }
                }
                
                withContext(Dispatchers.Main) {
                    callbacks?.onMessage("agent", responseBuilder.toString())
                }
                
                // Wait a bit before listening again (avoid picking up self)
                delay(1000)

            } catch (e: Exception) {
                Log.e(TAG, "Error in generation pipeline", e)
                withContext(Dispatchers.Main) {
                    callbacks?.onError("Error generating response")
                }
            } finally {
                isProcessing = false
                withContext(Dispatchers.Main) {
                    startListening()
                }
            }
        }
    }

    /**
     * End conversation
     */
    fun endConversation() {
        Log.d(TAG, "Ending conversation")
        isActive = false
        stopListening()
        streamingTTS?.stop()
        callbacks?.onDisconnect()
        callbacks = null
    }

    fun cleanup() {
        endConversation()
        voskSTT?.cleanup()
        streamingTTS?.shutdown()
        streamingLLM?.cleanup()
        scope.cancel()
    }
    
    // Interface compatibility methods
    fun isActive(): Boolean = isActive
    fun getSessionState(): String = if (isActive) "active" else "idle"
    fun toggleMute(): Boolean { return false }
    fun isMuted(): Boolean = false
    fun sendFeedback(isPositive: Boolean) {}
    
    fun sendUserMessage(text: String) {
         if (isActive) {
             handleUserSpeech(text)
         }
    }
}
