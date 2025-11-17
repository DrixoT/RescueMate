# RescueMate 2.0 - API Documentation

## Backend Emergency API

### Base URL
```
https://your-backend.com/api
```

### Authentication
All API requests require authentication via Bearer token or API key.

```http
Authorization: Bearer YOUR_TOKEN
```

## Endpoints

### 1. Emergency Contact Alert
Notifies the backend of an emergency event.

**Endpoint:** `POST /emergency/contact-alert`

**Request Body:**
```json
{
  "userId": "string",
  "emergencyType": "CARDIAC_ALERT | MANUAL_TRIGGER | FALL_DETECTED",
  "healthData": {
    "currentHeartRate": 180,
    "normalHeartRate": 70,
    "riskScore": 0.9,
    "alertReason": "Heart rate critically high"
  },
  "location": {
    "latitude": 37.7749,
    "longitude": -122.4194,
    "address": "123 Main St, San Francisco, CA",
    "accuracy": 10.5
  },
  "userInfo": {
    "userId": "string",
    "name": "John Doe",
    "age": 45,
    "phoneNumber": "+1234567890",
    "medicalInfo": {
      "bloodType": "O+",
      "allergies": ["Penicillin"],
      "medications": [...]
    }
  },
  "timestamp": "2024-12-01T10:30:00Z"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Emergency alert received",
  "emergencyId": "emergency-uuid",
  "estimatedResponse": "Contacting emergency contacts"
}
```

### 2. Emergency Contact Call
Initiates a call to an emergency contact via Twilio.

**Endpoint:** `POST /emergency/contact-call`

**Request Body:**
```json
{
  "userId": "string",
  "emergencyId": "string",
  "contactPhone": "+1234567890",
  "contactName": "Jane Doe",
  "messageType": "voice | sms",
  "healthSummary": "Heart rate 180 BPM (normal: 70 BPM)",
  "locationLink": "https://maps.google.com/?q=37.7749,-122.4194",
  "emergencyDetails": "Cardiac alert detected"
}
```

**Response:**
```json
{
  "success": true,
  "callSid": "twilio-call-sid",
  "status": "initiated",
  "message": "Call initiated to emergency contact"
}
```

### 3. Contact Response
Records a contact's response to an emergency.

**Endpoint:** `POST /emergency/contact-response`

**Request Body:**
```json
{
  "emergencyId": "string",
  "contactPhone": "+1234567890",
  "response": "USER_FINE | CHECKING_ON_USER | NEED_HELP | NO_RESPONSE",
  "timestamp": "2024-12-01T10:35:00Z",
  "notes": "Spoke with user, they are okay"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Response recorded",
  "action": "emergency_resolved | continue_monitoring"
}
```

### 4. Get Emergency Status
Retrieves the current status of an emergency event.

**Endpoint:** `GET /emergency/{emergencyId}/status`

**Response:**
```json
{
  "emergencyId": "string",
  "status": "INITIATED | PHASE_1 | PHASE_2 | RESOLVED | CANCELLED",
  "currentPhase": 2,
  "contactsNotified": 3,
  "contactsResponded": 1,
  "lastUpdate": "2024-12-01T10:35:00Z"
}
```

### 5. Cancel Emergency
Cancels an active emergency event.

**Endpoint:** `POST /emergency/{emergencyId}/cancel`

**Request Body:**
```json
{
  "userId": "string",
  "reason": "False alarm | User confirmed safe"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Emergency cancelled",
  "notificationsSent": 3
}
```

## Twilio Integration

### Voice Call Script
TwiML script for emergency voice calls:

```xml
<Response>
  <Say voice="alice">
    This is an emergency alert from RescueMate.
    {User Name} has triggered an emergency.
    Their current heart rate is {HeartRate} BPM.
    Location: {Address}.
    Press 1 if {User Name} is okay.
    Press 2 if you are checking on them.
    Press 3 if they need immediate help.
  </Say>
  <Gather numDigits="1" action="/api/emergency/contact-response" method="POST">
    <Pause length="3"/>
  </Gather>
</Response>
```

### SMS Template
```
EMERGENCY ALERT from RescueMate
{User Name} needs help!

Type: {Emergency Type}
Location: {Google Maps Link}
Heart Rate: {HeartRate} BPM (Normal: {NormalRate})

Reply:
1 - They're okay
2 - Checking on them
3 - Need help now
```

## ElevenLabs Voice AI Integration

### Configuration
```json
{
  "agentId": "YOUR_AGENT_ID",
  "apiKey": "YOUR_API_KEY",
  "model": "eleven_turbo_v2",
  "voiceId": "optional-voice-id",
  "language": "en"
}
```

### Conversation Events
```javascript
{
  "onConnect": "conversation-id",
  "onMessage": {
    "source": "user | agent",
    "message": "text content"
  },
  "onModeChange": "listening | speaking",
  "onError": "error message"
}
```

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 400 | Bad Request | Invalid request parameters |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |
| 503 | Service Unavailable | Service temporarily unavailable |

## Rate Limiting

- **Emergency Alerts**: 10 per hour per user
- **Contact Calls**: 50 per hour per account
- **Status Checks**: 100 per hour per user
- **Contact Responses**: Unlimited

## Webhooks

### Emergency Status Update
Your backend can receive status updates via webhook:

```json
POST {YOUR_WEBHOOK_URL}
{
  "event": "emergency_status_changed",
  "emergencyId": "string",
  "newStatus": "RESOLVED",
  "timestamp": "2024-12-01T10:40:00Z",
  "data": {
    "resolvedBy": "contact",
    "resolutionTime": 600
  }
}
```

### Twilio Call Status
```json
POST {YOUR_WEBHOOK_URL}
{
  "event": "call_status",
  "callSid": "twilio-sid",
  "status": "completed | failed | no-answer",
  "duration": 45,
  "emergencyId": "string"
}
```

## SDK Examples

### Android (Kotlin)
```kotlin
val emergencyService = TwilioEmergencyService(context)

val result = emergencyService.sendEmergencyContactAlert(emergencyEvent)
if (result.isSuccess) {
    val response = result.getOrNull()
    Log.d("Emergency", "Alert sent: ${response?.emergencyId}")
}
```

### cURL Examples

#### Send Emergency Alert
```bash
curl -X POST https://your-backend.com/api/emergency/contact-alert \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "emergencyType": "CARDIAC_ALERT",
    "healthData": {...},
    "location": {...}
  }'
```

#### Check Status
```bash
curl -X GET https://your-backend.com/api/emergency/{emergencyId}/status \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Testing

### Test Endpoints
Use test mode for development:
```
https://test.your-backend.com/api
```

### Test Credentials
```
API Key: test_key_xxxxxxxxxx
Test User ID: test-user-123
```

---

Last Updated: December 2024

