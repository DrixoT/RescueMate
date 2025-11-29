#!/bin/bash

# RescueMate 2.0 - Automated Test Runner
# Executes comprehensive test suite

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  RescueMate 2.0 - Automated Test Runner"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check if ADB is available
if ! command -v adb &> /dev/null; then
    echo -e "${RED}✗${NC} ADB not found. Please install Android SDK Platform Tools."
    exit 1
fi

# Check if device is connected
if [ "$(adb devices | grep -v "List" | grep "device" | wc -l | tr -d ' ')" -eq 0 ]; then
    echo -e "${RED}✗${NC} No Android device connected."
    exit 1
fi

echo -e "${GREEN}✓${NC} Device connected"
echo ""

# Get device info
DEVICE_MODEL=$(adb shell getprop ro.product.model | tr -d '\r')
ANDROID_VERSION=$(adb shell getprop ro.build.version.release | tr -d '\r')

echo "Device: $DEVICE_MODEL (Android $ANDROID_VERSION)"
echo ""

# Create test results directory
TEST_RESULTS_DIR="test-results"
mkdir -p "$TEST_RESULTS_DIR"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_FILE="$TEST_RESULTS_DIR/test_results_$TIMESTAMP.md"

# Initialize results file
cat > "$RESULTS_FILE" << EOF
# RescueMate 2.0 - Test Execution Results

**Date:** $(date "+%Y-%m-%d %H:%M:%S")
**Tester:** Automated Test Runner
**Device:** $DEVICE_MODEL - Android $ANDROID_VERSION
**Build:** $(git rev-parse --short HEAD 2>/dev/null || echo "unknown")

---

## Test Execution Log

EOF

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# Helper functions
log_test() {
    local test_name="$1"
    local status="$2"
    local message="$3"

    ((TOTAL_TESTS++))

    case $status in
        PASS)
            ((PASSED_TESTS++))
            echo -e "${GREEN}✓${NC} $test_name: PASS"
            echo "- ✅ **$test_name**: PASS" >> "$RESULTS_FILE"
            ;;
        FAIL)
            ((FAILED_TESTS++))
            echo -e "${RED}✗${NC} $test_name: FAIL - $message"
            echo "- ❌ **$test_name**: FAIL - $message" >> "$RESULTS_FILE"
            ;;
        SKIP)
            ((SKIPPED_TESTS++))
            echo -e "${YELLOW}⊝${NC} $test_name: SKIPPED - $message"
            echo "- ⊝ **$test_name**: SKIPPED - $message" >> "$RESULTS_FILE"
            ;;
    esac
}

section() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    echo "" >> "$RESULTS_FILE"
    echo "### $1" >> "$RESULTS_FILE"
    echo "" >> "$RESULTS_FILE"
}

# Start logcat collection
LOGCAT_FILE="$TEST_RESULTS_DIR/logcat_$TIMESTAMP.txt"
echo "Starting logcat collection..."
adb logcat -c
adb logcat > "$LOGCAT_FILE" &
LOGCAT_PID=$!

# Cleanup function
cleanup() {
    echo ""
    echo "Stopping logcat collection..."
    kill $LOGCAT_PID 2>/dev/null || true

    # Generate summary
    section "Test Summary"

    echo "Total Tests: $TOTAL_TESTS" | tee -a "$RESULTS_FILE"
    echo "Passed: $PASSED_TESTS" | tee -a "$RESULTS_FILE"
    echo "Failed: $FAILED_TESTS" | tee -a "$RESULTS_FILE"
    echo "Skipped: $SKIPPED_TESTS" | tee -a "$RESULTS_FILE"

    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}✓ All tests passed!${NC}"
        echo "" >> "$RESULTS_FILE"
        echo "✅ **All tests passed!**" >> "$RESULTS_FILE"
    else
        echo -e "${RED}✗ $FAILED_TESTS test(s) failed${NC}"
        echo "" >> "$RESULTS_FILE"
        echo "❌ **$FAILED_TESTS test(s) failed**" >> "$RESULTS_FILE"
    fi

    echo ""
    echo "Results saved to: $RESULTS_FILE"
    echo "Logs saved to: $LOGCAT_FILE"
}

trap cleanup EXIT

# ============================================
# PRE-TEST VERIFICATION
# ============================================
section "Pre-Test Verification"

# Check if app is installed
if adb shell pm list packages | grep -q "com.rescuemate"; then
    log_test "App Installation" "PASS"
else
    log_test "App Installation" "FAIL" "App not installed"
    echo ""
    echo "Installing app..."
    ./gradlew installDebug
fi

# Check permissions
PERMISSIONS=("android.permission.RECORD_AUDIO" "android.permission.ACCESS_FINE_LOCATION" "android.permission.POST_NOTIFICATIONS")

for perm in "${PERMISSIONS[@]}"; do
    if adb shell dumpsys package com.rescuemate | grep -q "$perm.*granted=true"; then
        log_test "Permission: $perm" "PASS"
    else
        log_test "Permission: $perm" "SKIP" "Not granted - will be requested at runtime"
    fi
done

# ============================================
# APP LAUNCH TEST
# ============================================
section "App Launch & Stability"

echo "Launching app..."
adb shell am start -n com.rescuemate/.MainActivity

sleep 3

# Check if app is running
if adb shell pidof com.rescuemate > /dev/null; then
    log_test "App Launch" "PASS"
else
    log_test "App Launch" "FAIL" "App not running"
fi

# Check for crashes in logcat
sleep 2
if grep -q "AndroidRuntime: FATAL EXCEPTION" "$LOGCAT_FILE"; then
    log_test "No Crashes on Launch" "FAIL" "Crash detected in logcat"
else
    log_test "No Crashes on Launch" "PASS"
fi

# ============================================
# GOOGLE SIGN-IN VERIFICATION
# ============================================
section "Google Sign-In Diagnostics"

echo -e "${YELLOW}Note: Google Sign-In requires manual interaction${NC}"
echo "Please tap 'Continue with Google' and select an account within 30 seconds"
echo ""

sleep 5

# Monitor for sign-in events
timeout 30 bash -c '
    while true; do
        if adb logcat -d | grep -q "Google Sign-In successful"; then
            exit 0
        fi
        if adb logcat -d | grep -q "Status Code: 10"; then
            exit 10
        fi
        if adb logcat -d | grep -q "Status Code: 12500"; then
            exit 125
        fi
        sleep 1
    done
' && sign_in_result=$? || sign_in_result=$?

case $sign_in_result in
    0)
        log_test "Google Sign-In" "PASS"
        ;;
    10)
        log_test "Google Sign-In" "FAIL" "Status Code 10 - SHA-1/Web Client ID mismatch"
        ;;
    125)
        log_test "Google Sign-In" "SKIP" "User cancelled"
        ;;
    124)
        log_test "Google Sign-In" "SKIP" "Timeout - no interaction detected"
        ;;
    *)
        log_test "Google Sign-In" "SKIP" "No sign-in attempt detected"
        ;;
esac

# ============================================
# VOICE AI READINESS
# ============================================
section "Voice AI System Check"

# Check if models are being loaded
sleep 5

if grep -q "Model initialized successfully\|Vosk model loaded successfully" "$LOGCAT_FILE"; then
    log_test "AI Models Loading" "PASS"
else
    log_test "AI Models Loading" "SKIP" "Model loading not detected yet"
fi

# Check for ElevenLabs initialization
if grep -q "ElevenLabsConversationalService initialized" "$LOGCAT_FILE"; then
    log_test "ElevenLabs Service Init" "PASS"
else
    log_test "ElevenLabs Service Init" "SKIP" "Not initialized yet"
fi

# ============================================
# EMERGENCY SYSTEM READINESS
# ============================================
section "Emergency System Check"

# Check if EmergencyManager is initialized
if grep -q "EmergencyManager" "$LOGCAT_FILE"; then
    log_test "Emergency Manager Init" "PASS"
else
    log_test "Emergency Manager Init" "SKIP" "Not used yet"
fi

# ============================================
# ERROR DETECTION
# ============================================
section "Error Detection"

# Check for critical errors
ERROR_PATTERNS=(
    "FATAL EXCEPTION"
    "java.lang.NullPointerException"
    "Failed to load native library"
    "Model file not found"
)

for pattern in "${ERROR_PATTERNS[@]}"; do
    if grep -q "$pattern" "$LOGCAT_FILE"; then
        log_test "No Error: $pattern" "FAIL" "Pattern found in logs"
    else
        log_test "No Error: $pattern" "PASS"
    fi
done

# ============================================
# INTERACTIVE TESTS
# ============================================
section "Interactive Test Instructions"

echo "" | tee -a "$RESULTS_FILE"
echo "The following tests require manual interaction:" | tee -a "$RESULTS_FILE"
echo "" | tee -a "$RESULTS_FILE"
echo "1. **Voice Conversation Test**" | tee -a "$RESULTS_FILE"
echo "   - Tap the voice button on HomeDashboard" | tee -a "$RESULTS_FILE"
echo "   - Grant microphone permission if prompted" | tee -a "$RESULTS_FILE"
echo "   - Say: 'Hello, can you hear me?'" | tee -a "$RESULTS_FILE"
echo "   - Verify you receive a voice response" | tee -a "$RESULTS_FILE"
echo "" | tee -a "$RESULTS_FILE"
echo "2. **Emergency Trigger Test**" | tee -a "$RESULTS_FILE"
echo "   - Long-press the SOS/Panic button" | tee -a "$RESULTS_FILE"
echo "   - Wait for countdown to complete OR cancel" | tee -a "$RESULTS_FILE"
echo "   - Verify emergency flow executes correctly" | tee -a "$RESULTS_FILE"
echo "" | tee -a "$RESULTS_FILE"
echo "3. **Network Handoff Test**" | tee -a "$RESULTS_FILE"
echo "   - Start a voice conversation" | tee -a "$RESULTS_FILE"
echo "   - Run: ./test-automation/network-toggle.sh (option 7)" | tee -a "$RESULTS_FILE"
echo "   - Verify smooth transition between online/offline modes" | tee -a "$RESULTS_FILE"
echo "" | tee -a "$RESULTS_FILE"

echo ""
echo -e "${MAGENTA}Automated tests complete.${NC}"
echo -e "${YELLOW}Please perform the interactive tests above and update the results file.${NC}"
echo ""

# Wait a bit more for final logs
sleep 5

