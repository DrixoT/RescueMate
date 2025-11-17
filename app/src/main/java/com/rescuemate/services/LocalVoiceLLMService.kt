package com.rescuemate.services

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.rescuemate.ai.TinyLlamaInferenceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * LocalVoiceLLMService
 * Provides local voice conversation using TinyLlama for text generation
 * and Android TTS for voice synthesis when network is unavailable
 * 
 * Implements the same interface as ElevenLabsConversationalService for seamless fallback
 */
class LocalVoiceLLMService(private val context: Context) {

    companion object {
        private const val TAG = "LocalVoiceLLM"
    }

    // Core services
    private val tinyLlamaService = TinyLlamaInferenceService(context)
    private val speechToTextService = LocalSpeechToTextService(context)
    private val voiceMatcher = VoiceMatcher(context)
    private val emergencyAssistant = EmergencyAssistantService()
    
    // TTS
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    
    // Component availability flags
    private var isTinyLlamaAvailable = false
    private var isSttAvailable = false
    
    // Conversation state
    private val conversationHistory = mutableListOf<Pair<String, String>>() // (user, assistant) pairs
    private var conversationId: String? = null
    private var isActive = false
    
    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Callbacks for conversation events (same interface as ElevenLabsConversationalService)
     */
    interface ConversationCallbacks {
        fun onConnect(conversationId: String)
        fun onModeChange(mode: String) // "listening", "speaking"
        fun onStatusChange(status: String)
        fun onMessage(source: String, messageJson: String) // source: "user" or "agent"
        fun onError(error: String)
        fun onDisconnect()
        fun onCanSendFeedback(canSend: Boolean)
        fun onAudioLevelChange(level: Float)
    }

    private var callbacks: ConversationCallbacks? = null

    init {
        Log.d(TAG, "LocalVoiceLLMService initialized")
    }

    /**
     * Initialize all required services
     * Makes components optional - only requires EmergencyAssistantService (which has no dependencies)
     */
    suspend fun initialize(): Boolean {
        try {
            val availableComponents = mutableListOf<String>()
            val warnings = mutableListOf<String>()
            
            // Try to initialize TinyLlama (optional - we have EmergencyAssistantService)
            isTinyLlamaAvailable = try {
                val initialized = tinyLlamaService.initialize()
                if (initialized) {
                    availableComponents.add("TinyLlama")
                    Log.d(TAG, "TinyLlama initialized successfully")
                } else {
                    Log.w(TAG, "TinyLlama not available (optional - using EmergencyAssistantService)")
                    warnings.add("TinyLlama model not available - using rule-based emergency assistant")
                }
                initialized
            } catch (e: Exception) {
                Log.w(TAG, "TinyLlama initialization error (optional)", e)
                warnings.add("TinyLlama unavailable - using rule-based emergency assistant")
                false
            }

            // Try to initialize Speech-to-Text (optional - can use text-only mode)
            isSttAvailable = try {
                val initialized = speechToTextService.initialize()
                if (initialized) {
                    availableComponents.add("Speech-to-Text")
                    Log.d(TAG, "Speech-to-Text initialized successfully")
                    
                    // Check offline speech recognition support
                    if (!speechToTextService.isOfflineRecognitionSupported()) {
                        Log.w(TAG, "Offline speech recognition may not be available")
                        warnings.add("Offline speech recognition may require language pack download")
                    }
                } else {
                    Log.w(TAG, "Speech-to-Text not available (optional - text-only mode available)")
                    warnings.add("Speech recognition unavailable - text input mode available")
                }
                initialized
            } catch (e: Exception) {
                Log.w(TAG, "Speech-to-Text initialization error (optional)", e)
                warnings.add("Speech recognition unavailable - text input mode available")
                false
            }

            // Try to initialize Text-to-Speech (optional - can work without it)
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true
                    
                    // Apply voice configuration
                    val voiceId = voiceMatcher.getStoredVoiceId()
                    voiceMatcher.applyVoiceConfig(textToSpeech!!, voiceId)
                    
                    availableComponents.add("Text-to-Speech")
                    Log.d(TAG, "Text-to-Speech initialized with voice: ${voiceMatcher.getVoiceName(voiceId)}")
                } else {
                    Log.w(TAG, "Text-to-Speech initialization failed (optional)")
                    isTtsInitialized = false
                    warnings.add("Text-to-Speech unavailable - responses will be text-only")
                }
            }

            // Wait for TTS initialization (with timeout)
            var waitCount = 0
            while (!isTtsInitialized && waitCount < 50) { // 5 second timeout
                kotlinx.coroutines.delay(100)
                waitCount++
            }
            
            // TTS was already added to availableComponents in the callback if successful

            // EmergencyAssistantService is always available (no dependencies)
            availableComponents.add("EmergencyAssistant")
            
            // Log initialization status
            Log.d(TAG, "Initialization complete. Available components: ${availableComponents.joinToString(", ")}")
            if (warnings.isNotEmpty()) {
                Log.w(TAG, "Warnings: ${warnings.joinToString("; ")}")
            }
            
            // At minimum, we need EmergencyAssistantService (which is always available)
            // So initialization should always succeed
            if (availableComponents.isEmpty()) {
                Log.e(TAG, "No components available - this should not happen")
                callbacks?.onError("Failed to initialize: No components available")
                return false
            }
            
            // Provide user feedback about available modes
            val modeInfo = when {
                isSttAvailable && isTtsInitialized -> "Voice mode available"
                isTtsInitialized -> "Text input with voice output available"
                isSttAvailable -> "Voice input with text output available"
                else -> "Text-only mode available"
            }
            Log.d(TAG, "Mode: $modeInfo")
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing LocalVoiceLLMService", e)
            // Even if there's an error, EmergencyAssistantService should still work
            // So we return true but log the error
            Log.w(TAG, "Continuing with EmergencyAssistantService despite initialization error")
            return true
        }
    }

    /**
     * Start a conversation
     * @param systemPrompt Optional system prompt (uses default if null)
     * @param callbacks Callbacks for conversation events
     */
    fun startConversation(
        systemPrompt: String? = null,
        callbacks: ConversationCallbacks
    ) {
        if (isActive) {
            Log.w(TAG, "Conversation already active")
            callbacks.onError("Conversation already in progress")
            return
        }

        this.callbacks = callbacks
        isActive = true
        conversationHistory.clear()
        conversationId = "local_${System.currentTimeMillis()}"

        Log.d(TAG, "Starting local voice conversation: $conversationId")

        scope.launch {
            val initialized = initialize()
            if (!initialized) {
                callbacks.onError("Failed to initialize local voice services")
                isActive = false
                return@launch
            }

            callbacks.onConnect(conversationId!!)
            callbacks.onStatusChange("connected")
            callbacks.onModeChange("listening")
            
            // Send initial greeting
            val greeting = emergencyAssistant.getGreeting()
            callbacks.onMessage("agent", greeting)
            
            // Speak greeting if TTS is available
            if (isTtsInitialized) {
                callbacks.onModeChange("speaking")
                speakText(greeting)
            } else {
                Log.w(TAG, "TTS not available - greeting sent as text only")
            }
            
            // Start continuous listening (or text-only mode if STT unavailable)
            startContinuousListening()
            
            Log.d(TAG, "Local conversation started successfully")
        }
    }

    /**
     * Start continuous listening for user speech
     * Automatically processes speech and generates responses
     * Falls back to text-only mode if speech recognition is unavailable
     */
    fun startContinuousListening() {
        if (!isActive) {
            Log.w(TAG, "No active conversation for listening")
            return
        }

        // If STT is not available, switch to text-only mode
        if (!isSttAvailable) {
            Log.w(TAG, "Speech-to-Text not available - using text-only mode")
            callbacks?.onModeChange("text_only")
            callbacks?.onStatusChange("connected_text_only")
            return
        }

        scope.launch {
            var consecutiveOfflineErrors = 0
            val maxOfflineErrors = 3
            
            while (isActive) {
                try {
                    callbacks?.onModeChange("listening")
                    val recognizedText = speechToTextService.startListening()
                    
                    if (recognizedText != null && recognizedText.isNotBlank()) {
                        consecutiveOfflineErrors = 0 // Reset counter on success
                        processRecognizedSpeech(recognizedText)
                    } else {
                        // No speech detected, continue listening
                        kotlinx.coroutines.delay(500)
                    }
                } catch (e: LocalSpeechToTextService.OfflineSpeechNotAvailableException) {
                    consecutiveOfflineErrors++
                    Log.e(TAG, "Offline speech error $consecutiveOfflineErrors/$maxOfflineErrors", e)
                    
                    if (consecutiveOfflineErrors >= maxOfflineErrors) {
                        Log.w(TAG, "Too many offline errors, switching to text-only mode")
                        callbacks?.onError("Offline speech recognition unavailable. Please use text input.")
                        callbacks?.onModeChange("text_only")
                        callbacks?.onStatusChange("connected_text_only")
                        // Don't stop the conversation - allow text input
                        break
                    }
                    kotlinx.coroutines.delay(2000) // Wait longer before retrying
                } catch (e: Exception) {
                    Log.e(TAG, "Error in continuous listening", e)
                    kotlinx.coroutines.delay(1000) // Wait before retrying
                }
            }
        }
    }

    /**
     * Process recognized speech and generate response
     */
    private suspend fun processRecognizedSpeech(recognizedText: String) {
        try {
            callbacks?.onModeChange("processing")
            callbacks?.onMessage("user", recognizedText)

            // Use TinyLlama as PRIMARY response generator (more dynamic and conversational)
            val systemPrompt = emergencyAssistant.getSystemPrompt()
            var response: String? = null
            
            // Try TinyLlama first if available
            if (isTinyLlamaAvailable) {
                response = tinyLlamaService.generateConversation(
                    systemPrompt = systemPrompt,
                    conversationHistory = conversationHistory.toList(),
                    userMessage = recognizedText
                )
                
                if (response != null && response.isNotBlank()) {
                    Log.d(TAG, "Generated response from TinyLlama")
                } else {
                    Log.w(TAG, "TinyLlama returned empty response, falling back to EmergencyAssistant")
                }
            }
            
            // Fallback to EmergencyAssistantService if TinyLlama unavailable or failed
            if (response == null || response.isBlank()) {
                Log.d(TAG, "Using EmergencyAssistantService as fallback")
                response = emergencyAssistant.generateResponse(
                    userMessage = recognizedText,
                    conversationHistory = conversationHistory.toList()
                )
            }
            
            // If still no response, show error
            if (response.isBlank()) {
                callbacks?.onError("Failed to generate response")
                callbacks?.onModeChange("listening")
                return
            }

            conversationHistory.add(Pair(recognizedText, response))
            callbacks?.onMessage("agent", response)
            callbacks?.onModeChange("speaking")

            speakText(response)
            callbacks?.onModeChange("listening")
        } catch (e: LocalSpeechToTextService.OfflineSpeechNotAvailableException) {
            Log.e(TAG, "Offline speech recognition failed", e)
            callbacks?.onError("Offline speech recognition unavailable. Please use text input or download language pack.")
            callbacks?.onModeChange("idle")
            // Switch to text-only mode
            isActive = false
        } catch (e: Exception) {
            Log.e(TAG, "Error processing speech", e)
            callbacks?.onError("Error: ${e.message}")
            callbacks?.onModeChange("listening")
        }
    }

    /**
     * Process user speech input (single recognition)
     * This should be called when user finishes speaking
     */
    suspend fun processUserSpeech(): Boolean {
        if (!isActive) {
            Log.w(TAG, "No active conversation")
            return false
        }

        callbacks?.onModeChange("processing")

        try {
            // Listen for speech
            val recognizedText = speechToTextService.startListening()
            
            if (recognizedText == null || recognizedText.isBlank()) {
                Log.w(TAG, "No speech recognized")
                callbacks?.onModeChange("listening")
                return false
            }

            Log.d(TAG, "Recognized user speech: $recognizedText")
            callbacks?.onMessage("user", recognizedText)

            // Use TinyLlama as PRIMARY response generator
            val systemPrompt = emergencyAssistant.getSystemPrompt()
            var response: String? = null
            
            // Try TinyLlama first if available
            if (isTinyLlamaAvailable) {
                response = tinyLlamaService.generateConversation(
                    systemPrompt = systemPrompt,
                    conversationHistory = conversationHistory.toList(),
                    userMessage = recognizedText
                )
                
                if (response != null && response.isNotBlank()) {
                    Log.d(TAG, "Generated response from TinyLlama")
                } else {
                    Log.w(TAG, "TinyLlama returned empty response, falling back to EmergencyAssistant")
                }
            }
            
            // Fallback to EmergencyAssistantService if TinyLlama unavailable or failed
            if (response == null || response.isBlank()) {
                Log.d(TAG, "Using EmergencyAssistantService as fallback")
                response = emergencyAssistant.generateResponse(
                    userMessage = recognizedText,
                    conversationHistory = conversationHistory.toList()
                )
            }
            
            // If still no response, show error
            if (response.isBlank()) {
                callbacks?.onError("Failed to generate response")
                callbacks?.onModeChange("listening")
                return false
            }

            // Store in conversation history
            conversationHistory.add(Pair(recognizedText, response))

            Log.d(TAG, "Generated response: $response")
            callbacks?.onMessage("agent", response)
            callbacks?.onModeChange("speaking")

            // Speak the response
            speakText(response)

            callbacks?.onModeChange("listening")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error processing user speech", e)
            callbacks?.onError("Error: ${e.message}")
            callbacks?.onModeChange("listening")
            return false
        }
    }

    /**
     * Send a text message (alternative to voice input)
     * This serves as a fallback when voice recognition fails
     */
    fun sendUserMessage(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "Empty message")
            return
        }

        // Allow text messages even if conversation not fully active
        // This enables text-only mode as fallback
        scope.launch {
            try {
                callbacks?.onModeChange("processing")
                callbacks?.onMessage("user", text)

                // Use TinyLlama as PRIMARY response generator
                val systemPrompt = emergencyAssistant.getSystemPrompt()
                var response: String? = null
                
                // Try TinyLlama first if available
                if (isTinyLlamaAvailable) {
                    response = tinyLlamaService.generateConversation(
                        systemPrompt = systemPrompt,
                        conversationHistory = conversationHistory.toList(),
                        userMessage = text
                    )
                    
                    if (response != null && response.isNotBlank()) {
                        Log.d(TAG, "Generated response from TinyLlama")
                    } else {
                        Log.w(TAG, "TinyLlama returned empty response, falling back to EmergencyAssistant")
                    }
                } else {
                    // Try to initialize TinyLlama if not already initialized
                    if (tinyLlamaService.initialize()) {
                        isTinyLlamaAvailable = true
                        response = tinyLlamaService.generateConversation(
                            systemPrompt = systemPrompt,
                            conversationHistory = conversationHistory.toList(),
                            userMessage = text
                        )
                        if (response != null && response.isNotBlank()) {
                            Log.d(TAG, "Generated response from TinyLlama (initialized on demand)")
                        }
                    }
                }
                
                // Fallback to EmergencyAssistantService if TinyLlama unavailable or failed
                if (response == null || response.isBlank()) {
                    Log.d(TAG, "Using EmergencyAssistantService as fallback")
                    response = emergencyAssistant.generateResponse(
                        userMessage = text,
                        conversationHistory = conversationHistory.toList()
                    )
                }
                
                // If still no response, show error
                if (response.isBlank()) {
                    callbacks?.onError("Failed to generate response")
                    callbacks?.onModeChange("text_input")
                    return@launch
                }

                // Use the generated response
                conversationHistory.add(Pair(text, response))
                callbacks?.onMessage("agent", response)
                
                // Only speak if TTS is available
                if (isTtsInitialized) {
                    callbacks?.onModeChange("speaking")
                    speakText(response)
                }

                callbacks?.onModeChange("text_input")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing text message", e)
                callbacks?.onError("Error: ${e.message}")
                callbacks?.onModeChange("text_input")
            }
        }
    }
    
    /**
     * Enable text-only mode (fallback when voice fails)
     */
    fun enableTextOnlyMode() {
        Log.d(TAG, "Switching to text-only mode")
        isActive = true // Keep conversation active
        conversationId = "local_text_${System.currentTimeMillis()}"
        
        scope.launch {
            // Initialize only TinyLlama (skip STT)
            val llamaInitialized = tinyLlamaService.initialize()
            if (!llamaInitialized) {
                callbacks?.onError("AI model not available for text mode")
                return@launch
            }
            
            // Try to initialize TTS (optional for text mode)
            if (textToSpeech == null) {
                textToSpeech = TextToSpeech(context) { status ->
                    isTtsInitialized = status == TextToSpeech.SUCCESS
                }
            }
            
            callbacks?.onConnect(conversationId!!)
            callbacks?.onStatusChange("connected_text_only")
            callbacks?.onModeChange("text_input")
            Log.d(TAG, "Text-only mode enabled")
        }
    }

    /**
     * Speak text using TTS
     */
    private fun speakText(text: String) {
        if (!isTtsInitialized || textToSpeech == null) {
            Log.e(TAG, "TTS not initialized")
            return
        }

        try {
            // Remove any JSON formatting if present
            val cleanText = text.replace(Regex("\\{.*?\\}"), "").trim()
            
            val result = textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, null)
            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "TTS speak error")
                callbacks?.onError("Failed to speak response")
            } else {
                Log.d(TAG, "Speaking: ${cleanText.take(50)}...")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text", e)
            callbacks?.onError("Error speaking: ${e.message}")
        }
    }

    /**
     * Toggle microphone mute/unmute
     */
    fun toggleMute(): Boolean {
        // For local service, we can pause TTS
        if (isTtsInitialized && textToSpeech != null) {
            textToSpeech?.stop()
        }
        return false // Always return false (not muted) for simplicity
    }

    /**
     * Check if microphone is muted
     */
    fun isMuted(): Boolean = false

    /**
     * Send feedback (not implemented for local service)
     */
    fun sendFeedback(isPositive: Boolean) {
        Log.d(TAG, "Feedback received: ${if (isPositive) "positive" else "negative"}")
        // Local service doesn't support feedback
    }

    /**
     * End the conversation
     */
    fun endConversation() {
        if (!isActive) {
            return
        }

        Log.d(TAG, "Ending local conversation")

        speechToTextService.stopListening()
        textToSpeech?.stop()
        
        isActive = false
        conversationHistory.clear()
        conversationId = null
        
        callbacks?.onDisconnect()
        callbacks = null

        Log.d(TAG, "Conversation ended")
    }

    /**
     * Check if conversation is active
     */
    fun isActive(): Boolean = isActive

    /**
     * Get session state
     */
    fun getSessionState(): String {
        return when {
            isActive -> "active"
            else -> "idle"
        }
    }

    /**
     * Clean up all resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up LocalVoiceLLMService")
        endConversation()
        speechToTextService.cleanup()
        textToSpeech?.shutdown()
        textToSpeech = null
        tinyLlamaService.shutdown()
        scope.cancel()
        Log.d(TAG, "Cleanup complete")
    }
}

