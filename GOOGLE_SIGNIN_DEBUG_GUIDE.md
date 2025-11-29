# RescueMate 2.0 - Google Sign-In Debugging Guide

## Critical Issue: Sign-In Gets Cancelled

If Google Sign-In cancels immediately after account selection, this guide will help you diagnose and fix the issue.

---

## Quick Diagnosis

### Check Logcat for Status Code

When sign-in fails, check logcat for these lines:

```
AuthRepository: Google Sign-In ApiException
AuthRepository:    Status Code: <NUMBER>
```

**Common Status Codes:**

| Code | Meaning | Fix |
|------|---------|-----|
| **10** | **DEVELOPER_ERROR** - Configuration issue | [See Fix Below](#fix-for-status-code-10) |
| 7 | NETWORK_ERROR - No internet | Enable WiFi/Data |
| 8 | INTERNAL_ERROR - Google service issue | Retry later |
| 12500 | SIGN_IN_CANCELLED - User cancelled | Normal, user can retry |
| 12501 | SIGN_IN_IN_PROGRESS - Already signing in | Wait for current attempt |

---

## Fix for Status Code 10 (Most Common Issue)

Status Code 10 indicates **DEVELOPER_ERROR** - a mismatch between your app's configuration and Firebase Console settings.

### Root Causes:
1. ❌ SHA-1 fingerprint not added to Firebase Console
2. ❌ Wrong SHA-1 fingerprint (debug vs release keystore)
3. ❌ Wrong Web Client ID in google-services.json
4. ❌ google-services.json not updated after Firebase changes

---

## Step-by-Step Fix

### Step 1: Get Your SHA-1 Fingerprint

**Option A: Using Android Studio**
1. Open Android Studio
2. Click **Gradle** tab (right side)
3. Expand: **RescueMate-2.0 → app → Tasks → android**
4. Double-click **signingReport**
5. Check **Run** tab at bottom
6. Look for: `SHA1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX`
7. Copy the SHA-1 value

**Option B: Using Terminal (if Java is configured)**
```bash
# For Debug Keystore
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey -storepass android -keypass android | grep SHA1

# For Release Keystore (if you have one)
keytool -list -v -keystore /path/to/your/release.keystore \
  -alias your-alias -storepass your-password | grep SHA1
```

**Option C: Using Gradle Task**
```bash
cd /path/to/RescueMate-2.0
./gradlew signingReport
```

Look for output like:
```
Variant: debug
Config: debug
Store: /Users/you/.android/debug.keystore
Alias: androiddebugkey
MD5: XX:XX:XX...
SHA1: 06:39:6E:57:DA:76:D4:07:F5:A8:69:36:E0:DD:DD:4D:AC:F3:58:85
SHA-256: XX:XX:XX...
```

**Copy the SHA1 value** (the one with colons)

---

### Step 2: Add SHA-1 to Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your **RescueMate** project
3. Click **⚙️ Settings** (gear icon) → **Project Settings**
4. Scroll down to **Your apps** section
5. Find your Android app (package: `com.rescuemate`)
6. Click **Add fingerprint** button
7. Paste your SHA-1 fingerprint
8. Click **Save**

**Important:** If you see an existing SHA-1 that doesn't match yours, you have two options:
- Add your new SHA-1 (you can have multiple)
- OR replace the old one if it's incorrect

---

### Step 3: Download Updated google-services.json

1. Still in Firebase Console, same page
2. Click **Download google-services.json** button
3. Save the file
4. Replace the existing file in your project:
   ```
   RescueMate-2.0/app/google-services.json
   ```
5. **Verify the file was replaced** - check file timestamp

---

### Step 4: Verify Web Client ID

The google-services.json should contain a Web Client ID with `client_type: 3`.

**Check manually:**
1. Open `app/google-services.json`
2. Search for `"client_type": 3`
3. You should see something like:
   ```json
   {
     "client_id": "1085665199694-urhu4004f6lq8bb1hgha5vbqh06ubssp.apps.googleusercontent.com",
     "client_type": 3,
     "android_info": {
       "package_name": "com.rescuemate"
     }
   }
   ```
4. The `client_id` value is your Web Client ID

**If Web Client ID is missing:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your project
3. Go to **APIs & Services** → **Credentials**
4. Find **OAuth 2.0 Client IDs**
5. You should see a **Web client (auto created by Google Service)** or similar
6. If not, create one:
   - Click **+ CREATE CREDENTIALS** → **OAuth client ID**
   - Application type: **Web application**
   - Name: "Web client (auto created by Google Service)"
   - Click **Create**
7. Go back to Firebase Console and re-download google-services.json

---

### Step 5: Clean Rebuild

**Critical:** Android Studio caches the google-services.json. You MUST clean rebuild.

```bash
# Terminal
cd /path/to/RescueMate-2.0

# Clean previous builds
./gradlew clean

# Rebuild
./gradlew assembleDebug

# Uninstall old app from device (removes old cached config)
adb uninstall com.rescuemate

# Install fresh
./gradlew installDebug
```

**Or in Android Studio:**
1. **Build** → **Clean Project**
2. Wait for completion
3. **Build** → **Rebuild Project**
4. Manually uninstall app from device
5. **Run** → **Run 'app'**

---

### Step 6: Test Again

1. Launch the app
2. Tap **"Continue with Google"**
3. Select your Google account
4. **Check logcat for:**

**Success:**
```
AuthRepository: Google Sign-In task successful
AuthRepository: Google account retrieved: your@gmail.com
AuthRepository: ID token retrieved successfully
AuthRepository: Firebase authentication successful for user: uid...
```

**Still failing with Status Code 10:**
- Double-check SHA-1 was added correctly
- Verify you downloaded and replaced google-services.json
- Ensure you did clean rebuild
- Try restarting Android Studio
- Check if you're using release build with debug SHA-1 (or vice versa)

---

## Advanced Troubleshooting

### Multiple Build Types

If you have both debug and release builds:

**Debug Build:**
- Uses `~/.android/debug.keystore`
- SHA-1 from debug keystore must be in Firebase

**Release Build:**
- Uses your custom keystore (if configured)
- SHA-1 from release keystore must be in Firebase

**Best Practice:** Add BOTH SHA-1 fingerprints to Firebase Console.

---

### Verify Current Configuration

Create a test file to check what Web Client ID your app is using:

1. Add this temporarily to `AuthRepository.kt` init block:
   ```kotlin
   val webClientId = try {
       context.getString(R.string.default_web_client_id)
   } catch (e: Exception) {
       "NOT_FOUND"
   }
   Log.d("AuthRepository", "Using Web Client ID: ${webClientId.take(50)}...")
   ```

2. Build and run
3. Check logcat for the Web Client ID being used
4. Compare with the one in google-services.json

---

### Check Firebase Authentication Settings

1. Go to Firebase Console
2. Click **Authentication** → **Sign-in method**
3. Ensure **Google** is **Enabled**
4. Click **Google** provider
5. Verify:
   - **Status:** Enabled
   - **Web SDK configuration** → **Web Client ID** matches google-services.json
   - **Support email** is set

---

### Common Mistakes

❌ **Forgot to clean rebuild** - Old google-services.json cached  
✅ Always run `./gradlew clean` after updating google-services.json

❌ **Using release SHA-1 for debug build** - Keystores are different  
✅ Add both debug and release SHA-1 to Firebase

❌ **Downloaded google-services.json but didn't replace file** - Old file still used  
✅ Verify file timestamp after replacing

❌ **Using wrong Firebase project** - Multiple projects with similar names  
✅ Double-check project name in Firebase Console

❌ **Package name mismatch** - google-services.json for different app  
✅ Verify package_name is "com.rescuemate"

---

## Testing Checklist

After applying fixes:

- [ ] SHA-1 added to Firebase Console
- [ ] google-services.json downloaded and replaced
- [ ] File timestamp of google-services.json is recent
- [ ] Ran `./gradlew clean`
- [ ] Ran `./gradlew assembleDebug`
- [ ] Uninstalled old app from device: `adb uninstall com.rescuemate`
- [ ] Installed fresh build: `./gradlew installDebug`
- [ ] Logcat is running and filtered for errors
- [ ] Tapped "Continue with Google"
- [ ] Selected Google account
- [ ] Checked logcat for status code
- [ ] Sign-in completed successfully ✅

---

## Quick Reference Commands

```bash
# Get SHA-1 (using Gradle)
./gradlew signingReport | grep SHA1

# Clean rebuild
./gradlew clean assembleDebug

# Uninstall old app
adb uninstall com.rescuemate

# Install fresh app
./gradlew installDebug

# Monitor logcat for auth events
adb logcat | grep -E "AuthRepository|SignInScreen|GoogleSignIn"

# Check if app is using correct Web Client ID
adb logcat | grep "Using Web Client ID"
```

---

## Still Not Working?

If you've followed all steps and sign-in still fails:

1. **Verify Firebase project:**
   - Check you're in the correct Firebase project
   - Verify package name matches exactly

2. **Check Google Play Services:**
   - Ensure device has Google Play Services installed
   - Update Google Play Services if outdated

3. **Try different account:**
   - Some Google accounts may have restrictions
   - Try with a different Google account

4. **Check device date/time:**
   - Incorrect date/time can cause authentication failures
   - Ensure device time is set to automatic

5. **Enable verbose logging:**
   - Add more log statements in AuthRepository.kt
   - Log every step of the sign-in process

6. **Test on different device:**
   - Try emulator vs physical device
   - Try different Android versions

7. **Contact Firebase Support:**
   - With your project ID
   - SHA-1 fingerprint
   - Complete logcat output

---

## Success Indicators

When everything is configured correctly, you should see:

**Logcat Output:**
```
AuthRepository: Google Sign-In client initialized
AuthRepository: Using Web Client ID: 1085665199694-urhu4004f6lq8bb1hgha5vbqh06ubssp...
SignInScreen: Google Sign-In clicked
AuthRepository: Starting Google Sign-In authentication...
AuthRepository: Google Sign-In task successful
AuthRepository: Google account retrieved: user@gmail.com
AuthRepository: ID token retrieved successfully
AuthRepository: Firebase credential created, signing in...
AuthRepository: Firebase authentication successful for user: AbCdEf123...
SignInScreen: ✅ Google Sign-In successful
```

**Visual Indicators:**
- ✅ Account picker appears smoothly
- ✅ After account selection, brief loading spinner
- ✅ Navigation to HomeDashboard
- ✅ No error toasts
- ✅ User profile data loads

---

**Last Updated:** November 28, 2025  
**For More Help:** See SIGNIN_ERRORS_AND_FIXES.md for all error codes

