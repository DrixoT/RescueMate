# RescueMate 2.0 - Voice AI Testing Guide

## Overview

RescueMate has **dual voice AI modes**:
1. **Online Mode:** ElevenLabs Conversational AI (cloud-based, high quality)
2. **Offline Mode:** TinyLlama + Vosk STT (local, works without internet)

The app automatically switches between modes based on network availability.

---

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Online Voice AI Testing (ElevenLabs)](#online-voice-ai-testing-elevenlabs)
3. [Offline Voice AI Testing (TinyLlama)](#offline-voice-ai-testing-tinyllama)
4. [Network Handoff Testing](#network-handoff-testing)
5. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### 1. Permissions

The app needs these permissions:
- ✅ **RECORD_AUDIO** - Required for voice input
- ✅ **INTERNET** - Required for ElevenLabs online mode
- ✅ **ACCESS_NETWORK_STATE** - Required to detect network availability

**Grant permissions:**
1. Launch app
2. When prompted, tap "Allow" for microphone access
3. Settings → Apps → RescueMate → Permissions
4. Ensure Microphone is "Allowed"

### 2. API Keys (for Online Mode)

Verify `.env` file contains:
```
ELEVEN_API_KEY=sk_...your_key...
ELEVEN_AGENT_ID=...your_agent_id...
```

**Check if configured:**
```bash
cat .env | grep ELEVEN
```

### 3. Model Files (for Offline Mode)

**TinyLlama Model:**
- Location: `app/src/main/assets/models/TinyLlama-1.1B-Chat-v0.4.Q4_K_M.gguf`
- Size: ~600MB
- Download: [HuggingFace](https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v0.4-GGUF)

**Vosk STT Model:**
- Location: `app/src/main/assets/model/vosk-model-small-en-us-0.15/`
- Size: ~40MB
- Download: [Vosk Models](https://alphacephei.com/vosk/models)

**Verify models exist:**
```bash
ls -lh app/src/main/assets/models/
ls -lh app/src/main/assets/model/
```

### 4. Device Requirements

- **Microphone:** Working and not muted
- **Speaker/Headphones:** For AI voice output
- **Network:** WiFi or mobile data (for online mode)
- **Storage:** 700MB+ free space for models
- **RAM:** 2GB+ recommended

---

## Online Voice AI Testing (ElevenLabs)

### Test 1: Basic Voice Conversation

**Setup:**
1. ✅ Network enabled (WiFi or mobile data)
2. ✅ Microphone permission granted
3. ✅ ELEVEN_API_KEY configured

**Steps:**
1. Launch RescueMate
2. Navigate to HomeDashboard
3. Tap the **Voice AI** button (microphone icon)
4. Wait for "Connected" status
5. Speak clearly: **"Hello, can you hear me?"**
6. Wait for AI response
7. Continue conversation with test phrases

**Test Phrases:**
- "Hello, can you hear me?"
- "What can you help me with?"
- "What's my current heart rate?" (if monitoring active)
- "I need help with an emergency"
- "Tell me about emergency services"

**Expected Behavior:**

**Initial Connection:**
```
Logcat:
ElevenLabsConversational: ElevenLabsConversationalService initialized
ElevenLabsConversational: ✅ Network available, using ElevenLabs online service
ElevenLabsConversational: Starting conversation with agent: <agent-id>
ElevenLabsConversational: ✅ Connected to conversation: <conversation-id>
```

**During Conversation:**
```
UI:
- Status: "Connected"
- Mode indicator changes: "Listening" ↔ "Speaking"
- Audio level indicator fluctuates when you speak

Logcat:
ElevenLabsConversational: 🎤 Mode changed: listening
ElevenLabsConversational: 📨 User message: {"text": "Hello, can you hear me?"}
ElevenLabsConversational: 🎤 Mode changed: speaking
ElevenLabsConversational: 📨 Agent message: {"text": "Yes, I can hear you clearly..."}
ElevenLabsConversational: 🔊 Audio level: 0.75
```

**Audio Behavior:**
- ✅ AI voice plays through speaker/headphones
- ✅ Natural conversation flow
- ✅ Minimal latency (< 2 seconds)
- ✅ No audio feedback loops or echoes

**✅ Pass Criteria:**
- [ ] Voice conversation starts without errors
- [ ] You can hear AI responses clearly
- [ ] AI understands your speech and responds appropriately
- [ ] Mode changes reflected in UI
- [ ] No crashes or freezes
- [ ] Conversation ID generated in logs

**❌ Failure Indicators:**
- Status: "Connection failed"
- Error toast: "ElevenLabs authentication failed"
- No audio output
- Long delays (> 10 seconds)
- Crashes when speaking

---

### Test 2: ElevenLabs Error Handling

**Test 2a: Invalid API Key**

**Setup:**
1. Temporarily corrupt ELEVEN_API_KEY in .env
2. Rebuild app

**Steps:**
1. Launch app
2. Tap Voice AI button
3. Observe error handling

**Expected:**
```
Logcat:
ElevenLabsConversational: ❌ ElevenLabs authentication failed
ErrorHandler: API key invalid or expired
```

**UI:**
- Error message shown
- App doesn't crash
- Can retry or switch to offline mode

---

**Test 2b: Network Loss During Conversation**

**Setup:**
1. Start voice conversation (online mode)
2. Have 2-3 conversation turns
3. Enable airplane mode

**Steps:**
1. Start conversation normally
2. Say: "What's the weather like?"
3. While AI is responding, enable airplane mode
4. Observe automatic fallback

**Expected:**
```
Logcat:
NetworkMonitor: 🔴 Network disconnected
ElevenLabsConversational: ⚠️ Network lost during conversation
ElevenLabsConversational: 📴 Switching to local voice LLM fallback
LocalVoiceLLM: 🔄 Initializing local voice service as fallback
LocalVoiceLLM: ✅ Local voice service ready
```

**UI:**
- Brief pause (< 3 seconds)
- Status changes to "Offline Mode"
- Conversation continues with LocalVoiceLLM
- User notified of mode change

**✅ Pass Criteria:**
- [ ] Network loss detected within 3 seconds
- [ ] Automatic switch to offline mode
- [ ] No crashes or data loss
- [ ] User can continue conversation offline

---

## Offline Voice AI Testing (TinyLlama)

### Test 3: Offline Voice Conversation

**Setup:**
1. ❌ **Disable network** (airplane mode or turn off WiFi/data)
2. ✅ Microphone permission granted
3. ✅ Model files present in assets

**Steps:**
1. Enable airplane mode
2. Launch RescueMate
3. Navigate to HomeDashboard
4. Tap Voice AI button
5. Wait for initialization (may take 10-20 seconds first time)
6. Speak test phrases

**Initialization Process:**

**First Launch (Model Loading):**
```
Logcat:
LocalVoiceLLM: LocalVoiceLLMService instantiated
LocalVoiceLLM: Initializing components...
TinyLlamaInference: Checking assets for model file...
TinyLlamaInference: Model file found in assets
TinyLlamaInference: Copying model from assets...
TinyLlamaInference: Copying 637MB... (progress updates)
TinyLlamaInference: Model copied successfully
TinyLlamaInference: Model prepared at: /data/user/0/com.rescuemate/app_models/TinyLlama-1.1B-Chat-v0.4.Q4_K_M.gguf
StreamingLLM: Native library 'llama-android' loaded successfully
StreamingLLM: Initializing model...
StreamingLLM: Model initialized successfully
VoskSTT: Unpacking Vosk model...
VoskSTT: Vosk model loaded successfully from /path/to/model
LocalVoiceLLM: ✅ All components initialized
LocalVoiceLLM: ✅ Connected to local conversation: local-<uuid>
```

**Subsequent Launches (Model Already Copied):**
```
Logcat:
LocalVoiceLLM: Model file already exists, skipping copy
StreamingLLM: Model initialized successfully (faster)
VoskSTT: Vosk model loaded successfully
LocalVoiceLLM: ✅ Local voice service ready
```

**Conversation Flow:**
```
Logcat:
VoskSTT: 🎤 Started listening
VoskSTT: 📝 Partial result: "hello"
VoskSTT: 📝 Partial result: "hello how"
VoskSTT: 📝 Partial result: "hello how are"
VoskSTT: ✅ Final result: "hello how are you"

StreamingLLM: 🧠 Generating response for: "hello how are you"
StreamingLLM: 📤 Token: "Hello"
StreamingLLM: 📤 Token: "!"
StreamingLLM: 📤 Token: " I"
StreamingLLM: 📤 Token: "'m"
StreamingLLM: 📤 Token: " well"
StreamingLLM: 📤 Token: ","
StreamingLLM: 📤 Token: " thank"
StreamingLLM: 📤 Token: " you"
... (tokens stream in real-time)

StreamingTTS: 🔊 Speaking: "Hello! I'm well, thank you. How can I assist you today?"
```

**Test Phrases for Offline Mode:**
- "Hello, how are you?"
- "What can you do?"
- "I have a medical emergency"
- "My heart rate is 120 BPM, is that normal?"
- "What should I do if I fall?"

**Performance Expectations:**

| Metric | Expected | Acceptable | Poor |
|--------|----------|------------|------|
| Initial Load (first time) | 15-25s | 30-45s | 60s+ |
| Initial Load (subsequent) | 3-5s | 8-12s | 20s+ |
| Speech-to-Text Latency | <500ms | <1000ms | >2000ms |
| LLM Token Generation | 2-5 tokens/s | 1-2 tokens/s | <1 token/s |
| TTS Latency | <200ms | <500ms | >1000ms |
| End-to-End Turn | <5s | <10s | >15s |

**✅ Pass Criteria:**
- [ ] Models load without errors
- [ ] Voice input transcribed correctly
- [ ] LLM generates coherent responses
- [ ] TTS speaks responses clearly
- [ ] Conversation continues smoothly
- [ ] Memory usage stable (< 1GB increase)
- [ ] No crashes during extended use

**❌ Failure Indicators:**
- "Model file not found" error
- "Failed to load native library" error
- No speech recognition (Vosk not working)
- Gibberish LLM responses
- App freezes or crashes
- Excessive memory usage (> 2GB)

---

### Test 4: Offline Performance & Stress Test

**Purpose:** Verify offline mode stability under extended use

**Steps:**
1. Enable airplane mode
2. Start offline voice conversation
3. Have continuous conversation for 5 minutes
4. Ask varied questions (emergency, health, general)
5. Monitor performance metrics

**Monitoring:**

**Terminal 1 - Memory Usage:**
```bash
watch -n 2 'adb shell dumpsys meminfo com.rescuemate | grep -A 10 "App Summary"'
```

**Terminal 2 - CPU Usage:**
```bash
adb shell top -n 1 | grep com.rescuemate
```

**Terminal 3 - Logcat:**
```bash
./test-automation/extract-logs.sh
```

**Test Scenarios:**
1. **Short Questions:** "Hello" → "How are you?" → "Thanks"
2. **Long Questions:** 50+ word inputs about medical history
3. **Rapid Fire:** Multiple questions back-to-back
4. **Interruptions:** Speak while AI is responding
5. **Silence:** Wait 30 seconds between questions

**✅ Pass Criteria:**
- [ ] Consistent response quality over 5 minutes
- [ ] Memory doesn't grow unbounded (< 100MB increase)
- [ ] CPU usage reasonable (< 80% sustained)
- [ ] No audio artifacts or degradation
- [ ] App remains responsive
- [ ] Battery drain acceptable (< 10%/hour with screen on)

---

## Network Handoff Testing

### Test 5: Online → Offline → Online Transition

**Purpose:** Verify seamless mode switching during network changes

**Automated Test:**
```bash
# Use the network toggle script
./test-automation/network-toggle.sh
# Select option 7: Test Network Handoff
```

**Manual Test:**

**Phase 1: Start Online**
1. Ensure network enabled
2. Start voice conversation
3. Say: "What's the current time?"
4. Verify online mode (ElevenLabs) is active
5. Wait for response

**Phase 2: Go Offline**
6. Enable airplane mode
7. Say: "Can you still hear me?"
8. Watch for mode switch
9. Verify offline mode (LocalVoiceLLM) takes over
10. Continue conversation offline

**Phase 3: Return Online**
11. Disable airplane mode
12. Wait for network detection
13. Say: "Testing network restoration"
14. Verify switch back to online mode (optional - may stay offline)
15. End conversation

**Expected Logcat Flow:**

**Phase 1 → Phase 2 (Online to Offline):**
```
ElevenLabsConversational: ✅ Connected to conversation
ElevenLabsConversational: 🎤 Mode changed: listening
NetworkMonitor: 🔴 Network disconnected
ElevenLabsConversational: ⚠️ Network lost during conversation
ElevenLabsConversational: 📴 Switching to local voice LLM fallback
LocalVoiceLLM: 🔄 Initializing local voice service as fallback
LocalVoiceLLM: ✅ Local voice service ready
```

**Phase 2 → Phase 3 (Offline to Online):**
```
NetworkMonitor: 🟢 Network connected
LocalVoiceLLM: ℹ️ Network restored, continuing with local mode
(or)
LocalVoiceLLM: 🔄 Switching back to ElevenLabs
ElevenLabsConversational: ✅ Reconnected to conversation
```

**UI Indicators:**
- Status text changes: "Connected" → "Offline Mode" → "Connected"
- Brief pause (< 3 seconds) during transitions
- Notification or toast about mode change
- Conversation continues without data loss

**✅ Pass Criteria:**
- [ ] Mode switch happens automatically
- [ ] Transition time < 3 seconds
- [ ] No crashes or freezes
- [ ] Conversation history preserved
- [ ] User informed of mode changes
- [ ] No duplicate responses

**❌ Failure Indicators:**
- Crash during mode switch
- Long pause (> 10 seconds)
- Conversation resets/lost
- Both modes try to respond simultaneously
- Audio feedback loops

---

## Troubleshooting

### Issue 1: "Microphone Permission Required"

**Cause:** RECORD_AUDIO permission not granted

**Fix:**
1. Settings → Apps → RescueMate → Permissions
2. Tap "Microphone"
3. Select "Allow"
4. Restart app

---

### Issue 2: "Model File Not Found"

**Cause:** TinyLlama or Vosk model files missing from assets

**Fix:**
1. Download TinyLlama model (600MB+)
2. Place in: `app/src/main/assets/models/TinyLlama-1.1B-Chat-v0.4.Q4_K_M.gguf`
3. Download Vosk model (40MB+)
4. Extract to: `app/src/main/assets/model/vosk-model-small-en-us-0.15/`
5. Rebuild: `./gradlew clean assembleDebug`
6. Reinstall app

**Verify files:**
```bash
ls -lh app/src/main/assets/models/
ls -lh app/src/main/assets/model/vosk-model-small-en-us-0.15/
```

---

### Issue 3: "Failed to Load Native Library"

**Cause:** libllama-android.so not included in APK or wrong architecture

**Fix:**
1. Check if library exists in build:
   ```bash
   unzip -l app/build/outputs/apk/debug/app-debug.apk | grep libllama-android.so
   ```
2. Should see:
   ```
   lib/arm64-v8a/libllama-android.so
   lib/armeabi-v7a/libllama-android.so
   ```
3. If missing, check CMakeLists.txt configuration
4. Rebuild NDK components:
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   ```

---

### Issue 4: ElevenLabs "Authentication Failed"

**Cause:** Invalid or missing API key

**Fix:**
1. Verify `.env` file exists in project root
2. Check ELEVEN_API_KEY value:
   ```bash
   cat .env | grep ELEVEN_API_KEY
   ```
3. Ensure key starts with `sk_` or matches your ElevenLabs dashboard
4. Update key if needed
5. Rebuild: `./gradlew clean assembleDebug`

**Get API Key:**
1. Go to [ElevenLabs Dashboard](https://elevenlabs.io/app)
2. Click profile icon → Settings
3. Copy API Key
4. Update .env file

---

### Issue 5: Voice Input Not Recognized

**Possible Causes:**
- Microphone muted or blocked
- Background noise too loud
- Speaking too quietly
- Vosk model not loaded (offline mode)

**Debug:**
1. Check microphone works in other apps
2. Test in quiet environment
3. Speak clearly and at normal volume
4. Check logcat for Vosk/ElevenLabs recognition events
5. Verify audio level indicator changes when speaking

**Logcat Check:**
```bash
adb logcat | grep -E "VoskSTT|ElevenLabs|AudioRecord"
```

Should see partial results as you speak.

---

### Issue 6: Slow Offline Performance

**Cause:** Device limitations, model size, or configuration

**Optimizations:**
1. **Close other apps** - Free up RAM
2. **Use smaller model** - Consider TinyLlama-1.1B instead of larger models
3. **Reduce context length** - Shorter conversation history
4. **Check CPU throttling** - Device may be hot
5. **Lower quality settings** - If configurable in StreamingLLM

**Benchmark:**
```bash
# Monitor during conversation
adb shell top -n 1 | grep com.rescuemate
```

Acceptable: 20-60% CPU during inference
Poor: 100% CPU sustained or < 5% (not utilizing resources)

---

### Issue 7: Network Handoff Not Working

**Cause:** NetworkMonitor not detecting state changes

**Debug:**
1. Check NetworkMonitor logs:
   ```bash
   adb logcat | grep NetworkMonitor
   ```
2. Manually check network state:
   ```bash
   adb shell dumpsys wifi | grep "Wi-Fi is"
   ```
3. Verify ACCESS_NETWORK_STATE permission granted

**Fix:**
- Ensure NetworkMonitor is initialized in service
- Check callback registration
- Verify connectivity manager usage

---

## Performance Benchmarks

### Expected Metrics (Good Performance)

| Metric | Online (ElevenLabs) | Offline (TinyLlama) |
|--------|---------------------|---------------------|
| Initial Connection | 1-3s | 5-15s (first) / 2-4s (subsequent) |
| Voice Recognition Latency | <500ms | <800ms |
| Response Generation | 1-3s | 3-8s |
| Audio Playback Start | <200ms | <300ms |
| End-to-End Turn | 2-5s | 5-12s |
| CPU Usage | 10-30% | 40-70% |
| Memory Increase | ~50MB | ~200MB |
| Battery Impact | Low | Medium |

### Signs of Poor Performance

- Initial load > 30s (offline first time)
- Response time > 15s per turn
- CPU sustained at 100%
- Memory growth > 500MB over time
- Frequent freezes or ANRs
- Battery drain > 20%/hour

---

## Test Checklist Summary

### Online Mode (ElevenLabs)
- [ ] Voice conversation starts successfully
- [ ] AI responds with voice output
- [ ] Low latency (< 5s per turn)
- [ ] Mode changes reflected in UI
- [ ] Error handling for invalid API key
- [ ] Automatic fallback on network loss

### Offline Mode (TinyLlama)
- [ ] Models load without errors
- [ ] Speech-to-text works (Vosk)
- [ ] LLM generates coherent responses
- [ ] Text-to-speech works
- [ ] Acceptable performance (< 15s per turn)
- [ ] Stable over 5-minute conversation

### Network Handoff
- [ ] Automatic switch online → offline
- [ ] Automatic switch offline → online (optional)
- [ ] Transition time < 3 seconds
- [ ] No crashes during switch
- [ ] Conversation preserved

---

**For Additional Help:**
- See `COMPREHENSIVE_TESTING_PLAN.md` for full test suite
- Use `./test-automation/extract-logs.sh` to collect detailed logs
- Check `test-automation/README.md` for script usage

**Last Updated:** November 28, 2025

