#include <bluefruit.h>
#include <LSM6DS3.h>
#include <Wire.h>
#include <math.h>
#include <stdint.h>

const uint8_t SERVICE_UUID[16]   = {0x4b, 0x91, 0x31, 0xc3, 0xc9, 0xc5, 0xcc, 0x8f, 0x9e, 0x45, 0xb5, 0x1f, 0x01, 0xc2, 0xaf, 0x4f};
const uint8_t DATA_CHAR_UUID[16] = {0xa8, 0x26, 0x1b, 0x36, 0x07, 0xea, 0xf5, 0xb7, 0x88, 0x46, 0xe1, 0x36, 0x3e, 0x48, 0xb5, 0xbe};

static const uint32_t SAMPLE_INTERVAL_US = 8000;
static const uint32_t STATS_INTERVAL_MS = 1000;
static const uint8_t PACKET_SIZE = 14;

BLEService imuService(SERVICE_UUID);
BLECharacteristic imuDataChar(DATA_CHAR_UUID);
LSM6DS3 imu(I2C_MODE, 0x6A);

uint32_t lastSampleUs = 0;
uint32_t lastStatsMs = 0;
uint16_t sampleSequence = 0;
bool isConnected = false;
bool imuReady = false;
uint32_t sentSamples = 0;
uint32_t notifyFailures = 0;
uint32_t clippedValues = 0;

int16_t scaleAxis(float value) {
  const int32_t scaled = static_cast<int32_t>(lroundf(value * 100.0f));
  if (scaled > INT16_MAX) {
    clippedValues++;
    return INT16_MAX;
  }
  if (scaled < INT16_MIN) {
    clippedValues++;
    return INT16_MIN;
  }
  return static_cast<int16_t>(scaled);
}

void writeUInt16LE(uint8_t* buffer, uint8_t offset, uint16_t value) {
  buffer[offset + 0] = static_cast<uint8_t>(value & 0xFF);
  buffer[offset + 1] = static_cast<uint8_t>((value >> 8) & 0xFF);
}

void writeInt16LE(uint8_t* buffer, uint8_t offset, int16_t value) {
  writeUInt16LE(buffer, offset, static_cast<uint16_t>(value));
}

void buildPacket(uint16_t sequence,
                 int16_t ax,
                 int16_t ay,
                 int16_t az,
                 int16_t gx,
                 int16_t gy,
                 int16_t gz,
                 uint8_t* packet) {
  writeUInt16LE(packet, 0, sequence);
  writeInt16LE(packet, 2, ax);
  writeInt16LE(packet, 4, ay);
  writeInt16LE(packet, 6, az);
  writeInt16LE(packet, 8, gx);
  writeInt16LE(packet, 10, gy);
  writeInt16LE(packet, 12, gz);
}

void printStatsIfDue() {
  const uint32_t nowMs = millis();
  if ((nowMs - lastStatsMs) < STATS_INTERVAL_MS) {
    return;
  }

  Serial.print("stats connected=");
  Serial.print(isConnected ? 1 : 0);
  Serial.print(" target_hz=");
  Serial.print(1000000UL / SAMPLE_INTERVAL_US);
  Serial.print(" sent_last_s=");
  Serial.print(sentSamples);
  Serial.print(" notify_fail_last_s=");
  Serial.print(notifyFailures);
  Serial.print(" clipped_last_s=");
  Serial.println(clippedValues);

  sentSamples = 0;
  notifyFailures = 0;
  clippedValues = 0;
  lastStatsMs = nowMs;
}

void connectCallback(uint16_t conn_handle) {
  (void) conn_handle;
  isConnected = true;
  BLEConnection* connection = Bluefruit.Connection(conn_handle);
  if (connection != nullptr) {
    const bool phyRequested = connection->requestPHY();
    const bool dataLengthRequested = connection->requestDataLengthUpdate();
    const bool mtuRequested = connection->requestMtuExchange(247);

    Serial.print("Link tuning phy=");
    Serial.print(phyRequested ? "ok" : "skip");
    Serial.print(" data_length=");
    Serial.print(dataLengthRequested ? "ok" : "skip");
    Serial.print(" mtu=");
    Serial.println(mtuRequested ? "ok" : "skip");
  }
  Serial.println("BLE receiver connected");
}

void disconnectCallback(uint16_t conn_handle, uint8_t reason) {
  (void) conn_handle;
  (void) reason;
  isConnected = false;
  Serial.println("BLE receiver disconnected");
}

void setup() {
  Serial.begin(115200);
  uint32_t serialStart = millis();
  while (!Serial && (millis() - serialStart) < 2000) {
    delay(10);
  }

  Serial.println("XIAO BLE IMU Sender");

  if (imu.begin() != 0) {
    Serial.println("Failed to initialize LSM6DS3");
  } else {
    imuReady = true;
    Serial.println("LSM6DS3 ready");
  }

  Bluefruit.configPrphBandwidth(BANDWIDTH_MAX);
  Bluefruit.begin(1, 0);
  Bluefruit.setTxPower(4);
  Bluefruit.setName("SmartRacket");
  Bluefruit.Periph.setConnectCallback(connectCallback);
  Bluefruit.Periph.setDisconnectCallback(disconnectCallback);
  Bluefruit.Periph.setConnInterval(6, 8);

  imuService.begin();

  imuDataChar.setProperties(CHR_PROPS_READ | CHR_PROPS_NOTIFY);
  imuDataChar.setPermission(SECMODE_OPEN, SECMODE_NO_ACCESS);
  imuDataChar.setFixedLen(PACKET_SIZE);
  imuDataChar.begin();

  uint8_t emptyPacket[PACKET_SIZE] = {0};
  imuDataChar.write(emptyPacket, sizeof(emptyPacket));

  Bluefruit.Advertising.addFlags(BLE_GAP_ADV_FLAGS_LE_ONLY_GENERAL_DISC_MODE);
  Bluefruit.Advertising.addTxPower();
  Bluefruit.Advertising.addService(imuService);
  Bluefruit.Advertising.addName();
  Bluefruit.Advertising.restartOnDisconnect(true);
  Bluefruit.Advertising.setInterval(32, 244);
  Bluefruit.Advertising.setFastTimeout(30);
  Bluefruit.Advertising.start(0);

  lastSampleUs = micros();
  lastStatsMs = millis();

  Serial.println("Advertising BLE IMU service");
}

void loop() {
  printStatsIfDue();

  const uint32_t nowUs = micros();
  if ((nowUs - lastSampleUs) < SAMPLE_INTERVAL_US) {
    return;
  }
  lastSampleUs = nowUs;

  if (!imuReady) {
    return;
  }

  if (!isConnected) {
    return;
  }

  const int16_t ax = scaleAxis(imu.readFloatAccelX());
  const int16_t ay = scaleAxis(imu.readFloatAccelY());
  const int16_t az = scaleAxis(imu.readFloatAccelZ());
  const int16_t gx = scaleAxis(imu.readFloatGyroX());
  const int16_t gy = scaleAxis(imu.readFloatGyroY());
  const int16_t gz = scaleAxis(imu.readFloatGyroZ());

  uint8_t packet[PACKET_SIZE] = {0};
  buildPacket(sampleSequence, ax, ay, az, gx, gy, gz, packet);

  if (imuDataChar.notify(packet, sizeof(packet))) {
    sentSamples++;
  } else {
    notifyFailures++;
  }

  sampleSequence++;
}
