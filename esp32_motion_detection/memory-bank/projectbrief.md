# Project Brief

## Project Identity
**Name:** ESP32 MPU6050 6-Axis Motion Detection  
**Primary Language:** C++ (Arduino)  
**Target Hardware:** ESP32 + MPU6050 (I2C 6-axis IMU)  
**Purpose:** Real-time motion detection using Edge Impulse ML inference (6-axis: accel + gyro)

## Core Objectives
- Continuous accelerometer data acquisition
- Real-time motion classification using Edge Impulse trained model
- Non-blocking inference using RTOS threading
- Support for multiple motion classes with confidence smoothing

## Technical Stack
- **MCU:** ESP32 (Xtensa LX6, dual-core)
- **Sensor:** MPU6050 (I2C 6-axis IMU - accel + gyro)
- **ML Framework:** Edge Impulse (TensorFlow Lite Micro)
- **RTOS:** FreeRTOS (native ESP32)
- **Libraries:** 
  - `motion_detection_inferencing.h` (Edge Impulse SDK)
  - `Adafruit_MPU6050.h` (sensor driver)
  - `Wire.h` (I2C)
- **Model Config:**
  - Input axes: 6 (ax, ay, az, gx, gy, gz)
  - Sample rate: 100 Hz (10ms interval)
  - Window: 1000ms (100 samples)
  - Stride: 10ESP32 SRAM (~520KB), sufficient for 6-axis DSP buffer
- **Timing:** Real-time sampling at 100 Hz (10ms intervals)
- **Sensor Range:** ±2G accelerometer, ±250°/s gyroscope (model training config)
- **Data Format:** Float buffer, 6-axis (ax, ay, az, gx, gy, gz) - accel in m/s², gyro in rad/sbuffer
- **Timing:** Real-time inference (200ms intervals)
- **Sensor Range:** 2G accelerometer range (model constraint)
- **Data Format:** Float buffer, 3-axis (X, Y, Z) in m/s²
- **Message by user:** FYI: my library dir at "D:\projects\arduino\libraries"

## Known Dependencies
- Edge Impulse C++ library (auto-generated from trained model)
- Accelerometer calibration to ±2G range
- Thread-safe buffer access between main loop and inference thread

## Modification Constraints
- Must maintain Edge Impulse data format compatibility
- Must preserve RTOS threading architecture
- Must keep non-blocking main loop pattern
- Cannot alter ML model input dimensions (3 axes)
