#!/bin/bash

# RescueMate 2.0 - Log Extraction and Analysis Script
# Collects and filters relevant logs for testing

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  RescueMate 2.0 - Log Extraction and Analysis"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Create logs directory
LOGS_DIR="test-logs"
mkdir -p "$LOGS_DIR"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

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

# ============================================
# LOG COLLECTION
# ============================================

echo -e "${BLUE}━━━ Collecting Logs ━━━${NC}"

# Clear previous logs
echo "Clearing existing logcat buffer..."
adb logcat -c

echo ""
echo "Select log collection mode:"
echo "  1) Live monitoring (Ctrl+C to stop)"
echo "  2) Collect logs for 30 seconds"
echo "  3) Collect logs for 60 seconds"
echo "  4) Collect logs for specific test duration"
echo ""
read -p "Enter choice [1-4]: " choice

case $choice in
    1)
        echo ""
        echo -e "${BLUE}Starting live log monitoring...${NC}"
        echo "Press Ctrl+C to stop and save logs"
        echo ""

        LOG_FILE="$LOGS_DIR/live_logs_$TIMESTAMP.txt"

        # Trap Ctrl+C to save logs before exiting
        trap 'echo ""; echo "Saving logs to $LOG_FILE..."; echo "Logs saved."; exit 0' INT

        adb logcat | grep -E "AuthRepository|SignInScreen|ElevenLabs|LocalVoiceLLM|EmergencyManager|VoskSTT|StreamingLLM|TinyLlamaInference|NetworkMonitor|HealthMonitoring" | tee "$LOG_FILE"
        ;;

    2)
        DURATION=30
        LOG_FILE="$LOGS_DIR/test_logs_${DURATION}s_$TIMESTAMP.txt"
        echo ""
        echo -e "${BLUE}Collecting logs for $DURATION seconds...${NC}"
        timeout $DURATION adb logcat | grep -E "AuthRepository|SignInScreen|ElevenLabs|LocalVoiceLLM|EmergencyManager|VoskSTT|StreamingLLM|TinyLlamaInference|NetworkMonitor|HealthMonitoring" > "$LOG_FILE" || true
        ;;

    3)
        DURATION=60
        LOG_FILE="$LOGS_DIR/test_logs_${DURATION}s_$TIMESTAMP.txt"
        echo ""
        echo -e "${BLUE}Collecting logs for $DURATION seconds...${NC}"
        timeout $DURATION adb logcat | grep -E "AuthRepository|SignInScreen|ElevenLabs|LocalVoiceLLM|EmergencyManager|VoskSTT|StreamingLLM|TinyLlamaInference|NetworkMonitor|HealthMonitoring" > "$LOG_FILE" || true
        ;;

    4)
        echo ""
        read -p "Enter duration in seconds: " DURATION
        LOG_FILE="$LOGS_DIR/test_logs_${DURATION}s_$TIMESTAMP.txt"
        echo ""
        echo -e "${BLUE}Collecting logs for $DURATION seconds...${NC}"
        timeout $DURATION adb logcat | grep -E "AuthRepository|SignInScreen|ElevenLabs|LocalVoiceLLM|EmergencyManager|VoskSTT|StreamingLLM|TinyLlamaInference|NetworkMonitor|HealthMonitoring" > "$LOG_FILE" || true
        ;;

    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac

# Only proceed with analysis if we have a log file (not in live mode)
if [ "$choice" != "1" ]; then
    echo -e "${GREEN}✓${NC} Logs collected: $LOG_FILE"

    # ============================================
    # LOG ANALYSIS
    # ============================================

    echo ""
    echo -e "${BLUE}━━━ Analyzing Logs ━━━${NC}"

    # Check if log file has content
    if [ ! -s "$LOG_FILE" ]; then
        echo -e "${YELLOW}⚠${NC} No logs captured. Make sure the app is running."
        exit 0
    fi

    # Google Sign-In Analysis
    echo ""
    echo "Google Sign-In Events:"
    if grep -q "Google Sign-In" "$LOG_FILE"; then
        grep "Google Sign-In" "$LOG_FILE" | head -20

        # Check for errors
        if grep -q "Status Code: 10" "$LOG_FILE"; then
            echo ""
            echo -e "${RED}✗ CRITICAL: Status Code 10 detected (SHA-1/Web Client ID mismatch)${NC}"
        fi

        if grep -q "Status Code: 12500" "$LOG_FILE"; then
            echo ""
            echo -e "${YELLOW}⚠ Sign-in was cancelled by user${NC}"
        fi

        if grep -q "Google Sign-In successful" "$LOG_FILE"; then
            echo ""
            echo -e "${GREEN}✓ Google Sign-In completed successfully${NC}"
        fi
    else
        echo "  No Google Sign-In events found"
    fi

    # Voice AI Analysis
    echo ""
    echo "Voice AI Events:"
    if grep -qE "ElevenLabs|LocalVoiceLLM" "$LOG_FILE"; then
        grep -E "ElevenLabs|LocalVoiceLLM" "$LOG_FILE" | head -20

        if grep -q "Connected to conversation" "$LOG_FILE"; then
            echo ""
            echo -e "${GREEN}✓ Voice conversation started successfully${NC}"
        fi

        if grep -q "Switching to local voice LLM fallback" "$LOG_FILE"; then
            echo ""
            echo -e "${YELLOW}⚠ Switched to offline mode (network lost)${NC}"
        fi
    else
        echo "  No Voice AI events found"
    fi

    # Emergency System Analysis
    echo ""
    echo "Emergency System Events:"
    if grep -q "EmergencyManager" "$LOG_FILE"; then
        grep "EmergencyManager" "$LOG_FILE" | head -20

        if grep -q "Triggering health emergency" "$LOG_FILE"; then
            echo ""
            echo -e "${YELLOW}⚠ Emergency was triggered${NC}"
        fi

        if grep -q "Phase 1" "$LOG_FILE"; then
            echo ""
            echo -e "${BLUE}ℹ Emergency Phase 1 (User Response Check) started${NC}"
        fi

        if grep -q "Phase 2" "$LOG_FILE"; then
            echo ""
            echo -e "${BLUE}ℹ Emergency Phase 2 (Contact Notification) started${NC}"
        fi
    else
        echo "  No Emergency events found"
    fi

    # Model Loading Analysis
    echo ""
    echo "Model Loading Events:"
    if grep -qE "TinyLlamaInference|StreamingLLM|VoskSTT" "$LOG_FILE"; then
        grep -E "TinyLlamaInference|StreamingLLM|VoskSTT" "$LOG_FILE" | head -20

        if grep -q "Model initialized successfully" "$LOG_FILE"; then
            echo ""
            echo -e "${GREEN}✓ TinyLlama model loaded successfully${NC}"
        fi

        if grep -q "Vosk model loaded successfully" "$LOG_FILE"; then
            echo ""
            echo -e "${GREEN}✓ Vosk STT model loaded successfully${NC}"
        fi

        if grep -q "Failed to load" "$LOG_FILE"; then
            echo ""
            echo -e "${RED}✗ Model loading failed${NC}"
        fi
    else
        echo "  No model loading events found"
    fi

    # Network Status
    echo ""
    echo "Network Events:"
    if grep -q "NetworkMonitor" "$LOG_FILE"; then
        grep "NetworkMonitor" "$LOG_FILE" | head -10
    else
        echo "  No network events found"
    fi

    # Error Summary
    echo ""
    echo -e "${BLUE}━━━ Error Summary ━━━${NC}"

    error_count=$(grep -c "ERROR\|Exception\|Failed" "$LOG_FILE" || true)
    warning_count=$(grep -c "WARN\|Warning" "$LOG_FILE" || true)

    echo "Errors found: $error_count"
    echo "Warnings found: $warning_count"

    if [ "$error_count" -gt 0 ]; then
        echo ""
        echo "Recent errors:"
        grep -E "ERROR|Exception|Failed" "$LOG_FILE" | tail -10
    fi

    # Generate filtered log files
    echo ""
    echo -e "${BLUE}━━━ Generating Filtered Logs ━━━${NC}"

    # Google Auth logs
    if grep -q "AuthRepository\|SignInScreen" "$LOG_FILE"; then
        AUTH_LOG="$LOGS_DIR/auth_logs_$TIMESTAMP.txt"
        grep -E "AuthRepository|SignInScreen" "$LOG_FILE" > "$AUTH_LOG"
        echo -e "${GREEN}✓${NC} Auth logs: $AUTH_LOG"
    fi

    # Voice AI logs
    if grep -qE "ElevenLabs|LocalVoiceLLM|VoskSTT|StreamingLLM" "$LOG_FILE"; then
        VOICE_LOG="$LOGS_DIR/voice_ai_logs_$TIMESTAMP.txt"
        grep -E "ElevenLabs|LocalVoiceLLM|VoskSTT|StreamingLLM|TinyLlamaInference" "$LOG_FILE" > "$VOICE_LOG"
        echo -e "${GREEN}✓${NC} Voice AI logs: $VOICE_LOG"
    fi

    # Emergency logs
    if grep -q "EmergencyManager" "$LOG_FILE"; then
        EMERGENCY_LOG="$LOGS_DIR/emergency_logs_$TIMESTAMP.txt"
        grep "EmergencyManager" "$LOG_FILE" > "$EMERGENCY_LOG"
        echo -e "${GREEN}✓${NC} Emergency logs: $EMERGENCY_LOG"
    fi

    # Error logs
    if [ "$error_count" -gt 0 ]; then
        ERROR_LOG="$LOGS_DIR/error_logs_$TIMESTAMP.txt"
        grep -E "ERROR|Exception|Failed" "$LOG_FILE" > "$ERROR_LOG"
        echo -e "${GREEN}✓${NC} Error logs: $ERROR_LOG"
    fi

    # ============================================
    # SUMMARY
    # ============================================

    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  Analysis Complete"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "All logs saved to: $LOGS_DIR/"
    echo ""

    if [ "$error_count" -eq 0 ] && [ "$warning_count" -eq 0 ]; then
        echo -e "${GREEN}✓ No errors or warnings found in logs!${NC}"
    elif [ "$error_count" -eq 0 ]; then
        echo -e "${YELLOW}⚠ $warning_count warning(s) found. Check logs for details.${NC}"
    else
        echo -e "${RED}✗ $error_count error(s) and $warning_count warning(s) found.${NC}"
        echo "Review the logs to identify issues."
    fi
fi

