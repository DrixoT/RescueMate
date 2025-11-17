package com.rescuemate.services

import android.content.Context
import android.content.SharedPreferences
import android.speech.tts.TextToSpeech
import android.util.Log
import com.rescuemate.emergency.EmergencyConstants
import java.util.Locale

/**
 * VoiceMatcher
 * Maps ElevenLabs voice IDs to Android TTS voice settings
 * Attempts to match voice characteristics (pitch, speed, tone) as closely as possible
 */
class VoiceMatcher(private val context: Context) {

    companion object {
        private const val TAG = "VoiceMatcher"
        
        // ElevenLabs Voice IDs (from ElevenLabsVoiceService)
        const val VOICE_SAM_ID = "scOwDtmlUjD3prqpp97I" // Sam - Professional and clear
        const val VOICE_PETE_ID = "ChO6kqkVouUn0s7HMunx" // Pete - Calm and reassuring
        
        // Default TTS settings
        private const val DEFAULT_PITCH = 1.0f
        private const val DEFAULT_SPEECH_RATE = 1.0f
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        EmergencyConstants.PREF_NAME_EMERGENCY,
        Context.MODE_PRIVATE
    )

    /**
     * Voice configuration for matching ElevenLabs voices
     */
    data class VoiceConfig(
        val pitch: Float,           // 0.0 - 2.0 (lower = deeper, higher = higher)
        val speechRate: Float,      // 0.0 - 2.0 (slower - faster)
        val language: Locale,       // Language locale
        val voiceName: String? = null // Preferred TTS voice name (if available)
    )

    /**
     * Get voice configuration for a given ElevenLabs voice ID
     */
    fun getVoiceConfig(elevenLabsVoiceId: String): VoiceConfig {
        return when (elevenLabsVoiceId) {
            VOICE_SAM_ID -> VoiceConfig(
                pitch = 1.0f,              // Neutral male voice
                speechRate = 1.1f,         // Slightly faster (professional, clear)
                language = Locale.US,      // US English
                voiceName = "en-US"        // Prefer US English voice
            )
            VOICE_PETE_ID -> VoiceConfig(
                pitch = 0.95f,             // Slightly deeper (calm, reassuring)
                speechRate = 0.95f,        // Slightly slower (calm tone)
                language = Locale.US,      // US English
                voiceName = "en-US"        // Prefer US English voice
            )
            else -> {
                // Default configuration for unknown voices
                Log.w(TAG, "Unknown voice ID: $elevenLabsVoiceId, using default config")
                VoiceConfig(
                    pitch = DEFAULT_PITCH,
                    speechRate = DEFAULT_SPEECH_RATE,
                    language = Locale.getDefault()
                )
            }
        }
    }

    /**
     * Apply voice configuration to TextToSpeech instance
     */
    fun applyVoiceConfig(tts: TextToSpeech, voiceId: String) {
        val config = getVoiceConfig(voiceId)
        
        try {
            // Set pitch
            val pitchResult = tts.setPitch(config.pitch)
            if (pitchResult != TextToSpeech.SUCCESS) {
                Log.w(TAG, "Failed to set pitch: $pitchResult")
            } else {
                Log.d(TAG, "Set pitch to: ${config.pitch}")
            }

            // Set speech rate
            val rateResult = tts.setSpeechRate(config.speechRate)
            if (rateResult != TextToSpeech.SUCCESS) {
                Log.w(TAG, "Failed to set speech rate: $rateResult")
            } else {
                Log.d(TAG, "Set speech rate to: ${config.speechRate}")
            }

            // Set language
            val langResult = tts.setLanguage(config.language)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || 
                langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Language not supported: ${config.language}, using default")
                tts.setLanguage(Locale.getDefault())
            } else {
                Log.d(TAG, "Set language to: ${config.language}")
            }

            // Try to set specific voice if available
            if (config.voiceName != null) {
                try {
                    val voices = tts.voices
                    val preferredVoice = voices?.find { voice ->
                        voice.locale.language == config.language.language &&
                        voice.locale.country == config.language.country
                    }
                    if (preferredVoice != null) {
                        val voiceResult = tts.setVoice(preferredVoice)
                        if (voiceResult == TextToSpeech.SUCCESS) {
                            Log.d(TAG, "Set voice to: ${preferredVoice.name}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set specific voice", e)
                }
            }

            Log.d(TAG, "Voice configuration applied for voice ID: $voiceId")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying voice configuration", e)
        }
    }

    /**
     * Get the stored ElevenLabs voice ID from preferences
     */
    fun getStoredVoiceId(): String {
        return prefs.getString("selected_voice_id", VOICE_SAM_ID) ?: VOICE_SAM_ID
    }

    /**
     * Get voice configuration for the stored voice ID
     */
    fun getStoredVoiceConfig(): VoiceConfig {
        val voiceId = getStoredVoiceId()
        return getVoiceConfig(voiceId)
    }

    /**
     * Get voice name for display
     */
    fun getVoiceName(voiceId: String): String {
        return when (voiceId) {
            VOICE_SAM_ID -> "Sam"
            VOICE_PETE_ID -> "Pete"
            else -> "Default"
        }
    }
}

