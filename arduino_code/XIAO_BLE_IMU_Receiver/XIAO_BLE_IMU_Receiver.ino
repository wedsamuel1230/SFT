#include <bluefruit.h>
#include <stdint.h>

const uint8_t SERVICE_UUID[16]   = {0x4b, 0x91, 0x31, 0xc3, 0xc9, 0xc5, 0xcc, 0x8f, 0x9e, 0x45, 0xb5, 0x1f, 0x01, 0xc2, 0xaf, 0x4f};
const uint8_t DATA_CHAR_UUID[16] = {0xa8, 0x26, 0x1b, 0x36, 0x07, 0xea, 0xf5, 0xb7, 0x88, 0x46, 0xe1, 0x36, 0x3e, 0x48, 0xb5, 0xbe};

static const uint8_t PACKET_SIZE = 14;
static const uint8_t SAMPLE_QUEUE_SIZE = 32;
static const uint32_t STATS_INTERVAL_MS = 1000;
bool DebugMode = 0;
BLEClientService imuService(SERVICE_UUID);
BLEClientCharacteristic imuDataChar(DATA_CHAR_UUID);

struct ImuSample {
  uint16_t sequence;
  int16_t ax;
  int16_t ay;
  int16_t az;
  int16_t gx;
  int16_t gy;
  int16_t gz;
};

ImuSample sampleQueue[SAMPLE_QUEUE_SIZE] = {};
volatile uint8_t queueHead = 0;
volatile uint8_t queueTail = 0;
bool hasSequence = false;
uint16_t lastSequence = 0;
uint32_t receivedPackets = 0;
uint32_t missingPackets = 0;
uint32_t queueOverflows = 0;
uint32_t invalidPackets = 0;
uint32_t lastStatsMs = 0;

uint16_t readUInt16LE(const uint8_t* buffer, uint8_t offset) {
  return static_cast<uint16_t>(
      static_cast<uint16_t>(buffer[offset + 0]) |
      (static_cast<uint16_t>(buffer[offset + 1]) << 8));
}

int16_t readInt16LE(const uint8_t* buffer, uint8_t offset) {
  return static_cast<int16_t>(readUInt16LE(buffer, offset));
}

void printScaledValue(int16_t value) {
  const bool isNegative = value < 0;
  const int32_t magnitude = isNegative ? -(static_cast<int32_t>(value)) : value;
  const int32_t whole = magnitude / 100;
  const int32_t fraction = magnitude % 100;
  if (isNegative) {
    Serial.print('-');
  }
  Serial.print(whole);
  Serial.print('.');
  if (fraction < 10) {
    Serial.print('0');
  }
  Serial.print(fraction);
}

void printImuLine(const ImuSample& sample) {
  printScaledValue(sample.ax);
  Serial.print(", ");
  printScaledValue(sample.ay);
  Serial.print(", ");
  printScaledValue(sample.az);
  Serial.print(", ");
  printScaledValue(sample.gx);
  Serial.print(", ");
  printScaledValue(sample.gy);
  Serial.print(", ");
  printScaledValue(sample.gz);
  Serial.println();
}

void notifyCallback(BLEClientCharacteristic* chr, uint8_t* data, uint16_t len) {
  (void) chr;

  if (len != PACKET_SIZE) {
    invalidPackets++;
    return;
  }

  ImuSample sample;
  sample.sequence = readUInt16LE(data, 0);
  sample.ax = readInt16LE(data, 2);
  sample.ay = readInt16LE(data, 4);
  sample.az = readInt16LE(data, 6);
  sample.gx = readInt16LE(data, 8);
  sample.gy = readInt16LE(data, 10);
  sample.gz = readInt16LE(data, 12);

  if (hasSequence) {
    const uint16_t gap = static_cast<uint16_t>(sample.sequence - lastSequence - 1);
    missingPackets += gap;
  }
  lastSequence = sample.sequence;
  hasSequence = true;
  receivedPackets++;

  const uint8_t nextHead = static_cast<uint8_t>((queueHead + 1) % SAMPLE_QUEUE_SIZE);
  if (nextHead == queueTail) {
    queueOverflows++;
    return;
  }

  sampleQueue[queueHead] = sample;
  queueHead = nextHead;
}

void scanCallback(ble_gap_evt_adv_report_t* report) {
  Bluefruit.Central.connect(report);
}

void connectCallback(uint16_t conn_handle) {
  Serial.println("Connected to sender");
  Serial.print("Discovering IMU service ... ");

  if (!imuService.discover(conn_handle)) {
    Serial.println("not found");
    Bluefruit.disconnect(conn_handle);
    return;
  }
  Serial.println("found");

  Serial.print("Discovering IMU characteristic ... ");
  if (!imuDataChar.discover()) {
    Serial.println("not found");
    Bluefruit.disconnect(conn_handle);
    return;
  }
  Serial.println("found");

  BLEConnection* connection = Bluefruit.Connection(conn_handle);
  if (connection != nullptr) {
    const bool phyRequested = connection->requestPHY();
    const bool dataLengthRequested = connection->requestDataLengthUpdate();
    const bool mtuRequested = connection->requestMtuExchange(247);

    // Serial.print("Link tuning phy=");
    // Serial.print(phyRequested ? "ok" : "skip");
    // Serial.print(" data_length=");
    // Serial.print(dataLengthRequested ? "ok" : "skip");
    // Serial.print(" mtu=");
    // Serial.println(mtuRequested ? "ok" : "skip");
  }

  if (imuDataChar.enableNotify()) {
    Serial.println("Subscribed to IMU notifications");
  } else {
    Serial.println("Failed to enable notifications");
    Bluefruit.disconnect(conn_handle);
  }
}

void disconnectCallback(uint16_t conn_handle, uint8_t reason) {
  (void) conn_handle;
  queueHead = 0;
  queueTail = 0;
  hasSequence = false;
  Serial.print("Disconnected, reason=0x");
  Serial.println(reason, HEX);
}

void setup() {
  Serial.begin(115200);
  uint32_t serialStart = millis();
  while (!Serial && (millis() - serialStart) < 2000) {
    delay(10);
  }

  Serial.println("XIAO BLE IMU Receiver");

  Bluefruit.configCentralBandwidth(BANDWIDTH_MAX);
  Bluefruit.begin(0, 1);
  Bluefruit.setName("SmartRacketRx");
  Bluefruit.Central.setConnInterval(6, 8);

  imuService.begin();
  imuDataChar.setNotifyCallback(notifyCallback);
  imuDataChar.begin();

  Bluefruit.Central.setConnectCallback(connectCallback);
  Bluefruit.Central.setDisconnectCallback(disconnectCallback);

  Bluefruit.Scanner.setRxCallback(scanCallback);
  Bluefruit.Scanner.restartOnDisconnect(true);
  Bluefruit.Scanner.setInterval(160, 80);
  Bluefruit.Scanner.filterUuid(imuService.uuid);
  Bluefruit.Scanner.useActiveScan(false);
  Bluefruit.Scanner.start(0);

  lastStatsMs = millis();

  Serial.println("Scanning for SmartRacket sender");
}

void loop() {
  while (queueTail != queueHead) {
    const ImuSample sample = sampleQueue[queueTail];
    queueTail = static_cast<uint8_t>((queueTail + 1) % SAMPLE_QUEUE_SIZE);
    printImuLine(sample);
  }

  const uint32_t nowMs = millis();
  if ((nowMs - lastStatsMs) >= STATS_INTERVAL_MS && DebugMode) {
    Serial.print("stats recv_last_s=");
    Serial.print(receivedPackets);
    Serial.print(" missing_total=");
    Serial.print(missingPackets);
    Serial.print(" queue_overflow_total=");
    Serial.print(queueOverflows);
    Serial.print(" invalid_total=");
    Serial.println(invalidPackets);

    receivedPackets = 0;
    lastStatsMs = nowMs;
  }
}
