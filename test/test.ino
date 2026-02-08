#include <ArduinoBLE.h>

// ====== BLE UUIDs (change if you want) ======
const char* SERVICE_UUID      = "12345678-1234-5678-1234-56789abcdef0";
const char* DATA_CHAR_UUID    = "12345678-1234-5678-1234-56789abcdef1";
const char* CTRL_CHAR_UUID    = "12345678-1234-5678-1234-56789abcdef2";

// ====== Config ======
const unsigned long SEND_INTERVAL_MS = 250;  // 4 Hz

BLEService modelService(SERVICE_UUID);

// JSON payload fits comfortably under BLE MTU
BLECharacteristic dataChar(DATA_CHAR_UUID, BLERead | BLENotify, 100);
BLEByteCharacteristic ctrlChar(CTRL_CHAR_UUID, BLEWrite);

unsigned long lastSend = 0;
bool streamingEnabled = true;

// Simple stroke types to mimic model output
const char* strokeTypes[] = {"fh", "bh", "sm", "ch"}; // forehand, backhand, smash, chop

void setup() {
  Serial.begin(115200);
  while (!Serial) {}

  if (!BLE.begin()) {
    Serial.println("BLE init failed!");
    while (1) {}
  }

  BLE.setLocalName("SmartRacketMock");
  BLE.setAdvertisedService(modelService);

  modelService.addCharacteristic(dataChar);
  modelService.addCharacteristic(ctrlChar);

  BLE.addService(modelService);

  // Initial values
  dataChar.setValue("{}");
  ctrlChar.setValue((byte)1);

  BLE.advertise();
  Serial.println("BLE advertising...");
  randomSeed(analogRead(A0));
}

void loop() {
  BLE.poll();

  // Handle control writes
  if (ctrlChar.written()) {
    byte value = ctrlChar.value();
    streamingEnabled = (value == 1);
    Serial.print("Streaming: ");
    Serial.println(streamingEnabled ? "ON" : "OFF");
  }

  // Send mock data at interval
  if (streamingEnabled && BLE.connected()) {
    unsigned long now = millis();
    if (now - lastSend >= SEND_INTERVAL_MS) {
      lastSend = now;

      // Generate plausible mock values
      const char* stroke = strokeTypes[random(0, 4)];
      int score = random(5, 10);                       // 5–9
      float conf = random(80, 99) / 100.0;             // 0.80–0.98
      float peak = random(80, 160) / 10.0;             // 8.0–16.0
      unsigned long ts = millis();

      // Compact JSON payload
      char payload[96];
      snprintf(payload, sizeof(payload),
               "{\"t\":%lu,\"s\":\"%s\",\"sc\":%d,\"c\":%.2f,\"p\":%.1f}",
               ts, stroke, score, conf, peak);

      dataChar.setValue(payload);
      Serial.println(payload);
    }
  }
}