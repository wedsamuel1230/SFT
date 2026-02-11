#include <bluefruit.h>
#include <LSM6DS3.h>
#include <math.h>

// ====== BLE UUIDs — must match BleDeviceProfile.DEFAULT in the Android app ======
// NOTE: Bluefruit expects 128-bit UUIDs in little-endian byte order.
const uint8_t SERVICE_UUID[16]   = {0x4b, 0x91, 0x31, 0xc3, 0xc9, 0xc5, 0xcc, 0x8f, 0x9e, 0x45, 0xb5, 0x1f, 0x01, 0xc2, 0xaf, 0x4f};
const uint8_t DATA_CHAR_UUID[16] = {0xa8, 0x26, 0x1b, 0x36, 0x07, 0xea, 0xf5, 0xb7, 0x88, 0x46, 0xe1, 0x36, 0x3e, 0x48, 0xb5, 0xbe};  // IMU / model-output characteristic
const uint8_t CTRL_CHAR_UUID[16] = {0xa9, 0x26, 0x1b, 0x36, 0x07, 0xea, 0xf5, 0xb7, 0x88, 0x46, 0xe1, 0x36, 0x3e, 0x48, 0xb5, 0xbe};  // control characteristic

// ====== Config ======
const uint32_t SAMPLE_INTERVAL_MS = 20;  // 50 Hz IMU polling
const uint32_t MOTION_COOLDOWN_MS = 500;
const size_t NOTIFY_CHUNK_SIZE = 20;

// Big motion thresholds (tune as needed)
const float ACCEL_DELTA_G = 1.8f;      // delta from 1g
const float GYRO_DPS_THRESHOLD = 250.0f;

BLEService modelService(SERVICE_UUID);

// JSON payload fits comfortably under BLE MTU
BLECharacteristic dataChar(DATA_CHAR_UUID);
BLECharacteristic ctrlChar(CTRL_CHAR_UUID);

uint32_t lastSample = 0;
uint32_t lastMotionSend = 0;
bool streamingEnabled = true;

LSM6DS3 imu(I2C_MODE, 0x6A);

bool motionArmed = true;

static void checkSerialReset() {
  if (!Serial.available()) {
    return;
  }

  const int ch = Serial.read();
  if (ch == 'r' || ch == 'R') {
    Serial.println("Software reset requested");
    Serial.flush();
    delay(50);
    NVIC_SystemReset();
  }
}

void onCtrlWrite(uint16_t connHandle, BLECharacteristic* characteristic, uint8_t* data, uint16_t len) {
  (void)connHandle;
  (void)characteristic;

  if (len < 1) {
    return;
  }

  streamingEnabled = (data[0] == 1);
  Serial.print("Streaming: ");
  Serial.println(streamingEnabled ? "ON" : "OFF");
}

void setup() {
  Serial.begin(115200);
  uint32_t serialStart = millis();
  while (!Serial && (millis() - serialStart) < 2000) {}

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
  ctrlChar.setWriteCallback(onCtrlWrite);
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
}

void loop() {
  checkSerialReset();
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

  const float accelMag = sqrtf(ax * ax + ay * ay + az * az);
  const float accelDelta = fabsf(accelMag - 1.0f);
  const float gyroMag = sqrtf(gx * gx + gy * gy + gz * gz);

  const bool bigMotion = (accelDelta > ACCEL_DELTA_G) || (gyroMag > GYRO_DPS_THRESHOLD);

  if (!bigMotion && accelDelta < (ACCEL_DELTA_G * 0.6f)) {
    motionArmed = true;
  }

  if (bigMotion && motionArmed && (now - lastMotionSend >= MOTION_COOLDOWN_MS)) {
    motionArmed = false;
    lastMotionSend = now;

    const char* stroke = "forehand";
    float conf = random(70, 99) / 100.0f;  // 0.70–0.98
    float peak = accelMag * 9.81f;         // convert g to m/s^2
    uint32_t ts = millis();

    // JSON payload — keys match Android BluetoothManager.parseModelOutputJson:
    //   ts → timestamp, stroke → stroke type, conf → confidence, peak → peak accel
    //   NOTE: score is NOT sent — Android app calculates score from conf
    char payload[96];
    snprintf(payload, sizeof(payload),
             "{\"ts\":%lu,\"stroke\":\"%s\",\"conf\":%.2f,\"peak\":%.1f}\n",
             ts, stroke, conf, peak);

    const size_t payloadLen = strlen(payload);
    for (size_t offset = 0; offset < payloadLen; offset += NOTIFY_CHUNK_SIZE) {
      const size_t chunkLen = min(NOTIFY_CHUNK_SIZE, payloadLen - offset);
      if (!dataChar.notify((const uint8_t*)(payload + offset), chunkLen)) {
        break;
      }
    }
    Serial.print(payload);
  }
}