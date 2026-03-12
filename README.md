# SmartRacket Project

Welcome to the SmartRacket repository! This project combines hardware, firmware, and software to create a smart sports racket system, featuring 3D models, Android app development, and embedded firmware.

## Repository Structure

```text
.
├── 3d model/                # 3D CAD files for hardware design
│   ├── base.f3z
│   └── smartracket.step
├── apps/
│   └── Android_Studio/      # Android app source code
│       ├── app/             # Main Android app module
│       ├── build.gradle.kts
│       ├── gradle.properties
│       └── ...
├── arduino_code/            # Embedded firmware for racket hardware
│   └── XIAOEI/
│       └── XIAOEI.ino
│   ├── XIAO_BLE_IMU_Sender/
│   │   └── XIAO_BLE_IMU_Sender.ino
│   └── XIAO_BLE_IMU_Receiver/
│       └── XIAO_BLE_IMU_Receiver.ino
├── doc/                     # Documentation
│   ├── 3d model/
│   └── apps/
└── LICENSE                  # Project license
```

## Key Components

### 1. 3D Model

- Contains CAD files (`.f3z`, `.step`) for the physical design of the smart racket.

### 2. Android App

- Located in `apps/Android_Studio/`.
- Built with Kotlin and Gradle.
- Features:
  - Bluetooth connectivity to the smart racket
  - Data visualization and user interface

### 3. Arduino Firmware

- Located in `arduino_code/XIAOEI/XIAOEI.ino`.
- Written for Arduino-compatible microcontrollers.
- Handles sensor data acquisition and communication with the Android app.
- Additional BLE IMU examples are available for two `Seeed Studio XIAO nRF52840 Sense` boards:
  - `arduino_code/XIAO_BLE_IMU_Sender/XIAO_BLE_IMU_Sender.ino` sends scaled IMU data over BLE.
  - `arduino_code/XIAO_BLE_IMU_Receiver/XIAO_BLE_IMU_Receiver.ino` receives that BLE stream and prints `ax, ay, az, gx, gy, gz` over Serial.

### 4. Documentation

- Technical whitepapers, investor decks, and canvas documentation in `apps/Android_Studio/docs/`.
- Additional documentation in `doc/`.

## Getting Started

### Android App

1. Open `apps/Android_Studio/` in Android Studio.
2. Sync Gradle and build the project.
3. Deploy to your Android device.

### Firmware

1. Open `arduino_code/XIAOEI/XIAOEI.ino` in Arduino IDE.
2. Select the correct board and port.
3. Upload the firmware to your device.
4. For two-board BLE IMU streaming, use the dedicated sender and receiver sketches listed below.

- `arduino_code/XIAO_BLE_IMU_Sender/XIAO_BLE_IMU_Sender.ino` on the transmitting XIAO board
- `arduino_code/XIAO_BLE_IMU_Receiver/XIAO_BLE_IMU_Receiver.ino` on the receiving XIAO board

### 3D Models

- Open `.f3z` or `.step` files in Fusion 360 or compatible CAD software.

## License

This project is licensed under the terms of the LICENSE file in the root directory.

---

For more details, see the documentation in the `doc/` and `apps/Android_Studio/docs/` folders.
