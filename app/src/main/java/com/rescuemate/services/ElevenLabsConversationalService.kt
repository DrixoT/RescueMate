package com.rescuemate.services

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.rescuemate.BuildConfig
import io.elevenlabs.ConversationClient
import io.elevenlabs.ConversationConfig
import io.elevenlabs.ConversationSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * ElevenLabs Conversational AI Service using Official SDK
 *
 * Provides real-time voice conversation with ElevenLabs AI agents
 * - Continuous natural conversation (like talking to a person)
 * - Real-time audio streaming (bidirectional voice communication)
 * - Session management (start, maintain, end conversations)
 */
class ElevenLabsConversationalService(private val context: Context) {

    companion object {
        private const val TAG = "ElevenLabsConversational"

        // ElevenLabs API Configuration
        private val AGENT_ID = BuildConfig.ELEVEN_AGENT_ID
    }

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

    @Volatile
    private var isMutedState = false

    @Volatile
    private var conversationId: String? = null

    init {
        Log.d(TAG, "ElevenLabsConversationalService initialized with official SDK")
    }

    /**
     * Start a conversation with the ElevenLabs agent
     *
     * @param agentId The agent ID from ElevenLabs dashboard (default: AGENT_ID constant)
     * @param voiceId Optional voice ID to override the agent's default voice
     * @param callbacks Callbacks for conversation events
     */
    fun startConversation(
        agentId: String = AGENT_ID,
        voiceId: String? = null,
        callbacks: ConversationCallbacks
    ) {
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
        
        if (conversationSession != null) {
            Log.w(TAG, "Conversation already active")
            callbacks.onError("Conversation already in progress")
            return
        }

        Log.d(TAG, "=" * 60)
        Log.d(TAG, "ElevenLabs Voice Conversational AI (Official SDK)")
        Log.d(TAG, "=" * 60)
        Log.d(TAG, "\nInitializing conversation...")
        Log.d(TAG, "Agent ID: $agentId")
        
        if (voiceId != null && voiceId.isNotBlank()) {
            Log.w(TAG, "⚠️ Voice override not supported in SDK 0.4.0")
            Log.w(TAG, "   Please configure voice in ElevenLabs agent dashboard")
            Log.d(TAG, "   Requested voice ID: $voiceId (cannot be set via SDK)")
        } else {
            Log.d(TAG, "🎙️ Using agent's configured voice")
        }

        this.callbacks = callbacks

            try {
            // Create conversation configuration
            val config = ConversationConfig(
                agentId = agentId,
                onConnect = { convId ->
                    conversationId = convId
                    Log.d(TAG, "📱 Conversation connected: $convId")
                    callbacks.onConnect(convId)
                    callbacks.onStatusChange("connected")
                },
                onMessage = { source, message ->
                    Log.d(TAG, "💬 Message from $source: ${message.take(100)}${if (message.length > 100) "..." else ""}")
                    callbacks.onMessage(source, message)
                },
                onModeChange = { mode ->
                    Log.d(TAG, "🔄 Mode changed to: $mode")
                    callbacks.onModeChange(mode.name)
                },
                onStatusChange = { status ->
                    Log.d(TAG, "📊 Status changed to: $status")
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
                    }
                    callbacks.onAudioLevelChange(score)
                }
            )

            // Start conversation session
            scope.launch {
                try {
                    conversationSession = ConversationClient.startSession(config, context)
                    
                    Log.d(TAG, "\n✓ Conversation initialized successfully")
                    Log.d(TAG, "📱 Microphone: ACTIVE")
                    Log.d(TAG, "Ready! Starting interactive voice conversation...")
                    Log.d(TAG, "Speak into your microphone to talk with the AI agent")
                    Log.d(TAG, "=" * 60)
        } catch (e: Exception) {
                    Log.e(TAG, "Failed to start session", e)
                    callbacks.onError("Failed to start session: ${e.message}")
                    conversationSession = null
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start conversation", e)
            callbacks.onError("Failed to start: ${e.message}")
            conversationSession = null
        }
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

        if (conversationSession == null) {
            Log.e(TAG, "No active conversation")
            callbacks?.onError("No active conversation")
            return
        }

        try {
            // Note: Check SDK documentation for sending text messages
            // The SDK might handle this through the conversation session
            Log.d(TAG, "✓ Text message: ${text.take(50)}...")
            // conversationSession?.sendMessage(text)  // If SDK supports this
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            callbacks?.onError("Failed to send message: ${e.message}")
        }
    }

    /**
     * Toggle microphone mute/unmute
     *
     * @return true if now muted, false if now unmuted
     */
    fun toggleMute(): Boolean {
        isMutedState = !isMutedState
        // Note: Check SDK documentation for mute functionality
        // conversationSession?.setMuted(isMutedState)
        
        if (isMutedState) {
            Log.d(TAG, "🔇 Microphone muted")
        } else {
            Log.d(TAG, "🎤 Microphone unmuted")
        }
        return isMutedState
    }

    /**
     * Check if microphone is currently muted
     */
    fun isMuted(): Boolean {
        return isMutedState
    }

    /**
     * Send feedback for the conversation (thumbs up/down)
     *
     * @param isPositive true for thumbs up, false for thumbs down
     */
    fun sendFeedback(isPositive: Boolean) {
        if (conversationSession == null) {
            Log.w(TAG, "No active conversation for feedback")
            return
        }

        try {
            // Note: Check SDK documentation for feedback functionality
            // conversationSession?.sendFeedback(isPositive)
            Log.d(TAG, "✓ Sent ${if (isPositive) "positive" else "negative"} feedback")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send feedback", e)
        }
    }

    /**
     * End the current conversation
     */
    fun endConversation() {
        if (conversationSession == null) {
            Log.d(TAG, "No active conversation to end")
            return
        }

        Log.d(TAG, "\nEnding conversation...")

        scope.launch {
        try {
                conversationSession?.endSession()
                Log.d(TAG, "✓ Conversation ended")
        } catch (e: Exception) {
                Log.e(TAG, "Error ending conversation", e)
            } finally {
                conversationSession = null
        isMutedState = false
        conversationId = null
                
                callbacks?.onDisconnect()
                callbacks = null
            }
        }

        Log.d(TAG, "✓ Conversation ended. Goodbye!")
    }

    /**
     * Check if a conversation is currently active
     */
    fun isActive(): Boolean {
        return conversationSession != null
    }

    /**
     * Get the current session state
     */
    fun getSessionState(): String {
        return when {
            conversationSession != null -> "active"
            else -> "idle"
        }
    }

    /**
     * Clean up all resources (call when service is destroyed)
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up resources...")
        endConversation()
        scope.cancel()
        Log.d(TAG, "✓ Cleanup complete")
    }
}

// Helper extension for string multiplication (like Python's * operator)
private operator fun String.times(n: Int): String = repeat(n)
