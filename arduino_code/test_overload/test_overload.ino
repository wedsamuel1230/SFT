/**
 * SmartRacket Overload Test (局部負荷過大)
 * 
 * Purpose: Test scenarios where the firmware encounters local overload conditions:
 *   1. Extreme IMU values (high acceleration/gyro peaks)
 *   2. Rapid-fire motion detection triggering
 *   3. Heavy BLE notification throughput
 *   4. Memory/CPU load under sustained patterns
 *   5. Multiple concurrent sensor reads
 * 
 * Test Modes (triggered via Serial):
 *   'a' - Accel Overload: Simulate 5G+ accelerations
 *   'g' - Gyro Overload: Simulate 500+ DPS rotations
 *   'r' - Rapid Fire: Trigger motion detections in quick succession
 *   'b' - BLE Flood: Send rapid notifications to maximize BLE throughput
 *   'm' - Memory Stress: Allocate/deallocate buffers repeatedly
 *   's' - Sustained: Run all tests in sequence for stress duration
 *   'c' - Clear/Calm: Return to normal operation
 *   'R' - Soft Reset: Software reset
 */

#include <bluefruit.h>
#include <LSM6DS3.h>
#include <math.h>

// ====== BLE UUIDs — must match BleDeviceProfile.DEFAULT in the Android app ======
const uint8_t SERVICE_UUID[16]   = {0x4b, 0x91, 0x31, 0xc3, 0xc9, 0xc5, 0xcc, 0x8f, 0x9e, 0x45, 0xb5, 0x1f, 0x01, 0xc2, 0xaf, 0x4f};
const uint8_t DATA_CHAR_UUID[16] = {0xa8, 0x26, 0x1b, 0x36, 0x07, 0xea, 0xf5, 0xb7, 0x88, 0x46, 0xe1, 0x36, 0x3e, 0x48, 0xb5, 0xbe};
const uint8_t CTRL_CHAR_UUID[16] = {0xa9, 0x26, 0x1b, 0x36, 0x07, 0xea, 0xf5, 0xb7, 0x88, 0x46, 0xe1, 0x36, 0x3e, 0x48, 0xb5, 0xbe};

// ====== Config ======
const uint32_t SAMPLE_INTERVAL_MS = 20;  // 50 Hz IMU polling
const uint32_t MOTION_COOLDOWN_MS = 500;
const size_t NOTIFY_CHUNK_SIZE = 20;
const size_t MAX_NOTIFY_LEN = 100;

// Normal thresholds
const float ACCEL_DELTA_G = 2.2f;
const float GYRO_DPS_THRESHOLD = 250.0f;
const uint32_t backtime = 800;

// Overload thresholds for testing
const float ACCEL_OVERLOAD_G = 5.0f;      // 5G+ overload threshold
const float GYRO_OVERLOAD_DPS = 500.0f;   // 500+ DPS overload threshold
const uint32_t RAPID_FIRE_INTERVAL = 100; // Min 100ms between rapid-fire detections
const size_t MEMORY_TEST_ALLOC_SIZE = 256; // bytes per allocation in memory test

BLEService modelService(SERVICE_UUID);
BLECharacteristic dataChar(DATA_CHAR_UUID);
BLECharacteristic ctrlChar(CTRL_CHAR_UUID);

uint32_t lastSample = 0;
uint32_t lastMotionSend = 0;
bool streamingEnabled = true;

LSM6DS3 imu(I2C_MODE, 0x6A);
bool motionArmed = true;

// ====== Overload Test State ======
enum TestMode {
  MODE_NORMAL,
  MODE_ACCEL_OVERLOAD,
  MODE_GYRO_OVERLOAD,
  MODE_RAPID_FIRE,
  MODE_BLE_FLOOD,
  MODE_MEMORY_STRESS,
  MODE_SUSTAINED
};

TestMode currentMode = MODE_NORMAL;
uint32_t testStartTime = 0;
uint32_t testDuration = 10000; // 10 seconds per test phase
uint32_t lastRapidFireSend = 0;
uint32_t lastBleFloodSend = 0;
uint32_t notificationsSent = 0;
uint32_t notificationsFailed = 0;

// ====== Serial Command Handler ======
static void handleSerialCommand() {
  if (!Serial.available()) {
    return;
  }

  const char cmd = Serial.read();
  
  switch (cmd) {
    case 'a':
    case 'A':
      currentMode = MODE_ACCEL_OVERLOAD;
      testStartTime = millis();
      Serial.println("[TEST] Accel Overload Mode started (5G+ simulation)");
      break;
      
    case 'g':
    case 'G':
      currentMode = MODE_GYRO_OVERLOAD;
      testStartTime = millis();
      Serial.println("[TEST] Gyro Overload Mode started (500+ DPS simulation)");
      break;
      
    case 'r':
      currentMode = MODE_RAPID_FIRE;
      testStartTime = millis();
      lastRapidFireSend = millis();
      notificationsSent = 0;
      Serial.println("[TEST] Rapid Fire Mode started (motion spam test)");
      break;
      
    case 'b':
      currentMode = MODE_BLE_FLOOD;
      testStartTime = millis();
      lastBleFloodSend = millis();
      notificationsSent = 0;
      notificationsFailed = 0;
      Serial.println("[TEST] BLE Flood Mode started (max throughput test)");
      break;
      
    case 'm':
      currentMode = MODE_MEMORY_STRESS;
      testStartTime = millis();
      Serial.println("[TEST] Memory Stress Mode started (alloc/dealloc cycle)");
      break;
      
    case 's':
      currentMode = MODE_SUSTAINED;
      testStartTime = millis();
      Serial.println("[TEST] Sustained Load Mode started (10sec accel + 10sec gyro + 10sec rapid + 10sec BLE)");
      break;
      
    case 'c':
    case 'C':
      currentMode = MODE_NORMAL;
      notificationsSent = 0;
      notificationsFailed = 0;
      Serial.println("[TEST] Normal operation resumed");
      break;
      
    case 'R':
      Serial.println("[RESET] Software reset requested");
      Serial.flush();
      delay(50);
      NVIC_SystemReset();
      break;
      
    default:
      Serial.println("[CMD] Unknown command. Use: a/g/r/b/m/s/c/R");
      break;
  }
}

// ====== Diagnostic: Print Free Memory ======
extern "C" char __brkval;
extern "C" char __heap_start;

uint32_t freeMemory() {
  return (uint32_t)(&__brkval) - (uint32_t)(&__heap_start);
}

// ====== Test: Accel Overload ======
static void runAccelOverloadTest() {
  if (millis() - testStartTime > testDuration) {
    currentMode = MODE_NORMAL;
    Serial.println("[TEST] Accel Overload Mode ended");
    return;
  }

  if (!streamingEnabled || !Bluefruit.connected()) {
    return;
  }

  const uint32_t now = millis();
  if (now - lastSample < SAMPLE_INTERVAL_MS) {
    return;
  }
  lastSample = now;

  // Read real IMU data
  float ax = imu.readFloatAccelX();
  float ay = imu.readFloatAccelY();
  float az = imu.readFloatAccelZ();
  float gx = imu.readFloatGyroX();
  float gy = imu.readFloatGyroY();
  float gz = imu.readFloatGyroZ();

  // Amplify accel to simulate overload
  ax *= 2.5;  // Boost to ~5G under normal swing
  ay *= 2.5;
  az *= 2.5;

  float accelMag = sqrtf(ax * ax + ay * ay + az * az);
  float accelDelta = fabsf(accelMag - 1.0f);
  float gyroMag = sqrtf(gx * gx + gy * gy + gz * gz);

  const bool overload = (accelDelta > ACCEL_OVERLOAD_G);

  if (!overload && accelDelta < (ACCEL_DELTA_G * 0.45f)) {
    motionArmed = true;
  }

  if (overload && motionArmed && (now - lastMotionSend >= MOTION_COOLDOWN_MS)) {
    motionArmed = false;
    lastMotionSend = now;

    char payload[96];
    snprintf(payload, sizeof(payload),
             "{\"ts\":%lu,\"stroke\":\"overload_accel\",\"conf\":0.99,\"peak\":%.1f}\n",
             now, accelMag * 9.81f);

    size_t payloadLen = strlen(payload);
    if (dataChar.notify((const uint8_t*)payload, payloadLen)) {
      notificationsSent++;
    } else {
      notificationsFailed++;
    }
    Serial.print(payload);
  }
}

// ====== Test: Gyro Overload ======
static void runGyroOverloadTest() {
  if (millis() - testStartTime > testDuration) {
    currentMode = MODE_NORMAL;
    Serial.println("[TEST] Gyro Overload Mode ended");
    return;
  }

  if (!streamingEnabled || !Bluefruit.connected()) {
    return;
  }

  const uint32_t now = millis();
  if (now - lastSample < SAMPLE_INTERVAL_MS) {
    return;
  }
  lastSample = now;

  float ax = imu.readFloatAccelX();
  float ay = imu.readFloatAccelY();
  float az = imu.readFloatAccelZ();
  float gx = imu.readFloatGyroX();
  float gy = imu.readFloatGyroY();
  float gz = imu.readFloatGyroZ();

  // Amplify gyro to simulate overload
  gx *= 2.2;  // Boost to 500+ DPS under normal swing
  gy *= 2.2;
  gz *= 2.2;

  float accelMag = sqrtf(ax * ax + ay * ay + az * az);
  float accelDelta = fabsf(accelMag - 1.0f);
  float gyroMag = sqrtf(gx * gx + gy * gy + gz * gz);

  const bool overload = (gyroMag > GYRO_OVERLOAD_DPS);

  if (!overload && accelDelta < (ACCEL_DELTA_G * 0.45f)) {
    motionArmed = true;
  }

  if (overload && motionArmed && (now - lastMotionSend >= MOTION_COOLDOWN_MS)) {
    motionArmed = false;
    lastMotionSend = now;

    char payload[96];
    snprintf(payload, sizeof(payload),
             "{\"ts\":%lu,\"stroke\":\"overload_gyro\",\"conf\":0.99,\"peak\":%.1f}\n",
             now, gyroMag);

    size_t payloadLen = strlen(payload);
    if (dataChar.notify((const uint8_t*)payload, payloadLen)) {
      notificationsSent++;
    } else {
      notificationsFailed++;
    }
    Serial.print(payload);
  }
}

// ====== Test: Rapid Fire Motion Spam ======
static void runRapidFireTest() {
  if (millis() - testStartTime > testDuration) {
    currentMode = MODE_NORMAL;
    Serial.print("[TEST] Rapid Fire Mode ended. Sent: ");
    Serial.print(notificationsSent);
    Serial.println(" events");
    notificationsSent = 0;
    return;
  }

  if (!streamingEnabled || !Bluefruit.connected()) {
    return;
  }

  const uint32_t now = millis();
  
  // Send rapid detections without cooldown to stress the pipeline
  if (now - lastRapidFireSend >= RAPID_FIRE_INTERVAL) {
    lastRapidFireSend = now;

    const char* strokes[] = {"forehand", "backhand", "volley", "serve"};
    uint8_t strokeIdx = notificationsSent % 4;
    float conf = 0.85f + (random(0, 15) / 100.0f);
    float peak = 15.0f + (random(0, 20) / 10.0f);

    char payload[96];
    snprintf(payload, sizeof(payload),
             "{\"ts\":%lu,\"stroke\":\"%s\",\"conf\":%.2f,\"peak\":%.1f}\n",
             now, strokes[strokeIdx], conf, peak);

    size_t payloadLen = strlen(payload);
    if (dataChar.notify((const uint8_t*)payload, payloadLen)) {
      notificationsSent++;
    } else {
      notificationsFailed++;
    }
    
    if (notificationsSent % 10 == 0) {
      Serial.print("[RAPID] Sent ");
      Serial.print(notificationsSent);
      Serial.print(" | Failed: ");
      Serial.println(notificationsFailed);
    }
  }
}

// ====== Test: BLE Flood (Maximum Throughput) ======
static void runBleFloodTest() {
  if (millis() - testStartTime > testDuration) {
    currentMode = MODE_NORMAL;
    Serial.print("[TEST] BLE Flood Mode ended. Sent: ");
    Serial.print(notificationsSent);
    Serial.print(" | Failed: ");
    Serial.print(notificationsFailed);
    Serial.print(" | FreeRAM: ");
    Serial.print(freeMemory());
    Serial.println(" bytes");
    notificationsSent = 0;
    notificationsFailed = 0;
    return;
  }

  if (!streamingEnabled || !Bluefruit.connected()) {
    return;
  }

  const uint32_t now = millis();
  
  // Send notifications as fast as possible (aggressive: 1ms minimum)
  if (now - lastBleFloodSend >= 1) {
    lastBleFloodSend = now;

    char payload[96];
    snprintf(payload, sizeof(payload),
             "{\"ts\":%lu,\"pkt\":%lu,\"ram\":%lu}\n",
             now, notificationsSent, freeMemory());

    size_t payloadLen = strlen(payload);
    if (dataChar.notify((const uint8_t*)payload, payloadLen)) {
      notificationsSent++;
    } else {
      notificationsFailed++;
      // Backoff on failure
      delay(10);
    }
    
    if (notificationsSent % 50 == 0) {
      Serial.print("[FLOOD] ");
      Serial.print(notificationsSent);
      Serial.print(" sent | ");
      Serial.print(notificationsFailed);
      Serial.print(" failed | RAM: ");
      Serial.print(freeMemory());
      Serial.println(" bytes");
    }
  }
}

// ====== Test: Memory Stress ======
static void runMemoryStressTest() {
  if (millis() - testStartTime > testDuration) {
    currentMode = MODE_NORMAL;
    Serial.println("[TEST] Memory Stress Mode ended");
    return;
  }

  // Allocate and deallocate in a tight loop to stress memory allocator
  uint8_t* buffer = (uint8_t*)malloc(MEMORY_TEST_ALLOC_SIZE);
  if (buffer) {
    // Simulate use
    for (size_t i = 0; i < MEMORY_TEST_ALLOC_SIZE; i++) {
      buffer[i] = (uint8_t)(i & 0xFF);
    }
    free(buffer);
  }

  // Print memory status periodically
  static uint32_t lastMemPrint = 0;
  uint32_t now = millis();
  if (now - lastMemPrint > 1000) {
    lastMemPrint = now;
    Serial.print("[MEM] Free RAM: ");
    Serial.print(freeMemory());
    Serial.println(" bytes");
  }
}

// ====== Test: Sustained Load (Sequential Test Phases) ======
static void runSustainedTest() {
  uint32_t elapsed = millis() - testStartTime;
  uint32_t phaseTime = 10000; // 10 seconds per phase

  if (elapsed > phaseTime * 4) {
    // All phases complete
    currentMode = MODE_NORMAL;
    Serial.println("[TEST] Sustained Load Mode ended (all 4 phases complete)");
    return;
  }

  uint32_t phase = elapsed / phaseTime;
  uint32_t phaseElapsed = elapsed % phaseTime;

  // Temporarily switch mode for this phase
  TestMode originalMode = currentMode;
  
  if (phase == 0) {
    currentMode = MODE_ACCEL_OVERLOAD;
    if (phaseElapsed < SAMPLE_INTERVAL_MS) {
      Serial.println("[SUSTAINED] Phase 0: Accel Overload");
    }
    runAccelOverloadTest();
  } else if (phase == 1) {
    currentMode = MODE_GYRO_OVERLOAD;
    if (phaseElapsed < SAMPLE_INTERVAL_MS) {
      Serial.println("[SUSTAINED] Phase 1: Gyro Overload");
    }
    runGyroOverloadTest();
  } else if (phase == 2) {
    currentMode = MODE_RAPID_FIRE;
    if (phaseElapsed < SAMPLE_INTERVAL_MS) {
      Serial.println("[SUSTAINED] Phase 2: Rapid Fire");
    }
    runRapidFireTest();
  } else if (phase == 3) {
    currentMode = MODE_BLE_FLOOD;
    if (phaseElapsed < SAMPLE_INTERVAL_MS) {
      Serial.println("[SUSTAINED] Phase 3: BLE Flood");
    }
    runBleFloodTest();
  }

  currentMode = originalMode;
}

// ====== Setup ======
void setup() {
  Serial.begin(115200);
  uint32_t serialStart = millis();
  while (!Serial && (millis() - serialStart) < 2000) {}

  Serial.println("\n\n=== SmartRacket Overload Test (局部負荷過大) ===");
  Serial.println("Commands: a/A=AccelOL | g/G=GyroOL | r=RapidFire | b=BLE_Flood | m=MemStress | s=Sustained | c=Clear | R=Reset");
  Serial.println();

  Bluefruit.begin();
  Bluefruit.setTxPower(4);
  Bluefruit.setName("SmartRacket");

  modelService.begin();

  dataChar.setProperties(CHR_PROPS_READ | CHR_PROPS_NOTIFY);
  dataChar.setPermission(SECMODE_OPEN, SECMODE_NO_ACCESS);
  dataChar.setMaxLen(100);
  dataChar.begin();
  dataChar.write((const uint8_t*)"{}", 2);

  ctrlChar.setProperties(CHR_PROPS_WRITE);
  ctrlChar.setPermission(SECMODE_NO_ACCESS, SECMODE_OPEN);
  ctrlChar.setFixedLen(1);
  ctrlChar.begin();

  Bluefruit.Advertising.addService(modelService);
  Bluefruit.Advertising.addName();
  Bluefruit.Advertising.restartOnDisconnect(true);
  Bluefruit.Advertising.setInterval(32, 244);
  Bluefruit.Advertising.start(0);

  Serial.println("BLE advertising...");
  randomSeed(analogRead(A0));

  if (imu.begin() != 0) {
    Serial.println("IMU init failed");
  } else {
    Serial.println("IMU ready");
  }

  Serial.print("Free RAM at startup: ");
  Serial.print(freeMemory());
  Serial.println(" bytes\n");
}

// ====== Loop ======
void loop() {
  handleSerialCommand();

  switch (currentMode) {
    case MODE_NORMAL:
      // Normal operation (could be added here if needed)
      break;
    case MODE_ACCEL_OVERLOAD:
      runAccelOverloadTest();
      break;
    case MODE_GYRO_OVERLOAD:
      runGyroOverloadTest();
      break;
    case MODE_RAPID_FIRE:
      runRapidFireTest();
      break;
    case MODE_BLE_FLOOD:
      runBleFloodTest();
      break;
    case MODE_MEMORY_STRESS:
      runMemoryStressTest();
      break;
    case MODE_SUSTAINED:
      runSustainedTest();
      break;
    default:
      break;
  }

  yield();
}
