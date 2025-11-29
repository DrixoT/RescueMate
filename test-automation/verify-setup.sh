#!/bin/bash

# RescueMate 2.0 - Pre-Test Configuration Verification Script
# Validates all required configurations before running tests

set -e

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  RescueMate 2.0 - Pre-Test Configuration Verification"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

ERRORS=0
WARNINGS=0

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

pass() {
    echo -e "${GREEN}✓${NC} $1"
}

fail() {
    echo -e "${RED}✗${NC} $1"
    ((ERRORS++))
}

warn() {
    echo -e "${YELLOW}⚠${NC} $1"
    ((WARNINGS++))
}

info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

section() {
    echo ""
    echo -e "${BLUE}━━━ $1 ━━━${NC}"
}

# ============================================
# 1. ENVIRONMENT FILES
# ============================================
section "1. Environment Configuration"

# Check .env file
if [ -f ".env" ]; then
    pass ".env file exists"

    # Check required keys
    required_keys=("ELEVEN_API_KEY" "ELEVEN_AGENT_ID" "TWILIO_ACCOUNT_SID" "TWILIO_AUTH_TOKEN" "TWILIO_PHONE_NUMBER")

    for key in "${required_keys[@]}"; do
        if grep -q "^$key=" .env; then
            value=$(grep "^$key=" .env | cut -d'=' -f2 | tr -d '"' | tr -d "'")
            if [ -n "$value" ] && [ "$value" != "" ]; then
                pass "$key is configured"
            else
                fail "$key is empty"
            fi
        else
            fail "$key is missing from .env"
        fi
    done
else
    fail ".env file not found"
    info "Create .env file in project root with required API keys"
fi

# Check google-services.json
if [ -f "app/google-services.json" ]; then
    pass "google-services.json exists"

    # Extract Web Client ID
    if command -v jq &> /dev/null; then
        web_client_id=$(jq -r '.client[0].oauth_client[] | select(.client_type == 3) | .client_id' app/google-services.json 2>/dev/null || echo "")
        if [ -n "$web_client_id" ]; then
            pass "Web Client ID found: ${web_client_id:0:30}..."
        else
            fail "Web Client ID (client_type: 3) not found in google-services.json"
        fi
    else
        warn "jq not installed, cannot verify Web Client ID (install with: brew install jq)"
    fi
else
    fail "google-services.json not found in app/ directory"
    info "Download from Firebase Console → Project Settings → Your Android App"
fi

# ============================================
# 2. SHA-1 FINGERPRINT
# ============================================
section "2. SHA-1 Fingerprint Verification"

debug_keystore="$HOME/.android/debug.keystore"

if [ -f "$debug_keystore" ]; then
    pass "Debug keystore exists"

    sha1=$(keytool -list -v -keystore "$debug_keystore" -alias androiddebugkey -storepass android -keypass android 2>/dev/null | grep "SHA1:" | cut -d' ' -f3)

    if [ -n "$sha1" ]; then
        pass "SHA-1 fingerprint extracted: $sha1"
        info "Add this to Firebase Console → Project Settings → SHA certificate fingerprints"

        # Check if it matches the one in google-services.json
        if [ -f "app/google-services.json" ] && command -v jq &> /dev/null; then
            firebase_sha1=$(jq -r '.client[0].android_client_info.certificate_hash[]?' app/google-services.json 2>/dev/null | head -1 || echo "")
            if [ -n "$firebase_sha1" ]; then
                # Convert to same format for comparison
                sha1_normalized=$(echo "$sha1" | tr '[:upper:]' '[:lower:]' | tr -d ':')
                firebase_sha1_normalized=$(echo "$firebase_sha1" | tr '[:upper:]' '[:lower:]' | tr -d ':')

                if [ "$sha1_normalized" == "$firebase_sha1_normalized" ]; then
                    pass "SHA-1 matches Firebase configuration"
                else
                    fail "SHA-1 MISMATCH! Local: $sha1, Firebase: $firebase_sha1"
                    info "This will cause Google Sign-In Status Code 10 error!"
                    info "Update Firebase Console with correct SHA-1 and re-download google-services.json"
                fi
            fi
        fi
    else
        fail "Could not extract SHA-1 fingerprint"
    fi
else
    fail "Debug keystore not found at $debug_keystore"
    info "Build the app once to generate debug keystore"
fi

# ============================================
# 3. MODEL FILES
# ============================================
section "3. AI Model Files Verification"

# Check TinyLlama model
tinyllama_path="app/src/main/assets/models/TinyLlama-1.1B-Chat-v0.4.Q4_K_M.gguf"
if [ -f "$tinyllama_path" ]; then
    size=$(ls -lh "$tinyllama_path" | awk '{print $5}')
    pass "TinyLlama model exists ($size)"

    # Check if size is reasonable (should be ~600MB+)
    size_bytes=$(stat -f%z "$tinyllama_path" 2>/dev/null || stat -c%s "$tinyllama_path" 2>/dev/null)
    if [ "$size_bytes" -lt 500000000 ]; then
        warn "TinyLlama model seems too small ($size), expected ~600MB+"
    fi
else
    fail "TinyLlama model not found at $tinyllama_path"
    info "Download and place in app/src/main/assets/models/"
fi

# Check Vosk model
vosk_path="app/src/main/assets/model/vosk-model-small-en-us-0.15"
if [ -d "$vosk_path" ]; then
    pass "Vosk model directory exists"

    # Check for required files
    if [ -f "$vosk_path/am/final.mdl" ]; then
        pass "Vosk model files appear complete"
    else
        warn "Vosk model files may be incomplete"
    fi
else
    fail "Vosk model not found at $vosk_path"
    info "Download and extract to app/src/main/assets/model/"
fi

# ============================================
# 4. ANDROID DEVELOPMENT TOOLS
# ============================================
section "4. Android Development Tools"

# Check ADB
if command -v adb &> /dev/null; then
    pass "ADB is installed"

    # Check for connected devices
    devices=$(adb devices | grep -v "List" | grep "device" | wc -l | tr -d ' ')
    if [ "$devices" -gt 0 ]; then
        pass "$devices Android device(s) connected"

        # Show device info
        while IFS= read -r line; do
            device_id=$(echo "$line" | awk '{print $1}')
            if [ -n "$device_id" ] && [ "$device_id" != "List" ]; then
                model=$(adb -s "$device_id" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
                android_version=$(adb -s "$device_id" shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')
                info "  Device: $model (Android $android_version)"
            fi
        done < <(adb devices | grep "device$")
    else
        warn "No Android devices connected"
        info "Connect a device or start an emulator"
    fi
else
    fail "ADB not found"
    info "Install Android SDK Platform Tools"
fi

# Check Gradle
if [ -f "gradlew" ]; then
    pass "Gradle wrapper exists"
else
    fail "gradlew not found"
fi

# Check Java/JDK
if command -v java &> /dev/null; then
    java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
    pass "Java is installed (version $java_version)"
else
    fail "Java not found"
    info "Install JDK 17 or higher"
fi

# ============================================
# 5. BACKEND SERVER
# ============================================
section "5. Backend Server Status"

if [ -f "backend-emergency/server.js" ]; then
    pass "Backend server files exist"

    # Check if Node.js is installed
    if command -v node &> /dev/null; then
        node_version=$(node --version)
        pass "Node.js is installed ($node_version)"

        # Check if server is running
        if curl -s http://localhost:3000/api/health &> /dev/null; then
            pass "Backend server is running on port 3000"
        else
            warn "Backend server is not running"
            info "Start with: cd backend-emergency && node server.js"
        fi
    else
        fail "Node.js not installed"
        info "Install Node.js to run backend server"
    fi

    # Check package.json
    if [ -f "backend-emergency/package.json" ]; then
        if [ -d "backend-emergency/node_modules" ]; then
            pass "Backend dependencies installed"
        else
            warn "Backend dependencies not installed"
            info "Run: cd backend-emergency && npm install"
        fi
    fi
else
    warn "Backend server not found (optional for basic testing)"
fi

# ============================================
# 6. BUILD VERIFICATION
# ============================================
section "6. Build Status"

if [ -d "app/build/outputs/apk/debug" ]; then
    latest_apk=$(ls -t app/build/outputs/apk/debug/*.apk 2>/dev/null | head -1)
    if [ -n "$latest_apk" ]; then
        apk_date=$(date -r "$latest_apk" "+%Y-%m-%d %H:%M:%S" 2>/dev/null || stat -c %y "$latest_apk" 2>/dev/null)
        pass "Debug APK exists (built: $apk_date)"

        # Check if APK contains native libraries
        if command -v unzip &> /dev/null; then
            if unzip -l "$latest_apk" 2>/dev/null | grep -q "libllama-android.so"; then
                pass "Native library (libllama-android.so) found in APK"
            else
                warn "Native library not found in APK (offline voice AI may not work)"
            fi
        fi
    else
        warn "Debug APK not found"
        info "Build with: ./gradlew assembleDebug"
    fi
else
    warn "Build output directory not found"
    info "Build the app first: ./gradlew assembleDebug"
fi

# ============================================
# 7. PERMISSIONS CHECK (if device connected)
# ============================================
section "7. App Installation & Permissions"

if command -v adb &> /dev/null && [ "$(adb devices | grep -v "List" | grep "device" | wc -l | tr -d ' ')" -gt 0 ]; then
    # Check if app is installed
    if adb shell pm list packages | grep -q "com.rescuemate"; then
        pass "RescueMate app is installed on device"

        # Check key permissions
        permissions=("android.permission.RECORD_AUDIO" "android.permission.ACCESS_FINE_LOCATION" "android.permission.CALL_PHONE")

        for perm in "${permissions[@]}"; do
            if adb shell dumpsys package com.rescuemate | grep -q "$perm.*granted=true"; then
                pass "Permission granted: $perm"
            else
                warn "Permission not granted: $perm (grant manually on first run)"
            fi
        done
    else
        warn "RescueMate app not installed on device"
        info "Install with: ./gradlew installDebug"
    fi
fi

# ============================================
# SUMMARY
# ============================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Verification Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed! Ready to run tests.${NC}"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠ $WARNINGS warning(s) found. You can proceed but some features may not work.${NC}"
    exit 0
else
    echo -e "${RED}✗ $ERRORS error(s) and $WARNINGS warning(s) found.${NC}"
    echo ""
    echo "Please fix the errors above before running tests."
    exit 1
fi

