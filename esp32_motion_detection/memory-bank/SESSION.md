# Session Log

## 2025-12-25 — v0.1.0
**Mission:** Replace LSM9DS1 with MPU6050 for motion detection  
**Phase:** Completed - Initial 3-axis implementation  
**Status:** ✅ Superseded by v1.0.0  
**Actions:**
- Created memory-bank directory structure
- Generated projectbrief.md with system architecture
- Generated activeContext.md with active blockers
- Completed Phase 0 reconnaissance
- Replaced LSM9DS1 with MPU6050 (3-axis only)

## 2025-12-25 — v1.0.0
**Mission:** Migrate to ESP32 + MPU6050 with 6-axis model (accel + gyro)  
**Phase:** Phase 5 - Final Report (Complete)  
**Status:** ✅ Mission Accomplished  
**Actions:**
- Updated memory-bank files for ESP32 target
- Replaced Mbed RTOS with FreeRTOS (ESP32 native)
- Expanded to 6-axis input (ax, ay, az, gx, gy, gz)
- Updated buffer rolling from 3 to 6 values
- Added gyroscope range configuration (±250°/s)
- Updated validation to check for 6 axes
- Fixed unit conversion (MPU6050 returns m/s² and rad/s directly)
- Updated clamping logic for accel and gyro
- Created comprehensive README.md with wiring diagram
- Generated and validated Mermaid architecture diagram

**Files Modified:**
1. `nano_ble33_sense_accelerometer_continuous.ino` (8 replacements)
2. `memory-bank/projectbrief.md` (updated for ESP32 + 6-axis)
3. `memory-bank/activeContext.md` (updated blockers)
4. `README.md` (created with full documentation)

**Next:** User testing on ESP32 hardware, Edge Impulse model deployment

## 2025-12-25 — v1.1.0
**Mission:** Fix unit mismatch between training data (raw ADC) and inference (physical units)  
**Phase:** Phase 5 - Final Report (Complete)  
**Status:** ✅ Critical Fix Deployed  
**Actions:**
- Diagnosed root cause: Edge Impulse model trained with raw 16-bit ADC values
- Added `readRawMPU6050()` helper function for direct register reads (0x3B-0x48)
- Replaced physical unit conversion with raw integer storage
- Removed unnecessary clamping (raw values inherently bounded to ±32768)
- Updated comments to reflect raw data format
- Enhanced I2C initialization with bus scan and explicit pin assignment
- Added real-time sensor debug output (1Hz) for diagnostics
- Added gravity sanity check on startup

**Files Modified:**
1. `nano_ble33_sense_accelerometer_continuous.ino` (lines 58-293)

**Evidence:** User confirmed sensor readings now match training data format (e.g., ax=-14817 vs ax=-2.16 m/s²)

**Next:** Verify predictions respond to motion, add confidence output

## 2025-12-25 — v1.2.0
**Mission:** Add confidence percentage display to serial output  
**Phase:** Phase 5 - Final Report (Complete)  
**Status:** ✅ Mission Accomplished  
**Actions:**
- Added per-class confidence display in prediction output
- Shows all classes with percentage values (e.g., "IDLE: 95.23%, SHAKE: 4.77%")
- Renamed smoothing count array output with "smooth:" prefix for clarity
- Maintained backward compatibility (no breaking changes)

**Files Modified:**
1. `nano_ble33_sense_accelerometer_continuous.ino` (lines 221-250)

**Output Format Change:**
- **Before:** `Predictions (DSP: 7 ms., ...): IDLE  [ 10, 0, 0, 0, 0, ]`
- **After:** `Predictions (DSP: 7 ms., ...): IDLE [ IDLE: 95.00%, SHAKE: 2.50%, ... ] smooth:[ 10, 0, 0, 0, 0, ]`

**Next:** User validation of confidence display accuracy
