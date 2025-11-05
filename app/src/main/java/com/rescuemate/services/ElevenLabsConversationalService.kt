package com.rescuemate.services

import android.content.Context
import android.util.Log
import io.elevenlabs.convai.ConversationClient
import io.elevenlabs.convai.ConversationConfig
import io.elevenlabs.convai.ConversationSession
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Service for managing real-time voice conversations with ElevenLabs Conversational AI
 * Uses WebRTC/LiveKit for low-latency voice interaction
 * 
 * Thread-safe implementation with proper state management
 */
class ElevenLabsConversationalService(private val context: Context) {
    
    companion object {
        private const val TAG = "ElevenLabsConversation"
    }
    
    @Volatile
    private var session: ConversationSession? = null
    
    @Volatile
    private var callbacks: ConversationCallbacks? = null
    
    private val isStarting = AtomicBoolean(false)
    
    /**
     * Callbacks for conversation events
     */
    interface ConversationCallbacks {
        fun onConnect(conversationId: String)
        fun onModeChange(mode: String) // "speaking" or "listening"
        fun onStatusChange(status: String) // "connected", "connecting", "disconnected"
        fun onMessage(source: String, messageJson: String)
        fun onError(error: String)
        fun onDisconnect()
        fun onCanSendFeedback(canSend: Boolean)
    }
    
    /**
     * Start a conversation with the ElevenLabs agent
     * 
     * @param agentId The agent ID from ElevenLabs dashboard
     * @param voiceId Optional voice ID to override the agent's default voice
     * @param callbacks Callbacks for conversation events
     */
    fun startConversation(
        agentId: String,
        voiceId: String? = null,
        callbacks: ConversationCallbacks
    ) {
        // Input validation
        if (agentId.isBlank()) {
            Log.e(TAG, "Invalid agent ID provided")
            callbacks.onError("Agent ID cannot be empty")
            return
        }
        
        // Prevent concurrent session starts
        if (!isStarting.compareAndSet(false, true)) {
            Log.w(TAG, "Conversation start already in progress")
            callbacks.onError("Conversation start already in progress")
            return
        }
        
        // Check if session already exists
        if (session != null) {
            isStarting.set(false)
            Log.w(TAG, "Active session already exists")
            callbacks.onError("Active conversation already exists. End current session first.")
            return
        }
        
        this.callbacks = callbacks
        
        try {
            Log.d(TAG, "Starting conversation with agent: ${agentId.take(10)}...")
            if (voiceId != null) {
                Log.d(TAG, "Using voice override: ${voiceId.take(10)}...")
            }
            
            val config = ConversationConfig(
                agentId = agentId,
                userId = "user-${System.currentTimeMillis()}", // Unique user ID
                overrides = voiceId?.let { 
                    mapOf("voice" to mapOf("voice_id" to it))
                } ?: emptyMap(),
                
                // Connection callback
                onConnect = { conversationId ->
                    isStarting.set(false)
                    Log.d(TAG, "✓ Connected: $conversationId")
                    try {
                        callbacks.onConnect(conversationId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onConnect callback", e)
                    }
                },
                
                // Mode change callback (speaking/listening)
                onModeChange = { mode ->
                    Log.d(TAG, "Mode changed: $mode")
                    try {
                        callbacks.onModeChange(mode)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onModeChange callback", e)
                    }
                },
                
                // Status change callback
                onStatusChange = { status ->
                    Log.d(TAG, "Status changed: $status")
                    try {
                        callbacks.onStatusChange(status)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onStatusChange callback", e)
                    }
                },
                
                // Message callback
                onMessage = { source, messageJson ->
                    Log.d(TAG, "Message from $source: ${messageJson.take(100)}...")
                    try {
                        callbacks.onMessage(source, messageJson)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onMessage callback", e)
                    }
                },
                
                // Disconnect callback
                onDisconnect = {
                    Log.d(TAG, "Disconnected")
                    try {
                        callbacks.onDisconnect()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onDisconnect callback", e)
                    }
                },
                
                // Error callback
                onError = { error ->
                    isStarting.set(false)
                    Log.e(TAG, "SDK Error: ${error.message}", error)
                    try {
                        callbacks.onError(error.message ?: "Unknown error occurred")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onError callback", e)
                    }
                },
                
                // Feedback callback
                onCanSendFeedbackChange = { canSend ->
                    Log.d(TAG, "Can send feedback: $canSend")
                    try {
                        callbacks.onCanSendFeedback(canSend)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onCanSendFeedback callback", e)
                    }
                }
            )
            
            // Start the conversation session
            session = ConversationClient.startSession(config, context)
            Log.d(TAG, "✓ Session created successfully")
            
        } catch (e: Exception) {
            isStarting.set(false)
            session = null
            Log.e(TAG, "Failed to start conversation", e)
            val errorMsg = when {
                e.message?.contains("network", ignoreCase = true) == true -> 
                    "Network error. Check internet connection."
                e.message?.contains("permission", ignoreCase = true) == true -> 
                    "Permission denied. Grant microphone access."
                e.message?.contains("agent", ignoreCase = true) == true -> 
                    "Invalid agent configuration. Check agent ID."
                else -> "Failed to start conversation: ${e.message}"
            }
            callbacks.onError(errorMsg)
        }
    }
    
    /**
     * Send a text message to the agent (will trigger a response)
     * 
     * @param text The text message to send
     */
    fun sendUserMessage(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "Attempted to send empty message")
            return
        }
        
        val currentSession = session
        if (currentSession == null) {
            Log.e(TAG, "No active session to send message")
            callbacks?.onError("No active conversation")
            return
        }
        
        try {
            currentSession.sendUserMessage(text)
            Log.d(TAG, "✓ Sent user message: ${text.take(50)}...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            callbacks?.onError("Failed to send message: ${e.message}")
        }
    }
    
    /**
     * Send a contextual update to the agent (won't trigger a response)
     * Used to provide additional context without expecting an immediate reply
     * 
     * @param context The context information to send
     */
    fun sendContextualUpdate(context: String) {
        try {
            session?.sendContextualUpdate(context)
            Log.d(TAG, "Sent contextual update: $context")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send contextual update", e)
            callbacks?.onError("Failed to send context: ${e.message}")
        }
    }
    
    /**
     * Toggle microphone mute/unmute
     * 
     * @return true if now muted, false if now unmuted
     */
    fun toggleMute(): Boolean {
        return try {
            val isMuted = session?.toggleMute() ?: false
            Log.d(TAG, "Microphone ${if (isMuted) "muted" else "unmuted"}")
            isMuted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle mute", e)
            callbacks?.onError("Failed to toggle mute: ${e.message}")
            false
        }
    }
    
    /**
     * Check if microphone is currently muted
     */
    fun isMuted(): Boolean {
        return try {
            session?.isMuted() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check mute status", e)
            false
        }
    }
    
    /**
     * Send feedback for the conversation (thumbs up/down)
     * 
     * @param isPositive true for thumbs up, false for thumbs down
     */
    fun sendFeedback(isPositive: Boolean) {
        try {
            if (isPositive) {
                session?.sendFeedback(io.elevenlabs.convai.FeedbackType.THUMBS_UP)
                Log.d(TAG, "Sent positive feedback")
            } else {
                session?.sendFeedback(io.elevenlabs.convai.FeedbackType.THUMBS_DOWN)
                Log.d(TAG, "Sent negative feedback")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send feedback", e)
            callbacks?.onError("Failed to send feedback: ${e.message}")
        }
    }
    
    /**
     * End the current conversation and clean up resources
     * Thread-safe cleanup with proper state management
     */
    fun endConversation() {
        val currentSession = session
        if (currentSession == null) {
            Log.d(TAG, "No active conversation to end")
            return
        }
        
        try {
            Log.d(TAG, "Ending conversation...")
            currentSession.endSession()
            Log.d(TAG, "✓ Conversation ended successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error ending conversation", e)
        } finally {
            session = null
            callbacks = null
            isStarting.set(false)
        }
    }
    
    /**
     * Clean up resources - call from onDispose
     */
    fun cleanup() {
        Log.d(TAG, "Cleanup called")
        endConversation()
    }
    
    /**
     * Check if conversation is active
     */
    fun isActive(): Boolean {
        return session != null
    }
    
    /**
     * Get current session state for debugging
     */
    fun getSessionState(): String {
        return when {
            isStarting.get() -> "STARTING"
            session != null -> "ACTIVE"
            else -> "IDLE"
        }
    }
}

