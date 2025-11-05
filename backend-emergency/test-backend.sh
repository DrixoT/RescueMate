#!/bin/bash

# RescueMate Emergency Backend - Test Script
# Tests all API endpoints and Twilio integration

BASE_URL="http://localhost:3000"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🚨 RescueMate Emergency Backend Test Suite 🚨"
echo "================================================"
echo ""

# Test 1: Health Check
echo "Test 1: Health Check"
response=$(curl -s -w "\n%{http_code}" ${BASE_URL}/health)
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" == "200" ]; then
    echo -e "${GREEN}✓ PASSED${NC} - Health check successful"
    echo "   Response: $body"
else
    echo -e "${RED}✗ FAILED${NC} - Health check failed (HTTP $http_code)"
fi
echo ""

# Test 2: Twilio Configuration
echo "Test 2: Twilio Configuration Check"
response=$(curl -s -w "\n%{http_code}" ${BASE_URL}/api/twilio/test)
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" == "200" ]; then
    echo -e "${GREEN}✓ PASSED${NC} - Twilio configuration valid"
    echo "   Response: $body"
else
    echo -e "${YELLOW}⚠ WARNING${NC} - Twilio may not be configured (HTTP $http_code)"
    echo "   Response: $body"
fi
echo ""

# Test 3: Create Emergency Alert
echo "Test 3: Create Emergency Alert"
response=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/api/emergency/contact-alert \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user-123",
    "emergencyType": "MANUAL_TRIGGER",
    "healthData": {
      "currentHeartRate": 120,
      "normalHeartRate": 70,
      "alertReason": "Test emergency alert",
      "riskScore": 0.5
    },
    "location": {
      "latitude": 37.7749,
      "longitude": -122.4194,
      "address": "San Francisco, CA",
      "accuracy": 10,
      "googleMapsLink": "https://maps.google.com/?q=37.7749,-122.4194"
    },
    "userInfo": {
      "name": "Test User",
      "age": 30,
      "phoneNumber": "+1234567890",
      "medicalInfo": {
        "baselineHeartRate": 70,
        "bloodType": "O+",
        "knownConditions": ["None"],
        "currentMedications": [],
        "allergies": []
      }
    },
    "timestamp": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'"
  }')

http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" == "200" ]; then
    echo -e "${GREEN}✓ PASSED${NC} - Emergency alert created"
    echo "   Response: $body"

    # Extract emergency ID for next tests
    EMERGENCY_ID=$(echo "$body" | grep -o '"emergencyId":"[^"]*' | sed 's/"emergencyId":"//')
    echo "   Emergency ID: $EMERGENCY_ID"
else
    echo -e "${RED}✗ FAILED${NC} - Failed to create emergency alert (HTTP $http_code)"
    echo "   Response: $body"
fi
echo ""

# Test 4: Get Emergency Status (if we have an ID)
if [ ! -z "$EMERGENCY_ID" ]; then
    echo "Test 4: Get Emergency Status"
    response=$(curl -s -w "\n%{http_code}" ${BASE_URL}/api/emergency/${EMERGENCY_ID}/status)
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" == "200" ]; then
        echo -e "${GREEN}✓ PASSED${NC} - Emergency status retrieved"
        echo "   Response: $body"
    else
        echo -e "${RED}✗ FAILED${NC} - Failed to get emergency status (HTTP $http_code)"
    fi
    echo ""
fi

# Test 5: Test Emergency Contact Call (SMS)
echo "Test 5: Test Emergency Contact Call (SMS)"
echo -e "${YELLOW}⚠ NOTE: This will attempt to send a test SMS if Twilio is configured${NC}"
read -p "Enter test phone number (or press Enter to skip): " TEST_PHONE

if [ ! -z "$TEST_PHONE" ] && [ ! -z "$EMERGENCY_ID" ]; then
    response=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/api/emergency/contact-call \
      -H "Content-Type: application/json" \
      -d '{
        "userId": "test-user-123",
        "emergencyId": "'$EMERGENCY_ID'",
        "contactPhone": "'$TEST_PHONE'",
        "contactName": "Test Contact",
        "messageType": "sms",
        "healthSummary": "Test User is experiencing a test emergency alert",
        "locationLink": "https://maps.google.com/?q=37.7749,-122.4194",
        "emergencyDetails": "This is a test emergency SMS from RescueMate"
      }')

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" == "200" ]; then
        echo -e "${GREEN}✓ PASSED${NC} - SMS sent successfully"
        echo "   Response: $body"
    else
        echo -e "${RED}✗ FAILED${NC} - Failed to send SMS (HTTP $http_code)"
        echo "   Response: $body"
    fi
else
    echo -e "${YELLOW}⊘ SKIPPED${NC} - No phone number provided or no emergency ID"
fi
echo ""

# Test 6: Test Emergency Response
if [ ! -z "$EMERGENCY_ID" ] && [ ! -z "$TEST_PHONE" ]; then
    echo "Test 6: Submit Contact Response"
    response=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/api/emergency/contact-response \
      -H "Content-Type: application/json" \
      -d '{
        "emergencyId": "'$EMERGENCY_ID'",
        "contactPhone": "'$TEST_PHONE'",
        "response": "user_fine",
        "timestamp": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'",
        "notes": "Test response - user confirmed safe"
      }')

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" == "200" ]; then
        echo -e "${GREEN}✓ PASSED${NC} - Contact response recorded"
        echo "   Response: $body"
    else
        echo -e "${RED}✗ FAILED${NC} - Failed to record response (HTTP $http_code)"
        echo "   Response: $body"
    fi
else
    echo -e "${YELLOW}⊘ SKIPPED${NC} - No emergency ID or phone number"
fi
echo ""

# Test 7: Cancel Emergency
if [ ! -z "$EMERGENCY_ID" ]; then
    echo "Test 7: Cancel Emergency"
    response=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/api/emergency/cancel \
      -H "Content-Type: application/json" \
      -d '{
        "emergencyId": "'$EMERGENCY_ID'",
        "reason": "Test cancellation"
      }')

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" == "200" ]; then
        echo -e "${GREEN}✓ PASSED${NC} - Emergency cancelled"
        echo "   Response: $body"
    else
        echo -e "${RED}✗ FAILED${NC} - Failed to cancel emergency (HTTP $http_code)"
        echo "   Response: $body"
    fi
else
    echo -e "${YELLOW}⊘ SKIPPED${NC} - No emergency ID"
fi
echo ""

# Summary
echo "================================================"
echo "Test Suite Complete"
echo ""
echo "Next Steps:"
echo "1. Review test results above"
echo "2. Check backend logs: logs/emergency.log"
echo "3. Test voice calls manually (will incur Twilio charges)"
echo "4. Configure webhooks in Twilio Console"
echo "5. Test emergency workflow end-to-end from Android app"
echo ""
echo "For more information, see EMERGENCY_SOS_INTEGRATION_GUIDE.md"

