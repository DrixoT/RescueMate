package com.rescuemate.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
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
import kotlinx.coroutines.delay
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
    private var isBound = false
    
    // Audio focus management
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus: Boolean = false
    
    // Queue for parallel processing
    private val ttsQueue = LinkedBlockingQueue<String>()
    private val scope = CoroutineScope(Dispatchers.Default)
    
    // Lock for thread safety of token buffer
    private val bufferLock = Any()
    
    // Track speaking state
    private var isCurrentlySpeaking = false

    suspend fun initialize(): Boolean {
        Log.d(TAG, "Starting TTS initialization...")
        
        if (isInitialized && isBound && tts != null) {
            Log.d(TAG, "TTS already initialized and bound, returning true")
            return true
        }
        
        // Initialize audio manager and audio focus
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        initializeAudioFocus()
        
        // Retry loop for TTS binding
        var attempts = 0
        val maxAttempts = 3
        
        while (attempts < maxAttempts) {
            val success = suspendCancellableCoroutine<Boolean> { continuation ->
                try {
                    Log.d(TAG, "Creating TextToSpeech instance (Attempt ${attempts + 1})...")
                    tts = TextToSpeech(context) { status ->
                        Log.d(TAG, "TTS onInit callback received, status = $status (SUCCESS=${TextToSpeech.SUCCESS}, ERROR=${TextToSpeech.ERROR})")
                        
                        if (status == TextToSpeech.SUCCESS) {
                            tts?.let {
                                val langResult = it.setLanguage(Locale.US)
                                Log.d(TAG, "TTS setLanguage result: $langResult (LANG_AVAILABLE=${TextToSpeech.LANG_AVAILABLE})")
                                it.setSpeechRate(0.85f)

                                it.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                    override fun onStart(utteranceId: String?) {
                                        isCurrentlySpeaking = true
                                        Log.d(TAG, "TTS started speaking: $utteranceId")
                                    }
                                    
                                    override fun onDone(utteranceId: String?) {
                                        isCurrentlySpeaking = false
                                        Log.d(TAG, "TTS finished speaking: $utteranceId")
                                    }
                                    
                                    override fun onError(utteranceId: String?) {
                                        isCurrentlySpeaking = false
                                        Log.e(TAG, "TTS utterance error: $utteranceId")
                                    }
                                })
                            }
                            isInitialized = true
                            isBound = true
                            Log.d(TAG, "SUCCESS: TTS Initialized and Bound")
                            startQueueProcessor()
                            if (continuation.isActive) continuation.resume(true)
                        } else {
                            Log.e(TAG, "FAILED: TTS Initialization failed with status $status")
                            isBound = false
                            if (continuation.isActive) continuation.resume(false)
                        }
                    }
                } catch (e: Exception) {
                     Log.e(TAG, "FAILED: TTS init crashed: ${e.message}", e)
                     if (continuation.isActive) continuation.resume(false)
                }
            }
            
            if (success) return true
            
            attempts++
            Log.w(TAG, "TTS initialization failed, retrying in 500ms...")
            delay(500)
        }
        
        Log.e(TAG, "TTS initialization failed after $maxAttempts attempts")
        return false
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
                Log.d(TAG, "Audio focus gained - resuming TTS")
                hasAudioFocus = true
                // TTS should automatically resume
            }
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.w(TAG, "Audio focus lost - pausing TTS")
                hasAudioFocus = false
                // Stop current speech
                try {
                    if (isInitialized && isBound && tts != null) {
                        tts?.stop()
                        isCurrentlySpeaking = false
                    }
                } catch (e: Exception) {
                    val errorMsg = e.message?.lowercase() ?: ""
                    val isNotBoundError = errorMsg.contains("not bound") || 
                                         errorMsg.contains("not bound to tts engine") ||
                                         e is android.os.DeadObjectException || 
                                         e is RemoteException
                    if (isNotBoundError) {
                        Log.w(TAG, "TTS not bound during audio focus loss - updating state")
                        isBound = false
                        isInitialized = false
                    } else {
                        Log.e(TAG, "Error stopping TTS on focus loss", e)
                    }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus ducked - continuing TTS at lower volume")
                hasAudioFocus = true
                // TTS continues but at lower volume
            }
        }
    }
    
    /**
     * Request audio focus for TTS playback
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

    private fun startQueueProcessor() {
        scope.launch {
            while (isActive) {
                try {
                    val sentence = ttsQueue.take() // Blocks until item available
                    if (sentence.isNotBlank()) {
                        try {
                            if (isInitialized && isBound && tts != null) {
                                // Request audio focus before speaking
                                if (requestAudioFocus()) {
                                    try {
                                        val utteranceId = UUID.randomUUID().toString()
                                        val speakResult = tts?.speak(sentence, TextToSpeech.QUEUE_ADD, null, utteranceId)
                                        Log.d(TAG, "TTS speak() called for: '${sentence.take(50)}...' (result=$speakResult, utteranceId=$utteranceId)")
                                        
                                        if (speakResult == TextToSpeech.ERROR) {
                                            Log.e(TAG, "TTS speak() returned ERROR for: $sentence")
                                            // Try to recover by requesting focus again
                                            delay(100)
                                            if (requestAudioFocus()) {
                                                try {
                                                    tts?.speak(sentence, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
                                                } catch (retryE: Exception) {
                                                    val retryErrorMsg = retryE.message?.lowercase() ?: ""
                                                    val isRetryNotBound = retryErrorMsg.contains("not bound") || 
                                                                         retryErrorMsg.contains("not bound to tts engine") ||
                                                                         retryE is android.os.DeadObjectException || 
                                                                         retryE is RemoteException
                                                    if (isRetryNotBound) {
                                                        Log.w(TAG, "TTS not bound during retry speak()")
                                                        isBound = false
                                                        isInitialized = false
                                                    } else {
                                                        Log.e(TAG, "Error during retry speak()", retryE)
                                                    }
                                                }
                                            }
                                        }
                                    } catch (speakE: Exception) {
                                        val speakErrorMsg = speakE.message?.lowercase() ?: ""
                                        val isSpeakNotBound = speakErrorMsg.contains("not bound") || 
                                                             speakErrorMsg.contains("not bound to tts engine") ||
                                                             speakE is android.os.DeadObjectException || 
                                                             speakE is RemoteException
                                        if (isSpeakNotBound) {
                                            Log.w(TAG, "TTS not bound during speak() - updating state")
                                            isBound = false
                                            isInitialized = false
                                        } else {
                                            Log.e(TAG, "Error during speak()", speakE)
                                        }
                                        // Exception handled, continue to outer catch for logging
                                    }
                                } else {
                                    Log.w(TAG, "Audio focus not granted, retrying in 200ms...")
                                    delay(200)
                                    // Retry once
                                    if (requestAudioFocus()) {
                                        try {
                                            val utteranceId = UUID.randomUUID().toString()
                                            tts?.speak(sentence, TextToSpeech.QUEUE_ADD, null, utteranceId)
                                            Log.d(TAG, "TTS speak() retry successful: $utteranceId")
                                        } catch (retryE: Exception) {
                                            val retryErrorMsg = retryE.message?.lowercase() ?: ""
                                            val isRetryNotBound = retryErrorMsg.contains("not bound") || 
                                                                 retryErrorMsg.contains("not bound to tts engine") ||
                                                                 retryE is android.os.DeadObjectException || 
                                                                 retryE is RemoteException
                                            if (isRetryNotBound) {
                                                Log.w(TAG, "TTS not bound during retry speak() - updating state")
                                                isBound = false
                                                isInitialized = false
                                            } else {
                                                Log.e(TAG, "Error during retry speak()", retryE)
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "Failed to get audio focus after retry, dropping: $sentence")
                                    }
                                }
                            } else {
                                Log.w(TAG, "TTS not ready or not bound, dropping sentence: $sentence")
                            }
                        } catch (e: Exception) {
                            val errorMsg = e.message?.lowercase() ?: ""
                            val isNotBoundError = errorMsg.contains("not bound") || 
                                                 errorMsg.contains("not bound to tts engine") ||
                                                 e is android.os.DeadObjectException || 
                                                 e.message?.contains("DeadObject") == true
                            
                            if (isNotBoundError) {
                                Log.w(TAG, "TTS Service not bound during speak() - updating state")
                                isBound = false
                                isInitialized = false
                                // Optionally: could trigger re-initialization here if queue has items
                            } else {
                                Log.e(TAG, "Error speaking: ${e.message}", e)
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
        
        // Always reset speaking state
        isCurrentlySpeaking = false
        
        try {
            if (isInitialized && isBound && tts != null) {
                tts?.stop()
                Log.d(TAG, "TTS stopped")
            }
        } catch (e: Exception) {
            val errorMsg = e.message?.lowercase() ?: ""
            val isNotBoundError = errorMsg.contains("not bound") || 
                                 errorMsg.contains("not bound to tts engine") ||
                                 e is android.os.DeadObjectException || 
                                 e is RemoteException || 
                                 e.message?.contains("DeadObject") == true
            
            if (isNotBoundError) {
                Log.w(TAG, "TTS Service not bound during stop() - updating state")
                isBound = false
                isInitialized = false
            } else {
                Log.e(TAG, "Error stopping TTS", e)
            }
        } finally {
            // Release audio focus when stopping
            releaseAudioFocus()
        }
    }
    
    fun shutdown() {
        stop()
        try {
            if (isInitialized && isBound && tts != null) {
                tts?.shutdown()
            }
        } catch (e: Exception) {
            val errorMsg = e.message?.lowercase() ?: ""
            val isNotBoundError = errorMsg.contains("not bound") || 
                                 errorMsg.contains("not bound to tts engine") ||
                                 e is android.os.DeadObjectException || 
                                 e is RemoteException ||
                                 e.message?.contains("DeadObject") == true
            
            if (isNotBoundError) {
                Log.w(TAG, "TTS Service not bound during shutdown() - already unbound")
            } else {
                Log.e(TAG, "Error shutting down TTS", e)
            }
        } finally {
            releaseAudioFocus()
            tts = null
            isBound = false
            isInitialized = false
            audioManager = null
            audioFocusRequest = null
        }
    }
    
    fun isSpeaking(): Boolean {
        // Early return if not initialized - use tracked state as fallback
        if (!isInitialized || !isBound || tts == null) {
            return isCurrentlySpeaking
        }
        
        return try {
            // Try to get actual TTS state
            val ttsSpeaking = tts?.isSpeaking ?: false
            ttsSpeaking || isCurrentlySpeaking
        } catch (e: Exception) {
            // Check for "not bound" errors specifically
            val errorMsg = e.message?.lowercase() ?: ""
            val isNotBoundError = errorMsg.contains("not bound") || 
                                 errorMsg.contains("not bound to tts engine") ||
                                 e is android.os.DeadObjectException || 
                                 e is RemoteException ||
                                 e.message?.contains("DeadObject") == true
            
            if (isNotBoundError) {
                Log.w(TAG, "TTS service not bound - updating state (error: ${e.message})")
                isBound = false
                isInitialized = false
            } else {
                Log.w(TAG, "isSpeaking failed safely: ${e.javaClass.simpleName} - ${e.message}")
            }
            // Always return tracked state as fallback
            isCurrentlySpeaking
        }
    }
    
    fun isReady(): Boolean {
        return isInitialized && isBound && tts != null
    }
}
