package com.rescuemate.services

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * ElevenLabs Voice AI Service for Android
 * Handles text-to-speech conversion and emergency voice calls
 */
class ElevenLabsVoiceService(private val context: Context) {

    companion object {
        private const val API_BASE_URL = "https://api.elevenlabs.io/v1"
        private const val DEFAULT_VOICE_ID = "scOwDtmlUjD3prqpp97I" // Sam

        // Available voices
        val VOICE_SAM = VoiceOption(
            id = "scOwDtmlUjD3prqpp97I",
            name = "Sam",
            description = "Professional and clear voice for emergency guidance",
            gender = "Male"
        )

        val VOICE_PETE = VoiceOption(
            id = "ChO6kqkVouUn0s7HMunx",
            name = "Pete",
            description = "Calm and reassuring voice for crisis situations",
            gender = "Male"
        )

        val AVAILABLE_VOICES = listOf(VOICE_SAM, VOICE_PETE)
    }

    data class VoiceOption(
        val id: String,
        val name: String,
        val description: String,
        val gender: String
    )

    data class VoiceSettings(
        val stability: Double = 0.5,
        val similarityBoost: Double = 0.75,
        val style: Double = 0.0,
        val useSpeakerBoost: Boolean = true
    )

    private val client = OkHttpClient()
    private var mediaPlayer: MediaPlayer? = null
    private var currentVoiceId = DEFAULT_VOICE_ID
    private var apiKey: String = ""

    /**
     * Set the ElevenLabs API key
     * Store securely in production (use Android Keystore)
     */
    fun setApiKey(key: String) {
        apiKey = key
    }

    /**
     * Set the voice to use for text-to-speech
     */
    fun setVoice(voiceId: String) {
        if (AVAILABLE_VOICES.any { it.id == voiceId }) {
            currentVoiceId = voiceId
        }
    }

    /**
     * Convert text to speech using ElevenLabs API
     * Returns the path to the generated audio file
     */
    suspend fun textToSpeech(
        text: String,
        voiceId: String = currentVoiceId,
        settings: VoiceSettings = VoiceSettings(),
        useCache: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check cache if enabled
            if (useCache) {
                val cachedFile = File(context.cacheDir, "voice_preview_$voiceId.mp3")
                if (cachedFile.exists() && cachedFile.length() > 0) {
                    android.util.Log.d("ElevenLabsVoiceService", "✓ Using cached audio for voice: $voiceId")
                    return@withContext Result.success(cachedFile.absolutePath)
                }
            }
            
            if (apiKey.isEmpty()) {
                android.util.Log.e("ElevenLabsVoiceService", " API key not set!")
                return@withContext Result.failure(Exception("API key not set. Please check your .env file and rebuild the app."))
            }
            
            android.util.Log.d("ElevenLabsVoiceService", " Generating speech:")
            android.util.Log.d("ElevenLabsVoiceService", "   Text: ${text.take(50)}...")
            android.util.Log.d("ElevenLabsVoiceService", "   Voice ID: $voiceId")
            android.util.Log.d("ElevenLabsVoiceService", "   API Key set: Yes (${apiKey.take(10)}...)")
            android.util.Log.d("ElevenLabsVoiceService", "   Use cache: $useCache")

            val json = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_multilingual_v2")
                put("voice_settings", JSONObject().apply {
                    put("stability", settings.stability)
                    put("similarity_boost", settings.similarityBoost)
                    put("style", settings.style)
                    put("use_speaker_boost", settings.useSpeakerBoost)
                })
            }

            val requestBody = json.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$API_BASE_URL/text-to-speech/$voiceId")
                .addHeader("Accept", "audio/mpeg")
                .addHeader("xi-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "No error details"
                android.util.Log.e("ElevenLabsVoiceService", " API request failed:")
                android.util.Log.e("ElevenLabsVoiceService", "   Status: ${response.code} - ${response.message}")
                android.util.Log.e("ElevenLabsVoiceService", "   Error body: $errorBody")
                android.util.Log.e("ElevenLabsVoiceService", "   API Key (first 10 chars): ${apiKey.take(10)}...")
                return@withContext Result.failure(
                    Exception("API request failed: ${response.code} - ${response.message}. Details: $errorBody")
                )
            }
            
            android.util.Log.d("ElevenLabsVoiceService", " API request successful, downloading audio...")

            // Save audio to file (use voice-specific name if caching)
            val audioFile = if (useCache) {
                File(context.cacheDir, "voice_preview_$voiceId.mp3")
            } else {
                File(context.cacheDir, "emergency_voice_${System.currentTimeMillis()}.mp3")
            }
            
            response.body?.byteStream()?.use { input ->
                FileOutputStream(audioFile).use { output ->
                    input.copyTo(output)
                }
            }

            android.util.Log.d("ElevenLabsVoiceService", " Audio saved to: ${audioFile.absolutePath}")
            Result.success(audioFile.absolutePath)

        } catch (e: Exception) {
            android.util.Log.e("ElevenLabsVoiceService", " Exception during text-to-speech", e)
            Result.failure(e)
        }
    }

    /**
     * Play generated audio file
     */
    suspend fun playAudio(audioPath: String): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            stopAudio()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)
                prepare()
                setOnCompletionListener {
                    // Reset when audio finishes playing
                    android.util.Log.d("ElevenLabsVoiceService", "Audio playback completed")
                    stopAudio()
                }
                start()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stop currently playing audio
     */
    fun stopAudio() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    /**
     * Generate and play emergency message
     */
    suspend fun generateEmergencyCall(
        userName: String,
        age: Int,
        condition: String,
        location: String,
        medicalInfo: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val emergencyScript = buildEmergencyScript(userName, age, condition, location, medicalInfo)

            val audioResult = textToSpeech(emergencyScript)
            if (audioResult.isFailure) {
                return@withContext Result.failure(audioResult.exceptionOrNull()!!)
            }

            val audioPath = audioResult.getOrNull()!!
            playAudio(audioPath)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Build emergency script for voice AI
     */
    private fun buildEmergencyScript(
        userName: String,
        age: Int,
        condition: String,
        location: String,
        medicalInfo: String?
    ): String {
        return buildString {
            appendLine("Emergency Alert from RescueMate.")
            appendLine()
            appendLine("This is an automated emergency notification for $userName.")
            appendLine()
            appendLine("An emergency has been detected. $userName, a $age-year-old, is experiencing: $condition.")
            appendLine()
            appendLine("Current location: $location")
            appendLine()

            if (!medicalInfo.isNullOrEmpty()) {
                appendLine("Medical information: $medicalInfo")
                appendLine()
            }

            appendLine("Immediate assistance is required.")
            appendLine("This message will be repeated and emergency services have been notified.")
            appendLine()
            appendLine("Please respond if you can hear this message.")
        }
    }

    /**
     * Preview a voice by generating a sample
     */
    suspend fun previewVoice(voiceId: String): Result<Unit> {
        val sampleText = "Hello, I am your RescueMate emergency assistant. " +
                "I will guide you through emergencies with calm, clear instructions."

        val audioResult = textToSpeech(sampleText, voiceId)
        if (audioResult.isFailure) {
            return Result.failure(audioResult.exceptionOrNull()!!)
        }

        return playAudio(audioResult.getOrNull()!!)
    }

    /**
     * Check if audio is currently playing
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopAudio()
        // Delete temporary audio files
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("emergency_voice_")) {
                file.delete()
            }
        }
    }
}

