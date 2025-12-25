# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-12-25

### Major Platform Migration
**BREAKING CHANGE:** Migrated from Arduino Nano 33 BLE Sense to ESP32 + MPU6050

### Added
- **6-Axis IMU Support:** Full accelerometer + gyroscope data input (ax, ay, az, gx, gy, gz)
- **ESP32 FreeRTOS Integration:** Replaced Mbed RTOS with native ESP32 dual-core tasking
- **Gyroscope Configuration:** Added ±250°/s range setting for MPU6050
- **Comprehensive README:** Complete hardware wiring, setup instructions, and troubleshooting
- **Architecture Diagram:** Mermaid flowchart showing dual-core processing pipeline
- **Unit Conversion Notes:** Documentation for m/s² (accel) and rad/s (gyro) units
- **Clamping Logic:** Separate range validation for accel (±2G) and gyro (±250°/s)

### Changed
- **Target Board:** Arduino Nano 33 BLE Sense → ESP32 DevKit (any variant)
- **RTOS Threading:** `rtos::Thread` (Mbed) → `xTaskCreatePinnedToCore()` (FreeRTOS)
- **Buffer Size:** 3 values per sample → 6 values per sample
- **Sample Rate:** Unspecified → 100 Hz (10ms interval, per Edge Impulse model)
- **Model Input:** 3-axis accelerometer → 6-axis IMU (accel + gyro)
- **Validation Check:** `EI_CLASSIFIER_RAW_SAMPLES_PER_FRAME != 3` → `!= 6`

### Removed
- **Mbed OS Dependencies:** No longer requires Arduino Mbed core
- **LSM9DS1 Library:** Completely replaced with Adafruit MPU6050
- **G-to-m/s² Conversion:** MPU6050 library returns SI units directly

### Fixed
- **Unit Conversion Bug:** Removed redundant `* CONVERT_G_TO_MS2` since Adafruit library handles conversion
- **Missing I2C Initialization:** Added explicit `Wire.begin()` call
- **Range Mismatch:** Configured MPU6050 to match Edge Impulse training data (±2G, ±250°/s)

### Technical Details
- **Library Dependencies:**
  - Added: Adafruit_MPU6050 (≥2.2.4)
  - Added: Adafruit_Unified_Sensor (≥1.1.9)
  - Removed: Arduino_LSM9DS1
- **Memory Footprint:** ~80KB SRAM (increased from ~60KB due to 6-axis buffer)
- **Processing:**
  - Core 0: ML inference task (FreeRTOS)
  - Core 1: Sensor sampling loop (Arduino main)

### Migration Guide
**From v0.x.x (Nano BLE) to v1.0.0 (ESP32):**

1. **Hardware Changes:**
   - Replace Nano 33 BLE Sense with ESP32 DevKit
   - Add external MPU6050 breakout board
   - Wire: SDA → GPIO21, SCL → GPIO22, VCC → 3.3V, GND → GND

2. **Software Changes:**
   - Install ESP32 board support in Arduino IDE
   - Install Adafruit_MPU6050 library
   - Re-train Edge Impulse model with 6-axis input or deploy existing 6-axis model

3. **Configuration:**
   - Update `EI_CLASSIFIER_INTERVAL_MS` in Edge Impulse library to match 10ms (if different)
   - Ensure model expects 100 Hz sample rate

### Known Issues
- **Serial Monitor Requirement:** Code waits for Serial connection (`while (!Serial)`), comment out for standalone operation
- **I2C Speed:** Default 100 kHz may cause timing jitter at 100 Hz sampling, consider 400 kHz fast mode
- **Task Stack Size:** 8192 bytes may be insufficient for large models, increase if OOM errors occur

### Risk Assessment
**Risk Level:** Medium

**Mitigations:**
- All RTOS calls use ESP32-native FreeRTOS (well-tested)
- MPU6050 library is mature (Adafruit, 5+ years stable)
- Buffer management unchanged (ring buffer pattern preserved)
- Edge Impulse SDK unmodified (only input dimensions changed)

---

## [0.1.0] - 2025-12-25 (Internal)

### Added
- Initial port from LSM9DS1 to MPU6050 (3-axis only)
- Memory-bank documentation structure
- Project reconnaissance and planning

### Notes
- Superseded by v1.0.0 (never released)

---

[1.0.0]: https://github.com/your-repo/compare/v0.1.0...v1.0.0
