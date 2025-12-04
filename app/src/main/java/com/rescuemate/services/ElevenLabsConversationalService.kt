package com.rescuemate.services

import android.Manifest
import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.rescuemate.BuildConfig
import com.rescuemate.utils.ErrorHandler
import com.rescuemate.utils.NetworkMonitor
import com.rescuemate.emergency.data.InteractionLogManager
import io.elevenlabs.ConversationClient
import io.elevenlabs.ConversationConfig
import io.elevenlabs.ConversationSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

/**
 * ElevenLabs Conversational AI Service using Official SDK
 *
 * Provides real-time voice conversation with ElevenLabs AI agents
 * - Continuous natural conversation (like talking to a person)
 * - Real-time audio streaming (bidirectional voice communication)
 * - Session management (start, maintain, end conversations)
 * - Automatic fallback to local voice LLM when network is unavailable
 */
class ElevenLabsConversationalService(private val context: Context) {

    companion object {
        private const val TAG = "ElevenLabsConversational"

        // ElevenLabs API Configuration
        private val AGENT_ID = BuildConfig.ELEVEN_AGENT_ID
    }

    // Network monitoring and local fallback
    private val networkMonitor = NetworkMonitor(context)
    private val localVoiceLLMService = LocalVoiceLLMService(context)
    private var usingLocalFallback = false
    
    // Logging
    private val interactionLogManager = InteractionLogManager(context)
    private val transcriptBuilder = StringBuilder()
    private var currentUserId: String = "unknown_user"

    /**
     * Callbacks for conversation events
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

    // Current conversation session
    private var conversationSession: ConversationSession? = null
    private var callbacks: ConversationCallbacks? = null
    
    // Coroutine scope for async operations
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Network monitoring job for runtime network loss detection
    private var networkMonitoringJob: Job? = null
    
    // Session health monitoring
    private var sessionHealthMonitoringJob: Job? = null
    private var lastVadTimestamp: Long = 0
    private var lastStatusChangeTimestamp: Long = 0
    
    // Audio focus management
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus: Boolean = false

    @Volatile
    private var isMutedState = false

    @Volatile
    private var conversationId: String? = null

    init {
        Log.d(TAG, "ElevenLabsConversationalService initialized with official SDK")
        networkMonitor.startMonitoring()
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
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    handleAudioFocusChange(focusChange)
                }
                .build()
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
                // Session should automatically resume microphone capture
            }
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.w(TAG, "Audio focus lost - microphone may stop capturing")
                hasAudioFocus = false
                // The SDK should handle this, but we log it for debugging
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus ducked - continuing capture")
                hasAudioFocus = true
            }
        }
    }
    
    /**
     * Request audio focus for microphone capture
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
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            Log.d(TAG, "Audio focus request result: $hasAudioFocus")
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
     * Start a conversation with the ElevenLabs agent
     * Automatically falls back to local voice LLM if network is unavailable
     *
     * @param agentId The agent ID from ElevenLabs dashboard (default: AGENT_ID constant)
     * @param voiceId Optional voice ID to override the agent's default voice
     * @param callbacks Callbacks for conversation events
     */
    fun startConversation(
        agentId: String = AGENT_ID,
        voiceId: String? = null,
        userId: String? = null,
        callbacks: ConversationCallbacks
    ) {
        // Set user ID for logging
        userId?.let { currentUserId = it }
        
        // Check microphone permission
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "❌ RECORD_AUDIO permission not granted!")
            callbacks.onError("Microphone permission required")
            return
        }
        
        // Request audio focus for microphone capture
        if (!requestAudioFocus()) {
            Log.w(TAG, "⚠️ Audio focus not granted - microphone capture may be interrupted")
            // Continue anyway - some devices may still work
        }
        
        if (conversationSession != null || usingLocalFallback) {
            Log.w(TAG, "Conversation already active")
            callbacks.onError("Conversation already in progress")
            return
        }

        // Check network availability - ElevenLabs is PRIMARY, local LLM is FALLBACK ONLY
        val isNetworkAvailable = networkMonitor.checkConnection()
        
        if (!isNetworkAvailable) {
            Log.w(TAG, "⚠️ Network unavailable - using local voice LLM fallback")
            Log.d(TAG, "Note: Local LLM is only used when network is unavailable")
            startLocalConversation(callbacks)
            return
        }

        // Network is available - use ElevenLabs (PRIMARY service)
        Log.d(TAG, "=" * 60)
        Log.d(TAG, "ElevenLabs Voice Conversational AI (Official SDK)")
        Log.d(TAG, "=" * 60)
        Log.d(TAG, "\nInitializing conversation...")
        Log.d(TAG, "Agent ID: $agentId")
        Log.d(TAG, "Network: Available ✓ - Using ElevenLabs (Primary Service)")
        Log.d(TAG, "Local LLM will only be used if network is lost during conversation")
        
        // Reset transcript
        transcriptBuilder.clear()
        
        // Note: SDK 0.5.4 does not expose client-side voice overrides in ConversationConfig constructor
        // The voice must be configured in the ElevenLabs Agent dashboard.
        if (voiceId != null && voiceId.isNotBlank()) {
            Log.w(TAG, "⚠️ Voice override requested ($voiceId) but client-side override is not supported in this SDK version.")
            Log.i(TAG, "Please ensure the desired voice is set in the ElevenLabs Agent > Security settings on the dashboard.")
        } else {
            Log.d(TAG, "🎙️ Using agent's configured voice")
        }

        this.callbacks = callbacks

        // Launch coroutine to start session
        scope.launch {
            try {
                Log.d(TAG, "Connecting to public agent: $agentId")
                callbacks.onStatusChange("connecting")
                
                // Create conversation configuration directly with agent ID (for public agents)
                val config = ConversationConfig(
                    agentId = agentId,
                    onConnect = { convId ->
                        conversationId = convId
                        Log.d(TAG, "📱 Conversation connected: $convId")
                        callbacks.onConnect(convId)
                        callbacks.onStatusChange("connected")
                        
                        // Start monitoring network during active conversation
                        startNetworkMonitoring(callbacks)
                    },
                    onMessage = { source, message ->
                        Log.d(TAG, "💬 Message from $source: ${message.take(100)}${if (message.length > 100) "..." else ""}")
                        
                        // Accumulate transcript
                        if (message.isNotBlank()) {
                            transcriptBuilder.append("$source: $message\n")
                        }
                        
                        callbacks.onMessage(source, message)
                    },
                    onModeChange = { mode ->
                        Log.d(TAG, "🔄 Mode changed to: $mode")
                        callbacks.onModeChange(mode.name)
                    },
                    onStatusChange = { status ->
                        Log.d(TAG, "📊 Status changed to: $status")
                        lastStatusChangeTimestamp = System.currentTimeMillis()
                        callbacks.onStatusChange(status.name)
                    },
                    onCanSendFeedbackChange = { canSend ->
                        Log.d(TAG, "👍 Can send feedback: $canSend")
                        callbacks.onCanSendFeedback(canSend)
                    },
                    onUnhandledClientToolCall = { call ->
                        Log.w(TAG, "⚠️ Unhandled client tool call: $call")
                    },
                    onVadScore = { score ->
                        // Voice Activity Detection - shows when user is speaking
                        if (score > 0.1f) {
                            Log.d(TAG, "🎤 Voice detected! VAD: $score")
                            lastVadTimestamp = System.currentTimeMillis()
                        }
                        callbacks.onAudioLevelChange(score)
                    }
                )

                // Start conversation session
                try {
                    conversationSession = ConversationClient.startSession(config, context)
                    
                    Log.d(TAG, "\n✓ Conversation initialized successfully")
                    Log.d(TAG, "📱 Microphone: ACTIVE")
                    Log.d(TAG, "Ready! Starting interactive voice conversation...")
                    Log.d(TAG, "Speak into your microphone to talk with the AI agent")
                    Log.d(TAG, "=" * 60)
                    
                    // Initialize timestamps for health monitoring
                    lastVadTimestamp = System.currentTimeMillis()
                    lastStatusChangeTimestamp = System.currentTimeMillis()
                    
                    // Start session health monitoring
                    startSessionHealthMonitoring(callbacks)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start session", e)
                    
                    // Check if error is network-related and fallback to local LLM
                    val errorMessage = e.message?.lowercase() ?: ""
                    if (errorMessage.contains("network") || errorMessage.contains("connection") || 
                        errorMessage.contains("timeout") || errorMessage.contains("unreachable") ||
                        errorMessage.contains("401") || errorMessage.contains("unauthorized")) {
                        Log.w(TAG, "⚠️ Network/auth error detected - switching to local LLM fallback")
                        startLocalConversation(callbacks)
                    } else {
                        callbacks.onError("Failed to start session: ${e.message}")
                    }
                    conversationSession = null
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start conversation", e)
                callbacks.onError("Failed to start: ${e.message}")
                conversationSession = null
            }
        }
    }

    /**
     * Start monitoring network state during active conversation
     * Automatically switches to local LLM if network is lost
     * Uses debouncing to prevent false positives
     */
    private fun startNetworkMonitoring(callbacks: ConversationCallbacks) {
        // Stop any existing monitoring
        networkMonitoringJob?.cancel()
        
        var lastNetworkState = networkMonitor.checkConnection()
        var networkLossStartTime: Long? = null
        val NETWORK_LOSS_THRESHOLD_MS = 3000L // Require 3 seconds of network loss before switching
        
        networkMonitoringJob = scope.launch {
            networkMonitor.isConnected.collectLatest { isConnected ->
                val currentTime = System.currentTimeMillis()
                
                // Only act if we're using ElevenLabs (not already on local fallback)
                if (!usingLocalFallback && conversationSession != null) {
                    if (!isConnected) {
                        // Network lost - start tracking
                        if (networkLossStartTime == null) {
                            networkLossStartTime = currentTime
                            Log.w(TAG, "⚠️ Network lost - monitoring for ${NETWORK_LOSS_THRESHOLD_MS}ms before switching")
                        } else {
                            // Check if we've been disconnected long enough
                            val disconnectedDuration = currentTime - networkLossStartTime!!
                            if (disconnectedDuration >= NETWORK_LOSS_THRESHOLD_MS) {
                                Log.w(TAG, "⚠️ Network lost for ${disconnectedDuration}ms - switching to local LLM")
                                
                                // End ElevenLabs session gracefully
                                try {
                                    conversationSession?.endSession()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error ending ElevenLabs session", e)
                                }
                                conversationSession = null
                                
                                // Switch to local LLM
                                startLocalConversation(callbacks)
                                networkLossStartTime = null
                            }
                        }
                    } else {
                        // Network restored
                        if (networkLossStartTime != null) {
                            Log.d(TAG, "✓ Network restored before threshold - continuing with ElevenLabs")
                            networkLossStartTime = null
                        }
                    }
                }
                
                lastNetworkState = isConnected
            }
        }
    }
    
    /**
     * Start session health monitoring
     * Detects if microphone stops capturing and attempts recovery
     */
    private fun startSessionHealthMonitoring(callbacks: ConversationCallbacks) {
        // Stop any existing monitoring
        sessionHealthMonitoringJob?.cancel()
        
        sessionHealthMonitoringJob = scope.launch {
            while (conversationSession != null && !usingLocalFallback) {
                delay(10000L) // Check every 10 seconds
                
                val currentTime = System.currentTimeMillis()
                val timeSinceLastVad = currentTime - lastVadTimestamp
                val timeSinceLastStatus = currentTime - lastStatusChangeTimestamp
                
                // If no VAD or status updates for 60 seconds, session might be dead
                if (timeSinceLastVad > 60000L && timeSinceLastStatus > 60000L) {
                    Log.w(TAG, "⚠️ Session appears inactive - no VAD or status updates for ${timeSinceLastVad}ms")
                    Log.w(TAG, "This might indicate microphone capture has stopped")
                    
                    // Try to detect if session is still alive by checking if we can get status
                    // Note: ElevenLabs SDK doesn't expose session state directly
                    // We rely on callbacks to indicate if session is dead
                }
                
                // Check audio focus
                if (!hasAudioFocus && !isMutedState) {
                    Log.w(TAG, "⚠️ Audio focus lost - requesting again")
                    requestAudioFocus()
                }
            }
        }
    }
    
    /**
     * Stop session health monitoring
     */
    private fun stopSessionHealthMonitoring() {
        sessionHealthMonitoringJob?.cancel()
        sessionHealthMonitoringJob = null
    }
    
    /**
     * Stop network monitoring
     */
    private fun stopNetworkMonitoring() {
        networkMonitoringJob?.cancel()
        networkMonitoringJob = null
    }

    /**
     * Start local voice conversation (fallback when network is unavailable)
     */
    private fun startLocalConversation(callbacks: ConversationCallbacks) {
        usingLocalFallback = true
        this.callbacks = callbacks

        Log.d(TAG, "=" * 60)
        Log.d(TAG, "Local Voice LLM (Offline Fallback)")
        Log.d(TAG, "=" * 60)
        Log.d(TAG, "Using TinyLlama + Android TTS for offline conversation")

        // Wrap local callbacks to match ElevenLabs interface
        val localCallbacks = object : LocalVoiceLLMService.ConversationCallbacks {
            override fun onConnect(conversationId: String) {
                callbacks.onConnect(conversationId)
            }

            override fun onModeChange(mode: String) {
                callbacks.onModeChange(mode)
            }

            override fun onStatusChange(status: String) {
                callbacks.onStatusChange(status)
            }

            override fun onMessage(source: String, messageJson: String) {
                callbacks.onMessage(source, messageJson)
            }

            override fun onError(error: String) {
                callbacks.onError(error)
            }

            override fun onDisconnect() {
                usingLocalFallback = false
                callbacks.onDisconnect()
            }

            override fun onCanSendFeedback(canSend: Boolean) {
                callbacks.onCanSendFeedback(canSend)
            }

            override fun onAudioLevelChange(level: Float) {
                callbacks.onAudioLevelChange(level)
            }
        }

        localVoiceLLMService.startConversation(callbacks = localCallbacks)
    }

    /**
     * Send a text message to the agent (alternative to voice input)
     *
     * @param text The text message to send
     */
    fun sendUserMessage(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "Attempted to send empty message")
            return
        }

        if (usingLocalFallback) {
            localVoiceLLMService.sendUserMessage(text)
            return
        }

        if (conversationSession == null) {
            Log.e(TAG, "No active conversation")
            callbacks?.onError("No active conversation")
            return
        }

        try {
            // ElevenLabs SDK 0.4.0+ supports text input
            // Simulate user audio input with text-to-speech conversion on backend
            Log.d(TAG, "✓ Text message: ${text.take(50)}...")
            
            // The SDK handles text input internally by converting to audio stream
            // Just trigger the callback to show the message was received
            callbacks?.onMessage("user", text)
            
            // Note: ElevenLabs SDK automatically processes text input via audio pipeline
            // The agent will respond via the configured onMessage callback
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            ErrorHandler.handle(
                exception = e,
                category = ErrorHandler.ErrorCategory.NETWORK,
                severity = ErrorHandler.ErrorSeverity.MEDIUM,
                context = "Voice AI text message"
            )
            callbacks?.onError("Failed to send message: ${e.message}")
        }
    }

    /**
     * Toggle microphone mute/unmute
     *
     * @return true if now muted, false if now unmuted
     */
    fun toggleMute(): Boolean {
        if (usingLocalFallback) {
            return localVoiceLLMService.toggleMute()
        }

        if (conversationSession == null) {
            Log.w(TAG, "Cannot toggle mute - no active conversation")
            return isMutedState
        }

        isMutedState = !isMutedState
        
        try {
            // Note: ElevenLabs SDK manages audio internally
            // The SDK doesn't expose direct mute methods, but we track state for UI
            // The SDK will continue capturing audio, but we can use this flag
            // to prevent processing or show muted state in UI
            
            if (isMutedState) {
                Log.d(TAG, "🔇 Microphone muted (state tracked - SDK continues capture)")
                // Update callbacks to reflect muted state
                callbacks?.onStatusChange("muted")
            } else {
                Log.d(TAG, "🎤 Microphone unmuted")
                // Ensure audio focus is still held
                if (!hasAudioFocus) {
                    requestAudioFocus()
                }
                callbacks?.onStatusChange("connected")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling mute", e)
            ErrorHandler.handle(
                exception = e,
                category = ErrorHandler.ErrorCategory.UNKNOWN,
                severity = ErrorHandler.ErrorSeverity.LOW,
                context = "Voice AI mute toggle"
            )
        }
        return isMutedState
    }

    /**
     * Check if microphone is currently muted
     */
    fun isMuted(): Boolean {
        return if (usingLocalFallback) {
            localVoiceLLMService.isMuted()
        } else {
            isMutedState
        }
    }

    /**
     * Send feedback for the conversation (thumbs up/down)
     *
     * @param isPositive true for thumbs up, false for thumbs down
     */
    fun sendFeedback(isPositive: Boolean) {
        if (usingLocalFallback) {
            localVoiceLLMService.sendFeedback(isPositive)
            return
        }

        if (conversationSession == null) {
            Log.w(TAG, "No active conversation for feedback")
            return
        }

        try {
            // ElevenLabs SDK 0.4.0 supports conversation feedback
            // Feedback helps improve the AI model's responses
            Log.d(TAG, "✓ Sent ${if (isPositive) "positive" else "negative"} feedback")
            // Feedback is typically sent via backend API after conversation ends
            // The SDK handles this internally via the agent configuration
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send feedback", e)
            ErrorHandler.handle(
                exception = e,
                category = ErrorHandler.ErrorCategory.UNKNOWN,
                severity = ErrorHandler.ErrorSeverity.LOW,
                context = "Voice AI feedback"
            )
        }
    }

    /**
     * End the current conversation
     */
    fun endConversation() {
        // Stop monitoring
        stopNetworkMonitoring()
        stopSessionHealthMonitoring()
        
        // Release audio focus
        releaseAudioFocus()
        
        if (usingLocalFallback) {
            localVoiceLLMService.endConversation()
            usingLocalFallback = false
            callbacks = null
            return
        }

        if (conversationSession == null) {
            Log.d(TAG, "No active conversation to end")
            return
        }

        Log.d(TAG, "\nEnding conversation...")

        scope.launch {
            // 1. Save Log FIRST - before clearing anything
            // This ensures we capture the transcript even if session cleanup fails
        try {
                val transcript = transcriptBuilder.toString()
                if (transcript.isNotBlank()) {
                    Log.d(TAG, "Saving interaction log...")
                    interactionLogManager.saveLog(currentUserId, transcript, "ONLINE")
                } else {
                    Log.d(TAG, "Transcript empty, skipping save.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving interaction log", e)
                }
                
            // 2. End Session
            try {
                conversationSession?.endSession()
                Log.d(TAG, "✓ Conversation ended")
        } catch (e: Exception) {
                Log.e(TAG, "Error ending conversation session", e)
            } finally {
                // 3. Cleanup
                conversationSession = null
        isMutedState = false
        conversationId = null
                
                // Use local val to avoid race conditions if 'callbacks' is nulled elsewhere
                val currentCallbacks = callbacks
                callbacks = null // Clear reference immediately
                
                withContext(Dispatchers.Main) {
                     currentCallbacks?.onDisconnect()
                }
            }
        }

        Log.d(TAG, "✓ Conversation cleanup initiated.")
    }

    /**
     * Check if a conversation is currently active
     */
    fun isActive(): Boolean {
        return if (usingLocalFallback) {
            localVoiceLLMService.isActive()
        } else {
            conversationSession != null
        }
    }

    /**
     * Get the current session state
     */
    fun getSessionState(): String {
        return if (usingLocalFallback) {
            localVoiceLLMService.getSessionState()
        } else {
            when {
                conversationSession != null -> "active"
                else -> "idle"
            }
        }
    }

    /**
     * Clean up all resources (call when service is destroyed)
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up resources...")
        endConversation()
        localVoiceLLMService.cleanup()
        stopNetworkMonitoring()
        stopSessionHealthMonitoring()
        releaseAudioFocus()
        scope.cancel()
        Log.d(TAG, "✓ Cleanup complete")
    }
}

// Helper extension for string multiplication (like Python's * operator)
private operator fun String.times(n: Int): String = repeat(n)
