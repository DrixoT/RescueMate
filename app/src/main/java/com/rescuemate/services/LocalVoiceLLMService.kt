package com.rescuemate.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
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
import kotlinx.coroutines.withTimeoutOrNull
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
        private const val LOW_CONFIDENCE_THRESHOLD = 0.5f
    }

    // Components
    private val tinyLlamaService = TinyLlamaInferenceService.getInstance(context)
    private var voskSTT: VoskSTT? = null
    private var streamingTTS: StreamingTTS? = null
    private val emergencyAssistant = EmergencyAssistantService()
    
    // Audio focus management
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus: Boolean = false
    
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
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        initializeAudioFocus()
    }
    
    /**
     * Initialize audio focus request for Android 8.0+
     */
    private fun initializeAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    handleAudioFocusChange(focusChange)
                }
                .build()
            Log.d(TAG, "Audio focus request initialized for Android O+")
        } else {
            Log.d(TAG, "Using legacy audio focus for Android < O")
        }
    }
    
    /**
     * Handle audio focus changes
     */
    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus gained")
                hasAudioFocus = true
            }
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.w(TAG, "Audio focus lost - pausing conversation")
                hasAudioFocus = false
                streamingTTS?.stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus ducked - continuing conversation")
                hasAudioFocus = true
            }
        }
    }
    
    /**
     * Request audio focus for conversation
     */
    private fun requestAudioFocus(): Boolean {
        return try {
            val audioMgr = audioManager ?: return false
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioMgr.requestAudioFocus(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioMgr.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            Log.d(TAG, "Audio focus request result: $hasAudioFocus (result=$result)")
            hasAudioFocus
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio focus", e)
            false
        }
    }
    
    /**
     * Release audio focus
     */
    private fun releaseAudioFocus() {
        try {
            val audioMgr = audioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioMgr.abandonAudioFocusRequest(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioMgr.abandonAudioFocus(null)
            }
            hasAudioFocus = false
            Log.d(TAG, "Audio focus released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio focus", e)
        }
    }

    /**
     * Initialize all required services
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext true

        // Add a timeout to prevent infinite hangs
        val result = withTimeoutOrNull(60000) { // 60s global timeout
            try {
                Log.d(TAG, "Initializing components...")
                
                // 1. Initialize TTS (needs time)
                Log.d(TAG, "Step 1/3: Initializing StreamingTTS...")
                streamingTTS = StreamingTTS(context)
                val ttsInitialized = streamingTTS?.initialize() ?: false
                Log.d(TAG, "Step 1/3: TTS initialized = $ttsInitialized")
                if (!ttsInitialized) {
                    Log.e(TAG, "FAILED at Step 1: TTS initialization failed - check if device has offline TTS engine")
                    return@withTimeoutOrNull false
                }
                
                // 2. Initialize Shared TinyLlama Service
                // This manages the model file and the shared native instance
                Log.d(TAG, "Step 2/3: Initializing TinyLlama service...")
                val modelReady = tinyLlamaService.initialize()
                Log.d(TAG, "Step 2/3: TinyLlama initialized = $modelReady")
                
                // We don't fail immediately here if model not ready, as it might load asynchronously or we might
                // have a partial failure. But for conversation we really need it.
                if (!modelReady) {
                    Log.e(TAG, "FAILED at Step 2: TinyLlama service initialization failed - check model file and native library")
                    // Attempting to reload once more or checking if path is valid
                    val path = tinyLlamaService.getModelPath()
                    Log.e(TAG, "Model path was: $path")
                    return@withTimeoutOrNull false
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
                    return@withTimeoutOrNull false
                }
                
                isInitialized = true
                Log.d(TAG, "LocalVoiceLLMService initialized successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing LocalVoiceLLMService", e)
                false
            }
        }
        
        if (result == null) {
            Log.e(TAG, "Initialization timed out")
            false
        } else {
            result
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
            // Ensure initialization is robust
            if (!isInitialized) {
                callbacks.onStatusChange("initializing")
                val success = initialize()
                if (!success) {
                    callbacks.onError("Failed to initialize voice AI components. Please check your connection or storage.")
                    isActive = false
                    return@launch
                }
            }

            // Double check TTS Readiness
            if (streamingTTS == null || streamingTTS?.isReady() != true) {
                Log.w(TAG, "TTS not ready immediately after init, waiting...")
                // Wait a bit for binding to stabilize
                var ttsReady = false
                for (i in 0..5) {
                    delay(500)
                    if (streamingTTS?.isReady() == true) {
                        ttsReady = true
                        break
                    }
                }
                
                if (!ttsReady) {
                    callbacks.onError("TTS Service not available/bound")
                    isActive = false
                    return@launch
                }
            }

            callbacks.onConnect(conversationId!!)
            callbacks.onStatusChange("connected")
            
            // Request audio focus before starting conversation
            if (!requestAudioFocus()) {
                Log.w(TAG, "Audio focus not granted initially, will retry when speaking")
            }
            
            // Initial greeting
            val greeting = emergencyAssistant.getGreeting()
            callbacks.onMessage("agent", greeting)
            
            // Clear and start transcript
            transcriptBuilder.setLength(0)
            transcriptBuilder.append("agent: $greeting\n")
            
            callbacks.onModeChange("speaking")
            
            // Allow extra time for TTS service binding to fully complete
            // and for any initial audio focus to settle
            delay(800)

            // Stop listening before speaking to avoid echo
            stopListening()

            // Ensure audio focus before speaking greeting
            if (!hasAudioFocus) {
                requestAudioFocus()
                delay(200) // Brief delay for focus to be granted
            }

            // Speak greeting
            Log.d(TAG, "Speaking greeting: $greeting")
            streamingTTS?.speakToken(greeting)
            streamingTTS?.speakFinal()
            
            // Wait for greeting to finish (estimate based on length) or until speaking done
            // Better approach: monitor isSpeaking or just wait a safe buffer
            var waitTime = (greeting.length * 70).toLong() // approx 70ms per char (slower speech rate)
            delay(waitTime.coerceAtLeast(2000)) 
            
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
        // Always log receipt of partial speech for debugging
        // Log.v(TAG, "Partial speech received: $partial")

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

        // Cut command detection
        if (text.lowercase().contains("stop") || text.lowercase().contains("cut") || text.lowercase().contains("bye")) {
             Log.d(TAG, "User requested to cut conversation")
             endConversation()
             return
        }

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
                var hasReceivedTokens = false
                
                // Notify UI we are about to speak (optimistic)
                withContext(Dispatchers.Main) {
                    callbacks?.onModeChange("speaking")
                }
                
                // Ensure audio focus before speaking response
                if (!hasAudioFocus) {
                    Log.d(TAG, "Requesting audio focus before speaking response")
                    requestAudioFocus()
                    delay(200) // Brief delay for focus to be granted
                }
                
                // Stream tokens from Shared LLM Service -> TTS
                try {
                    Log.d(TAG, "Starting LLM response generation for: '${text.take(50)}...'")
                     tinyLlamaService.generateResponseStream(text, systemPrompt) { token ->
                        hasReceivedTokens = true
                        responseBuilder.append(token)
                        streamingTTS?.speakToken(token)
                    }
                    Log.d(TAG, "LLM response generation completed")
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during token generation", e)
                }
                
                // Fallback if no tokens received (native crash or model failure)
                if (!hasReceivedTokens || responseBuilder.isEmpty()) {
                    Log.e(TAG, "LLM failed to generate response")
                    val errorMsg = "I'm having trouble thinking right now. Please call 911 if this is an emergency."
                    responseBuilder.append(errorMsg)
                    // Ensure audio focus for error message
                    if (!hasAudioFocus) {
                        requestAudioFocus()
                    }
                    streamingTTS?.speakToken(errorMsg)
                }
                
                // Flush TTS buffer
                Log.d(TAG, "Flushing TTS buffer - finalizing speech")
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
                        // Ensure audio focus for safety message
                        if (!hasAudioFocus) {
                            requestAudioFocus()
                        }
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
        
        // Safe stop with try-catch inside streamingTTS
        streamingTTS?.stop()
        
        // Release audio focus
        releaseAudioFocus()
        
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
        
        // Release audio focus
        releaseAudioFocus()
        
        // IMPORTANT: Do NOT shutdown the shared TinyLlama service here blindly
        // But we should ensure any pending ops are cancelled.
        scope.cancel()
        
        // Reset init flag so we re-check everything next time
        isInitialized = false
        audioManager = null
        audioFocusRequest = null
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

    /**
     * Test method to simulate conversation flow without physical voice input.
     * Can be called from UI for verification.
     */
    fun testLocalConversationFlow() {
        scope.launch {
            Log.d(TAG, "🧪 Starting Local Conversation Flow Test")
            
            // 1. Simulate Agent Greeting (already happens on start, but let's assume it's done)
            delay(2000)
            
            // 2. Simulate User Input
            Log.d(TAG, "🧪 Simulating User Input: 'I am feeling dizzy'")
            handleUserSpeech("I am feeling dizzy")
            
            delay(5000) // Wait for processing
            
            // 3. Simulate another User Input
            Log.d(TAG, "🧪 Simulating User Input: 'Also my chest hurts'")
            handleUserSpeech("Also my chest hurts")
            
            delay(5000)
            
            // 4. Simulate Cut/End
            Log.d(TAG, "🧪 Simulating User Input: 'Bye'")
            handleUserSpeech("Bye")
        }
    }
}
