/**
 * @file main.cpp
 * @brief ESP32-S3 MPU6050 Bluetooth Touch Sensor
 * 
 * Hardware: Seeed Studio XIAO ESP32-S3
 * Sensor: MPU6050 (I2C)
 * Communication: Bluetooth Serial (Classic SPP)
 * Input: Touch1 capacitive sensor
 * 
 * Wiring:
 *   MPU6050 SDA -> GPIO5 (D4)
 *   MPU6050 SCL -> GPIO6 (D5)
 *   MPU6050 VCC -> 3.3V
 *   MPU6050 GND -> GND
 *   Touch1 -> GPIO1 (D0) - internal capacitive touch
 * 
 * @author Copilot
 * @date 2026-01-12
 */

#include <Arduino.h>
#include <Wire.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include "BluetoothSerial.h"

// ============================================================================
// Configuration
// ============================================================================

// I2C pins for XIAO ESP32-S3
#define I2C_SDA_PIN         5   // GPIO5 (D4)
#define I2C_SCL_PIN         6   // GPIO6 (D5)

// Touch pin (Touch1 on XIAO ESP32-S3)
#define TOUCH_PIN           1   // GPIO1 (D0) = Touch1

// Touch threshold (lower = more sensitive, typical range 20000-60000 for ESP32-S3)
#define TOUCH_THRESHOLD     40000

// Bluetooth device name
#define BT_DEVICE_NAME      "XIAO_MPU6050_Touch"

// Data transmission interval (ms)
#define SEND_INTERVAL_MS    100

// ============================================================================
// Global Objects
// ============================================================================

Adafruit_MPU6050 mpu;
BluetoothSerial SerialBT;

// Timing
unsigned long lastSendTime = 0;

// Touch state
bool touchDetected = false;
touch_value_t touchValue = 0;

// ============================================================================
// Function Prototypes
// ============================================================================

void initI2C();
void initMPU6050();
void initBluetooth();
void initTouch();
void readAndSendSensorData();
void readAndSendTouchData();
void printStartupInfo();

// ============================================================================
// Setup
// ============================================================================

void setup() {
    // Initialize USB Serial for debugging
    Serial.begin(115200);
    delay(1000);  // Wait for serial connection
    
    Serial.println("========================================");
    Serial.println("XIAO ESP32-S3 MPU6050 Bluetooth Touch");
    Serial.println("========================================");
    
    // Initialize subsystems
    initI2C();
    initMPU6050();
    initBluetooth();
    initTouch();
    
    printStartupInfo();
    
    Serial.println("\n[READY] System initialized. Connect via Bluetooth.");
    Serial.println("========================================\n");
}

// ============================================================================
// Main Loop
// ============================================================================

void loop() {
    unsigned long currentTime = millis();
    
    // Non-blocking periodic data transmission
    if (currentTime - lastSendTime >= SEND_INTERVAL_MS) {
        lastSendTime = currentTime;
        
        // Read and send MPU6050 data
        readAndSendSensorData();
        
        // Read and send Touch data
        readAndSendTouchData();
    }
    
    // Small yield to prevent watchdog reset
    delay(1);
}

// ============================================================================
// Initialization Functions
// ============================================================================

/**
 * @brief Initialize I2C bus with custom pins
 */
void initI2C() {
    Serial.print("[INIT] I2C on SDA=GPIO");
    Serial.print(I2C_SDA_PIN);
    Serial.print(", SCL=GPIO");
    Serial.println(I2C_SCL_PIN);
    
    Wire.begin(I2C_SDA_PIN, I2C_SCL_PIN);
}

/**
 * @brief Initialize MPU6050 IMU sensor
 */
void initMPU6050() {
    Serial.print("[INIT] MPU6050... ");
    
    if (!mpu.begin()) {
        Serial.println("FAILED!");
        Serial.println("[ERROR] Could not find MPU6050. Check wiring!");
        while (1) {
            delay(1000);
        }
    }
    
    Serial.println("OK");
    
    // Configure MPU6050 ranges
    mpu.setAccelerometerRange(MPU6050_RANGE_8_G);
    mpu.setGyroRange(MPU6050_RANGE_500_DEG);
    mpu.setFilterBandwidth(MPU6050_BAND_21_HZ);
    
    Serial.println("[CONFIG] Accel: ±8g, Gyro: ±500°/s, Filter: 21Hz");
}

/**
 * @brief Initialize Bluetooth Serial (Classic SPP)
 */
void initBluetooth() {
    Serial.print("[INIT] Bluetooth Serial as '");
    Serial.print(BT_DEVICE_NAME);
    Serial.print("'... ");
    
    if (!SerialBT.begin(BT_DEVICE_NAME)) {
        Serial.println("FAILED!");
        Serial.println("[ERROR] Bluetooth initialization failed!");
        while (1) {
            delay(1000);
        }
    }
    
    Serial.println("OK");
}

/**
 * @brief Initialize capacitive touch sensor
 */
void initTouch() {
    Serial.print("[INIT] Touch1 on GPIO");
    Serial.print(TOUCH_PIN);
    Serial.print(" (threshold=");
    Serial.print(TOUCH_THRESHOLD);
    Serial.println(")... OK");
    
    // ESP32-S3 uses touchRead() directly, no special init needed
    // Initial read to stabilize
    touchValue = touchRead(TOUCH_PIN);
    Serial.print("[INFO] Initial touch value: ");
    Serial.println(touchValue);
}

// ============================================================================
// Data Functions
// ============================================================================

/**
 * @brief Read MPU6050 sensor data and send via Bluetooth Serial
 */
void readAndSendSensorData() {
    sensors_event_t accel, gyro, temp;
    mpu.getEvent(&accel, &gyro, &temp);
    
    // Format: MPU,ax,ay,az,gx,gy,gz,temp
    char buffer[128];
    snprintf(buffer, sizeof(buffer),
        "MPU,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.1f",
        accel.acceleration.x,
        accel.acceleration.y,
        accel.acceleration.z,
        gyro.gyro.x,
        gyro.gyro.y,
        gyro.gyro.z,
        temp.temperature
    );
    
    // Send via Bluetooth
    SerialBT.println(buffer);
    
    // Debug output to USB Serial
    Serial.println(buffer);
}

/**
 * @brief Read Touch1 data and send via Bluetooth Serial
 */
void readAndSendTouchData() {
    // Read capacitive touch value
    touchValue = touchRead(TOUCH_PIN);
    
    // Detect touch (value drops below threshold when touched)
    // Note: ESP32-S3 touch behavior - lower value = touch detected
    bool currentTouch = (touchValue < TOUCH_THRESHOLD);
    
    // Format: TOUCH,value,state
    char buffer[64];
    snprintf(buffer, sizeof(buffer),
        "TOUCH1,%lu,%s",
        (unsigned long)touchValue,
        currentTouch ? "TOUCHED" : "RELEASED"
    );
    
    // Send via Bluetooth
    SerialBT.println(buffer);
    
    // Debug output to USB Serial
    Serial.println(buffer);
    
    // Update state for edge detection (if needed in future)
    touchDetected = currentTouch;
}

/**
 * @brief Print startup information
 */
void printStartupInfo() {
    Serial.println("\n[INFO] Configuration:");
    Serial.println("  - Board: Seeed XIAO ESP32-S3");
    Serial.println("  - Sensor: MPU6050 (I2C)");
    Serial.print("  - Bluetooth: ");
    Serial.println(BT_DEVICE_NAME);
    Serial.print("  - Touch Pin: GPIO");
    Serial.println(TOUCH_PIN);
    Serial.print("  - Send Interval: ");
    Serial.print(SEND_INTERVAL_MS);
    Serial.println("ms");
    
    Serial.println("\n[INFO] Data Format:");
    Serial.println("  MPU: MPU,ax,ay,az,gx,gy,gz,temp");
    Serial.println("  Touch: TOUCH1,value,state");
}
