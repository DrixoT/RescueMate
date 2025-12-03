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
import android.os.RemoteException

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
    
    // Lock for thread safety of token buffer
    private val bufferLock = Any()

    suspend fun initialize(): Boolean = suspendCancellableCoroutine { continuation ->
        Log.d(TAG, "Starting TTS initialization...")
        
        if (isInitialized) {
            Log.d(TAG, "TTS already initialized, returning true")
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }
        
        try {
            Log.d(TAG, "Creating TextToSpeech instance...")
            tts = TextToSpeech(context) { status ->
                Log.d(TAG, "TTS onInit callback received, status = $status (SUCCESS=${TextToSpeech.SUCCESS}, ERROR=${TextToSpeech.ERROR})")
                
                if (status == TextToSpeech.SUCCESS) {
                    tts?.let {
                        val langResult = it.setLanguage(Locale.US)
                        Log.d(TAG, "TTS setLanguage result: $langResult (LANG_AVAILABLE=${TextToSpeech.LANG_AVAILABLE})")
                        it.setSpeechRate(1.1f)

                        it.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {}
                            override fun onDone(utteranceId: String?) {}
                            override fun onError(utteranceId: String?) {
                                Log.e(TAG, "TTS utterance error: $utteranceId")
                            }
                        })
                    }
                    isInitialized = true
                    Log.d(TAG, "SUCCESS: TTS Initialized")
                    startQueueProcessor()
                    if (continuation.isActive) continuation.resume(true)
                } else {
                    Log.e(TAG, "FAILED: TTS Initialization failed with status $status - ensure device has TTS engine installed")
                    if (continuation.isActive) continuation.resume(false)
                }
            }
        } catch (e: Exception) {
             Log.e(TAG, "FAILED: TTS init crashed: ${e.message}", e)
             if (continuation.isActive) continuation.resume(false)
        }
    }

    private fun startQueueProcessor() {
        scope.launch {
            while (isActive) {
                try {
                    val sentence = ttsQueue.take() // Blocks until item available
                    if (sentence.isNotBlank()) {
                        try {
                            if (isInitialized && tts != null) {
                                tts?.speak(sentence, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
                            } else {
                                Log.w(TAG, "TTS not ready, dropping sentence: $sentence")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error speaking: ${e.message}")
                            if (e is android.os.DeadObjectException || e.message?.contains("DeadObject") == true) {
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
        
        synchronized(bufferLock) {
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
        synchronized(bufferLock) {
            if (tokenBuffer.isNotEmpty()) {
                val text = tokenBuffer.toString().trim()
                if (text.isNotEmpty()) {
                    ttsQueue.offer(text)
                }
                tokenBuffer.clear()
            }
        }
    }
    
    /**
     * Stop speaking immediately (interruption)
     */
    fun stop() {
        ttsQueue.clear()
        synchronized(bufferLock) {
            tokenBuffer.clear()
        }
        try {
            if (isInitialized && tts != null) {
                tts?.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
            // If stop fails, it might be dead
            if (e is android.os.DeadObjectException || e.message?.contains("DeadObject") == true) {
                isInitialized = false
            }
        }
    }
    
    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
    
    fun isSpeaking(): Boolean {
        return try {
            // Add null check and initialization check
            if (!isInitialized || tts == null) {
                return false
            }
            tts?.isSpeaking ?: false
        } catch (e: Exception) {
            // Catch DeadObjectException, RemoteException, and any other IPC errors
            Log.w(TAG, "isSpeaking failed safely: ${e.javaClass.simpleName} - ${e.message}")
            if (e is android.os.DeadObjectException || e is RemoteException || e.message?.contains("DeadObject") == true) {
                 isInitialized = false
                 // Optionally try to re-init or just fail gracefully
            }
            false
        }
    }
}
