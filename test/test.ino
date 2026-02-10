#include <ArduinoBLE.h>

// ====== BLE UUIDs — must match BleDeviceProfile.DEFAULT in the Android app ======
const char* SERVICE_UUID      = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
const char* DATA_CHAR_UUID    = "beb5483e-36e1-4688-b7f5-ea07361b26a8";  // IMU / model-output characteristic
const char* CTRL_CHAR_UUID    = "beb5483e-36e1-4688-b7f5-ea07361b26a9";  // control characteristic

// ====== Config ======
const unsigned long SEND_INTERVAL_MS = 250;  // 4 Hz

BLEService modelService(SERVICE_UUID);

// JSON payload fits comfortably under BLE MTU
BLECharacteristic dataChar(DATA_CHAR_UUID, BLERead | BLENotify, 100);
BLEByteCharacteristic ctrlChar(CTRL_CHAR_UUID, BLEWrite);

unsigned long lastSend = 0;
bool streamingEnabled = true;

// Prototype stroke types: forehand, backhand, drive
const char* strokeTypes[] = {"forehand", "backhand", "drive"};

void setup() {
  Serial.begin(115200);
  while (!Serial) {}

  if (!BLE.begin()) {
    Serial.println("BLE init failed!");
    while (1) {}
  }

  BLE.setLocalName("SmartRacket");
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

      // Generate plausible mock values (3 stroke types only)
      const char* stroke = strokeTypes[random(0, 3)];
      float conf = random(50, 99) / 100.0;             // 0.50–0.98
      float peak = random(80, 160) / 10.0;             // 8.0–16.0
      unsigned long ts = millis();

      // JSON payload — keys match Android BluetoothManager.parseModelOutputJson:
      //   ts → timestamp, stroke → stroke type, conf → confidence, peak → peak accel
      //   NOTE: score is NOT sent — Android app calculates score from conf
      char payload[96];
      snprintf(payload, sizeof(payload),
               "{\"ts\":%lu,\"stroke\":\"%s\",\"conf\":%.2f,\"peak\":%.1f}",
               ts, stroke, conf, peak);

      dataChar.setValue(payload);
      Serial.println(payload);
    }
  }
}