# 🎯 VOICE LLM DEMO - QUICK REFERENCE CARD

## Before Demo (5 mins)
- [ ] Install APK: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- [ ] Device on WiFi, volume at 70%
- [ ] Launch app, complete onboarding
- [ ] Settings > Voice AI Setup > Select voice > Complete Setup
- [ ] Test one conversation (warm up)

---

## Demo Flow (5 mins)

### 1. Introduction (30 sec)
> "RescueMate 2.0 with AI-powered wellness support using ElevenLabs Conversational AI"

### 2. Show Home Screen (30 sec)
- Point to animated SOS button
- Explain: Tap = Wellness AI | Hold 3s = Emergency

### 3. Start Conversation (3 mins)
1. **Tap SOS button** 
2. **Press "Start Conversation"**
3. Wait for "AI is listening..."
4. **Say:** "Hello, I'm feeling stressed today"
5. **Let AI respond** (audience hears voice)
6. Continue 1-2 more exchanges
7. **Press "End Conversation"**

### 4. Technical Points (1 min)
- Real-time voice with WebRTC/LiveKit
- ElevenLabs Conversational AI SDK
- Thread-safe Kotlin implementation
- Production-ready error handling

---

## If Things Go Wrong

### Stuck on "Connecting..."
- Check WiFi connection
- Check LogCat: `adb logcat -s ElevenLabsConversation:D`
- Restart app

### No Audio
- Check device volume
- Disconnect Bluetooth
- Use backup device

### Permission Denied
- Settings > Apps > RescueMate > Permissions > Enable Microphone

### Complete Failure
**Backup Option 1:** Show code in Android Studio  
**Backup Option 2:** Play pre-recorded video  
**Backup Option 3:** Walk through UI without active conversation

---

## Key Files
- Service: `app/src/main/java/com/rescuemate/services/ElevenLabsConversationalService.kt`
- UI: `app/src/main/java/com/rescuemate/ui/screens/WellnessAIConversationScreen.kt`
- Config: `.env` → `ELEVEN_AGENT_ID=agent_9701k9b2gbqvfx0rkr6cmmty44ak`

---

## Success Indicators ✅
- ✅ Connects without errors
- ✅ User speaks → AI responds with voice
- ✅ State changes visible (listening/speaking)
- ✅ Clear audio
- ✅ No crashes

---

## Talking Points
**Technical:**
- WebRTC for <500ms latency
- Thread-safe with AtomicBoolean
- MVVM + Jetpack Compose
- Proper lifecycle management

**Business:**
- 24/7 emotional support
- Reduces emergency service burden
- Natural voice interaction
- Scalable architecture

---

## Emergency Contacts
- LogCat: `adb logcat -s ElevenLabsConversation:D WellnessAI:D`
- Full Guide: `VOICE_LLM_DEMO_GUIDE.md`
- APK: `app/build/outputs/apk/debug/app-debug.apk`

