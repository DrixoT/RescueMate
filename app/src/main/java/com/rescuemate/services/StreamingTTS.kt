package com.rescuemate.services

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * StreamingTTS
 * Buffers tokens from LLM and speaks them in natural chunks (sentences/phrases).
 * Supports interruption and queue management.
 * Optimized for incremental streaming with low latency.
 */
class StreamingTTS(private val context: Context) {
    
    companion object {
        private const val TAG = "StreamingTTS"
    }

    private var tts: TextToSpeech? = null
    private val tokenBuffer = StringBuilder()
    private var isInitialized = false
    
    // Queue for parallel processing
    private val ttsQueue = LinkedBlockingQueue<String>()
    private val scope = CoroutineScope(Dispatchers.Default)

    suspend fun initialize(): Boolean = suspendCancellableCoroutine { continuation ->
        if (isInitialized) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }
        
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.let {
                        it.language = Locale.US
                        it.setSpeechRate(1.1f) // Slightly faster for conversational feel

                        it.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {}
                            override fun onDone(utteranceId: String?) {}
                            override fun onError(utteranceId: String?) {}
                        })
                    }
                    isInitialized = true
                    Log.d(TAG, "TTS Initialized")
                    startQueueProcessor()
                    if (continuation.isActive) continuation.resume(true)
                } else {
                    Log.e(TAG, "TTS Initialization failed")
                    if (continuation.isActive) continuation.resume(false)
                }
            }
        } catch (e: Exception) {
             Log.e(TAG, "TTS init crashed", e)
             if (continuation.isActive) continuation.resume(false)
        }
    }

    private fun startQueueProcessor() {
        scope.launch {
            while (isActive) {
                try {
                    val sentence = ttsQueue.take() // Blocks until item available
                    if (sentence.isNotBlank()) {
                         withContext(Dispatchers.Main) {
                            try {
                                tts?.speak(sentence, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
                            } catch (e: Exception) {
                                Log.e(TAG, "Error speaking: ${e.message}")
                                isInitialized = false
                            }
                        }
                    }
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }

    /**
     * Called for each token from LLM
     */
    fun speakToken(token: String) {
        if (!isInitialized) return
        
        tokenBuffer.append(token)
        
        // Speak when we have a complete sentence or meaningful phrase
        if (shouldSpeak(tokenBuffer.toString())) {
            val textToSpeak = tokenBuffer.toString().trim()
            
            if (textToSpeak.isNotEmpty()) {
                ttsQueue.offer(textToSpeak)
            }
            
            tokenBuffer.clear()
        }
    }
    
    private fun shouldSpeak(text: String): Boolean {
        // Speak at punctuation marks, commas (for pausing), or if buffer gets too long
        // Tuning for faster incremental response: Split on commas and semicolons too
        val sentenceEnders = listOf(". ", "! ", "? ", ": ", "; ", ", ", "\n")
        
        // Also check for end of string punctuation if it looks like a complete thought
        val endsWithPunctuation = text.trim().let { t -> 
            t.endsWith(".") || t.endsWith("!") || t.endsWith("?") || t.endsWith(",") 
        }

        // Limit buffer length to ensure we don't wait too long for a pause
        val bufferLimit = 50 

        return (sentenceEnders.any { text.contains(it) } && endsWithPunctuation) || 
               text.length > bufferLimit
    }
    
    /**
     * Flush remaining buffer (called at end of stream)
     */
    fun speakFinal() {
        if (tokenBuffer.isNotEmpty()) {
            val text = tokenBuffer.toString().trim()
            if (text.isNotEmpty()) {
                ttsQueue.offer(text)
            }
            tokenBuffer.clear()
        }
    }
    
    /**
     * Stop speaking immediately (interruption)
     */
    fun stop() {
        ttsQueue.clear()
        tokenBuffer.clear()
        try {
            if (isInitialized && tts != null) {
                tts?.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
            // If stop fails, it might be dead
            if (e.message?.contains("DeadObject") == true) {
                isInitialized = false
            }
        }
    }
    
    fun shutdown() {
        stop()
        tts?.shutdown()
    }
    
    fun isSpeaking(): Boolean {
        return try {
            if (!isInitialized || tts == null) return false
            tts?.isSpeaking ?: false
        } catch (e: Exception) {
            Log.w(TAG, "isSpeaking failed: ${e.message}") // Changed to warning
            if (e.message?.contains("DeadObject") == true) {
                 isInitialized = false
            }
            false
        }
    }
}
