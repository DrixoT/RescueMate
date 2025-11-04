# 📋 RescueMate Deployment Checklist

Use this checklist to ensure successful deployment to Android Virtual Device.

---

## ☑️ PRE-DEPLOYMENT CHECKLIST

### Environment Setup
- [ ] Java JDK 17+ installed
- [ ] JAVA_HOME environment variable set
- [ ] Android SDK installed
- [ ] Node.js 18+ installed
- [ ] Android Studio installed (for AVD)

### Project Setup
- [ ] `local.properties` exists with correct SDK path
- [ ] `.env` file created (if using environment variables)
- [ ] API keys configured (Google Maps, etc.)

---

## ☑️ FIRST-TIME SETUP

### Step 1: Download Dependencies
- [ ] Run `setup_project.bat`
- [ ] Verify gradle-wrapper.jar downloaded
- [ ] Verify npm packages installed
- [ ] No errors displayed

### Step 2: Verify Configuration
- [ ] Check `local.properties` has correct SDK path
- [ ] Verify Android SDK version 34 installed
- [ ] Check build tools are available

---

## ☑️ BUILD PROCESS

### Clean Build
- [ ] Run `clean_build.bat`
- [ ] Verify build directories deleted
- [ ] Verify .gradle cache cleared

### Build Android App
- [ ] Run `build_and_test.bat` OR `gradlew.bat assembleDebug`
- [ ] Build completes without errors
- [ ] APK file created at `app\build\outputs\apk\debug\app-debug.apk`
- [ ] APK file size is reasonable (> 5MB)

### Build Web App (Optional)
- [ ] Run `npm run build`
- [ ] Build completes without errors
- [ ] `dist\` directory created
- [ ] index.html exists in dist folder

---

## ☑️ ANDROID VIRTUAL DEVICE (AVD) SETUP

### Create AVD (First Time)
- [ ] Open Android Studio
- [ ] Go to Tools → Device Manager
- [ ] Click "Create Device"
- [ ] Select Phone → Pixel 6 (or similar)
- [ ] Select System Image → API 34 (Android 14)
- [ ] Click "Finish"

### Start AVD
- [ ] Open Android Studio
- [ ] Open Device Manager
- [ ] Click ▶️ on your AVD
- [ ] Wait for emulator to fully boot
- [ ] Home screen is visible

---

## ☑️ DEPLOYMENT TO AVD

### Method 1: Using Gradle (Recommended)
- [ ] Ensure AVD is running
- [ ] Run `gradlew.bat installDebug`
- [ ] Installation completes successfully
- [ ] App icon appears in AVD app drawer

### Method 2: Drag and Drop
- [ ] Locate `app\build\outputs\apk\debug\app-debug.apk`
- [ ] Drag APK file onto AVD window
- [ ] Wait for installation
- [ ] App icon appears in AVD app drawer

### Method 3: Using ADB
- [ ] Run `adb devices` to verify connection
- [ ] Run `adb install app\build\outputs\apk\debug\app-debug.apk`
- [ ] Installation succeeds
- [ ] App icon appears

---

## ☑️ POST-DEPLOYMENT TESTING

### Launch Application
- [ ] Find "RescueMate" icon in app drawer
- [ ] Tap to launch
- [ ] App opens without crashes
- [ ] Splash/onboarding screen appears

### Test Core Features
- [ ] Onboarding flow works
- [ ] Sign in screen displays
- [ ] Sign up screen displays
- [ ] Navigation works between screens
- [ ] Buttons are responsive

### Test Permissions
- [ ] Location permission prompt appears when needed
- [ ] Bluetooth permission prompt appears when needed
- [ ] Permissions can be granted
- [ ] App handles permission denial gracefully

### Test Location Features
- [ ] Open Live Location screen
- [ ] Map displays correctly
- [ ] Current location shown (if granted)
- [ ] Location tracking works

### Test Bluetooth Features
- [ ] Open Settings → Connect Smartwatch
- [ ] Bluetooth pairing screen displays
- [ ] Can scan for devices (if granted)
- [ ] Shows paired devices

### Test Emergency Contacts
- [ ] Open Emergency Contacts screen
- [ ] Sample contacts display
- [ ] Add Contact button works
- [ ] Navigation back works

### Test Settings
- [ ] Open Settings screen
- [ ] All toggles work
- [ ] Theme settings display
- [ ] Navigation works

---

## ☑️ ERROR HANDLING

### If Build Fails
- [ ] Run `clean_build.bat`
- [ ] Check error messages in console
- [ ] Verify JAVA_HOME is set
- [ ] Verify SDK path in local.properties
- [ ] Run `setup_project.bat` again
- [ ] Try building again

### If Installation Fails
- [ ] Verify AVD is running
- [ ] Run `adb devices` to check connection
- [ ] Restart AVD
- [ ] Try installation again
- [ ] Check logcat for errors

### If App Crashes
- [ ] Check Android Studio Logcat
- [ ] Look for red error messages
- [ ] Note the error stack trace
- [ ] Check if permissions are required
- [ ] Reinstall the app

---

## ☑️ VERIFICATION CHECKLIST

### Build Artifacts
- [ ] APK exists: `app\build\outputs\apk\debug\app-debug.apk`
- [ ] APK size > 5MB
- [ ] Web build exists: `dist\index.html`

### Code Quality
- [ ] No compilation errors
- [ ] All imports resolved
- [ ] No red underlines in IDE
- [ ] All dependencies installed

### Functionality
- [ ] App launches
- [ ] No crashes on startup
- [ ] All screens accessible
- [ ] Permissions work
- [ ] Location features work
- [ ] Bluetooth features work

---

## ☑️ PRODUCTION READINESS (Future)

### Security
- [ ] API keys restricted in Cloud Console
- [ ] ProGuard enabled for release build
- [ ] Code obfuscation enabled
- [ ] Security audit completed

### Performance
- [ ] App startup time < 3 seconds
- [ ] Smooth scrolling
- [ ] No memory leaks
- [ ] Battery usage optimized

### Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed
- [ ] Tested on multiple devices

### Release
- [ ] Version number updated
- [ ] Release notes written
- [ ] APK signed with release key
- [ ] Tested release build

---

## 🎯 QUICK DEPLOYMENT STEPS

For fastest deployment:

```batch
1. clean_build.bat
2. build_and_test.bat
3. [Start AVD in Android Studio]
4. gradlew.bat installDebug
5. [Launch app in AVD]
```

---

## 📞 COMMON ISSUES & SOLUTIONS

| Issue | Solution |
|-------|----------|
| "JAVA_HOME not set" | Set JAVA_HOME to JDK path |
| "SDK not found" | Update local.properties |
| "Gradle sync failed" | Run setup_project.bat |
| "Build failed" | Run clean_build.bat then rebuild |
| "Installation failed" | Restart AVD and try again |
| "App crashes" | Check Logcat for errors |
| "Permissions not working" | Grant permissions in AVD settings |

---

## ✅ COMPLETION CHECKLIST

- [ ] All build steps completed
- [ ] APK successfully created
- [ ] App installed on AVD
- [ ] App launches without errors
- [ ] Core features tested and working
- [ ] No critical bugs found

**When all items are checked, deployment is COMPLETE! 🎉**

---

**Last Updated:** November 4, 2025  
**Version:** 2.0.0

