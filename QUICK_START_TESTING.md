# RescueMate 2.0 - Testing Quick Start Guide

**Get started with testing in 5 minutes!**

---

## Step 1: Pre-Flight Check (2 minutes)

### Run the automated setup verifier:

```bash
cd "/Users/drixot/Library/CloudStorage/GoogleDrive-bhuv5603@gmail.com/My Drive/Drixot/Rescuemate/RescueMate2.0/RescueMate-2.0"

./test-automation/verify-setup.sh
```

**Expected Output:**
```
✓ .env file exists
✓ google-services.json exists
✓ Model files exist
✓ Device connected
```

### If you see errors:

**❌ `.env` file not found**
→ Create `.env` in project root with your API keys (see `.env.example`)

**❌ `google-services.json` not found**
→ Download from Firebase Console and place in `app/` directory

**❌ Model files missing**
→ Download TinyLlama and Vosk models (see `VOICE_AI_TESTING_GUIDE.md`)

**❌ SHA-1 mismatch**
→ Follow `GOOGLE_SIGNIN_DEBUG_GUIDE.md` Step 1-3

---

## Step 2: Build & Install (2 minutes)

```bash
# Clean build (important after config changes)
./gradlew clean

# Build and install on connected device
./gradlew installDebug
```

**Expected:** App installs on your device without errors.

---

## Step 3: Start Testing Google Sign-In (3 minutes)

### Test the critical issue you reported:

**Terminal 1 - Start log monitoring:**
```bash
./test-automation/extract-logs.sh
# Select option: 1 (Live monitoring)
```

**On your device:**
1. Launch RescueMate app
2. Tap "Get Started"
3. Tap "Continue with Google"
4. Select your Google account
5. Watch what happens

### Check the logs for status code:

**If you see:**
```
Status Code: 10
```
→ **This is your issue!** Follow `GOOGLE_SIGNIN_DEBUG_GUIDE.md` immediately.

**If you see:**
```
Status Code: 12500
```
→ You cancelled sign-in. Try again.

**If you see:**
```
Google Sign-In task successful
Google account retrieved: your@email.com
```
→ ✅ **Sign-in works!** Continue to other tests.

---

## Step 4: Test Voice AI (5 minutes)

### Online Mode (ElevenLabs):

**Prerequisites:**
- Network enabled
- Microphone permission granted

**Steps:**
1. Navigate to HomeDashboard
2. Tap Voice AI button
3. Say: "Hello, can you hear me?"
4. Wait for AI response

**Success:** You hear AI voice responding clearly.

**Failure:** Check logs for "ElevenLabs authentication failed" or "API key invalid"

---

### Offline Mode (TinyLlama):

**Prerequisites:**
- Airplane mode enabled
- Model files exist (verified in Step 1)

**Steps:**
1. Enable airplane mode
2. Tap Voice AI button  
3. Wait 10-20 seconds for initialization (first time)
4. Say: "Hello, how are you?"
5. Wait for AI response

**Success:** You hear synthesized voice responding (may be slower than online).

**Failure:** Check logs for "Model file not found" or "Failed to load native library"

---

### Network Handoff Test:

**Automated test:**
```bash
# Terminal 2 (while conversation is active)
./test-automation/network-toggle.sh
# Select option: 7 (Test Network Handoff)
```

**Watch for:**
- Smooth transition from online → offline → online
- No crashes
- Conversation continues throughout

---

## Step 5: Test Emergency System (3 minutes)

### Configure Emergency Contacts:

1. Go to Emergency Contacts screen
2. Add at least one contact
3. Save

### Trigger Emergency:

1. From HomeDashboard, tap SOS button
2. Wait for 10-second countdown
3. Let it complete OR tap "I'm OK" to cancel

**Check logs for:**
```
Triggering health emergency
Emergency event created
Starting Phase 1
```

---

## Common Issues & Quick Fixes

### Issue: Google Sign-In Cancels Immediately

**Quick Fix:**
```bash
# 1. Get SHA-1
./gradlew signingReport | grep SHA1

# 2. Add to Firebase Console (see guide)

# 3. Re-download google-services.json

# 4. Clean rebuild
./gradlew clean installDebug
```

→ Full details in `GOOGLE_SIGNIN_DEBUG_GUIDE.md`

---

### Issue: Voice AI Not Working

**Quick Fix:**
```bash
# Check if API keys are set
cat .env | grep ELEVEN

# Verify microphone permission
adb shell dumpsys package com.rescuemate | grep RECORD_AUDIO
```

→ Full details in `VOICE_AI_TESTING_GUIDE.md`

---

### Issue: Model Files Missing

**Quick Fix:**
```bash
# Check if files exist
ls -lh app/src/main/assets/models/
ls -lh app/src/main/assets/model/
```

If missing:
1. Download TinyLlama: `TinyLlama-1.1B-Chat-v0.4.Q4_K_M.gguf` (~600MB)
2. Download Vosk: `vosk-model-small-en-us-0.15` (~40MB)
3. Place in assets
4. Rebuild

---

## Test Automation Scripts

### 1. Setup Verification
```bash
./test-automation/verify-setup.sh
```
Checks all configurations before testing.

### 2. Log Collection
```bash
./test-automation/extract-logs.sh
```
Captures and analyzes app logs.

### 3. Network Toggle
```bash
./test-automation/network-toggle.sh
```
Automates network state changes for testing.

### 4. Automated Tests
```bash
./test-automation/run-tests.sh
```
Runs full automated test suite.

---

## Documentation Reference

| Document | Purpose |
|----------|---------|
| `COMPREHENSIVE_TESTING_PLAN.md` | Complete testing strategy and scenarios |
| `GOOGLE_SIGNIN_DEBUG_GUIDE.md` | **Fix Google Sign-In Status Code 10** |
| `VOICE_AI_TESTING_GUIDE.md` | Detailed voice AI testing procedures |
| `TEST_EXECUTION_CHECKLIST.md` | Printable checklist for manual testing |
| `test-automation/README.md` | Test script usage guide |

---

## Recommended Testing Order

### Priority 1: Critical Features
1. ✅ Google Sign-In (your reported issue)
2. ✅ Voice AI Online (ElevenLabs)
3. ✅ Emergency System

### Priority 2: Advanced Features
4. ⬜ Voice AI Offline (TinyLlama)
5. ⬜ Network Handoff
6. ⬜ Emergency Queuing

### Priority 3: Edge Cases
7. ⬜ Extended offline performance
8. ⬜ Multiple sign-in methods
9. ⬜ Error recovery scenarios

---

## Getting Help

**For Google Sign-In issues:**
→ `GOOGLE_SIGNIN_DEBUG_GUIDE.md` (comprehensive SHA-1 and config fix)

**For Voice AI issues:**
→ `VOICE_AI_TESTING_GUIDE.md` (model setup and troubleshooting)

**For general testing:**
→ `COMPREHENSIVE_TESTING_PLAN.md` (full test scenarios)

**For automation:**
→ `test-automation/README.md` (script usage)

---

## Next Steps

After completing quick tests:

1. **Document Results:**
   - Use `TEST_EXECUTION_CHECKLIST.md`
   - Fill in all test results
   - Note any issues found

2. **Collect Logs:**
   ```bash
   ./test-automation/extract-logs.sh
   # Logs saved to test-logs/
   ```

3. **Fix Critical Issues:**
   - Focus on Google Sign-In first (if Status Code 10)
   - Then verify voice AI functionality
   - Test emergency system last

4. **Full Test Suite:**
   ```bash
   ./test-automation/run-tests.sh
   # Generates complete test report
   ```

---

## Success Indicators

**You're ready for production if:**
- ✅ Google Sign-In works consistently
- ✅ ElevenLabs voice AI responds within 5 seconds
- ✅ TinyLlama offline mode initializes and responds
- ✅ Network handoff happens smoothly (< 3 seconds)
- ✅ Emergency system triggers and notifies contacts
- ✅ No crashes during normal usage
- ✅ All critical permissions granted and working

---

## Test Summary Template

```
Date: [Today's Date]
Tester: [Your Name]
Device: [Device Model]

GOOGLE SIGN-IN:        ⬜ PASS  ⬜ FAIL  ⬜ NEEDS FIX
VOICE AI (ONLINE):     ⬜ PASS  ⬜ FAIL  ⬜ NEEDS FIX
VOICE AI (OFFLINE):    ⬜ PASS  ⬜ FAIL  ⬜ NEEDS FIX
NETWORK HANDOFF:       ⬜ PASS  ⬜ FAIL  ⬜ NEEDS FIX
EMERGENCY SYSTEM:      ⬜ PASS  ⬜ FAIL  ⬜ NEEDS FIX

CRITICAL ISSUES:
[List any blocking issues]

RECOMMENDATIONS:
[List recommended fixes or improvements]
```

---

**Happy Testing! 🚀**

**For immediate support:** Review the specific guide for the feature you're testing.

**Last Updated:** November 28, 2025

