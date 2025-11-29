# Test Automation Scripts

This directory contains automated testing scripts for RescueMate 2.0.

## Available Scripts

### 1. verify-setup.sh
**Purpose:** Pre-test configuration verification  
**Usage:** `./test-automation/verify-setup.sh`

Checks:
- ✅ Environment variables (.env file)
- ✅ Firebase configuration (google-services.json)
- ✅ SHA-1 fingerprint matching
- ✅ AI model files (TinyLlama, Vosk)
- ✅ Android development tools (ADB, Gradle, Java)
- ✅ Backend server status
- ✅ Build status and native libraries
- ✅ App installation and permissions

**Run this first before any testing!**

---

### 2. run-tests.sh
**Purpose:** Automated test execution  
**Usage:** `./test-automation/run-tests.sh`

Executes:
- 🔍 Pre-test verification
- 📱 App launch and stability checks
- 🔐 Google Sign-In diagnostics
- 🎙️ Voice AI system checks
- 🚨 Emergency system readiness
- ❌ Error detection

Generates:
- Test results report (markdown)
- Complete logcat output
- Pass/fail summary

---

### 3. extract-logs.sh
**Purpose:** Log collection and analysis  
**Usage:** `./test-automation/extract-logs.sh`

Features:
- Live log monitoring (with filtering)
- Timed log collection (30s, 60s, custom)
- Automatic log analysis
- Component-specific log filtering (Auth, Voice AI, Emergency)
- Error/warning summary

Modes:
1. **Live Monitoring** - Real-time filtered logs (Ctrl+C to stop)
2. **30 Second Collection** - Quick test log capture
3. **60 Second Collection** - Extended test log capture
4. **Custom Duration** - Specify your own duration

Generates:
- `test_logs_*.txt` - Complete filtered logs
- `auth_logs_*.txt` - Authentication events only
- `voice_ai_logs_*.txt` - Voice AI events only
- `emergency_logs_*.txt` - Emergency system events only
- `error_logs_*.txt` - Errors and exceptions only

---

### 4. network-toggle.sh
**Purpose:** Network state management for testing  
**Usage:** `./test-automation/network-toggle.sh`

Options:
1. Disable WiFi
2. Enable WiFi
3. Disable Mobile Data
4. Enable Mobile Data
5. Disable All Network (Airplane mode simulation)
6. Enable All Network
7. **Test Network Handoff** (Auto cycle: Online → Offline → Online)
8. Monitor Network State (Live)

**Option 7 (Network Handoff Test)** is especially useful for testing:
- Voice AI automatic fallback (ElevenLabs → LocalVoiceLLM)
- Emergency event queuing and sync
- Network restoration handling

---

## Quick Start Guide

### First Time Setup

```bash
# 1. Verify your setup
./test-automation/verify-setup.sh

# 2. Fix any errors reported
# - Add missing API keys to .env
# - Download missing model files
# - Update SHA-1 in Firebase Console
# - Re-download google-services.json
```

### Running Tests

```bash
# 1. Build and install the app
./gradlew clean installDebug

# 2. Run automated tests
./test-automation/run-tests.sh

# 3. Collect logs during manual testing
./test-automation/extract-logs.sh
# (Select option 2 or 3 for timed collection)
```

### Testing Specific Features

**Google Sign-In:**
```bash
# Start log monitoring
./test-automation/extract-logs.sh
# (Select option 1 - Live monitoring)

# In another terminal, check logcat for errors
# Look for Status Code 10 (config error) or 12500 (cancelled)
```

**Voice AI Online (ElevenLabs):**
```bash
# 1. Start app and log monitoring
./test-automation/extract-logs.sh

# 2. In app: Tap voice button, grant permissions
# 3. Speak test phrases
# 4. Check logs for:
#    - "Connected to conversation"
#    - "Mode changed: listening/speaking"
#    - Agent responses
```

**Voice AI Offline (TinyLlama):**
```bash
# 1. Enable airplane mode OR run:
./test-automation/network-toggle.sh
# (Select option 5 - Disable All Network)

# 2. Start voice conversation
# 3. Check logs for:
#    - "Model initialized successfully"
#    - "Vosk model loaded successfully"
#    - "Local voice service ready"
```

**Network Handoff Test:**
```bash
# 1. Start voice conversation in app
# 2. Run automated handoff test:
./test-automation/network-toggle.sh
# (Select option 7 - Test Network Handoff)

# 3. Script will automatically:
#    - Keep network online for 10s
#    - Disable network for 15s (tests offline mode)
#    - Re-enable network for 10s (tests recovery)

# 4. Monitor app to verify smooth transitions
```

**Emergency System:**
```bash
# 1. Start log collection
./test-automation/extract-logs.sh

# 2. In app: Trigger emergency
# 3. Check logs for:
#    - "Triggering health emergency"
#    - "Phase 1: User Response Check"
#    - "Phase 2: Contact Notification"
```

---

## Interpreting Results

### verify-setup.sh Output

```
✓ = Check passed
✗ = Check failed (must fix)
⚠ = Warning (optional, may impact some features)
ℹ = Information
```

**Critical Errors:**
- Missing .env file or API keys
- google-services.json not found
- SHA-1 mismatch (causes Google Sign-In Status Code 10)
- Model files missing (breaks offline voice AI)

**Warnings:**
- Backend server not running (needed for Twilio calls)
- Permissions not granted (will be requested at runtime)
- Build not found (need to compile)

### run-tests.sh Output

**Test Results:**
- `PASS` - Test passed successfully ✅
- `FAIL` - Test failed, requires attention ❌
- `SKIP` - Test skipped (feature not used yet or requires manual interaction) ⊝

**Results File:** Check `test-results/test_results_*.md` for detailed report

### extract-logs.sh Output

**Log Analysis Sections:**
1. **Google Sign-In Events** - Auth flow, status codes, errors
2. **Voice AI Events** - Conversation sessions, mode changes
3. **Emergency System Events** - Phase transitions, contact notifications
4. **Model Loading Events** - AI model initialization status
5. **Network Events** - Connection state changes
6. **Error Summary** - All errors and exceptions found

**Key Indicators:**
- `Status Code: 10` → Google Sign-In config error (SHA-1/Web Client ID)
- `Status Code: 12500` → User cancelled sign-in
- `Connected to conversation` → Voice AI session started
- `Model initialized successfully` → TinyLlama loaded
- `Vosk model loaded successfully` → Speech-to-text ready
- `Triggering health emergency` → Emergency activated
- `Network lost during conversation` → Offline transition

---

## Troubleshooting

### Script Permission Denied
```bash
chmod +x test-automation/*.sh
```

### ADB Device Not Found
```bash
# Check if device is connected
adb devices

# If no devices, check:
# - USB debugging enabled on device
# - Device connected via USB
# - USB drivers installed (Windows)
# - Device authorized (check device screen)

# Restart ADB server
adb kill-server
adb start-server
```

### No Logs Captured
```bash
# Make sure app is running
adb shell pidof com.rescuemate

# Check if logcat is working
adb logcat | head -20

# Try clearing logcat buffer
adb logcat -c
```

### Script Fails with "jq: command not found"
```bash
# macOS
brew install jq

# Linux
sudo apt-get install jq  # Debian/Ubuntu
sudo yum install jq      # RedHat/CentOS
```

---

## Advanced Usage

### Custom Log Filtering

```bash
# Monitor specific component
adb logcat | grep "ElevenLabs"

# Multiple components
adb logcat | grep -E "AuthRepository|EmergencyManager"

# Save to file
adb logcat | grep "YourTag" > custom_logs.txt
```

### Combine Scripts

```bash
# Run full test suite with network handoff
./test-automation/run-tests.sh &
sleep 20
./test-automation/network-toggle.sh  # (option 7)
```

### Continuous Monitoring

```bash
# Terminal 1: Network state monitor
./test-automation/network-toggle.sh  # (option 8)

# Terminal 2: Live log monitoring
./test-automation/extract-logs.sh    # (option 1)

# Terminal 3: Use the app
```

---

## Test Logs Location

All generated files are saved in:
- `test-logs/` - Log files from extract-logs.sh
- `test-results/` - Test reports from run-tests.sh

**File Naming:**
- `live_logs_YYYYMMDD_HHMMSS.txt` - Live monitoring logs
- `test_logs_XXs_YYYYMMDD_HHMMSS.txt` - Timed collection logs
- `auth_logs_*.txt` - Filtered authentication logs
- `voice_ai_logs_*.txt` - Filtered voice AI logs
- `emergency_logs_*.txt` - Filtered emergency logs
- `error_logs_*.txt` - Filtered error logs
- `test_results_*.md` - Test execution reports

---

## Integration with CI/CD

These scripts can be integrated into CI/CD pipelines:

```yaml
# Example GitHub Actions workflow
- name: Run Tests
  run: |
    ./test-automation/verify-setup.sh
    ./test-automation/run-tests.sh
    
- name: Upload Test Results
  uses: actions/upload-artifact@v3
  with:
    name: test-results
    path: test-results/
```

---

## Support

For issues or questions:
1. Check the main testing plan: `COMPREHENSIVE_TESTING_PLAN.md`
2. Review error fixes: `SIGNIN_ERRORS_AND_FIXES.md`
3. Check debug fixes: `DEBUG_FIXES_SUMMARY.md`

---

**Last Updated:** November 28, 2025

