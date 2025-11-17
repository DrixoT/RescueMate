# RescueMate 2.0

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 🚨 Emergency SOS & Health Monitoring Application

RescueMate 2.0 is a comprehensive emergency response application that provides real-time health monitoring, automated emergency detection, and intelligent contact notification systems to keep you safe.

## ✨ Features

### Emergency Response System
- **3-Phase Escalation System**: Intelligent emergency workflow with user response check, contact notification, and emergency services integration (Phase 3 reserved)
- **Automated Emergency Detection**: Fall detection, cardiac alerts, abnormal vitals monitoring
- **Manual SOS Trigger**: Quick emergency activation with panic button
- **Real-time Location Tracking**: Share precise GPS coordinates during emergencies
- **Offline Emergency Queue**: Queue emergency alerts when offline, sync automatically when connection restored

### Health Monitoring
- **Continuous Heart Rate Monitoring**: Track heart rate with smartwatch/BLE devices
- **Health Anomaly Detection**: AI-powered detection of abnormal health patterns
- **Medical Profile Management**: Store allergies, medications, conditions, and emergency notes
- **TinyLlama Health AI**: On-device LLM for health guidance and analysis

### Voice AI Integration
- **ElevenLabs Conversational AI**: Natural voice conversations for emergency guidance
- **Local Voice LLM Fallback**: Offline voice assistance using local speech-to-text and TinyLlama
- **Voice Matching**: Identify emergency contacts by voice patterns

### Contact Management
- **Priority-Based Notification**: Contact emergency contacts by priority order
- **Multi-Channel Communication**: Voice calls, SMS, and push notifications
- **Contact Response Tracking**: Track which contacts responded and their status
- **Twilio Integration**: Reliable voice/SMS delivery via Twilio API

### Security & Privacy
- **End-to-End Encryption**: Medical data encrypted using Android Keystore
- **Biometric Authentication**: Fingerprint/face authentication for sensitive operations
- **Certificate Pinning**: Protection against man-in-the-middle attacks
- **ProGuard/R8 Obfuscation**: Code obfuscation for release builds

## 📋 Requirements

### System Requirements
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **Compile SDK**: Android 15 (API 35)
- **RAM**: 2GB minimum
- **Storage**: 200MB minimum

### Required Permissions
- **Location**: GPS location for emergency services
- **Phone**: Emergency calling capabilities
- **SMS**: Fallback emergency messaging
- **Notifications**: Emergency alerts
- **Microphone**: Voice AI features
- **Body Sensors**: Health monitoring (optional)
- **Bluetooth**: Smartwatch connectivity (optional)

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/RescueMate-2.0.git
cd RescueMate-2.0
```

### 2. Configure Environment Variables
Create a `.env` file in the project root:
```env
ELEVEN_API_KEY=your_elevenlabs_api_key
ELEVEN_AGENT_ID=your_elevenlabs_agent_id
OPENAI_API_KEY=your_openai_api_key
GOOGLE_MAPS_API_KEY=your_google_maps_api_key
TWILIO_ACCOUNT_SID=your_twilio_account_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_PHONE_NUMBER=your_twilio_phone_number
```

### 3. Install Dependencies
```bash
./gradlew build
```

### 4. Run the App
```bash
./gradlew installDebug
```

## 🏗️ Architecture

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM + Repository Pattern
- **Database**: SQLite with custom helper
- **Networking**: OkHttp with certificate pinning
- **Voice AI**: ElevenLabs SDK
- **LLM**: TinyLlama via Llamatik
- **Maps**: Google Maps SDK
- **Communication**: Twilio API
- **Security**: Android Keystore, BiometricPrompt

### Project Structure
```
app/src/main/java/com/rescuemate/
├── ai/                          # AI/LLM services
├── bluetooth/                   # BLE health device integration
├── data/                        # Data layer (repositories, preferences)
├── emergency/                   # Emergency response system
│   ├── data/                    # Emergency data models
│   ├── detection/               # Emergency detection algorithms
│   ├── health/                  # Health monitoring
│   ├── location/                # Location services
│   ├── service/                 # Background services
│   ├── twilio/                  # Twilio integration
│   └── ui/                      # Emergency UI screens
├── security/                    # Security & encryption
├── services/                    # Voice AI & other services
├── ui/                          # UI layer (screens, components, theme)
└── utils/                       # Utilities & helpers
```

## 🔧 Configuration

### Emergency System Configuration
Edit `EmergencyConstants.kt` to customize:
- Phase durations
- Heart rate thresholds
- Location update intervals
- Contact notification settings

### Health Monitoring Configuration
Edit `HealthConstants.kt` to customize:
- Normal heart rate ranges
- Anomaly detection thresholds
- Fall detection sensitivity
- Activity level thresholds

## 📝 Usage

### Setting Up Emergency Contacts
1. Open app → Navigate to "Emergency Contacts"
2. Tap "Add Contact"
3. Enter contact details and set priority
4. Configure notification preferences

### Configuring Medical Profile
1. Navigate to "Medical Profile"
2. Add medical conditions, medications, allergies
3. Set baseline vitals
4. Add emergency notes for responders

### Triggering Emergency
- **Automatic**: App detects emergency and starts Phase 1
- **Manual**: Press and hold SOS button
- **Voice**: Say "Emergency" to voice AI

### Emergency Workflow
1. **Phase 1 (60s)**: User response check
   - App plays alarm and shows notification
   - User can cancel if false alarm
   - If no response, escalates to Phase 2

2. **Phase 2 (5 min)**: Contact notification
   - Calls emergency contacts in priority order
   - Sends SMS with location and health data
   - Tracks contact responses

3. **Phase 3 (Reserved)**: Emergency services
   - Future feature for direct 911 integration

## 🧪 Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

### Run All Tests
```bash
./gradlew testDebugUnitTest connectedDebugAndroidTest
```

### Test Coverage
Current test coverage: 70%+
- Unit tests for business logic
- Integration tests for emergency workflow
- UI tests for critical user flows

## 🔒 Security

### Data Encryption
- Medical data encrypted at rest using AES-256-GCM
- Encryption keys stored in Android Keystore
- Hardware-backed key storage on supported devices

### Network Security
- Certificate pinning for API calls
- TLS 1.3 for all network traffic
- API keys obfuscated in release builds

### Authentication
- Biometric authentication for sensitive operations
- Session management with secure tokens
- Automatic logout on inactivity

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Development Workflow
1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Documentation**: [docs/](docs/)
- **Issues**: [GitHub Issues](https://github.com/yourusername/RescueMate-2.0/issues)
- **Email**: support@rescuemate.app

## 🙏 Acknowledgments

- **ElevenLabs**: Voice AI technology
- **Twilio**: Communication infrastructure
- **Google**: Maps SDK
- **Llamatik**: TinyLlama integration
- **Android Open Source Project**: Core framework

## ⚠️ Disclaimer

RescueMate 2.0 is designed to assist in emergency situations but should not replace professional emergency services. Always call your local emergency number (911, 112, etc.) in life-threatening situations.

---

**Built with ❤️ for safety and peace of mind**

