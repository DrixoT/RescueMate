package com.rescuemate.services

import android.content.Context
import android.util.Log
import com.rescuemate.ai.TinyLlamaInferenceService
import com.rescuemate.emergency.data.InteractionLogManager
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
    private val tinyLlamaService = TinyLlamaInferenceService.getInstance(context)
    private var voskSTT: VoskSTT? = null
    private var streamingTTS: StreamingTTS? = null
    private val emergencyAssistant = EmergencyAssistantService()
    
    // Logging
    private val interactionLogManager = InteractionLogManager(context)
    private val transcriptBuilder = StringBuffer()
    private var currentUserId: String = "unknown_user"
    
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
            Log.d(TAG, "Step 1/3: Initializing StreamingTTS...")
            streamingTTS = StreamingTTS(context)
            val ttsInitialized = streamingTTS?.initialize() ?: false
            Log.d(TAG, "Step 1/3: TTS initialized = $ttsInitialized")
            if (!ttsInitialized) {
                Log.e(TAG, "FAILED at Step 1: TTS initialization failed - check if device has offline TTS engine")
                return false
            }
            
            // 2. Initialize Shared TinyLlama Service
            // This manages the model file and the shared native instance
            Log.d(TAG, "Step 2/3: Initializing TinyLlama service...")
            val modelReady = tinyLlamaService.initialize()
            Log.d(TAG, "Step 2/3: TinyLlama initialized = $modelReady")
            if (!modelReady) {
                Log.e(TAG, "FAILED at Step 2: TinyLlama service initialization failed - check model file and native library")
                return false
            }
            
            // 3. Initialize Vosk STT
            // Use suspendCancellableCoroutine to wait for initialization
            Log.d(TAG, "Step 3/3: Initializing Vosk STT...")
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
                    Log.d(TAG, "Step 3/3: Vosk callback received, success = $success")
                    if (continuation.isActive) {
                        continuation.resume(success)
                    }
                }
            }
            
            Log.d(TAG, "Step 3/3: Vosk initialized = $voskInitialized")
            if (!voskInitialized) {
                Log.e(TAG, "FAILED at Step 3: Vosk initialization failed - check model files in assets/models/")
                return false
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
        userId: String? = null,
        callbacks: ConversationCallbacks
    ) {
        userId?.let { currentUserId = it }
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
            val greeting = "Hey Res!, How can I Help you today?"
            callbacks.onMessage("agent", greeting)
            
            // Clear and start transcript
            transcriptBuilder.setLength(0)
            transcriptBuilder.append("agent: $greeting\n")
            
            callbacks.onModeChange("speaking")
            
            // Allow extra time for TTS service binding to fully complete
            // The 'ttsInitialized' flag is set on client init, but service binding is async
            delay(500)

            // Speak greeting
            streamingTTS?.speakToken(greeting)
            streamingTTS?.speakFinal()
            
            // Wait for greeting to finish (estimate) or just start listening
            delay(2500) 
            
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
        // Use safe call on streamingTTS which handles its own exceptions
        val isSpeaking = streamingTTS?.isSpeaking() ?: false
        
        if (isSpeaking && partial.length > 2) {
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
        transcriptBuilder.append("user: $text\n")
        
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
                
                // Stream tokens from Shared LLM Service -> TTS
                tinyLlamaService.generateResponseStream(text, systemPrompt) { token ->
                    responseBuilder.append(token)
                    streamingTTS?.speakToken(token)
                }
                
                // Flush TTS buffer
                streamingTTS?.speakFinal()
                
                val fullResponse = responseBuilder.toString()
                Log.d(TAG, "Full response: $fullResponse")
                transcriptBuilder.append("agent: $fullResponse\n")
                
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
        
        // 1. Save Log FIRST - before cleanup
        try {
            val transcript = transcriptBuilder.toString()
            if (transcript.isNotBlank()) {
                Log.d(TAG, "Saving interaction log...")
                interactionLogManager.saveLog(currentUserId, transcript, "OFFLINE")
            } else {
                Log.d(TAG, "Transcript empty, skipping save.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving interaction log", e)
        }
        
        // Use local val to avoid race conditions
        val currentCallbacks = callbacks
        callbacks = null
        
        currentCallbacks?.onDisconnect()
    }

    fun cleanup() {
        endConversation()
        voskSTT?.cleanup()
        streamingTTS?.shutdown()
        // Do NOT shut down tinyLlamaService here as it is a shared singleton
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
