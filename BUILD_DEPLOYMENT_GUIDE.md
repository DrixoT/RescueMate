 # 🚀 RescueMate Build & Deployment Guide

## Quick Start

### First Time Setup
```batch
# Run this once to set up the project
setup_project.bat
```

### Build Everything
```batch
# Clean and build both Android and Web apps
build_and_test.bat
```

---

## Prerequisites

### Required Software
- ✅ **Java Development Kit (JDK) 17 or higher**
- ✅ **Android SDK** (via Android Studio)
- ✅ **Node.js 18+** and npm
- ✅ **Git** (for version control)

### Optional but Recommended
- Android Studio (for AVD and debugging)
- VS Code or IntelliJ IDEA

---

## Build Instructions

### 🤖 Android Application

#### Option 1: Using Build Script (Recommended)
```batch
build_and_test.bat
```

#### Option 2: Manual Build
```batch
# Clean previous builds
clean_build.bat

# Build debug APK
gradlew.bat assembleDebug

# Build release APK (for production)
gradlew.bat assembleRelease
```

**Output Location:**
- Debug: `app\build\outputs\apk\debug\app-debug.apk`
- Release: `app\build\outputs\apk\release\app-release.apk`

---

### 🌐 Web Application

#### Development Server
```batch
npm run dev
```
Access at: http://localhost:5173

#### Production Build
```batch
npm run build
```
Output: `dist\` directory

---

## Deploy to Android Virtual Device

### Method 1: Using Gradle
```batch
# Make sure AVD is running
gradlew.bat installDebug
```

### Method 2: Drag and Drop
1. Start Android Studio
2. Open AVD Manager (Tools → Device Manager)
3. Start an emulator
4. Drag `app-debug.apk` onto the emulator window

### Method 3: Using ADB
```batch
adb install app\build\outputs\apk\debug\app-debug.apk
```

---

## Deploy to Physical Device

### Enable Developer Mode
1. Go to **Settings → About Phone**
2. Tap **Build Number** 7 times
3. Enable **USB Debugging** in Developer Options

### Install App
```batch
# Connect device via USB
adb devices  # Verify device is connected
gradlew.bat installDebug
```

---

## Troubleshooting

### ❌ "gradle-wrapper.jar not found"
**Solution:**
```batch
setup_project.bat
```

### ❌ "JAVA_HOME is not set"
**Solution:**
1. Install JDK 17 or higher
2. Set JAVA_HOME environment variable:
   ```
   JAVA_HOME=C:\Program Files\Java\jdk-17
   ```

### ❌ "SDK location not found"
**Solution:**
1. Create/edit `local.properties`
2. Add: `sdk.dir=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk`

### ❌ Compilation errors persist
**Solution:**
```batch
# Deep clean
clean_build.bat

# Delete .gradle cache (if needed)
rmdir /s /q .gradle

# Rebuild
build_and_test.bat
```

### ❌ npm errors
**Solution:**
```batch
# Clear npm cache
npm cache clean --force

# Delete node_modules
rmdir /s /q node_modules

# Reinstall
npm install
```

---

## Project Structure

```
RescueMate-2.0/
├── app/                          # Android app source
│   ├── src/main/
│   │   ├── java/com/rescuemate/  # Kotlin source files
│   │   ├── res/                  # Android resources
│   │   └── AndroidManifest.xml   # App manifest
│   └── build.gradle.kts          # App build config
├── src/                          # Web app source
│   ├── components/               # React components
│   ├── services/                 # Services (Voice AI, etc.)
│   ├── App.tsx                   # Main app component
│   └── main.tsx                  # Entry point
├── build.gradle.kts              # Root build config
├── settings.gradle.kts           # Gradle settings
├── package.json                  # npm dependencies
├── vite.config.ts                # Vite configuration
├── tsconfig.json                 # TypeScript config
├── gradlew.bat                   # Gradle wrapper
├── setup_project.bat             # Setup script
├── clean_build.bat               # Clean script
└── build_and_test.bat            # Build script
```

---

## Build Variants

### Debug Build (Development)
- Debuggable
- No code obfuscation
- Logs enabled
```batch
gradlew.bat assembleDebug
```

### Release Build (Production)
- Optimized
- Code obfuscation (ProGuard)
- Logs disabled
```batch
gradlew.bat assembleRelease
```

---

## Testing

### Run Unit Tests
```batch
gradlew.bat test
```

### Run Instrumented Tests
```batch
gradlew.bat connectedAndroidTest
```

### Run Web Tests
```batch
npm test
```

---

## Environment Configuration

### Android
Edit `local.properties`:
```properties
sdk.dir=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

### Web
Create `.env` file:
```env
VITE_API_KEY=your_api_key_here
VITE_ELEVENLABS_API_KEY=your_elevenlabs_key
```

---

## API Keys

⚠️ **Security Warning:** Never commit API keys to version control!

### Google Maps
- Located in: `app/src/main/AndroidManifest.xml`
- Replace placeholder with your key
- Restrict key in Google Cloud Console

### ElevenLabs (Voice AI)
- Add to `.env` file
- Never commit `.env` to Git

---

## Common Commands Reference

```batch
# Setup
setup_project.bat                  # Initial setup

# Clean
clean_build.bat                    # Clean build artifacts
gradlew.bat clean                  # Gradle clean

# Build
gradlew.bat assembleDebug          # Build debug APK
gradlew.bat assembleRelease        # Build release APK
npm run build                      # Build web app

# Install
gradlew.bat installDebug           # Install debug to device
gradlew.bat installRelease         # Install release to device

# Test
gradlew.bat test                   # Run unit tests
gradlew.bat lint                   # Run code quality checks

# Web Development
npm run dev                        # Start dev server
npm run build                      # Build for production
```

---

## Performance Tips

### Faster Builds
1. Enable Gradle daemon (default in modern versions)
2. Increase heap size in `gradle.properties`:
   ```properties
   org.gradle.jvmargs=-Xmx4096m
   ```
3. Enable parallel builds:
   ```properties
   org.gradle.parallel=true
   ```

### Reduce APK Size
1. Enable ProGuard for release builds
2. Use APK splits for different architectures
3. Remove unused resources

---

## CI/CD Integration

### GitHub Actions Example
```yaml
- name: Build Android
  run: |
    chmod +x gradlew
    ./gradlew assembleDebug

- name: Build Web
  run: |
    npm install
    npm run build
```

---

## Support & Documentation

- **Technical Documentation:** `TECHNICAL_DOCUMENTATION.md`
- **Validation Report:** `PROJECT_VALIDATION_REPORT.md`
- **Security Guide:** `SECURITY_IMPLEMENTATION_COMPLETE.md`
- **Code Review:** `CODE_REVIEW.md`

---

## License

Copyright © 2025 RescueMate. All rights reserved.

---

**Last Updated:** November 4, 2025  
**Version:** 2.0.0

