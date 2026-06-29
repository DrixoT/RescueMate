# RescueMate

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Demo](https://img.shields.io/badge/Demo-Clickhere-red.svg)](https://tryrescuemate.netlify.app/)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![Backend](https://img.shields.io/badge/Backend-Node.js-green.svg)](https://nodejs.org/)
[![Cloud](https://img.shields.io/badge/Cloud-Firebase-orange.svg)](https://firebase.google.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Terms](https://img.shields.io/badge/Terms-Read%20Here-blue.svg)](TERMS_OF_SERVICE.md)

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
-   **Android Studio**: Koala or newer (Project targets Android 15 / API 35)
-   **Node.js**: v18+ (for backend services)
-   **Firebase**: A valid `google-services.json` file
-   **Local AI Models**: TinyLlama and Vosk models for offline functionality (see below)

### Step 1: Install Android Studio

1. Download and install [Android Studio](https://developer.android.com/studio) (Koala or newer)
2. During installation, ensure the following components are installed:
   - Android SDK (API 35)
   - Android SDK Platform-Tools
   - Android SDK Build-Tools
   - NDK (Native Development Kit)
3. Open Android Studio and complete the initial setup wizard

### Step 2: Clone the Repository

1. Clone the repository:
   ```bash
   git clone (https://github.com/DrixoT/RescueMate.git)
   cd RescueMate-2.0
   ```

### Step 3: Download Local AI Models

RescueMate requires local AI models for offline functionality. These models must be downloaded and placed in the correct directories before building the application.

**TinyLlama Model** (Required for offline LLM):
- **Model**: `TinyLlama-1.1B-Chat-v0.4-Q4_K_M.gguf` (~680MB)
- **Download**: [Hugging Face - TinyLlama Model](https://huggingface.co/TinyLlama/TinyLlama-1.1B-Chat-v0.4-GGUF)
- **Location**: Place the `.gguf` file in `app/src/main/assets/models/`
- **Note**: Look for the Q4_K_M quantized version specifically

**Vosk Speech Recognition Model** (Required for offline STT):
- **Model**: `vosk-model-small-en-us-0.15` (~40MB)
- **Download**: [Vosk Models - Small English US](https://alphacephei.com/vosk/models)
- **Location**: Extract the model directory to `app/src/main/assets/model/` (note: "model" singular, not "models")
- **Note**: Ensure the directory structure is `app/src/main/assets/model/vosk-model-small-en-us-0.15/`

**Directory Structure After Download**:
```
app/src/main/assets/
├── models/
│   └── TinyLlama-1.1B-Chat-v0.4-Q4_K_M.gguf
└── model/
    └── vosk-model-small-en-us-0.15/
        ├── am/
        ├── graph/
        └── ... (other model files)
```

### Step 4: Configure Firebase

1. Place your `google-services.json` file in the `app/` directory
2. Ensure the file is properly configured with your Firebase project credentials

### Step 5: Configure Environment Variables

Create a `.env` file in the **project root** (parent of `app/`) to configure the build:
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

Sync Gradle. The build script will automatically inject these keys into `BuildConfig`.

### Step 6: Build and Run

1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Connect an Android device via USB (or use an emulator, though Bluetooth and Sensors support is limited)
4. Enable USB debugging on your device
5. Click "Run" or press `Shift+F10` to build and install the app

**Note**: The first build may take several minutes as it compiles native libraries (llama.cpp) and processes the AI models. Ensure you have sufficient disk space (~2GB free recommended).

### Backend Setup (Optional)

The backend handles reliable emergency notifications. If you plan to use emergency contact calling features:

1. Navigate to the backend directory:
   ```bash
   cd backend-emergency
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Configure environment variables in `backend-emergency/.env`:
   ```properties
   PORT=3000
   MONGODB_URI=mongodb://localhost:27017/rescuemate
   TWILIO_ACCOUNT_SID=your_sid
   TWILIO_AUTH_TOKEN=your_token
   TWILIO_PHONE_NUMBER=your_number
   ```

4. Start the server:
   ```bash
   npm start
   ```

## License

This project uses **dual licensing**:

- **Source Code**: Licensed under the [MIT License](LICENSE) - you are free to use, modify, and distribute the code
- **Application Use**: Subject to [Terms of Service](TERMS_OF_SERVICE.md) - prohibits recreational use and restricts misuse

**Important**: While the source code is open-source under MIT License, using the RescueMate application is subject to strict Terms of Service that prohibit recreational, frivolous, or non-serious use. Please read the [Terms of Service](TERMS_OF_SERVICE.md) before using the application.

## Technical Documentation

For comprehensive technical details, architecture diagrams, implementation specifics, and evaluation results, refer to the [ACM SIG format report](report/main.tex) included in this repository.

## Disclaimer

**RescueMate** is a support tool designed to assist in emergencies but **does not** replace professional emergency services (911). The health analysis provided by the AI is for informational purposes only and is not a medical diagnosis. Always seek professional medical help in life-threatening situations.
