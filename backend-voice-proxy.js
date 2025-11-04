/**
 * SECURE BACKEND EXAMPLE - Voice AI Proxy Server
 *
 * This is a Node.js/Express backend that securely handles ElevenLabs API calls.
 * Deploy this on a server (Vercel, Railway, Heroku, etc.)
 *
 * Setup:
 * 1. npm install express cors dotenv node-fetch
 * 2. Create .env file with: ELEVENLABS_API_KEY=your-key-here
 * 3. Run: node backend-voice-proxy.js
 */

const express = require('express');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(express.json());
app.use(cors({
  origin: process.env.FRONTEND_URL || 'http://localhost:5173', // Your frontend URL
  credentials: true
}));

// SECURITY: Get API keys from environment variables (NEVER from frontend)
const ELEVENLABS_API_KEY = process.env.ELEVENLABS_API_KEY;
const GOOGLE_MAPS_API_KEY = process.env.GOOGLE_MAPS_API_KEY;

if (!ELEVENLABS_API_KEY) {
  console.error('❌ ELEVENLABS_API_KEY not found in environment variables!');
  process.exit(1);
}

if (!GOOGLE_MAPS_API_KEY) {
  console.warn('⚠️  GOOGLE_MAPS_API_KEY not found. Location services will be limited.');
}

// Simple rate limiting (in production, use redis + express-rate-limit)
const callLimits = new Map(); // userId -> { count, resetTime }

function checkRateLimit(userId) {
  const now = Date.now();
  const limit = callLimits.get(userId);

  if (!limit || now > limit.resetTime) {
    // Reset or create new limit
    callLimits.set(userId, {
      count: 1,
      resetTime: now + 15 * 60 * 1000 // 15 minutes
    });
    return true;
  }

  if (limit.count >= 5) { // Max 5 calls per 15 minutes
    return false;
  }

  limit.count++;
  return true;
}

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

/**
 * Emergency Voice Call Endpoint
 * POST /api/voice/emergency-call
 *
 * Body: {
 *   text: string,
 *   voiceId: string,
 *   userId: string (from authentication)
 * }
 */
app.post('/api/voice/emergency-call', async (req, res) => {
  try {
    // 1. AUTHENTICATION (In production, verify JWT token)
    const userId = req.headers['x-user-id'] || 'demo-user';

    // For production, use proper JWT:
    // const token = req.headers['authorization']?.replace('Bearer ', '');
    // const user = verifyJWT(token);
    // if (!user) return res.status(401).json({ error: 'Unauthorized' });

    // 2. RATE LIMITING
    if (!checkRateLimit(userId)) {
      return res.status(429).json({
        error: 'Rate limit exceeded. Max 5 calls per 15 minutes.'
      });
    }

    // 3. VALIDATE INPUT
    const { text, voiceId } = req.body;

    if (!text || text.length > 5000) {
      return res.status(400).json({
        error: 'Text is required and must be under 5000 characters'
      });
    }

    if (!voiceId) {
      return res.status(400).json({ error: 'Voice ID is required' });
    }

    console.log(`📞 Processing emergency call for user: ${userId}`);
    console.log(`📝 Text length: ${text.length} characters`);

    // 4. CALL ELEVENLABS API (API key secure on backend)
    const response = await fetch(
      `https://api.elevenlabs.io/v1/text-to-speech/${voiceId}`,
      {
        method: 'POST',
        headers: {
          'Accept': 'audio/mpeg',
          'Content-Type': 'application/json',
          'xi-api-key': ELEVENLABS_API_KEY // ✅ Secure - only exists on backend
        },
        body: JSON.stringify({
          text: text,
          model_id: 'eleven_multilingual_v2',
          voice_settings: {
            stability: 0.5,
            similarity_boost: 0.75
          }
        })
      }
    );

    if (!response.ok) {
      const errorText = await response.text();
      console.error('ElevenLabs API Error:', errorText);
      return res.status(response.status).json({
        error: 'Failed to generate audio',
        details: errorText
      });
    }

    // 5. LOG USAGE (for monitoring and billing)
    logAPIUsage({
      userId,
      endpoint: 'emergency-call',
      characterCount: text.length,
      timestamp: new Date(),
      success: true
    });

    // 6. RETURN AUDIO TO FRONTEND
    const audioBuffer = await response.arrayBuffer();
    res.set('Content-Type', 'audio/mpeg');
    res.send(Buffer.from(audioBuffer));

    console.log(`✅ Emergency call generated successfully for ${userId}`);

  } catch (error) {
    console.error('❌ Server Error:', error);
    res.status(500).json({
      error: 'Internal server error',
      message: process.env.NODE_ENV === 'development' ? error.message : undefined
    });
  }
});

/**
 * Test Voice AI Endpoint
 * POST /api/voice/test
 */
app.post('/api/voice/test', async (req, res) => {
  try {
    const { userName } = req.body;
    const testText = `Hello ${userName || 'User'}, this is a test of the RescueMate Voice AI system. If you can hear this message clearly, the Voice AI is working correctly.`;

    const response = await fetch(
      `https://api.elevenlabs.io/v1/text-to-speech/JBFqnCBsd6RMkjVDRZzb`,
      {
        method: 'POST',
        headers: {
          'Accept': 'audio/mpeg',
          'Content-Type': 'application/json',
          'xi-api-key': ELEVENLABS_API_KEY
        },
        body: JSON.stringify({
          text: testText,
          model_id: 'eleven_multilingual_v2'
        })
      }
    );

    if (!response.ok) {
      throw new Error('ElevenLabs API request failed');
    }

    const audioBuffer = await response.arrayBuffer();
    res.set('Content-Type', 'audio/mpeg');
    res.send(Buffer.from(audioBuffer));

  } catch (error) {
/**
 * Reverse Geocoding Endpoint
 * POST /api/location/reverse-geocode
 * Converts GPS coordinates to human-readable address
 *
 * Body: {
 *   latitude: number,
 *   longitude: number
 * }
 */
app.post('/api/location/reverse-geocode', async (req, res) => {
  try {
    const { latitude, longitude } = req.body;

    if (!latitude || !longitude) {
      return res.status(400).json({ error: 'Latitude and longitude are required' });
    }

    if (!GOOGLE_MAPS_API_KEY) {
      return res.status(503).json({ error: 'Google Maps API not configured' });
    }

    const url = `https://maps.googleapis.com/maps/api/geocode/json?latlng=${latitude},${longitude}&key=${GOOGLE_MAPS_API_KEY}`;

    const response = await fetch(url);
    const data = await response.json();

    if (data.status !== 'OK') {
  console.log('🚀 RescueMate Secure API Proxy Server');
      return res.status(400).json({
  console.log(`🔐 ElevenLabs API: ${ELEVENLABS_API_KEY.substring(0, 15)}...`);
  console.log(`🗺️  Google Maps API: ${GOOGLE_MAPS_API_KEY ? GOOGLE_MAPS_API_KEY.substring(0, 15) + '...' : 'NOT CONFIGURED'}`);
        details: data.status
      });
  console.log('Available Endpoints:');

    const result = data.results[0];
    res.json({
  console.log('  POST /api/location/reverse-geocode');
  console.log('  POST /api/location/geocode');
  console.log('');
  console.log('✅ All API keys secured on backend - Safe for mobile app deployment!');
      formattedAddress: result.formatted_address,
      addressComponents: result.address_components,
      placeId: result.place_id
    });

    console.log(`✅ Reverse geocoded: ${latitude}, ${longitude} -> ${result.formatted_address}`);

  } catch (error) {
    console.error('Reverse geocoding error:', error);
    res.status(500).json({ error: 'Failed to reverse geocode location' });
  }
});

/**
 * Forward Geocoding Endpoint
 * POST /api/location/geocode
 * Converts address to GPS coordinates
 *
 * Body: {
 *   address: string
 * }
 */
app.post('/api/location/geocode', async (req, res) => {
  try {
    const { address } = req.body;

    if (!address) {
      return res.status(400).json({ error: 'Address is required' });
    }

    if (!GOOGLE_MAPS_API_KEY) {
      return res.status(503).json({ error: 'Google Maps API not configured' });
    }

    const url = `https://maps.googleapis.com/maps/api/geocode/json?address=${encodeURIComponent(address)}&key=${GOOGLE_MAPS_API_KEY}`;

    const response = await fetch(url);
    const data = await response.json();

    if (data.status !== 'OK') {
      console.error('Google Maps API Error:', data.status);
      return res.status(400).json({
        error: 'Geocoding failed',
        details: data.status
      });
    }

    const result = data.results[0];
    const location = result.geometry.location;

    res.json({
      latitude: location.lat,
      longitude: location.lng,
      formattedAddress: result.formatted_address,
      placeId: result.place_id
    });

    console.log(`✅ Geocoded: ${address} -> ${location.lat}, ${location.lng}`);

  } catch (error) {
    console.error('Geocoding error:', error);
    res.status(500).json({ error: 'Failed to geocode address' });
  }
});

    console.error('Test endpoint error:', error);
    res.status(500).json({ error: 'Test failed' });
  }
});

// Simple usage logging (in production, use a database)
function logAPIUsage(log) {
  const timestamp = log.timestamp.toISOString();
  console.log(`[${timestamp}] API Usage:`, {
    userId: log.userId,
    endpoint: log.endpoint,
    characters: log.characterCount,
    success: log.success
  });

  // In production:
  // await db.apiLogs.insert(log);
  // await updateUserQuota(log.userId, log.characterCount);
}

// Error handling middleware
app.use((error, req, res, next) => {
  console.error('Unhandled error:', error);
  res.status(500).json({ error: 'Internal server error' });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({ error: 'Endpoint not found' });
});

// Start server
app.listen(PORT, () => {
  console.log('🚀 RescueMate Voice AI Proxy Server');
  console.log(`📡 Server running on port ${PORT}`);
  console.log(`🔐 API key loaded: ${ELEVENLABS_API_KEY.substring(0, 10)}...`);
  console.log(`🌍 CORS enabled for: ${process.env.FRONTEND_URL || 'localhost'}`);
  console.log('');
  console.log('Endpoints:');
  console.log('  GET  /health');
  console.log('  POST /api/voice/emergency-call');
  console.log('  POST /api/voice/test');
});

module.exports = app;

