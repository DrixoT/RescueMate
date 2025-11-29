# RescueMate 2.0 - Comprehensive Testing Plan

**Date:** November 28, 2025  
**Version:** 1.0.0  
**Focus Areas:** Google Authentication, Voice AI (Online/Offline), Emergency System

---

## Table of Contents
1. [Pre-Test Setup](#pre-test-setup)
2. [Google Authentication Testing](#google-authentication-testing)
3. [Voice AI Testing - Online (ElevenLabs)](#voice-ai-testing---online-elevenlabs)
4. [Voice AI Testing - Offline (TinyLlama)](#voice-ai-testing---offline-tinyllama)
5. [Emergency System Testing](#emergency-system-testing)
6. [Integration Testing](#integration-testing)
7. [Test Results Documentation](#test-results-documentation)

---

## Pre-Test Setup

### 1.1 Environment Configuration Checklist

**Firebase Configuration:**
- [ ] `google-services.json` present in `app/` directory
- [ ] Extract SHA-1 fingerprint for debug keystore
- [ ] Verify SHA-1 in Firebase Console matches local keystore
- [ ] Confirm Web Client ID (client_type: 3) in google-services.json
- [ ] Firebase Authentication enabled (Email, Phone, Google Sign-In)

**API Keys Verification:**
```bash
# Check .env file exists and has required keys
cat .env | grep -E "ELEVEN_API_KEY|ELEVEN_AGENT_ID|OPENAI_API_KEY|TWILIO"
```

Required keys:
- [ ] `ELEVEN_API_KEY` - ElevenLabs API key
- [ ] `ELEVEN_AGENT_ID` - ElevenLabs agent ID
- [ ] `OPENAI_API_KEY` - OpenAI API key (if used)
- [ ] `TWILIO_ACCOUNT_SID` - Twilio account SID
- [ ] `TWILIO_AUTH_TOKEN` - Twilio auth token
- [ ] `TWILIO_PHONE_NUMBER` - Twilio phone number
- [ ] `GOOGLE_MAPS_API_KEY` - Google Maps API key

**Model Files Verification:**
- [ ] TinyLlama model exists: `app/src/main/assets/models/TinyLlama-1.1B-Chat-v0.4.Q4_K_M.gguf`
- [ ] Vosk model exists: `app/src/main/assets/model/vosk-model-small-en-us-0.15/`
- [ ] Native library compiled: `libllama-android.so`

**Backend Services:**
- [ ] Backend server running at `backend-emergency/`
- [ ] Test backend connectivity: `curl http://localhost:3000/api/health`

**Test Device Setup:**
- [ ] Android device/emulator (API 26+)
- [ ] Developer options enabled
- [ ] USB debugging enabled
- [ ] Internet connection available
- [ ] Google Play Services installed
- [ ] Logcat accessible via ADB or Android Studio

### 1.2 SHA-1 Fingerprint Extraction

```bash
# Debug keystore
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1

# Release keystore (if applicable)
keytool -list -v -keystore /path/to/release.keystore -alias your-alias -storepass your-password | grep SHA1
```

**Expected Output:**
```
SHA1: 06:39:6e:57:da:76:d4:07:f5:a8:69:36:e0:dd:dd:4d:ac:f3:58:85
```

**Action Required:**
1. Copy SHA-1 fingerprint
2. Go to Firebase Console → Project Settings → Your Android App
3. Add SHA certificate fingerprint
4. Download updated `google-services.json`
5. Replace existing file in `app/` directory

### 1.3 Build and Install

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Or build and install in one step
./gradlew clean installDebug
```

### 1.4 Enable Detailed Logging

```bash
# Filter logcat for RescueMate logs
adb logcat -c  # Clear existing logs
adb logcat | grep -E "AuthRepository|SignInScreen|ElevenLabs|LocalVoiceLLM|EmergencyManager|VoskSTT|StreamingLLM"

# Save logs to file
adb logcat | grep -E "AuthRepository|SignInScreen|ElevenLabs|LocalVoiceLLM|EmergencyManager" > test_logs.txt
```

---

## Google Authentication Testing

### 2.1 Google Sign-In - Happy Path

**Test Steps:**
1. Launch app → Should show OnboardingScreen
2. Tap "Get Started" → Navigate to SignInScreen
3. Tap "Continue with Google" button
4. Google account picker should appear
5. Select an account
6. Grant permissions if requested
7. Should navigate to HomeDashboard

**Expected Logcat Output:**
```
AuthRepository: Google Sign-In client initialized
SignInScreen: Google Sign-In clicked
AuthRepository: Starting Google Sign-In authentication...
AuthRepository: Google Sign-In task created, checking if successful...
AuthRepository: Google Sign-In task successful
AuthRepository: Google account retrieved: user@gmail.com
AuthRepository: ID token retrieved successfully
AuthRepository: Firebase credential created, signing in...
AuthRepository: Firebase authentication successful for user: uid123...
SignInScreen: ✅ Google Sign-In successful
```

**Test Results:**
- [ ] Account picker appears
- [ ] No crashes during flow
- [ ] Successfully navigates to HomeDashboard
- [ ] User preferences saved (check SharedPreferences)
- [ ] Firebase user created/logged in

---

### 2.2 Google Sign-In - Cancellation Flow

**Test Steps:**
1. Tap "Continue with Google"
2. Google account picker appears
3. Press back button or tap outside to cancel
4. Should return to SignInScreen with "Sign in cancelled" message

**Expected Logcat Output:**
```
AuthRepository: Google Sign-In ApiException
AuthRepository:    Status Code: 12500
AuthRepository:    Error Message: 12500:
SignInScreen: Google Sign-In cancelled
```

**Test Results:**
- [ ] No crash on cancellation
- [ ] Status code 12500 detected
- [ ] User-friendly message shown
- [ ] Can retry sign-in

---

### 2.3 Google Sign-In - Configuration Error (Status Code 10)

**This is the critical test for your reported issue!**

**Symptoms:**
- Sign-in cancels immediately after account selection
- No visible error to user
- Logcat shows status code 10

**Test Steps:**
1. Temporarily corrupt Web Client ID (edit AuthRepository.kt or google-services.json)
2. Tap "Continue with Google"
3. Select account
4. Should fail with status code 10

**Expected Logcat Output:**
```
AuthRepository: Google Sign-In ApiException
AuthRepository:    Status Code: 10
AuthRepository:    Error Message: 10: 
SignInScreen: ❌ Google Sign-In failed: Please reinstall the app or contact support (Code: 10)
```

**Root Causes:**
- Wrong Web Client ID in google-services.json
- SHA-1 fingerprint not added to Firebase Console
- Mismatch between debug/release keystore SHA-1

**Fixes:**
1. Re-download `google-services.json` from Firebase Console
2. Add correct SHA-1 fingerprint to Firebase Console
3. Clean and rebuild app
4. Reinstall on device

**Test Results:**
- [ ] Error detected and logged
- [ ] User sees meaningful error message
- [ ] After fixing config, sign-in works

---

### 2.4 Google Sign-In - Network Error (Status Code 7)

**Test Steps:**
1. Enable airplane mode
2. Tap "Continue with Google"
3. Should fail with network error

**Expected Behavior:**
- Status code 7 detected
- Error message: "Network error. Please check your internet connection."

**Test Results:**
- [ ] Network error detected
- [ ] User-friendly message shown
- [ ] Can retry after restoring network

---

### 2.5 Google Sign-In - Null ID Token Error

**Test Steps:**
1. This is rare but can happen if OAuth client misconfigured
2. Monitor for ID token null checks in logs

**Expected Logcat Output (if error occurs):**
```
AuthRepository: Google account ID token is null - check Web Client ID configuration
```

**Fixes:**
- Verify OAuth 2.0 Client ID in Firebase Console
- Ensure Web Client ID matches google-services.json

---

### 2.6 Email/Password Authentication

**Test Steps:**
1. From SignInScreen, tap "Continue with Email"
2. Navigate to EmailLoginScreen
3. Enter email: `test@rescuemate.com`
4. Enter password: `TestPassword123!`
5. Tap "Sign In"

**Expected Behavior:**
- Loading indicator appears
- Firebase authentication
- Navigate to HomeDashboard

**Error Cases to Test:**
- [ ] Wrong password → "Incorrect password. Please try again."
- [ ] User not found → "Account not found. Please create an account."
- [ ] Network error → "Network error. Please check your connection."
- [ ] Invalid email format → Validation error before submission

---

### 2.7 Phone Authentication

**Test Steps:**
1. Tap "Continue with Phone"
2. Enter phone: `+1234567890`
3. Tap "Send Code"
4. Enter verification code from SMS
5. Tap "Verify"

**Error Cases to Test:**
- [ ] Invalid phone format → "Please enter a valid phone number with country code"
- [ ] SMS quota exceeded → "SMS quota exceeded. Please try again later."
- [ ] Wrong verification code → "Invalid verification code. Please check and try again."
- [ ] Code expired → "Verification code expired. Please request a new code."

---

## Voice AI Testing - Online (ElevenLabs)

### 3.1 ElevenLabs Initialization

**Prerequisites:**
- [ ] `ELEVEN_API_KEY` configured in .env
- [ ] `ELEVEN_AGENT_ID` configured in .env
- [ ] RECORD_AUDIO permission granted
- [ ] Internet connection active
- [ ] Microphone working

**Test Steps:**
1. Navigate to HomeDashboard
2. Tap voice conversation button
3. Grant RECORD_AUDIO permission if prompted
4. ElevenLabsConversationalService should initialize

**Expected Logcat Output:**
```
ElevenLabsConversational: ElevenLabsConversationalService initialized with official SDK
ElevenLabsConversational: ✅ Network available, using ElevenLabs online service
ElevenLabsConversational: Starting conversation with agent: <agent-id>
ElevenLabsConversational: 🎙️ Conversation session starting...
ElevenLabsConversational: ✅ Connected to conversation: <conversation-id>
```

**Test Results:**
- [ ] No crashes during initialization
- [ ] Conversation session created
- [ ] Callback onConnect() fired with conversation ID
- [ ] Status updates received

---

### 3.2 Voice Conversation - Speech Recognition

**Test Steps:**
1. After conversation started, speak clearly: "Hello, can you hear me?"
2. Wait for response
3. Speak: "What's my current heart rate?"
4. Speak: "I need help with an emergency"

**Expected Logcat Output:**
```
ElevenLabsConversational: 🎤 Mode changed: listening
ElevenLabsConversational: 📨 User message: {"text": "Hello, can you hear me?"}
ElevenLabsConversational: 🎤 Mode changed: speaking
ElevenLabsConversational: 📨 Agent message: {"text": "Yes, I can hear you..."}
ElevenLabsConversational: 🔊 Audio level: 0.75
```

**Test Results:**
- [ ] Voice input captured
- [ ] Agent responds appropriately
- [ ] Audio plays through speaker
- [ ] Mode changes (listening → speaking) detected
- [ ] Audio level indicator updates
- [ ] No audio feedback loops

---

### 3.3 Network Loss During Conversation

**This tests automatic fallback to offline mode!**

**Test Steps:**
1. Start ElevenLabs conversation
2. Have a brief exchange (2-3 turns)
3. Enable airplane mode (or disable WiFi/data)
4. Continue speaking - should automatically switch to LocalVoiceLLMService

**Expected Logcat Output:**
```
ElevenLabsConversational: ⚠️ Network lost during conversation
NetworkMonitor: 🔴 Network disconnected
ElevenLabsConversational: 📴 Switching to local voice LLM fallback
LocalVoiceLLM: 🔄 Initializing local voice service as fallback
LocalVoiceLLM: ✅ Local voice service ready
```

**Test Results:**
- [ ] Network loss detected within 3 seconds
- [ ] Conversation pauses briefly
- [ ] Automatically switches to offline mode
- [ ] User notified of mode change
- [ ] Conversation continues with LocalVoiceLLMService
- [ ] No data loss or crashes

---

### 3.4 Network Restoration

**Test Steps:**
1. While in offline mode (from previous test)
2. Re-enable network (disable airplane mode)
3. Should detect network and offer to switch back

**Expected Behavior:**
- Network restoration detected
- Option to switch back to ElevenLabs
- Or continue with local service

---

### 3.5 ElevenLabs Error Handling

**Error Cases:**
- [ ] Invalid API key → "ElevenLabs authentication failed"
- [ ] Invalid agent ID → "Agent not found"
- [ ] API rate limit → "Rate limit exceeded, please try again later"
- [ ] Microphone permission denied → "Microphone permission required"

---

## Voice AI Testing - Offline (TinyLlama)

### 4.1 Model File Verification

**Test Steps:**
```bash
# Check TinyLlama model exists
ls -lh app/src/main/assets/models/TinyLlama-1.1B-Chat-v0.4.Q4_K_M.gguf

# Check Vosk model exists
ls -lh app/src/main/assets/model/vosk-model-small-en-us-0.15/

# Check native library in APK
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep libllama-android.so
```

**Expected Output:**
```
-rw-r--r--  1 user  staff  637M Nov 28 10:00 TinyLlama-1.1B-Chat-v0.4.Q4_K_M.gguf
drwxr-xr-x  5 user  staff   160 Nov 28 10:00 vosk-model-small-en-us-0.15/
lib/arm64-v8a/libllama-android.so
lib/armeabi-v7a/libllama-android.so
```

**Test Results:**
- [ ] TinyLlama model file exists (600MB+)
- [ ] Vosk model directory exists
- [ ] Native library included in APK for device architecture

---

### 4.2 LocalVoiceLLMService Initialization

**Prerequisites:**
- [ ] Network disabled (airplane mode)
- [ ] RECORD_AUDIO permission granted
- [ ] Model files verified

**Test Steps:**
1. Enable airplane mode
2. Navigate to HomeDashboard
3. Tap voice conversation button
4. LocalVoiceLLMService should initialize automatically

**Expected Logcat Output:**
```
LocalVoiceLLM: LocalVoiceLLMService instantiated
LocalVoiceLLM: Initializing components...
TinyLlamaInference: Copying model from assets...
TinyLlamaInference: Model copied successfully
TinyLlamaInference: Model prepared at: /data/user/0/com.rescuemate/app_models/TinyLlama...
StreamingLLM: Native library 'llama-android' loaded successfully
StreamingLLM: Model initialized successfully
VoskSTT: Vosk model loaded successfully from /path/to/model
LocalVoiceLLM: ✅ All components initialized
LocalVoiceLLM: ✅ Connected to local conversation: local-<uuid>
```

**Test Results:**
- [ ] No crashes during initialization
- [ ] Model copied from assets (first run only)
- [ ] Native library loads successfully
- [ ] Vosk STT initializes
- [ ] Conversation ready callback fired

---

### 4.3 Offline Voice Conversation Flow

**Test Steps:**
1. With airplane mode enabled and LocalVoiceLLM active
2. Speak: "Hello, how are you?"
3. Wait for response
4. Speak: "What can you help me with?"
5. Speak: "My heart rate is 120 BPM, is that normal?"

**Expected Logcat Output:**
```
VoskSTT: 🎤 Started listening
VoskSTT: 📝 Partial result: "hello"
VoskSTT: 📝 Partial result: "hello how"
VoskSTT: ✅ Final result: "hello how are you"
StreamingLLM: 🧠 Generating response for: "hello how are you"
StreamingLLM: 📤 Token: "Hello"
StreamingLLM: 📤 Token: "!"
StreamingLLM: 📤 Token: " I'm"
StreamingTTS: 🔊 Speaking: "Hello! I'm here to help..."
```

**Test Results:**
- [ ] Voice input transcribed by Vosk
- [ ] Partial results shown in real-time
- [ ] LLM generates response tokens
- [ ] TTS speaks response
- [ ] Conversation continues smoothly
- [ ] Response quality is reasonable for emergency scenarios

---

### 4.4 Offline Mode Performance

**Metrics to Monitor:**
- [ ] Speech-to-text latency: < 500ms after speech ends
- [ ] LLM token generation: 2-5 tokens/second (acceptable for emergency)
- [ ] TTS latency: < 200ms to start speaking
- [ ] End-to-end conversation turn: < 5 seconds
- [ ] Memory usage: < 1GB additional RAM
- [ ] CPU usage: Spikes during inference (normal)
- [ ] Battery impact: Monitor over 5-minute conversation

**Stress Test:**
- Continuous 5-minute conversation
- Multiple back-to-back questions
- Long user inputs (50+ words)
- Check for memory leaks or crashes

---

### 4.5 Offline Error Handling

**Error Cases to Test:**

**Missing Model File:**
```bash
# Temporarily rename model file
mv app/src/main/assets/models/TinyLlama-1.1B-Chat-v0.4.Q4_K_M.gguf \
   app/src/main/assets/models/TinyLlama-BACKUP.gguf
```
- [ ] Error detected: "Model file not found in assets"
- [ ] User-friendly error shown
- [ ] Doesn't crash

**Native Library Load Failure:**
- [ ] JNI initialization fails gracefully
- [ ] Error: "Failed to load native library"
- [ ] Fallback to error state

**Vosk Initialization Failure:**
- [ ] Vosk model unpack fails
- [ ] Error: "Failed to load speech model"
- [ ] Speech recognition unavailable but app doesn't crash

---

## Emergency System Testing

### 5.1 Emergency Contact Configuration

**Test Steps:**
1. Navigate to Emergency Contacts screen
2. Tap "Add Contact"
3. Fill in contact details:
   - Name: "John Doe"
   - Phone: "+1234567890"
   - Relationship: "Spouse"
   - Priority: 1
4. Save contact
5. Add 2-3 more contacts with different priorities

**Test Results:**
- [ ] Contacts saved to database
- [ ] Contacts display in priority order
- [ ] Can edit/delete contacts
- [ ] Phone validation works

---

### 5.2 Manual SOS Trigger - Happy Path

**Test Steps:**
1. From HomeDashboard
2. Long-press panic button (or tap SOS)
3. 10-second countdown appears
4. Let countdown complete (don't cancel)
5. Emergency should trigger

**Expected Logcat Output:**
```
EmergencyManager: 🚨 Triggering health emergency for user: <user-id>
EmergencyManager: 📍 Location: <address> (<lat>, <lon>)
EmergencyManager: 📞 Found 3 emergency contacts
EmergencyManager: ✅ Emergency event created: <event-id>
EmergencyManager: ⏱️ Starting Phase 1: User Response Check (60s)
```

**Test Results:**
- [ ] Countdown shows 10 → 0
- [ ] Location acquired
- [ ] Emergency event created in database
- [ ] Phase 1 starts (60-second user check)
- [ ] Notification shows emergency active
- [ ] User can cancel during Phase 1

---

### 5.3 Emergency Phase 1 - User Cancellation

**Test Steps:**
1. Trigger emergency (from 5.2)
2. During Phase 1 (within 60 seconds)
3. Tap "I'm OK" button
4. Emergency should cancel

**Expected Logcat Output:**
```
EmergencyManager: ✅ User responded during Phase 1 - emergency cancelled
```

**Test Results:**
- [ ] Emergency cancelled
- [ ] Database updated: status = CANCELLED
- [ ] Notification dismissed
- [ ] No contacts notified

---

### 5.4 Emergency Phase 2 - Contact Notification

**Test Steps:**
1. Trigger emergency
2. Let Phase 1 timeout (wait 60 seconds, don't cancel)
3. Phase 2 should start and notify contacts

**Expected Logcat Output:**
```
EmergencyManager: ⏰ Phase 1 timeout - User did not respond, escalating to Phase 2
EmergencyManager: 📞 Starting Phase 2: Emergency Contact Notification
EmergencyManager: 📞 Notifying 3 emergency contacts
EmergencyManager: 📱 Notifying contact: John Doe (+1234567890) - Priority 1
TwilioEmergency: 📞 Calling contact: +1234567890
```

**Test Results:**
- [ ] Phase 2 starts after Phase 1 timeout
- [ ] Contacts notified in priority order
- [ ] Voice calls initiated via Twilio
- [ ] SMS sent as backup
- [ ] Location shared with contacts
- [ ] Medical info shared (if configured)

---

### 5.5 Offline Emergency Queuing

**Test Steps:**
1. Enable airplane mode
2. Trigger emergency
3. Emergency should queue locally
4. Re-enable network
5. Emergency should auto-sync

**Expected Logcat Output:**
```
EmergencyManager: 📴 Network unavailable, queueing emergency event
EmergencyManager: 💾 Emergency queued locally: <event-id>
NetworkMonitor: 🌐 Network restored
EmergencyManager: 🌐 Network restored, processing 1 queued events
EmergencyManager: 📤 Processing queued event: <event-id>
EmergencyManager: ✅ Queued event synced successfully
```

**Test Results:**
- [ ] Emergency queued when offline
- [ ] User notified it will sync when online
- [ ] Event stored in local database
- [ ] Auto-syncs when network returns
- [ ] No data loss

---

### 5.6 Health Anomaly Detection → Automatic Emergency

**Test Steps:**
1. Enable health monitoring
2. Simulate abnormal heart rate (or use mock data)
3. Set heart rate to 180 BPM (above threshold)
4. Emergency should trigger automatically

**Expected Logcat Output:**
```
HealthMonitoring: ⚠️ Abnormal heart rate detected: 180 BPM (normal: 70 BPM)
HealthMonitoring: 🚨 Triggering automatic emergency
EmergencyManager: 🚨 Triggering health emergency for user: <user-id>
EmergencyManager: 💓 Health anomaly: Heart rate critically high
```

**Test Results:**
- [ ] Abnormal vitals detected
- [ ] Emergency triggered automatically
- [ ] Health data included in alert
- [ ] User gets notification before Phase 1
- [ ] Can cancel false positive

---

## Integration Testing

### 6.1 Complete Emergency Flow with Voice AI

**Scenario:** User has medical emergency, voice AI assists, then triggers emergency system

**Test Steps:**
1. Start voice conversation (ElevenLabs or offline)
2. Say: "I'm feeling dizzy and my chest hurts"
3. Voice AI should recognize emergency keywords
4. Voice AI asks clarifying questions
5. User confirms emergency needed
6. System triggers emergency automatically
7. Voice AI continues to comfort user during Phase 1
8. Contacts are notified in Phase 2

**Test Results:**
- [ ] Voice AI recognizes emergency context
- [ ] Emergency triggered from conversation
- [ ] Voice AI continues during emergency
- [ ] All emergency phases execute correctly
- [ ] Seamless integration between systems

---

### 6.2 Network Handoff During Emergency

**Test Steps:**
1. Start emergency with online voice AI active
2. Disable network mid-emergency
3. Should switch to offline voice AI
4. Emergency should queue
5. Re-enable network
6. Should switch back to online voice AI
7. Emergency should sync and continue

**Test Results:**
- [ ] Voice AI switches offline smoothly
- [ ] Emergency queues without data loss
- [ ] Voice AI switches back online
- [ ] Emergency syncs and progresses
- [ ] User experience minimally disrupted

---

### 6.3 Full User Journey - First Time User

**Complete flow from installation to emergency:**
1. Install app → Onboarding
2. Sign in with Google
3. Complete Setup Wizard (medical profile, contacts)
4. Grant permissions (location, mic, notifications)
5. Explore HomeDashboard
6. Test voice conversation
7. Configure emergency settings
8. Simulate emergency trigger
9. Cancel emergency
10. Sign out and sign back in

**Test Results:**
- [ ] All screens accessible
- [ ] Data persists across sessions
- [ ] No crashes throughout flow
- [ ] Permissions properly requested
- [ ] Setup wizard saves data correctly

---

## Test Results Documentation

### 7.1 Test Execution Log Template

```markdown
## Test Execution: [Date]

**Tester:** [Name]
**Device:** [Device Model] - Android [Version]
**Build:** [App Version] - [Git Commit]

### Test Results Summary
- Total Tests: X
- Passed: X
- Failed: X
- Blocked: X

### Critical Issues Found
1. [Issue Description]
   - Severity: Critical/High/Medium/Low
   - Steps to Reproduce: ...
   - Expected: ...
   - Actual: ...
   - Logs: ...

### Google Authentication Results
- [ ] Sign-in Happy Path: PASS/FAIL
- [ ] Cancellation Flow: PASS/FAIL
- [ ] Error Handling: PASS/FAIL
- Issues: ...

### Voice AI Results
- [ ] ElevenLabs Online: PASS/FAIL
- [ ] TinyLlama Offline: PASS/FAIL
- [ ] Network Handoff: PASS/FAIL
- Issues: ...

### Emergency System Results
- [ ] Manual Trigger: PASS/FAIL
- [ ] Auto Detection: PASS/FAIL
- [ ] Contact Notification: PASS/FAIL
- Issues: ...
```

---

## Known Issues & Workarounds

### Google Sign-In Cancels Immediately

**Issue:** Sign-in cancels after account selection (Status Code 10)

**Diagnosis:**
1. Check logcat for `Status Code: 10`
2. Indicates SHA-1 mismatch or wrong Web Client ID

**Fix:**
1. Extract SHA-1: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`
2. Add to Firebase Console
3. Re-download google-services.json
4. Clean rebuild: `./gradlew clean installDebug`

### Voice AI Not Initializing

**Issue:** ElevenLabs or LocalVoiceLLM fails to initialize

**Diagnosis:**
1. Check API keys in .env
2. Verify model files exist
3. Check RECORD_AUDIO permission granted

**Fix:**
1. Validate .env configuration
2. Re-copy model files to assets
3. Request permission explicitly

### Emergency Contacts Not Notified

**Issue:** Twilio calls/SMS not sent

**Diagnosis:**
1. Check Twilio credentials in .env
2. Verify backend server running
3. Check network connectivity

**Fix:**
1. Restart backend server
2. Verify Twilio account active
3. Check phone number format (+E.164)

---

## Automation Opportunities

### Test Automation Scripts

See `test-automation/` directory for:
- `verify-setup.sh` - Pre-test configuration verification
- `run-tests.sh` - Automated test execution
- `extract-logs.sh` - Log collection and analysis
- `network-toggle.sh` - Automated network state changes

### CI/CD Integration

Recommended tools:
- Firebase Test Lab - Cloud device testing
- GitHub Actions - Automated builds and tests
- Fastlane - Deployment automation

---

## Next Steps

1. ✅ Review this testing plan
2. ⬜ Execute Pre-Test Setup (Section 1)
3. ⬜ Run Google Authentication Tests (Section 2)
4. ⬜ Run Voice AI Tests (Sections 3-4)
5. ⬜ Run Emergency System Tests (Section 5)
6. ⬜ Run Integration Tests (Section 6)
7. ⬜ Document Results (Section 7)
8. ⬜ Fix Critical Issues
9. ⬜ Regression Testing
10. ⬜ Production Release

---

**Last Updated:** November 28, 2025

