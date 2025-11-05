# RescueMate Emergency Backend

Emergency SOS backend with Twilio integration for emergency contact calling.

## Features

- ✅ **Emergency Contact Calling** via Twilio Voice API
- ✅ **Emergency SMS** with location and health data
- ✅ **Contact Response Tracking** (user safe/need help)
- ✅ **Interactive Voice Response** (IVR) for contact confirmations
- ✅ **SMS Reply Handling** with emergency ID validation
- ✅ **Emergency Event Database** with MongoDB
- ✅ **Webhook Support** for Twilio callbacks
- ⚠️ **Phase 3 Reserved:** Emergency services (911) integration

## Quick Start

```bash
# Install dependencies
npm install

# Configure environment
cp .env.example .env
# Edit .env with your credentials

# Start server
npm run dev
```

## Environment Setup

Required environment variables:

```env
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token_here
TWILIO_PHONE_NUMBER=+1234567890
MONGODB_URI=mongodb://localhost:27017/rescuemate-emergency
```

## API Endpoints

### Emergency Management

- `POST /api/emergency/contact-alert` - Create emergency alert
- `POST /api/emergency/contact-call` - Call emergency contact
- `POST /api/emergency/contact-response` - Record contact response
- `POST /api/emergency/cancel` - Cancel active emergency
- `GET /api/emergency/:emergencyId/status` - Get emergency status
- `GET /api/emergency/call-status/:callSid` - Get Twilio call status

### Webhooks (Twilio Callbacks)

- `GET /api/webhooks/emergency-voice` - Generate TwiML for voice call
- `POST /api/webhooks/emergency-response` - Handle voice keypress response
- `POST /api/webhooks/call-status` - Receive call status updates
- `POST /api/webhooks/sms-status` - Receive SMS status updates
- `POST /api/webhooks/sms-reply` - Handle incoming SMS replies

### Utility

- `GET /health` - Health check endpoint
- `GET /api/twilio/test` - Test Twilio configuration

## Testing

```bash
# Test health check
curl http://localhost:3000/health

# Test Twilio config
curl http://localhost:3000/api/twilio/test

# Test emergency alert
curl -X POST http://localhost:3000/api/emergency/contact-alert \
  -H "Content-Type: application/json" \
  -d @test/sample-emergency.json
```

## Production Deployment

### Heroku

```bash
heroku create rescuemate-emergency
heroku addons:create mongolab
heroku config:set TWILIO_ACCOUNT_SID=xxx
heroku config:set TWILIO_AUTH_TOKEN=xxx
heroku config:set TWILIO_PHONE_NUMBER=xxx
git push heroku main
```

### AWS/Digital Ocean

1. Set up Node.js server
2. Install MongoDB
3. Configure nginx reverse proxy
4. Set up SSL certificate
5. Configure environment variables
6. Set up PM2 for process management

## Twilio Configuration

1. Purchase phone number with Voice + SMS capabilities
2. Configure Voice webhook: `https://your-domain.com/api/webhooks/emergency-voice`
3. Configure SMS webhook: `https://your-domain.com/api/webhooks/sms-reply`
4. Configure status callbacks

## Database Schema

### Emergency Collection

```javascript
{
  userId: String,
  emergencyType: String,
  status: String,
  phase: Number,
  healthData: Object,
  location: Object,
  userInfo: Object,
  contactAttempts: [Object],
  responses: [Object],
  triggeredAt: Date,
  resolvedAt: Date
}
```

## Security

- Rate limiting enabled (100 requests per 15 minutes)
- Helmet.js for HTTP security headers
- CORS configured
- Input validation on all endpoints
- Secure MongoDB connection

## Monitoring

- Winston logging to files
- Error logging to `logs/error.log`
- Emergency logs to `logs/emergency.log`
- Console logging in development

## License

MIT License - Part of RescueMate Emergency SOS System

## Support

For issues or questions, see `EMERGENCY_SOS_INTEGRATION_GUIDE.md`

