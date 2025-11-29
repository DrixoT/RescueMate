#!/bin/bash

# RescueMate 2.0 - Network State Toggle Script
# Automates network state changes for testing offline/online transitions

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  RescueMate 2.0 - Network State Toggle"
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

# Get current network state
check_network_state() {
    wifi_state=$(adb shell dumpsys wifi | grep "Wi-Fi is" | awk '{print $3}' || echo "unknown")
    mobile_data_state=$(adb shell svc data check | tr -d '\r' || echo "unknown")

    echo ""
    echo "Current Network State:"
    echo "  WiFi: $wifi_state"
    echo "  Mobile Data: $mobile_data_state"
}

# Show current state
check_network_state

echo ""
echo "Select action:"
echo "  1) Disable WiFi"
echo "  2) Enable WiFi"
echo "  3) Disable Mobile Data"
echo "  4) Enable Mobile Data"
echo "  5) Disable All Network (Airplane Mode simulation)"
echo "  6) Enable All Network"
echo "  7) Test Network Handoff (Auto cycle: Online → Offline → Online)"
echo "  8) Monitor Network State (Live)"
echo ""
read -p "Enter choice [1-8]: " choice

case $choice in
    1)
        echo ""
        echo -e "${BLUE}Disabling WiFi...${NC}"
        adb shell svc wifi disable
        sleep 2
        echo -e "${GREEN}✓${NC} WiFi disabled"
        check_network_state
        ;;

    2)
        echo ""
        echo -e "${BLUE}Enabling WiFi...${NC}"
        adb shell svc wifi enable
        sleep 3
        echo -e "${GREEN}✓${NC} WiFi enabled"
        check_network_state
        ;;

    3)
        echo ""
        echo -e "${BLUE}Disabling Mobile Data...${NC}"
        adb shell svc data disable
        sleep 2
        echo -e "${GREEN}✓${NC} Mobile Data disabled"
        check_network_state
        ;;

    4)
        echo ""
        echo -e "${BLUE}Enabling Mobile Data...${NC}"
        adb shell svc data enable
        sleep 2
        echo -e "${GREEN}✓${NC} Mobile Data enabled"
        check_network_state
        ;;

    5)
        echo ""
        echo -e "${BLUE}Disabling all network connections...${NC}"
        adb shell svc wifi disable
        adb shell svc data disable
        sleep 2
        echo -e "${GREEN}✓${NC} All network disabled (airplane mode simulation)"
        check_network_state
        echo ""
        echo -e "${YELLOW}Note:${NC} This simulates airplane mode. Bluetooth is still active."
        ;;

    6)
        echo ""
        echo -e "${BLUE}Enabling all network connections...${NC}"
        adb shell svc wifi enable
        adb shell svc data enable
        sleep 3
        echo -e "${GREEN}✓${NC} All network enabled"
        check_network_state
        ;;

    7)
        echo ""
        echo -e "${BLUE}Starting Network Handoff Test...${NC}"
        echo ""

        # Initial state - Online
        echo -e "${GREEN}Phase 1: Online Mode${NC}"
        echo "Ensuring network is enabled..."
        adb shell svc wifi enable
        adb shell svc data enable
        sleep 3
        check_network_state

        echo ""
        echo "Testing with network available for 10 seconds..."
        echo "Use the app's voice AI or emergency features now."
        sleep 10

        # Go offline
        echo ""
        echo -e "${YELLOW}Phase 2: Offline Mode${NC}"
        echo "Disabling network to trigger offline fallback..."
        adb shell svc wifi disable
        adb shell svc data disable
        sleep 2
        check_network_state

        echo ""
        echo "Testing offline mode for 15 seconds..."
        echo "Voice AI should switch to LocalVoiceLLMService."
        echo "Emergency system should queue events."
        sleep 15

        # Back online
        echo ""
        echo -e "${GREEN}Phase 3: Back Online${NC}"
        echo "Re-enabling network..."
        adb shell svc wifi enable
        adb shell svc data enable
        sleep 3
        check_network_state

        echo ""
        echo "Network restored. Monitoring for 10 seconds..."
        echo "Voice AI should switch back to ElevenLabs."
        echo "Queued emergencies should sync."
        sleep 10

        echo ""
        echo -e "${GREEN}✓${NC} Network Handoff Test Complete"
        echo ""
        echo "Check app logs to verify:"
        echo "  - Voice AI switched from ElevenLabs → LocalVoiceLLM → ElevenLabs"
        echo "  - Emergency events queued and synced"
        echo "  - No crashes or data loss"
        ;;

    8)
        echo ""
        echo -e "${BLUE}Starting Network State Monitor...${NC}"
        echo "Press Ctrl+C to stop"
        echo ""

        trap 'echo ""; echo "Monitoring stopped."; exit 0' INT

        while true; do
            # Clear line and show current state
            wifi=$(adb shell dumpsys wifi | grep "Wi-Fi is" | awk '{print $3}' 2>/dev/null || echo "unknown")
            data=$(adb shell svc data check 2>/dev/null | tr -d '\r' || echo "unknown")

            # Get timestamp
            timestamp=$(date "+%H:%M:%S")

            # Color based on state
            if [ "$wifi" == "enabled" ] || [ "$data" == "enabled" ]; then
                color=$GREEN
                status="ONLINE"
            else
                color=$RED
                status="OFFLINE"
            fi

            echo -ne "\r[$timestamp] ${color}${status}${NC} - WiFi: $wifi | Mobile: $data     "
            sleep 1
        done
        ;;

    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo -e "${YELLOW}Tip:${NC} Run './test-automation/extract-logs.sh' to collect logs after testing"

