        val result = textToSpeech("Sample text", voiceId)
        result.getOrNull() ?: ""
    }
}
```

### 2. Preload Emergency Script:
```kotlin
// Generate and cache emergency audio on app start
fun preloadEmergencyAudio(userProfile: UserProfile) {
    scope.launch {
        val script = buildEmergencyScript(userProfile)
        textToSpeech(script) // Cached for instant use
    }
}
```

### 3. Cleanup Old Files:
```kotlin
// Automatically delete audio files older than 1 hour
fun cleanupOldAudioFiles() {
    val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
    context.cacheDir.listFiles()?.forEach { file ->
        if (file.name.startsWith("emergency_voice_") && file.lastModified() < oneHourAgo) {
            file.delete()
        }
    }
}
```

---

## 🔄 Future Enhancements

### Planned Features:
1. **Multi-language Support:** Spanish, French, Mandarin
2. **Custom Voice Training:** User-specific voice models
3. **Real-time Conversation:** Two-way AI dialogue
4. **Emotion Detection:** Adjust voice tone based on situation
5. **Voice Biometrics:** Verify user identity via voice

### Integration Ideas:
- **Smartwatch Voice Commands:** "Hey RescueMate, I need help"
- **Background Listening:** Always-on emergency detection
- **Voice Status Updates:** Real-time situation reporting
- **Family Notifications:** Voice messages to emergency contacts

---

## 📚 Documentation References

- **ElevenLabs API Docs:** https://docs.elevenlabs.io/
- **OkHttp Documentation:** https://square.github.io/okhttp/
- **Android MediaPlayer:** https://developer.android.com/reference/android/media/MediaPlayer

---

## ✅ Implementation Checklist

- [x] Create ElevenLabsVoiceService.kt
- [x] Add OkHttp dependency
- [x] Update VoiceAISetupScreen with 2 voices
- [x] Integrate voice preview functionality
- [x] Add emergency call generation
- [x] Implement audio playback
- [x] Add cleanup mechanisms
- [x] Update VoiceAIService.ts
- [x] Document API usage
- [x] Create troubleshooting guide

---

## 🎉 Summary

Your RescueMate application now features:
- ✅ **2 Professional ElevenLabs Voices** (Sam & Pete)
- ✅ **Real-time Voice Preview**
- ✅ **Emergency Call Generation**
- ✅ **Secure API Integration**
- ✅ **Production-ready Implementation**

**The voice AI system is fully functional and ready for use!** 🎙️

---

**Integrated By:** GitHub Copilot  
**Date:** November 4, 2025  
**Status:** ✅ PRODUCTION READY
# 🎙️ ElevenLabs Voice AI Integration - Complete Guide

**Date:** November 4, 2025  
**Status:** ✅ FULLY INTEGRATED

---

## 📋 Overview

RescueMate now uses **ElevenLabs AI** for high-quality text-to-speech in emergency situations. The integration includes:
- ✅ 2 Professional voices (Sam & Pete)
- ✅ Voice preview functionality
- ✅ Emergency call generation
- ✅ Real-time audio playback
- ✅ Secure API key management

---

## 🎤 Available Voices

### Voice 1: Sam
- **ID:** `scOwDtmlUjD3prqpp97I`
- **Gender:** Male
- **Accent:** American
- **Description:** Professional and clear voice for emergency guidance
- **Use Case:** Default voice, clear instructions during emergencies

### Voice 2: Pete
- **ID:** `ChO6kqkVouUn0s7HMunx`
- **Gender:** Male
- **Accent:** American
- **Description:** Calm and reassuring voice for crisis situations
- **Use Case:** Soothing voice for high-stress situations

---

## 🏗️ Architecture

### Android Service: `ElevenLabsVoiceService.kt`

**Location:** `app/src/main/java/com/rescuemate/services/`

**Key Features:**
- Text-to-speech conversion
- Audio file management
- MediaPlayer integration
- Voice preview system
- Emergency script generation

### Web Service: `VoiceAIService.ts`

**Location:** `src/services/`

**Key Features:**
- Backend proxy for API calls
- Secure key management
- Location formatting
- Emergency call orchestration

---

## 🔧 Implementation Details

### 1. **Android Implementation**

#### Initialize Service:
```kotlin
val voiceService = ElevenLabsVoiceService(context)
voiceService.setApiKey("your_elevenlabs_api_key")
```

#### Text-to-Speech:
```kotlin
scope.launch {
    val result = voiceService.textToSpeech(
        text = "Emergency alert message",
        voiceId = "scOwDtmlUjD3prqpp97I" // Sam's voice
    )
    
    if (result.isSuccess) {
        val audioPath = result.getOrNull()
        voiceService.playAudio(audioPath)
    }
}
```

#### Preview Voice:
```kotlin
scope.launch {
    voiceService.previewVoice("scOwDtmlUjD3prqpp97I")
}
```

#### Emergency Call:
```kotlin
scope.launch {
    voiceService.generateEmergencyCall(
        userName = "John Doe",
        age = 45,
        condition = "Chest pain",
        location = "123 Main St, City",
        medicalInfo = "Allergic to penicillin, takes blood pressure medication"
    )
}
```

### 2. **Voice Settings Configuration**

```kotlin
val settings = VoiceSettings(
    stability = 0.5,          // 0.0 - 1.0
    similarityBoost = 0.75,   // 0.0 - 1.0
    style = 0.0,              // 0.0 - 1.0
    useSpeakerBoost = true
)

voiceService.textToSpeech(text, settings = settings)
```

**Parameter Guide:**
- **Stability:** Lower = more expressive, Higher = more consistent
- **Similarity Boost:** How closely to match the original voice
- **Style:** Stylistic exaggeration (0 = neutral)
- **Speaker Boost:** Enhance clarity and presence

---

## 🔐 API Key Management

### Development Setup:

1. **Get API Key:**
   - Go to https://elevenlabs.io
   - Sign up / Log in
   - Navigate to Profile → API Keys
   - Copy your API key

2. **Set API Key (Android):**
   ```kotlin
   // In MainActivity or Application class
   val voiceService = ElevenLabsVoiceService(context)
   voiceService.setApiKey("your_api_key_here")
   ```

3. **Set API Key (Web):**
   ```typescript
   // In backend environment variables
   ELEVENLABS_API_KEY=your_api_key_here
   ```

### Production Setup:

⚠️ **NEVER hardcode API keys in source code!**

**Android - Use Android Keystore:**
```kotlin
// Store in encrypted preferences
val encryptedPrefs = EncryptedSharedPreferences.create(
    "secure_prefs",
    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
    context,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
val apiKey = encryptedPrefs.getString("elevenlabs_api_key", "")
```

**Web - Use Environment Variables:**
```bash
# .env file (never commit this!)
ELEVENLABS_API_KEY=your_api_key_here
```

---

## 📱 User Flow

### Voice AI Setup Screen

1. **Access:** Settings → Setup Voice AI
2. **Select Voice:** Tap on Sam or Pete
3. **Preview:** Tap play button to hear sample
4. **Configure Wake Word:** Enable and customize
5. **Complete Setup:** Tap "Complete Setup"

### Voice Preview Flow:
```
User taps Play button
    ↓
App sends text to ElevenLabs API
    ↓
API generates audio (MP3)
    ↓
Audio saved to temp file
    ↓
MediaPlayer plays audio
    ↓
User hears voice preview
```

### Emergency Call Flow:
```
SOS Activated
    ↓
Generate emergency script
    ↓
Convert to speech (selected voice)
    ↓
Play emergency message
    ↓
Call emergency contacts
    ↓
Notify emergency services
```

---

## 🎯 Emergency Script Template

```
Emergency Alert from RescueMate.

This is an automated emergency notification for [USER_NAME].

An emergency has been detected. [USER_NAME], a [AGE]-year-old, 
is experiencing: [CONDITION].

Current location: [ADDRESS or GPS COORDINATES]

Medical information: [ALLERGIES, MEDICATIONS, CONDITIONS]

Immediate assistance is required.
This message will be repeated and emergency services have been notified.

Please respond if you can hear this message.
```

---

## 🛠️ Dependencies Added

### build.gradle.kts:
```kotlin
// HTTP Client for ElevenLabs API
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// JSON parsing
implementation("org.json:json:20231013")
```

---

## 📊 API Usage & Pricing

### ElevenLabs API Limits:

**Free Tier:**
- 10,000 characters/month
- Standard voices
- API access

**Paid Tiers:**
- More characters
- Professional voices
- Higher quality
- Commercial usage

### Character Count Estimate:
- Average emergency message: ~300-500 characters
- Voice preview: ~100 characters
- Monthly free tier = ~20-30 emergency calls

---

## 🧪 Testing

### Test Voice Preview:
```kotlin
@Test
fun testVoicePreview() {
    val service = ElevenLabsVoiceService(context)
    service.setApiKey(testApiKey)
    
    runBlocking {
        val result = service.previewVoice("scOwDtmlUjD3prqpp97I")
        assertTrue(result.isSuccess)
    }
}
```

### Test Emergency Call:
```kotlin
@Test
fun testEmergencyCall() {
    val service = ElevenLabsVoiceService(context)
    service.setApiKey(testApiKey)
    
    runBlocking {
        val result = service.generateEmergencyCall(
            userName = "Test User",
            age = 30,
            condition = "Test emergency",
            location = "Test location"
        )
        assertTrue(result.isSuccess)
    }
}
```

---

## 🐛 Troubleshooting

### Issue: "API key not set"
**Solution:** Call `setApiKey()` before using the service

### Issue: "API request failed: 401"
**Solution:** Check if API key is valid and active

### Issue: "Audio not playing"
**Solution:** 
- Check device volume
- Verify audio permissions
- Test with different voice

### Issue: "Network error"
**Solution:**
- Check internet connection
- Verify ElevenLabs API is accessible
- Check firewall/proxy settings

---

## 📈 Performance Optimization

### 1. Cache Voice Samples:
```kotlin
// Cache preview samples to avoid repeated API calls
private val voiceCache = mutableMapOf<String, String>()

suspend fun getCachedPreview(voiceId: String): String {
    return voiceCache.getOrPut(voiceId) {

