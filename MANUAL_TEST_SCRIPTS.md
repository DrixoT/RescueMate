# Manual Test Scripts - RescueMate Android Application

## Test Environment
- **Device:** Android 8.0+ (API 26+)
- **Build:** Debug
- **Prerequisites:** `.env` file configured with valid API keys

---

## Voice AI Test Suite

### Test 1: Voice AI Setup & Configuration
**Objective:** Verify voice setup flow works correctly

**Steps:**
1. Open RescueMate app
2. Navigate to Settings > Voice AI Setup
3. Select a voice (Sam or Pete)
4. Tap "Play" button to preview voice
5. Tap "Complete Setup"

**Expected Results:**
- ✅ Voice preview plays audio successfully
- ✅ Selected voice is saved
- ✅ No crashes or errors
- ✅ Returns to previous screen

**Pass/Fail:** _____

---

### Test 2: Start Voice Conversation
**Objective:** Verify voice conversation can be started from home dashboard

**Steps:**
1. From Home Dashboard, tap the main SOS/Shield button once
2. Grant microphone permission if prompted
3. Observe button state changes

**Expected Results:**
- ✅ Microphone permission requested (first time)
- ✅ Button shows "Connected" state
- ✅ Audio level visualization appears
- ✅ Status indicator shows "Listening"

**Pass/Fail:** _____

---

### Test 3: Voice Conversation - Speaking
**Objective:** Verify AI responds to user speech

**Steps:**
1. Start voice conversation (Test 2)
2. Speak into microphone: "Hello, can you hear me?"
3. Wait for AI response
4. Observe status changes

**Expected Results:**
- ✅ Status changes to "AI is speaking"
- ✅ Audio plays from speaker
- ✅ Border pulses with audio level
- ✅ Returns to "Listening" after response

**Pass/Fail:** _____

---

### Test 4: End Voice Conversation
**Objective:** Verify conversation can be ended cleanly

**Steps:**
1. Start voice conversation
2. Tap SOS button again
3. Observe state changes

**Expected Results:**
- ✅ Conversation ends immediately
- ✅ Button returns to idle state
- ✅ No audio continues playing
- ✅ No memory leaks

**Pass/Fail:** _____

---

### Test 5: Voice Conversation - No API Key
**Objective:** Verify graceful handling of missing API key

**Steps:**
1. Remove ELEVEN_API_KEY from `.env`
2. Rebuild app
3. Try to start voice conversation

**Expected Results:**
- ✅ Error dialog appears
- ✅ Message: "Voice AI is not configured..."
- ✅ No crash
- ✅ User can dismiss and continue

**Pass/Fail:** _____

---

## Emergency Services Test Suite

### Test 6: Start Health Monitoring
**Objective:** Verify health monitoring service starts

**Steps:**
1. From Home Dashboard
2. Tap "Start Monitoring" button
3. Grant required permissions
4. Observe monitoring status

**Expected Results:**
- ✅ Service starts successfully
- ✅ Notification appears in status bar
- ✅ Heart rate readings appear
- ✅ "Monitoring Active" status shown

**Pass/Fail:** _____

---

### Test 7: Stop Health Monitoring
**Objective:** Verify monitoring can be stopped

**Steps:**
1. Start monitoring (Test 6)
2. Tap "Stop Monitoring" button
3. Observe status changes

**Expected Results:**
- ✅ Service stops
- ✅ Notification disappears
- ✅ Heart rate readings stop
- ✅ "Monitoring Inactive" status shown

**Pass/Fail:** _____

---

### Test 8: Manual Emergency Trigger (Hold)
**Objective:** Verify manual emergency can be triggered by holding SOS button

**Prerequisites:** At least one emergency contact configured

**Steps:**
1. From Home Dashboard
2. Press and hold SOS button for 4 seconds
3. Observe hold animation (circular progress)
4. Release before complete OR hold until complete

**Expected Results:**
- ✅ Circular progress indicator appears
- ✅ Haptic feedback on press
- ✅ If held to completion: Confirmation dialog appears
- ✅ If released early: No emergency triggered

**Pass/Fail:** _____

---

### Test 9: Emergency Contacts Management
**Objective:** Verify emergency contacts can be added/removed

**Steps:**
1. Navigate to Emergency Contacts screen
2. Tap "Add Contact"
3. Fill in contact details
4. Save contact
5. Verify contact appears in list
6. Delete contact

**Expected Results:**
- ✅ Add contact form works
- ✅ Contact saved successfully
- ✅ Contact appears in list
- ✅ Delete removes contact
- ✅ Changes persist after app restart

**Pass/Fail:** _____

---

### Test 10: No Emergency Contacts Warning
**Objective:** Verify warning when no contacts configured

**Steps:**
1. Delete all emergency contacts
2. Try to trigger manual emergency (hold SOS button)

**Expected Results:**
- ✅ Warning appears
- ✅ Emergency not triggered
- ✅ User directed to add contacts

**Pass/Fail:** _____

---

## Core App Functionality Tests

### Test 11: Onboarding Flow
**Objective:** Verify onboarding works for new users

**Steps:**
1. Clear app data
2. Launch app
3. Complete onboarding steps
4. Reach home dashboard

**Expected Results:**
- ✅ Onboarding screens appear
- ✅ Can proceed through all steps
- ✅ Reaches home dashboard
- ✅ Onboarding doesn't show again

**Pass/Fail:** _____

---

### Test 12: Sign In / Sign Up
**Objective:** Verify authentication flow

**Steps:**
1. Clear app data
2. Complete onboarding
3. Sign up with email/password
4. Sign out
5. Sign in again

**Expected Results:**
- ✅ Sign up creates account
- ✅ Can sign out
- ✅ Can sign in with same credentials
- ✅ User remains logged in after app restart

**Pass/Fail:** _____

---

### Test 13: Navigation
**Objective:** Verify all navigation paths work

**Steps:**
1. From Home, navigate to:
   - Emergency Contacts
   - Live Location
   - Settings
   - User Profile
2. Use back button to return

**Expected Results:**
- ✅ All screens load correctly
- ✅ Back button works everywhere
- ✅ No navigation stack issues
- ✅ No crashes

**Pass/Fail:** _____

---

### Test 14: Live Location Tracking
**Objective:** Verify location tracking works

**Steps:**
1. Navigate to Live Location screen
2. Grant location permission if needed
3. Observe map and current location

**Expected Results:**
- ✅ Map loads
- ✅ Current location marker appears
- ✅ Address geocoded correctly
- ✅ Accuracy displayed

**Pass/Fail:** _____

---

### Test 15: Settings Persistence
**Objective:** Verify settings are saved

**Steps:**
1. Navigate to Settings
2. Change various settings
3. Close app completely
4. Reopen app
5. Check settings

**Expected Results:**
- ✅ Settings saved on change
- ✅ Settings persist after restart
- ✅ No data loss

**Pass/Fail:** _____

---

## Performance Tests

### Test 16: Memory Usage During Voice Conversation
**Objective:** Verify no memory leaks during extended conversation

**Steps:**
1. Start voice conversation
2. Keep conversation active for 5 minutes
3. End conversation
4. Check memory usage in Android Studio Profiler

**Expected Results:**
- ✅ Memory usage stable
- ✅ No continuous growth
- ✅ Memory released after end
- ✅ No crashes

**Pass/Fail:** _____

---

### Test 17: Battery Usage During Monitoring
**Objective:** Verify reasonable battery consumption

**Steps:**
1. Start health monitoring
2. Let run for 30 minutes
3. Check battery usage in Settings > Battery

**Expected Results:**
- ✅ Battery usage < 5% per hour
- ✅ Wake lock released properly
- ✅ No excessive CPU usage

**Pass/Fail:** _____

---

## Error Handling Tests

### Test 18: No Network Connection
**Objective:** Verify graceful handling of network errors

**Steps:**
1. Enable airplane mode
2. Try to start voice conversation
3. Try to trigger emergency (with backend configured)

**Expected Results:**
- ✅ Voice AI shows connection error
- ✅ Emergency queues for later
- ✅ Fallback SMS attempted
- ✅ User informed of network issue

**Pass/Fail:** _____

---

### Test 19: Permission Denial Handling
**Objective:** Verify app handles denied permissions

**Steps:**
1. Deny microphone permission
2. Try to start voice conversation
3. Deny location permission
4. Try to trigger emergency

**Expected Results:**
- ✅ Clear error messages shown
- ✅ User directed to grant permissions
- ✅ No crashes
- ✅ Can retry after granting

**Pass/Fail:** _____

---

### Test 20: App Backgrounding
**Objective:** Verify services continue in background

**Steps:**
1. Start health monitoring
2. Start voice conversation
3. Press home button (background app)
4. Wait 1 minute
5. Return to app

**Expected Results:**
- ✅ Monitoring continues in background
- ✅ Voice conversation may end (expected)
- ✅ App state preserved
- ✅ No crashes on resume

**Pass/Fail:** _____

---

## Test Summary

**Total Tests:** 20
**Passed:** _____
**Failed:** _____
**Skipped:** _____

**Overall Pass Rate:** _____% 

**Critical Issues Found:**
1. ___________________________________
2. ___________________________________
3. ___________________________________

**Notes:**
___________________________________________
___________________________________________
___________________________________________

**Tester Name:** _____________________
**Date:** __________________________
**Build Version:** _________________

