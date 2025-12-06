# SmartRacket Coach

A native Android app for table tennis training analytics that receives motion data from a hardware-embedded IMU sensor in a table tennis paddle (ESP32-based) and provides real-time stroke classification and performance feedback.

## Features

### 🏓 Real-Time Stroke Classification
- TensorFlow Lite-powered stroke recognition
- Support for 14 stroke types (forehand/backhand loops, drives, chops, blocks, etc.)
- 1-10 performance scoring with instant feedback
- Live motion data visualization

### 📊 Training Analytics
- Session history with filtering by date
- Stroke distribution charts
- Score trends over time
- Performance evolution tracking

### ⭐ Highlight Capture
- 3-minute circular buffer for motion data
- Automatic save for exceptional strokes (score 8+)
- Manual highlight saving during training
- Share highlights with metadata

### ❤️ Health Integration
- Samsung Health / Health Connect integration
- Real-time heart rate monitoring during training
- Calorie burn tracking
- Exercise session sync

### ⌚ Galaxy Watch Support
- Heart rate data sync via Wear OS
- Training alerts on watch
- Quick session controls

## Project Structure

```
app/src/main/java/smartracket/com/
├── MainActivity.kt              # App entry point with navigation
├── SmartRacketApplication.kt    # Hilt Application class
├── di/
│   └── AppModule.kt             # Dependency injection module
├── ui/
│   ├── theme/
│   │   ├── Theme.kt             # Material 3 theming
│   │   └── Type.kt              # Typography
│   └── screens/
│       ├── HomeScreen.kt        # Today's summary dashboard
│       ├── TrainingScreen.kt    # Real-time training UI
│       ├── AnalyticsScreen.kt   # History and statistics
│       ├── HighlightsScreen.kt  # Saved highlights gallery
│       ├── SettingsScreen.kt    # App preferences
│       └── PrivacyPolicyActivity.kt
├── viewmodel/
│   ├── TrainingViewModel.kt     # Training session management
│   ├── HomeViewModel.kt         # Dashboard data
│   ├── AnalyticsViewModel.kt    # Analytics processing
│   ├── HighlightsViewModel.kt   # Highlight management
│   └── SettingsViewModel.kt     # Settings state
├── repository/
│   ├── BluetoothRepository.kt   # Paddle communication
│   ├── TrainingRepository.kt    # Session & stroke data
│   ├── HighlightRepository.kt   # Highlight management
│   └── HealthRepository.kt      # Health Connect integration
├── db/
│   ├── SmartRacketDatabase.kt   # Room database
│   ├── TrainingSessionDao.kt    # Session queries
│   ├── StrokeDao.kt             # Stroke queries
│   ├── HighlightClipDao.kt      # Highlight queries
│   ├── DevicePairingDao.kt      # Device pairing
│   └── Converters.kt            # Type converters
├── model/
│   ├── TrainingSession.kt       # Session entity
│   ├── Stroke.kt                # Stroke entity & types
│   ├── HighlightClip.kt         # Highlight entity
│   └── DevicePairing.kt         # Bluetooth device entity
├── utils/
│   ├── BluetoothManager.kt      # BLE communication
│   └── StrokeClassifier.kt      # TensorFlow Lite inference
└── service/
    ├── BluetoothTrainingService.kt  # BLE foreground service
    ├── TrainingSessionService.kt    # Session foreground service
    └── WearableListenerService.kt   # Wear OS message handling
```

## Setup Instructions

### Prerequisites

1. **Android Studio** Ladybug (2024.2.1) or later
2. **Android SDK** 35 (API level 35)
3. **JDK 17** or later
4. **Kotlin** 2.0+

### Dependencies

The project uses the following key dependencies:

- **Jetpack Compose** - Modern UI toolkit
- **Room** - Local database
- **Hilt** - Dependency injection
- **TensorFlow Lite** - ML inference
- **Health Connect** - Samsung Health integration
- **Wear OS** - Galaxy Watch integration
- **MPAndroidChart** - Analytics charts

### Building the Project

1. Clone the repository:
   ```bash
   git clone https://github.com/your-repo/smartracket-coach.git
   cd smartracket-coach/Android_Studio
   ```

2. Open the project in Android Studio

3. Sync Gradle files

4. Build and run:
   ```bash
   ./gradlew assembleDebug
   ```

### Bundling the TensorFlow Lite Model

1. Place your trained model file at:
   ```
   app/src/main/assets/stroke_classifier.tflite
   ```

2. Model specifications:
   - **Input**: `[1, 50, 6]` (batch, sequence, features)
   - **Features**: accelX, accelY, accelZ, gyroX, gyroY, gyroZ
   - **Output**: `[1, 14]` (probabilities for 14 stroke types)

3. If you don't have a model, create a placeholder:
   ```bash
   mkdir -p app/src/main/assets
   # Add your .tflite file here
   ```

## ESP32 Paddle Setup

### Bluetooth Configuration

The SmartRacket paddle uses BLE with the following UUIDs:

- **Service UUID**: `4fafc201-1fb5-459e-8fcc-c5c9c331914b`
- **IMU Characteristic**: `beb5483e-36e1-4688-b7f5-ea07361b26a8` (Notify)
- **Control Characteristic**: `beb5483e-36e1-4688-b7f5-ea07361b26a9` (Write)
- **Battery Characteristic**: `beb5483e-36e1-4688-b7f5-ea07361b26aa` (Read)

### Data Packet Format

IMU data is sent in 31-byte packets:

| Offset | Size | Description        |
|--------|------|--------------------|
| 0-1    | 2    | Header (0x5A 0xA5) |
| 2-5    | 4    | Timestamp (ms)     |
| 6-9    | 4    | AccelX (float)     |
| 10-13  | 4    | AccelY (float)     |
| 14-17  | 4    | AccelZ (float)     |
| 18-21  | 4    | GyroX (float)      |
| 22-25  | 4    | GyroY (float)      |
| 26-29  | 4    | GyroZ (float)      |
| 30     | 1    | Checksum (XOR)     |

### Pairing Steps

1. Power on the SmartRacket paddle
2. Open the SmartRacket Coach app
3. Go to **Settings** > **Bluetooth**
4. Tap **Scan for Devices**
5. Select your SmartRacket paddle from the list
6. Wait for connection confirmation

## Health Connect Setup

1. Install **Health Connect** from Play Store
2. Open SmartRacket Coach
3. Go to **Settings** > **Health & Fitness**
4. Tap **Connect** to grant permissions
5. Allow access to heart rate and exercise data

## Testing Strategy

### Unit Tests

```bash
./gradlew test
```

Tests cover:
- ViewModel logic
- Repository operations
- Stroke classification
- Data conversions

### Instrumented Tests

```bash
./gradlew connectedAndroidTest
```

Tests cover:
- Room database operations
- Compose UI components
- Navigation flows

### Manual Testing Checklist

- [ ] Bluetooth pairing with ESP32 paddle
- [ ] Real-time stroke detection and classification
- [ ] Session start/pause/stop
- [ ] Highlight auto-save and manual save
- [ ] Analytics chart rendering
- [ ] Health Connect data sync
- [ ] Galaxy Watch connectivity

## Samsung Device Optimization

### Galaxy Phone Optimization

1. **Battery Optimization**: Exclude SmartRacket from battery optimization to prevent connection drops
2. **Bluetooth Settings**: Enable "Bluetooth scanning" in Location settings
3. **Health Connect**: Grant all requested permissions

### Galaxy Watch Setup

1. Install **Galaxy Wearable** companion app
2. Pair watch with phone
3. Enable heart rate sharing permissions
4. SmartRacket will auto-detect paired watches

## Architecture

The app follows **MVVM (Model-View-ViewModel)** architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Layer (Compose)                        │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐│
│  │  Home   │ │Training │ │Analytics│ │Highlights│ │Settings ││
│  │ Screen  │ │ Screen  │ │ Screen  │ │ Screen  │ │ Screen  ││
│  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘│
└───────┼──────────┼──────────┼──────────┼──────────┼────────┘
        │          │          │          │          │
┌───────▼──────────▼──────────▼──────────▼──────────▼────────┐
│                   ViewModel Layer                           │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐│
│  │  Home   │ │Training │ │Analytics│ │Highlights│ │Settings ││
│  │ViewModel│ │ViewModel│ │ViewModel│ │ViewModel│ │ViewModel││
│  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘│
└───────┼──────────┼──────────┼──────────┼──────────┼────────┘
        │          │          │          │          │
┌───────▼──────────▼──────────▼──────────▼──────────▼────────┐
│                   Repository Layer                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │Bluetooth │ │ Training │ │Highlight │ │  Health  │       │
│  │Repository│ │Repository│ │Repository│ │Repository│       │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘       │
└───────┼────────────┼────────────┼────────────┼─────────────┘
        │            │            │            │
┌───────▼────────────▼────────────▼────────────▼─────────────┐
│                    Data Layer                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │ BLE Mgr  │  │ Room DB  │  │ TF Lite  │  │Health Cnn│    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## License

Copyright © 2024 SmartRacket. All rights reserved.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Submit a pull request

## Support

For issues or feature requests, please open a GitHub issue or contact support@smartracket.com.

