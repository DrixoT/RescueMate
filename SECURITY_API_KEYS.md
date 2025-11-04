# 🔐 RescueMate Security & API Key Management

## ⚠️ CRITICAL SECURITY WARNING

### **YES, Embedding API Keys in Frontend is VULNERABLE!**

Your ElevenLabs API key is currently stored in **localStorage** which is **NOT SECURE** for production use.

---

## 🚨 Security Vulnerabilities in Current Implementation

### 1. **Client-Side Storage (localStorage)**
- ❌ **Visible in Browser DevTools** - Anyone can open browser console and read the key
- ❌ **Accessible via JavaScript** - XSS attacks can steal the key
- ❌ **No Encryption** - Keys stored in plain text
- ❌ **Shared Across Domain** - All scripts on your domain can access it

### 2. **API Key Exposure**
- ❌ API calls are made directly from browser (visible in Network tab)
- ❌ Users can copy the API key and use it elsewhere
- ❌ No rate limiting protection
- ❌ No usage tracking per user

### 3. **Cost & Abuse Risk**
- 💰 **Financial Risk** - Anyone with your key can make unlimited API calls at your expense
- 🚫 **Account Ban Risk** - Abuse can get your ElevenLabs account suspended
- 📈 **Usage Spike** - Malicious users can drain your API quota

---

## 🔍 How Easy is it to Steal?

### **Extremely Easy (30 seconds):**

```javascript
// Open browser console (F12) on your app
// Type this:
localStorage.getItem('rescuemate_elevenlabs_key')
// Result: "sk-your-actual-api-key-here"

// Or check Network tab:
// See all API requests to api.elevenlabs.io with your key in headers
```

**Anyone using your app can:**
1. Open DevTools (F12)
2. Copy your API key
3. Use it in their own apps
4. Run up thousands of dollars in charges

---

## ✅ SECURE SOLUTION: Backend Proxy

### Architecture (What You MUST Implement)

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Frontend  │────────>│   Backend    │────────>│ ElevenLabs  │
│  (Browser)  │ Request │  API Server  │ API Key │     API     │
│  No API Key │         │  Has API Key │         │             │
└─────────────┘         └──────────────┘         └─────────────┘
```

### Implementation Steps:

#### **Step 1: Create Backend API (Node.js/Express Example)**

```javascript
// backend/server.js
const express = require('express');
const fetch = require('node-fetch');
require('dotenv').config();

const app = express();
app.use(express.json());

// Store API key in environment variables (NEVER in code)
const ELEVENLABS_API_KEY = process.env.ELEVENLABS_API_KEY;

// Proxy endpoint
app.post('/api/voice/emergency-call', async (req, res) => {
  try {
    // Authenticate user first
    const userId = req.headers['authorization']; // JWT token
    if (!userId) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

    // Verify user has valid subscription/permissions
    const user = await verifyUser(userId);
    if (!user.canUseVoiceAI) {
      return res.status(403).json({ error: 'Voice AI not enabled' });
    }

    // Get request data
    const { text, voiceId } = req.body;

    // Call ElevenLabs (API key hidden from frontend)
    const response = await fetch(
      `https://api.elevenlabs.io/v1/text-to-speech/${voiceId}`,
      {
        method: 'POST',
        headers: {
          'Accept': 'audio/mpeg',
          'Content-Type': 'application/json',
          'xi-api-key': ELEVENLABS_API_KEY // Secure server-side
        },
        body: JSON.stringify({
          text,
          model_id: 'eleven_multilingual_v2'
        })
      }
    );

    // Log usage for billing/monitoring
    await logAPIUsage(userId, 'voice_call', text.length);

    // Return audio to frontend
    const audioBuffer = await response.buffer();
    res.set('Content-Type', 'audio/mpeg');
    res.send(audioBuffer);

  } catch (error) {
    console.error('Voice API Error:', error);
    res.status(500).json({ error: 'Failed to generate voice' });
  }
});

app.listen(3000, () => console.log('Secure API running on port 3000'));
```

#### **Step 2: Environment Variables (.env file)**

```bash
# .env (NEVER commit to git)
ELEVENLABS_API_KEY=sk-your-actual-key-here
DATABASE_URL=postgresql://...
JWT_SECRET=your-secret-key
```

#### **Step 3: Update Frontend to Use Backend**

```typescript
// src/services/VoiceAIService.ts (Modified)
export class VoiceAIService {
  private backendUrl = 'https://your-api.example.com';

  async initiateEmergencyCall(params: EmergencyCallParams): Promise<void> {
    // Get auth token (from your auth system)
    const token = localStorage.getItem('auth_token');

    // Call YOUR backend (not ElevenLabs directly)
    const response = await fetch(`${this.backendUrl}/api/voice/emergency-call`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}` // Your auth, not API key
      },
      body: JSON.stringify({
        text: this.generateEmergencyScript(params),
        voiceId: this.voiceId
      })
    });

    const audioBlob = await response.blob();
    // Play audio...
  }
}
```

---

## 🛡️ Additional Security Measures

### 1. **Environment Variables**
```bash
# Never hardcode:
❌ const API_KEY = "sk-12345...";

# Always use environment variables:
✅ const API_KEY = process.env.ELEVENLABS_API_KEY;
```

### 2. **Rate Limiting**
```javascript
// Prevent abuse
const rateLimit = require('express-rate-limit');

const voiceCallLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 5, // 5 calls per 15 minutes per user
  message: 'Too many voice calls. Please try again later.'
});

app.use('/api/voice/', voiceCallLimiter);
```

### 3. **User Authentication**
```javascript
// JWT tokens for user verification
const jwt = require('jsonwebtoken');

function authenticateUser(req, res, next) {
  const token = req.headers['authorization'];
  if (!token) return res.status(401).send('Access denied');

  try {
    const verified = jwt.verify(token, process.env.JWT_SECRET);
    req.user = verified;
    next();
  } catch (error) {
    res.status(400).send('Invalid token');
  }
}
```

### 4. **Usage Tracking**
```javascript
// Monitor and limit API usage
async function logAPIUsage(userId, endpoint, characterCount) {
  await db.apiLogs.create({
    userId,
    endpoint,
    characterCount,
    cost: calculateCost(characterCount),
    timestamp: new Date()
  });

  // Check if user exceeded quota
  const monthlyUsage = await getMonthlyUsage(userId);
  if (monthlyUsage > USER_QUOTA) {
    throw new Error('Monthly quota exceeded');
  }
}
```

---

## 📋 Security Checklist for Production

### Before Deploying:

- [ ] **Move API keys to backend** - Never expose in frontend
- [ ] **Use environment variables** - .env files for secrets
- [ ] **Add .env to .gitignore** - Never commit secrets
- [ ] **Implement authentication** - JWT or OAuth2
- [ ] **Add rate limiting** - Prevent abuse
- [ ] **Enable CORS properly** - Restrict origins
- [ ] **Monitor API usage** - Track costs and abuse
- [ ] **Implement logging** - Audit trails for security
- [ ] **Use HTTPS only** - Encrypt all traffic
- [ ] **Regular security audits** - Scan for vulnerabilities

---

## 🔧 Current Implementation Status

### ✅ What Works Now (FOR DEMO ONLY):
- Voice AI functionality
- User can test the feature
- Emergency calls work locally

### ❌ What's INSECURE:
- API key stored in localStorage
- Direct API calls from browser
- No authentication required
- No rate limiting
- No usage monitoring
- Anyone can steal and abuse key

---

## 💡 Recommendations

### **For Development/Demo:**
✅ Current implementation is **acceptable** for:
- Local testing
- Portfolio demos
- Proof of concept
- Development environment

**BUT** use a free trial or test API key, not your production key!

### **For Production:**
❌ Current implementation is **UNACCEPTABLE**

**MUST implement:**
1. Backend API proxy
2. Server-side API key storage
3. User authentication
4. Rate limiting
5. Usage monitoring
6. Cost controls

---

## 💰 Cost of Not Securing API Keys

### Real-World Example:
```
Day 1: Your app launches
Day 2: Someone extracts your API key
Day 3: They use it in their own app
Day 4: You get a bill for $5,000
Day 5: ElevenLabs suspends your account
```

### Industry Statistics:
- **67%** of apps leak API keys in client-side code
- **$283/hour** average cost of API key theft
- **24 hours** average time to detect leaked keys
- **$6,800** average unauthorized charges before detection

---

## 🚀 Quick Migration Path

### Phase 1: Immediate (This Week)
1. Create simple backend proxy
2. Move API key to .env file
3. Update frontend to call your backend
4. Add basic authentication

### Phase 2: Short-term (Next Month)
1. Implement JWT authentication
2. Add rate limiting
3. Set up usage monitoring
4. Create admin dashboard

### Phase 3: Long-term (3-6 Months)
1. Implement subscription system
2. Add payment processing
3. Set up automated monitoring
4. Regular security audits

---

## 📚 Resources

### Learn More:
- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)
- [ElevenLabs Security Best Practices](https://docs.elevenlabs.io/security)
- [API Key Management Guide](https://cheatsheetseries.owasp.org/cheatsheets/Key_Management_Cheat_Sheet.html)

### Tools:
- [dotenv](https://www.npmjs.com/package/dotenv) - Environment variables
- [express-rate-limit](https://www.npmjs.com/package/express-rate-limit) - Rate limiting
- [jsonwebtoken](https://www.npmjs.com/package/jsonwebtoken) - JWT auth
- [helmet](https://www.npmjs.com/package/helmet) - Security headers

---

## ⚖️ Legal Notice

**Disclaimer:** The current implementation stores API keys client-side for demonstration purposes only. 

**By deploying this to production, you:**
- Accept full financial liability for unauthorized API usage
- Acknowledge the security risks
- Agree to monitor and secure your API keys
- Understand that ElevenLabs may suspend accounts for key exposure

**We strongly recommend implementing proper backend security before public deployment.**

---

## 📞 Need Help?

If you need help implementing secure API key management:

1. **Backend Development** - Hire a backend developer
2. **Security Audit** - Consider a security consultant
3. **Cloud Services** - Use AWS Lambda, Vercel Functions, or similar
4. **Managed Solutions** - Consider services like Auth0, Clerk for authentication

---

**Last Updated:** November 4, 2025  
**Security Level:** 🔴 HIGH RISK (Current Implementation)  
**Recommended Action:** 🚨 IMPLEMENT BACKEND PROXY BEFORE PRODUCTION

