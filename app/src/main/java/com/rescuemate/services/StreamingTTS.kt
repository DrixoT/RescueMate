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

/**
 * StreamingTTS
 * Buffers tokens from LLM and speaks them in natural chunks (sentences/phrases).
 * Supports interruption and queue management.
 */
class StreamingTTS(context: Context) {
    
    companion object {
        private const val TAG = "StreamingTTS"
    }

    private var tts: TextToSpeech? = null
    private val tokenBuffer = StringBuilder()
    private var isInitialized = false
    private var onInitListener: ((Boolean) -> Unit)? = null
    
    // Queue for parallel processing
    private val ttsQueue = LinkedBlockingQueue<String>()
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
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
                onInitListener?.invoke(true)
                Log.d(TAG, "TTS Initialized")
                startQueueProcessor()
            } else {
                Log.e(TAG, "TTS Initialization failed")
                onInitListener?.invoke(false)
            }
        }
    }

    private fun startQueueProcessor() {
        scope.launch {
            while (isActive) {
                try {
                    val sentence = ttsQueue.take() // Blocks until item available
                    if (sentence.isNotBlank()) {
                         withContext(Dispatchers.Main) {
                            tts?.speak(sentence, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
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
        // Speak at punctuation marks or if buffer gets too long
        val sentenceEnders = listOf(". ", "! ", "? ", ": ", "\n")
        return sentenceEnders.any { text.endsWith(it) } || text.length > 100
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
        tts?.stop()
    }
    
    fun shutdown() {
        stop()
        tts?.shutdown()
    }
    
    fun isSpeaking(): Boolean {
        return tts?.isSpeaking ?: false
    }
}

