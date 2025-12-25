# Active Context

## Current Sprint Focus
**Mission:** Migrate from Arduino Nano 33 BLE Sense to ESP32 + MPU6050 with 6-axis model (accel + gyro).

## Active Blockers
⚠️ **BLOCKER-001:** RTOS threading uses Mbed OS - must replace with FreeRTOS for ESP32  
⚠️ **BLOCKER-002:** Model expects 6 axes (ax,ay,az,gx,gy,gz) but current code only reads 3 - need gyro integration  
⚠️ **BLOCKER-003:** Sample rate needs updating from unknown to 100Hz (10ms interval)  
⚠️ **BLOCKER-004:** Buffer rolling/reading logic hardcoded for 3 values - must expand to 6

## Recently Cleared Blockers
✅ **CLEARED:** MPU6050 sensor integration (initial 3-axis implementation complete)

## Active Assumptions
- MPU6050 will use default I2C address (0x68)
- User has Adafruit_MPU6050 library installed or can install it
- User will connect MPU6050 to Nano BLE33's I2C pins (SDA/SCL)
- Edge Impulse model expects data in same format (m/s², ±2G range)

## Context Snapshots
**Last Modified File:** (none yet - reconnaissance phase)  
**Last Test Result:** (awaiting implementation)
