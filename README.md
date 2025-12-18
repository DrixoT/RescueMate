# RescueMate

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Demo](https://img.shields.io/badge/Demo-red.svg)](https://tryrescuemate.netlify.app/)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![Backend](https://img.shields.io/badge/Backend-Node.js-green.svg)](https://nodejs.org/)
[![Cloud](https://img.shields.io/badge/Cloud-Firebase-orange.svg)](https://firebase.google.com/)

## Hybrid AI Emergency Response System

RescueMate is an Android-based emergency response application that combines on-device AI (TinyLlama via llama.cpp) with cloud-based services (ElevenLabs, OpenAI) to provide reliable emergency assistance regardless of network connectivity. The system features a 3-phase emergency workflow, real-time health monitoring via Bluetooth sensors, and hybrid voice AI that seamlessly transitions between online and offline modes.

## Key Features

### Hybrid AI Architecture

**Online Mode**: When network connectivity is available, RescueMate utilizes ElevenLabs conversational AI to deliver context-aware, natural voice interactions with advanced reasoning and real-time response capabilities. The system also uses OpenAI GPT-3.5-turbo for generating structured medical summaries from conversation transcripts.

**Offline Mode**: In the absence of network access, the application automatically transitions to an offline mode powered by the TinyLlama language model (1.1B parameters, Q4_K_M quantization) executed locally via llama.cpp. The offline pipeline integrates Vosk for speech-to-text and Android TTS for text-to-speech, enabling essential conversational and decision-support functionalities without external dependencies. The transition between modes is seamless and requires no user intervention.

### Emergency Response System

**Three-Phase Emergency Workflow**:
1. **Phase 1 (60 seconds)**: User response check with prominent notification and audible alarm. If the user responds within this interval, the emergency is cancelled.
2. **Phase 2 (5 minutes)**: If no user response is detected, the system escalates to notifying predefined emergency contacts via Twilio. Contacts receive automated voice calls (generated using ElevenLabs text-to-speech) and SMS notifications containing real-time location links.
3. **Phase 3 (Reserved)**: Direct integration with emergency services (911) planned for future implementation.

**Emergency Triggers**:
- **Manual**: Panic button in UI or volume key combination (both volume keys held for 2 seconds)
- **Automatic**: Fall detection via accelerometer (sudden acceleration >15 m/s² followed by prolonged stillness) or abnormal vitals (risk score ≥0.7 for 5+ minutes)

### Health Monitoring

**Real-time Tracking**: The app continuously monitors health via Bluetooth Low Energy (BLE) sensors, tracking heart rate every 5 seconds. The Health Monitoring Service analyzes patterns using TinyLlama (primary, offline, private) or OpenAI GPT-4o (optional enhancement for complex cases), detecting anomalies such as sudden spikes (>120 BPM while resting) or critical drops (<40 BPM).

**Anomaly Detection**: The system maintains a sliding window of the last 100 heart rate readings and uses LLM-based analysis to identify abnormal patterns. Emergency is triggered when `isAbnormal = true`, `riskScore ≥ 0.7`, and `confidence ≥ 0.6`.

### Data Management

**Firebase Integration**: Secure user authentication and real-time data syncing across devices. User profiles, medical data (allergies, medications, conditions), and interaction logs are stored in Firestore with client-side encryption before transmission.

**Local Storage**: All sensitive data is encrypted using Android Keystore before storage. Emergency events and interaction transcripts are logged locally and synced to Firebase when connectivity is restored.

## System Architecture

### Android Application

**Technology Stack**:
- **Language**: Kotlin with Jetpack Compose for declarative UI
- **Architecture**: MVVM (Model-View-ViewModel) pattern with repository abstraction
- **Local AI**: JNI bindings for llama.cpp (TinyLlama inference) and Vosk-Android (offline speech recognition)
- **Cloud Services**: Firebase (Auth, Firestore, Storage, Functions)
- **Network**: OkHttp with certificate pinning for security

**Core Components**:
- **Emergency Detection Service**: Monitors sensors and health data for anomaly detection
- **Hybrid Voice AI Service**: Manages online/offline transitions between ElevenLabs and TinyLlama
- **Emergency Manager**: Orchestrates the 3-phase emergency workflow using coroutines
- **Health Monitoring Service**: Analyzes heart rate patterns using LLM-based analysis

### Backend Services

**Node.js Express Server**: Handles reliable external communications including:
- Emergency alert processing and storage in MongoDB
- Twilio integration for voice calls and SMS dispatch
- ElevenLabs API integration for emergency call audio generation
- Webhook handling for Twilio call status updates and contact responses
- FCM push notifications to emergency contacts with the app installed

## Performance Metrics

Based on comprehensive evaluation using 35 representative test cases:

- **Emergency Detection Accuracy**: 90.3% true positive rate (online mode), 83.2% (offline mode)
- **False Positive Rate**: 2.1% (online), 3.8% (offline)
- **AI Response Latency**: 420ms median (online), 1.9s median (offline)
- **System Availability**: 100% during network outages (offline mode)
- **Emergency Notification Success**: 97.1% when network is available

## Getting Started

### Prerequisites
-   **Android Studio**: Koala or newer (Project targets Android 15 / API 35).
-   **Node.js**: v18+ (for backend services).
-   **Firebase**: A valid `google-services.json` file.

### 1. Android Setup
1.  Clone the repository.
2.  Place your `google-services.json` file in the `app/` directory.
3.  Create a `.env` file in the **project root** (parent of `app/`) to configure the build:
    ```properties
    # AI Services
    OPENAI_API_KEY=sk-your_key_here
    ELEVEN_API_KEY=your_elevenlabs_key
    ELEVEN_AGENT_ID=your_agent_id

    # Maps & Location
    GOOGLE_MAPS_API_KEY=your_maps_key

    # Twilio (Client-side fallback)
    TWILIO_ACCOUNT_SID=your_sid
    TWILIO_AUTH_TOKEN=your_token
    TWILIO_PHONE_NUMBER=your_number
    ```
4.  Sync Gradle. The build script will automatically inject these keys into `BuildConfig`.
5.  Run on a device. (Note: Emulator support for Bluetooth and Sensors is limited; physical device recommended).

### 2. Backend Setup
The backend handles reliable emergency notifications.

1.  Navigate to the backend directory:
    ```bash
    cd backend-emergency
    ```
2.  Install dependencies:
    ```bash
    npm install
    ```
3.  Configure environment variables in `backend-emergency/.env`:
    ```properties
    PORT=3000
    MONGODB_URI=mongodb://localhost:27017/rescuemate
    TWILIO_ACCOUNT_SID=your_sid
    TWILIO_AUTH_TOKEN=your_token
    TWILIO_PHONE_NUMBER=your_number
    ```
4.  Start the server:
    ```bash
    npm start
    ```

## Technical Documentation

For comprehensive technical details, architecture diagrams, implementation specifics, and evaluation results, refer to the [ACM SIG format report](report/main.tex) included in this repository.

## Disclaimer

**RescueMate** is a support tool designed to assist in emergencies but **does not** replace professional emergency services (911). The health analysis provided by the AI is for informational purposes only and is not a medical diagnosis. Always seek professional medical help in life-threatening situations.
