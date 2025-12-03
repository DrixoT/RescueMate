# RescueMate Architecture

RescueMate is designed as an **offline-first** emergency response application. It prioritizes local processing for critical features (emergency detection, basic voice assistance) while leveraging cloud services for high-fidelity interactions and reliable external communications when connectivity is available.

## System Architecture Diagram

```mermaid
C4Context
    title System Context Diagram for RescueMate

    Person(user, "User", "A person using the app for safety monitoring")
    Person(contact, "Emergency Contact", "Friend/Family receiving alerts")

    System_Boundary(rescuemate_system, "RescueMate System") {
        
        Container_Boundary(android_app, "Android Application") {
            Component(ui_layer, "UI Layer", "Jetpack Compose", "User interactions, Panic Button, Dashboards")
            
            Component(emergency_manager, "Emergency Manager", "Kotlin", "Orchestrates 3-Phase Workflow, State Machine")
            
            Component(detection_service, "Detection Service", "Kotlin/Sensors", "Fall detection, Heart rate monitoring")
            
            Component(local_ai, "Local AI Engine", "C++/JNI", "Vosk (STT), TinyLlama (LLM) via llama.cpp")
            
            Component(cloud_integration, "Cloud Integration", "Retrofit/OkHttp", "API Client for Backend & AI Services")
        }

        Container_Boundary(backend, "Backend Services") {
            Component(api_server, "Emergency API", "Node.js/Express", "Handles alert dispatch, webhooks")
            Component(db_mongo, "Backend DB", "MongoDB", "Stores logs, temporary state")
        }
    }

    System_Ext(firebase, "Firebase", "Auth, Firestore (User Profiles), Storage")
    System_Ext(twilio, "Twilio", "Voice Calls, SMS Dispatch")
    System_Ext(elevenlabs, "ElevenLabs", "High-fidelity TTS & Voice Intelligence")
    System_Ext(openai, "OpenAI", "Medical summarization & Analysis")

    %% Relationships
    Rel(user, ui_layer, "Interacts with")
    Rel(detection_service, emergency_manager, "Triggers Alert")
    Rel(ui_layer, emergency_manager, "Manual SOS")
    
    %% Internal Android Flows
    Rel(emergency_manager, cloud_integration, "Sends Alerts")
    Rel(emergency_manager, local_ai, "Fallback Voice/Guidance")
    
    %% Cloud Flows
    Rel(cloud_integration, api_server, "POST /emergency/alert")
    Rel(cloud_integration, firebase, "Syncs Profile/Logs")
    Rel(cloud_integration, openai, "Analysis Requests")
    Rel(cloud_integration, elevenlabs, "Streaming Audio")

    %% Backend Flows
    Rel(api_server, twilio, "Initiates Calls/SMS")
    Rel(api_server, db_mongo, "Persists Events")

    %% External Deliveries
    Rel(twilio, contact, "Calls/Texts")
```

## Core Data Flows

### 1. Emergency Trigger Workflow
1.  **Detection**: The `Detection Service` monitors sensor data (accelerometer, heart rate). If an anomaly is detected (or User presses SOS), an event is sent to `EmergencyManager`.
2.  **Orchestration**: `EmergencyManager` starts the 3-Phase workflow:
    *   **Phase 1**: Local countdown & UI prompt.
    *   **Phase 2**: If no user response, it calls `CloudIntegration` to notify the backend.
3.  **Dispatch**: The `Node.js Backend` receives the alert and instructs `Twilio` to call/text configured `Emergency Contacts`.

### 2. Hybrid AI Voice Assistance
*   **Online Mode**:
    *   User audio is streamed to **ElevenLabs** for low-latency, natural conversation.
    *   Transcripts are sent to **OpenAI** to generate a structured medical summary (e.g., "User reports chest pain").
*   **Offline Mode (Fallback)**:
    *   User audio is processed locally by **Vosk** (Speech-to-Text).
    *   Text is fed into **TinyLlama** (running via `llama.cpp` JNI bindings) to generate guidance.
    *   Response is synthesized locally or displayed on screen.

### 3. Data Synchronization
*   **User Profile**: Critical medical data (allergies, contacts) is synced from **Firebase Firestore** to an encrypted local database (`EncryptedSharedPreferences` / Room) at startup to ensure it's available offline.
*   **Logs**: Interaction logs and emergency event history are uploaded to Firestore for permanent record-keeping when the device is online.
