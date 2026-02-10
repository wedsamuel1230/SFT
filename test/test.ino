#include <bluefruit.h>

// ====== BLE UUIDs — must match BleDeviceProfile.DEFAULT in the Android app ======
// NOTE: Bluefruit expects 128-bit UUIDs in little-endian byte order.
const uint8_t SERVICE_UUID[16]   = {0x4b, 0x91, 0x31, 0xc3, 0xc9, 0xc5, 0xcc, 0x8f, 0x9e, 0x45, 0xb5, 0x1f, 0x01, 0xc2, 0xaf, 0x4f};
const uint8_t DATA_CHAR_UUID[16] = {0xa8, 0x26, 0x1b, 0x36, 0x07, 0xea, 0xf5, 0xb7, 0x88, 0x46, 0xe1, 0x36, 0x3e, 0x48, 0xb5, 0xbe};  // IMU / model-output characteristic
const uint8_t CTRL_CHAR_UUID[16] = {0xa9, 0x26, 0x1b, 0x36, 0x07, 0xea, 0xf5, 0xb7, 0x88, 0x46, 0xe1, 0x36, 0x3e, 0x48, 0xb5, 0xbe};  // control characteristic

// ====== Config ======
const uint32_t SEND_INTERVAL_MS = 50;  // 20 Hz
const size_t NOTIFY_CHUNK_SIZE = 20;

BLEService modelService(SERVICE_UUID);

// JSON payload fits comfortably under BLE MTU
BLECharacteristic dataChar(DATA_CHAR_UUID);
BLECharacteristic ctrlChar(CTRL_CHAR_UUID);

uint32_t lastSend = 0;
bool streamingEnabled = true;

// Prototype stroke types: forehand, backhand, drive
const char* strokeTypes[] = {"forehand", "backhand", "drive"};

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
}

void loop() {
  // Send mock data at interval
  if (streamingEnabled && Bluefruit.connected()) {
    uint32_t now = millis();
    if (now - lastSend >= SEND_INTERVAL_MS) {
      lastSend = now;

      // Generate plausible mock values (3 stroke types only)
      const char* stroke = strokeTypes[random(0, 3)];
      float conf = random(50, 99) / 100.0;             // 0.50–0.98
      float peak = random(80, 160) / 10.0;             // 8.0–16.0
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
}