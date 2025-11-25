# RescueMate 2.0 - Testing Checklist

**Generated:** 2025-11-23  
**Purpose:** Comprehensive testing checklist for account creation, sign-in, and navigation flows after debugging fixes

---

## Pre-Testing Setup

### Required Configuration
- [ ] Firebase Authentication is enabled in Firebase Console
- [ ] Google Sign-In is configured with proper OAuth credentials
- [ ] `google-services.json` is present in `app/` directory
- [ ] `.env` file exists with all required API keys
- [ ] App is built in debug mode for testing
- [ ] Logcat is enabled and visible for monitoring logs

### Test Device Requirements
- [ ] Android device or emulator (API 26+)
- [ ] Internet connection available
- [ ] Permissions can be granted (location, notifications, etc.)

---

## Test Suite 1: Account Creation Flow

### 1.1 Email/Password Sign Up (Happy Path)
**Test Steps:**
1. Launch app (should show Onboarding screen)
2. Tap "Get Started" → Navigate to Sign In screen
3. Tap "Create New Account"
4. Fill in the sign-up form:
   - Email: `test@example.com`
   - Password: `TestPassword123!`
   - Confirm Password: `TestPassword123!`
   - Name: `Test User`
   - Date of Birth: `15/01/1990`
   - Gender: Select any
   - Phone: `+1234567890`
   - (Optional) Fill medical info
5. Tap "Complete Registration"

**Expected Results:**
- [ ] Loading indicator appears
- [ ] Console logs show: "🚀 Starting Firebase account creation..."
- [ ] Console logs show: "✅ Firebase account created successfully"
- [ ] Console logs show: "💾 Saving user profile to SharedPreferences"
- [ ] Console logs show: "✅ ALL DATA SAVED SUCCESSFULLY"
- [ ] Toast message: "Registration complete!"
- [ ] Navigates to Setup Wizard automatically
- [ ] No crashes occur

**Failure Scenarios to Test:**
- [ ] Try with existing email → Should show "This email is already registered"
- [ ] Try with weak password → Should show "Password is too weak"
- [ ] Try with invalid email format → Should show "Invalid email format"
- [ ] Test with network disabled → Should show "Network error"

### 1.2 Setup Wizard Completion
**Test Steps:**
1. After sign-up, should be on Setup Wizard
2. Step 1: Verify name is pre-filled, click "Next"
3. Step 2: Fill Date of Birth and Gender, click "Next"
4. Step 3: Select Blood Type (required), add conditions/allergies (optional), click "Next"
5. Step 4: Add emergency contact (name, phone, relationship), click "Finish"

**Expected Results:**
- [ ] Each step validates before allowing "Next"
- [ ] Console logs show: "🚀 Starting setup wizard completion"
- [ ] Console logs show: "💾 Saving user profile locally..."
- [ ] Console logs show: "✅ User profile saved locally"
- [ ] Console logs show: "☁️ Syncing to Firestore..."
- [ ] Console logs show: "✅ Setup wizard completed successfully"
- [ ] Navigates to Home Dashboard
- [ ] No crashes if Firestore sync fails (shows: "Profile saved locally. Cloud sync will retry later.")

### 1.3 Invalid Input Validation
**Test Steps:**
Test each invalid input:
- [ ] Empty name → Button disabled
- [ ] Name with profanity → Shows error
- [ ] Invalid date format → Shows "Invalid date" error
- [ ] Date in future → Shows "Date must be in the past"
- [ ] Invalid phone format → Shows "Invalid phone format"
- [ ] Passwords don't match → Shows "Passwords do not match"

**Expected Results:**
- [ ] All validation errors display clearly
- [ ] Form submission is blocked until fixed
- [ ] No crashes occur with invalid inputs

---

## Test Suite 2: Sign-In Flows

### 2.1 Google Sign-In
**Test Steps:**
1. Launch app → Sign In screen
2. Tap "Continue with Google"
3. Select Google account
4. Grant permissions if requested

**Expected Results:**
- [ ] Google Sign-In picker appears
- [ ] Console logs show: "📡 Calling Firebase signInWithGoogle..."
- [ ] Console logs show: "✅ Google Sign-In successful"
- [ ] Console logs show: "Login state after Google auth: true"
- [ ] Toast: "Welcome back!"
- [ ] Navigates to Setup Wizard (first time) or Home (returning user)
- [ ] No crashes occur

**Failure Scenarios:**
- [ ] Cancel sign-in → Should return to Sign In screen gracefully
- [ ] No internet → Should show "Network error"
- [ ] Web Client ID missing → Should show "Configuration error"

### 2.2 Apple Sign-In (Mock)
**Test Steps:**
1. On Sign In screen, tap "Continue with Apple"

**Expected Results:**
- [ ] Toast appears: "Apple Sign-In (Demo Mode)"
- [ ] Console logs show: "🍎 Apple Sign-In clicked (Mock)"
- [ ] Console logs show: "🔄 Starting mock sign-in for: apple_user@rescuemate.com"
- [ ] Console logs show: "✅ Mock credentials saved"
- [ ] Console logs show: "✅ Mock sign-in successful, navigating..."
- [ ] Navigates to Setup Wizard or Home
- [ ] No crashes occur

### 2.3 Email Sign-In (Returning User)
**Test Steps:**
1. On Sign In screen, tap "Continue with Email"
2. Enter previously registered email and password
3. Tap "Sign In"

**Expected Results:**
- [ ] Loading indicator appears
- [ ] Sign-in succeeds
- [ ] Navigates to Home Dashboard (if setup complete)
- [ ] No crashes occur

**Failure Scenarios:**
- [ ] Wrong password → Should show "Invalid credentials"
- [ ] Email not registered → Should show "User not found"

### 2.4 Phone Sign-In
**Test Steps:**
1. On Sign In screen, tap "Continue with Phone"
2. Enter phone number
3. Tap "Send Code"
4. Enter verification code
5. Tap "Verify"

**Expected Results:**
- [ ] SMS verification code sent
- [ ] Code verification succeeds
- [ ] Navigates to Setup Wizard or Home
- [ ] No crashes occur

---

## Test Suite 3: Navigation Flow

### 3.1 Navigation State Determination
**Test Steps:**
1. Clear app data
2. Launch app

**Expected Results:**
- [ ] Console logs show: "🧭 Determining navigation start destination"
- [ ] Console logs show all user state flags (isLoggedIn, isSetupComplete, isOnboardingComplete)
- [ ] Console logs show: "🎯 Starting at: [correct route]"
- [ ] Starts at correct screen based on user state:
  - New user → Onboarding
  - Onboarding complete → Sign In
  - Logged in but setup incomplete → Setup Wizard (after login)
  - Fully set up → Home Dashboard

### 3.2 Navigation After Sign-Up
**Test Steps:**
1. Complete sign-up flow
2. Observe navigation

**Expected Results:**
- [ ] After sign-up → Setup Wizard
- [ ] Console logs show: "🧭 Navigating to Setup Wizard"
- [ ] After setup complete → Home Dashboard
- [ ] Console logs show: "🧭 Setup wizard completed, navigating to Home..."

### 3.3 Navigation Error Recovery
**Test Steps:**
Simulate navigation errors by:
1. Force-stopping during navigation
2. Corrupting SharedPreferences

**Expected Results:**
- [ ] App doesn't crash
- [ ] Fallback routes are used
- [ ] Console logs show: "❌ Navigation error... defaulting to [fallback]"
- [ ] User can still proceed

### 3.4 Back Navigation
**Test Steps:**
Test back button on each screen:
- [ ] Email Login → Back to Sign In
- [ ] Phone Login → Back to Sign In
- [ ] Sign Up → Back to Sign In
- [ ] Add Contact → Back to Emergency Contacts
- [ ] All settings screens → Back properly

**Expected Results:**
- [ ] All back navigations work correctly
- [ ] No navigation loops
- [ ] No crashes on back button

---

## Test Suite 4: Error Handling

### 4.1 Network Errors
**Test Steps:**
1. Enable airplane mode
2. Try to sign up
3. Try to sign in
4. Try to complete setup wizard

**Expected Results:**
- [ ] Sign-up shows: "Network error. Please check your internet connection."
- [ ] Sign-in shows: "Network error. Please check your internet connection."
- [ ] Setup wizard saves locally and shows: "Profile saved locally. Cloud sync will retry later."
- [ ] No crashes occur

### 4.2 Database Errors
**Test Steps:**
1. Check logs for database operations
2. Verify no crashes from database parsing

**Expected Results:**
- [ ] Database operations log: "📖 Parsing emergency event from database cursor..."
- [ ] If parsing fails, logs show: "⚠️ Returning fallback emergency event to prevent crash"
- [ ] App continues functioning even with corrupted data
- [ ] No crashes from database operations

### 4.3 Null Pointer Prevention
**Test Steps:**
1. Test with missing user data
2. Test with null Firebase user
3. Test with missing preferences

**Expected Results:**
- [ ] All nullable values handled with safe calls (?.)
- [ ] Default values provided where necessary
- [ ] Console logs show warnings for missing data
- [ ] No NullPointerException crashes

---

## Test Suite 5: Data Persistence

### 5.1 User Profile Data
**Test Steps:**
1. Complete sign-up and setup
2. Force close app
3. Reopen app

**Expected Results:**
- [ ] User remains logged in
- [ ] User data is preserved
- [ ] Opens directly to Home Dashboard
- [ ] Profile information displays correctly

### 5.2 Medical Information
**Test Steps:**
1. Add medical conditions, allergies, medications
2. Close and reopen app
3. Navigate to profile

**Expected Results:**
- [ ] All medical data is preserved
- [ ] Data displays correctly
- [ ] Can add/edit/remove items

### 5.3 Emergency Contacts
**Test Steps:**
1. Add emergency contact in setup wizard
2. Add more contacts in app
3. Close and reopen app

**Expected Results:**
- [ ] All contacts are preserved
- [ ] Contacts display correctly
- [ ] Can add/delete contacts

---

## Test Suite 6: Edge Cases

### 6.1 Rapid Button Clicks
**Test Steps:**
1. Rapidly click "Complete Registration" button
2. Rapidly click navigation buttons

**Expected Results:**
- [ ] Button disabled during loading
- [ ] No duplicate operations
- [ ] No crashes

### 6.2 App Lifecycle Events
**Test Steps:**
1. Start sign-up process
2. Put app in background (Home button)
3. Return to app
4. Complete sign-up

**Expected Results:**
- [ ] Form data preserved
- [ ] Process continues normally
- [ ] No crashes

### 6.3 Permission Denied
**Test Steps:**
1. Deny permissions when requested
2. Try to use features requiring permissions

**Expected Results:**
- [ ] App doesn't crash
- [ ] Clear error messages shown
- [ ] Guidance to enable permissions

---

## Test Suite 7: Logging Verification

### 7.1 Comprehensive Logging Check
**Monitor Logcat for these log patterns:**

**Sign-Up Flow:**
```
SignUpScreen: ════════════════════════════════════════
SignUpScreen: 📝 Starting account creation process
SignUpScreen: ✅ All validations passed
SignUpScreen: 🚀 Starting Firebase account creation...
SignUpScreen: ✅ Firebase account created successfully
SignUpScreen: ✅ ALL DATA SAVED SUCCESSFULLY
SignUpScreen: 🎯 Navigating to Setup Wizard
```

**Setup Wizard:**
```
SetupWizardScreen: ════════════════════════════════════════
SetupWizardScreen: 🚀 Starting setup wizard completion
SetupWizardScreen: ✅ User profile saved locally
SetupWizardScreen: ☁️ Syncing to Firestore...
SetupWizardScreen: ✅ Setup wizard completed successfully
```

**Navigation:**
```
RescueMateNavigation: ════════════════════════════════════════
RescueMateNavigation: 🧭 Determining navigation start destination
RescueMateNavigation: 🎯 Starting at: [route]
```

**Database Operations:**
```
EmergencyDatabaseHelper: 📖 Parsing emergency event from database cursor...
EmergencyDatabaseHelper: ✅ Successfully parsed emergency event: [id]
```

### 7.2 Error Logging Check
**Verify error logs appear for failures:**
```
[TAG]: ❌ [Error description]
[TAG]: ❌❌❌ CRITICAL: [Critical error]
[TAG]:    Exception type: [exception class]
[TAG]:    Exception message: [message]
```

---

## Test Results Summary

### Test Execution Date: _______________

### Overall Results
- [ ] All critical paths working
- [ ] No crashes during normal operation
- [ ] Error handling working correctly
- [ ] Navigation flows properly
- [ ] Data persistence working
- [ ] Logging comprehensive and helpful

### Issues Found
| Issue # | Severity | Description | Status |
|---------|----------|-------------|--------|
| 1 | | | |
| 2 | | | |
| 3 | | | |

### Sign-Off
- **Tested By:** _______________
- **Date:** _______________
- **Build Version:** _______________
- **Device/Emulator:** _______________
- **Android Version:** _______________

---

## Quick Smoke Test (5 minutes)

For rapid verification after changes:
1. [ ] Launch app → Onboarding → Sign In
2. [ ] Create account → Setup Wizard → Home
3. [ ] Sign out and sign in again
4. [ ] Check logs for errors
5. [ ] No crashes observed

---

## Automated Testing Recommendations

### Unit Tests to Add
```kotlin
// toryTest.kt
- testSignUpWithEmail_Success()
- testSignUpWithEmail_DuplicateUser()
- testSignUpWithEmail_NetworkError()
- testSignInWithGoogle_Success()
- testUpdateLocalUser_WithNullValues()

// UserPreferencesTest.kt
- testSaveUserProfile_ValidData()
- testSaveUserProfile_BlankName_ThrowsException()
- testIsLoggedIn_AfterSaveCredentials()

// EmergencyDatabaseHelperTest.kt
- testParseEmergencyEvent_ValidData()
- testParseEmergencyEvent_CorruptedData_ReturnsFallback()
- testParseEmergencyEvent_NullData_ReturnsDefaults()
```

### UI Tests to Add
```kotlin
// SignUpFlowTest.kt
- testCompleteSignUpFlow_Success()
- testSignUp_InvalidEmail_ShowsError()
- testSignUp_NetworkError_ShowsMessage()

// NavigationTest.kt
- testNavigation_NewUser_StartsAtOnboarding()
- testNavigation_AfterSignUp_GoesToSetupWizard()
- testNavigation_ReturningUser_GoesToHome()
```

---

## Notes for Developers

### Key Improvements Made
1. ✅ Comprehensive logging throughout authentication flow
2. ✅ Null safety checks in all critical paths
3. ✅ Database parsing with fallback for corrupted data
4. ✅ Navigation error handling with fallback routes
5. ✅ User-friendly error messages
6. ✅ Apple Sign-In mock properly implemented

### Known Limitations
- Apple Sign-In is mock implementation (not fully integrated)
- Firestore sync failures are handled gracefully but don't auto-retry
- Some medical data fields are optional and may be empty

### Debugging Tips
- Use `adb logcat | grep -E "SignUpScreen|SignInScreen|SetupWizard|Navigation|Emergency"` to filter relevant logs
- Check `SharedPreferences` with: `adb shell run-as com.rescuemate cat /data/data/com.rescuemate/shared_prefs/rescuemate_user_prefs.xml`
- Clear app data for fresh test: `adb shell pm clear com.rescuemate`

---

**End of Testing Checklist**
