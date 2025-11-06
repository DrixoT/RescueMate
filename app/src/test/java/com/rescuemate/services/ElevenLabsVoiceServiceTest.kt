package com.rescuemate.services

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for ElevenLabsVoiceService
 */
@RunWith(MockitoJUnitRunner::class)
class ElevenLabsVoiceServiceTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var service: ElevenLabsVoiceService

    @Before
    fun setup() {
        service = ElevenLabsVoiceService(mockContext)
    }

    @Test
    fun testServiceInitialization() {
        assertNotNull(service)
    }

    @Test
    fun testAvailableVoices() {
        val voices = ElevenLabsVoiceService.AVAILABLE_VOICES
        assertTrue(voices.isNotEmpty())
        assertEquals(2, voices.size)
    }

    @Test
    fun testVoiceSamAndPeteExist() {
        val sam = ElevenLabsVoiceService.VOICE_SAM
        val pete = ElevenLabsVoiceService.VOICE_PETE
        
        assertNotNull(sam)
        assertNotNull(pete)
        assertEquals("Sam", sam.name)
        assertEquals("Pete", pete.name)
    }

    @Test
    fun testSetApiKey() {
        service.setApiKey("test_api_key_123")
        // Should not crash
    }

    @Test
    fun testSetVoice() {
        service.setVoice(ElevenLabsVoiceService.VOICE_SAM.id)
        service.setVoice(ElevenLabsVoiceService.VOICE_PETE.id)
        // Should not crash
    }

    @Test
    fun testSetInvalidVoice() {
        service.setVoice("invalid_voice_id")
        // Should handle gracefully
    }

    @Test
    fun testIsPlayingInitiallyFalse() {
        assertFalse(service.isPlaying())
    }

    @Test
    fun testStopAudioWhenNotPlaying() {
        service.stopAudio()
        // Should not crash
    }

    @Test
    fun testCleanup() {
        service.cleanup()
        // Should not crash
    }

    @Test
    fun testMultipleCleanups() {
        service.cleanup()
        service.cleanup()
        service.cleanup()
        // Should handle multiple cleanups
    }

    @Test
    fun testTextToSpeechWithEmptyApiKey() = runBlocking {
        val result = service.textToSpeech("Test text")
        assertTrue(result.isFailure)
        assertEquals("API key not set. Please check your .env file and rebuild the app.", 
            result.exceptionOrNull()?.message)
    }

    @Test
    fun testVoiceSettings() {
        val settings = ElevenLabsVoiceService.VoiceSettings(
            stability = 0.5,
            similarityBoost = 0.75,
            style = 0.0,
            useSpeakerBoost = true
        )
        assertEquals(0.5, settings.stability)
        assertEquals(0.75, settings.similarityBoost)
    }
}

