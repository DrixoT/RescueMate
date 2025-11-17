# RescueMate 2.0 - Architecture Documentation

## System Architecture Overview

RescueMate 2.0 follows a modern Android architecture using MVVM pattern with clean architecture principles.

## Architecture Layers

### 1. Presentation Layer (UI)
- **Technology**: Jetpack Compose
- **Pattern**: MVVM with State Hoisting
- **Components**:
  - Screens: Full-screen composables
  - Components: Reusable UI elements
  - ViewModels: UI state management
  - Navigation: Compose Navigation

### 2. Domain Layer (Business Logic)
- **Emergency Manager**: Core emergency workflow orchestration
- **Health Monitoring Service**: Continuous health tracking
- **Detection Services**: Fall detection, anomaly detection
- **AI Services**: TinyLlama, ElevenLabs integration

### 3. Data Layer
- **Database**: SQLite with custom helper (Result-based API)
- **Repositories**: Data access abstraction
- **Preferences**: SharedPreferences wrapper
- **Network**: OkHttp with certificate pinning

### 4. Security Layer
- **Encryption Service**: AES-256-GCM encryption
- **Biometric Auth Manager**: Fingerprint/face authentication
- **Certificate Pinning**: MITM protection

### 5. Integration Layer
- **Twilio Service**: Voice/SMS communication
- **ElevenLabs Service**: Conversational AI
- **Google Maps**: Location services
- **Bluetooth BLE**: Health device connectivity

## Key Design Patterns

### 1. Repository Pattern
```kotlin
EmergencyRepository
├── Local Data Source (Database)
├── Remote Data Source (API)
└── Cache Management
```

### 2. Observer Pattern
- StateFlow for reactive state management
- Callbacks for async operations
- LiveData alternatives using Compose State

### 3. Dependency Injection (Manual)
- Context-based injection
- Service locator pattern
- Factory pattern for complex objects

### 4. Result Pattern
All database and network operations return `Result<T>`:
```kotlin
fun operation(): Result<Data> = try {
    Result.success(data)
} catch (e: Exception) {
    Result.failure(e)
}
```

## Component Interactions

### Emergency Flow
```
User/Sensor → Detection Service → Emergency Manager
                                  ↓
                            Database (Save Event)
                                  ↓
                            Phase Manager
                                  ↓
                    ┌───────────┴───────────┐
                    ↓                       ↓
            Notification System      Twilio Service
                    ↓                       ↓
              User Response        Contact Notification
```

### Health Monitoring Flow
```
BLE Device/Sensor → Health Monitoring Service
                           ↓
                    Data Aggregation
                           ↓
                   Anomaly Detection
                           ↓
                    Risk Assessment
                           ↓
            (If High Risk) Emergency Trigger
```

### Voice AI Flow
```
User Voice → ElevenLabs SDK → Agent Response
                ↓                    ↓
         (If Offline)            Audio Output
                ↓
         Local Voice LLM
                ↓
            TinyLlama
                ↓
          Android TTS
```

## Data Models

### Core Emergency Models
- `EmergencyEvent`: Complete emergency occurrence
- `EmergencyContact`: Contact information
- `HealthData`: Health snapshot
- `LocationData`: GPS coordinates
- `MedicalInfo`: User medical profile
- `UserInfo`: User identification

### Database Schema
```sql
emergency_contacts
├── id (TEXT PRIMARY KEY)
├── name (TEXT)
├── phone_number (TEXT)
├── relationship (TEXT)
├── priority (INTEGER)
└── ... (11 more columns)

medical_info
├── user_id (TEXT PRIMARY KEY)
├── date_of_birth (TEXT)
├── blood_type (TEXT)
├── known_conditions (TEXT JSON)
├── current_medications (TEXT JSON)
└── ... (7 more columns)

emergency_events
├── id (TEXT PRIMARY KEY)
├── user_id (TEXT)
├── emergency_type (TEXT)
├── status (TEXT)
├── current_phase (INTEGER)
├── health_data_json (TEXT)
├── location_data_json (TEXT)
└── ... (8 more columns)
```

## Security Architecture

### Encryption Flow
```
Sensitive Data → EncryptionService
                       ↓
                 Android Keystore
                       ↓
                 AES-256-GCM Key
                       ↓
                Encrypted Data → Database
```

### Authentication Flow
```
User Action → BiometricPrompt
                   ↓
            Hardware Check
                   ↓
           Biometric Scan
                   ↓
        ┌──────────┴──────────┐
        ↓                     ↓
   Success                 Failure
        ↓                     ↓
  Grant Access         Show Error
```

## Performance Optimizations

### Database
- Indexed columns for frequent queries
- Pagination for large datasets
- Transaction batching for bulk operations
- Result caching in memory

### Network
- Request queuing for offline mode
- Retry logic with exponential backoff
- Connection pooling
- Certificate pinning caching

### UI
- Lazy loading with LazyColumn
- State hoisting for recomposition optimization
- Remember for expensive computations
- Immutable data classes

## Error Handling Strategy

### Centralized Error Handler
```kotlin
ErrorHandler
├── Error Categories (Network, Database, Permission, etc.)
├── Severity Levels (Low, Medium, High, Critical)
├── Recovery Strategies (Retry, Settings, Contact Support)
└── User Notifications (Snackbar, Dialog, Toast)
```

### Error Flow
```
Error Occurs → ErrorHandler.handle()
                     ↓
              Log Error
                     ↓
         Determine User Message
                     ↓
    ErrorNotificationManager.show()
                     ↓
         Display to User
```

## Testing Architecture

### Test Pyramid
```
        UI Tests (Espresso/Compose)
              ↑ 10%
    Integration Tests
              ↑ 20%
        Unit Tests
              ↑ 70%
```

### Test Coverage Goals
- Unit Tests: 70%+ coverage
- Integration Tests: Critical paths
- UI Tests: User flows
- Manual Tests: Edge cases

## Module Dependencies

```
app
├── Security Module
│   ├── Encryption
│   └── Biometric Auth
├── Emergency Module
│   ├── Detection
│   ├── Health Monitoring
│   ├── Location Services
│   └── Contact Notification
├── AI Module
│   ├── TinyLlama
│   └── ElevenLabs
├── Communication Module
│   ├── Twilio
│   └── Network Monitor
└── UI Module
    ├── Screens
    ├── Components
    └── Navigation
```

## Future Architecture Enhancements

1. **Phase 3 Integration**: Emergency services API
2. **Cloud Sync**: Multi-device synchronization
3. **Advanced Analytics**: Health trend analysis
4. **Wearable SDK**: Dedicated smartwatch app
5. **Modularization**: Feature modules for dynamic delivery
6. **Dependency Injection**: Hilt/Koin integration
7. **Paging 3**: Efficient pagination library
8. **Room Database**: Migration from SQLite helper
9. **WorkManager**: Background job scheduling
10. **Crashlytics**: Error reporting and analytics

## Performance Metrics

### Target Metrics
- App startup: < 2 seconds
- Emergency trigger: < 500ms
- Location acquisition: < 3 seconds
- Database queries: < 100ms
- UI frame rate: 60 FPS
- Memory usage: < 150MB

### Monitoring
- Firebase Performance Monitoring (future)
- Android Profiler (development)
- Custom analytics (production)

## Scalability Considerations

- Pagination prevents memory issues with large datasets
- Offline queue handles network instability
- Modular architecture allows feature additions
- Clean interfaces enable easy testing and mocking
- Result-based APIs provide consistent error handling

---

Last Updated: December 2024

