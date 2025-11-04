# RescueMate Android App

Emergency response system Android application built with Kotlin and Jetpack Compose.

## Features

- **Onboarding & Authentication**: Welcome screens and sign-in/sign-up flows
- **Home Dashboard**: Main SOS button with quick access to contacts and location
- **Emergency Contacts**: Manage your safety network contacts
- **Live Location**: Real-time location tracking with Google Maps integration
- **Settings**: Configure emergency settings and preferences
- **Bluetooth Pairing**: Connect and pair smartwatch devices for emergency alerts

## Prerequisites

- Android Studio Hedgehog or later
- JDK 17 or later
- Android SDK 24+ (minimum), 34+ (target)
- Google Maps API Key

## Setup

1. Clone the repository
2. Open the project in Android Studio
3. Get a Google Maps API key from [Google Cloud Console](https://console.cloud.google.com/)
4. Add your API key to `app/src/main/res/values/strings.xml`:
   ```xml
   <string name="google_maps_api_key">YOUR_API_KEY_HERE</string>
   ```
5. Sync Gradle files
6. Build and run the app

## Project Structure

```
app/
├── src/main/
│   ├── java/com/rescuemate/
│   │   ├── MainActivity.kt
│   │   ├── ui/
│   │   │   ├── navigation/
│   │   │   ├── screens/
│   │   │   └── theme/
│   │   └── utils/
│   ├── res/
│   │   ├── values/
│   │   └── drawable/
│   └── AndroidManifest.xml
├── build.gradle.kts
└── proguard-rules.pro
```

## Dependencies

- Jetpack Compose - Modern UI toolkit
- Google Maps SDK for Android - Maps integration
- Material 3 - Material Design components
- Navigation Compose - Navigation between screens
- Accompanist Permissions - Runtime permission handling

## Permissions

The app requires the following permissions:
- Location (FINE and COARSE) - For location tracking
- Bluetooth - For smartwatch pairing
- Internet - For maps and network operations

## Design

The app uses a cosmic dark theme with:
- Primary color: `#E91E63` (Pink)
- Background: `#0a0510` (Dark purple/black)
- Cards: `#1a0f23` (Dark purple)
- Borders: `#5A1E3C` (Purple border)

## Building

```bash
./gradlew assembleDebug
```

For release builds:
```bash
./gradlew assembleRelease
```

## License

Private project - All rights reserved
