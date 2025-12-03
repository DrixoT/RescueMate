# RescueMate

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![Backend](https://img.shields.io/badge/Backend-Node.js-green.svg)](https://nodejs.org/)
[![Cloud](https://img.shields.io/badge/Cloud-Firebase-orange.svg)](https://firebase.google.com/)

## 🛡️ Hybrid AI Emergency & Health Assistant

RescueMate is an offline-first emergency response and health monitoring application. It combines on-device AI with cloud capabilities to ensure safety even without internet access.

## ✨ Key Features

### 🤖 Hybrid AI Assistance
-   **Online Mode**:
    -   **ElevenLabs Integration**: Natural, high-fidelity voice conversations for calming guidance during emergencies.
    -   **OpenAI Analysis**: Generates detailed medical summaries from conversation transcripts for first responders.
-   **Offline Mode (Privacy-First)**:
    -   **On-Device Intelligence**: Uses **TinyLlama** (via llama.cpp) for strictly local health advice and emergency triage when disconnected.
    -   **Offline STT**: **Vosk** speech recognition ensures voice commands and transcription work without data.

### 🚨 Emergency Response System
-   **3-Phase Workflow**:
    1.  **User Verification**: 60s countdown with alarm to prevent false positives.
    2.  **Contact Notification**: Automated calls and SMS to emergency contacts via **Twilio**.
    3.  **Emergency Services**: (Reserved for future direct 911 integration).
-   **Panic Button**: Instant manual SOS trigger.
-   **Automated Detection**: Fall detection and abnormal vitals monitoring algorithms.

### ☁️ Cloud & Data Sync
-   **Firebase Integration**: Secure user authentication and real-time data syncing across devices.
-   **Medical Profile**: Encrypted storage of allergies, medications, conditions, and emergency notes.
-   **Interaction Logs**: Comprehensive history of all AI conversations and emergency events stored in Firestore.

### 🩺 Health Monitoring
-   **Real-time Tracking**: Continuous monitoring of heart rate and activity levels via Bluetooth LE sensors.
-   **Anomaly Detection**: On-device algorithms to detect irregular health patterns.
-   **Mock Data Support**: Built-in simulation tools for testing alerts and workflows without physical hardware.

## 🏗️ Architecture

### Mobile (Android)
-   **Language**: Kotlin (Jetpack Compose for UI).
-   **Local AI**: JNI bindings for `llama.cpp` (LLM) and `Vosk-Android` (Speech-to-Text).
-   **Cloud**: Firebase (Auth, Firestore, Storage, Functions).
-   **Network**: OkHttp with certificate pinning for security.

### Backend (Node.js)
-   **Service**: Dedicated Express.js server for reliable external communications.
-   **Database**: MongoDB (for backend-specific logs and state management).
-   **Telephony**: Twilio API integration for high-priority voice calls and SMS dispatch.

## 🚀 Getting Started

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

## ⚠️ Disclaimer
**RescueMate** is a support tool designed to assist in emergencies but **does not** replace professional emergency services (911/112). The "Health Analysis" provided by the AI is for informational purposes only and is not a medical diagnosis. Always seek professional medical help in life-threatening situations.
