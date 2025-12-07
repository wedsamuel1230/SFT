// Basic demo for accelerometer readings from Adafruit MPU6050

#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <cstring>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <Wire.h>

Adafruit_MPU6050 mpu;

#define SERVICE_UUID "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
const int characteristicNum = 6;
// Define UUIDs for each characteristic into a constant array
const char* AccelX_UUID = "4c319445-c14b-424a-8af2-f61fb2e03dd5";
const char* AccelY_UUID = "ab1ffb68-6cef-4dea-9276-eadd8c8595e4";
const char* AccelZ_UUID = "04be36ae-ee20-4c8b-9bdf-900f5f6667bc";
const char* GryoX_UUID = "1c49de54-cb40-494b-9a76-47333055495f";
const char* GryoY_UUID = "1a185490-663c-4dfd-8b50-bee798ede5fb";
const char* GryoZ_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8";

const char* CHARACTERISTIC_UUIDS[characteristicNum] = {
  AccelX_UUID,
  AccelY_UUID,
  AccelZ_UUID,
  GryoX_UUID,
  GryoY_UUID,
  GryoZ_UUID
};

BLECharacteristic *characteristics[characteristicNum] = { nullptr };

class MyCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pCharacteristic) {
    String value = pCharacteristic->getValue();

    if (value.length() > 0) {
      Serial.println("*********");
      Serial.print("New value: ");
      for (int i = 0; i < value.length(); i++) {
        Serial.print(value[i]);
      }

      Serial.println();
      Serial.println("*********");
    }
  }
};


void setup(void) {
  Serial.begin(115200);
  while (!Serial) {
    delay(10); // will pause Zero, Leonardo, etc until serial console opens
  }
  
  Serial.println("SFT Project");
  // Try to initialize!
  if (!mpu.begin()) {
    Serial.println("Failed to find MPU6050 chip");
    while (1) {
      delay(10);
    }
  }
  
  Serial.println("Gryo sensor init successfully!");

  mpu.setAccelerometerRange(MPU6050_RANGE_16_G);
  mpu.setGyroRange(MPU6050_RANGE_250_DEG);
  mpu.setFilterBandwidth(MPU6050_BAND_21_HZ);
  Serial.println("");

  BLEDevice::init("MyESP32");
  BLEServer *pServer = BLEDevice::createServer();

  BLEService *pService = pServer->createService(SERVICE_UUID);

  for (int i = 0; i < characteristicNum; i++) {
    characteristics[i] = pService->createCharacteristic(
      CHARACTERISTIC_UUIDS[i],
      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
    characteristics[i]->setCallbacks(new MyCallbacks());
    characteristics[i]->setValue("0.0");
  }
  pService->start();

  BLEAdvertising *pAdvertising = pServer->getAdvertising();
  pAdvertising->start();
  
  delay(100);
}

void loop() {

  /* Get new sensor events with the readings */
  sensors_event_t a, g, temp;
  mpu.getEvent(&a, &g, &temp);

  /* Print out the values */
  Serial.print("AccelX:");
  Serial.print(a.acceleration.x);
  Serial.print(",");
  Serial.print("AccelY:");
  Serial.print(a.acceleration.y);
  Serial.print(",");
  Serial.print("AccelZ:");
  Serial.print(a.acceleration.z);
  Serial.print(", ");
  Serial.print("GyroX:");
  Serial.print(g.gyro.x);
  Serial.print(",");
  Serial.print("GyroY:");
  Serial.print(g.gyro.y);
  Serial.print(",");
  Serial.print("GyroZ:");
  Serial.print(g.gyro.z);
  Serial.println("");

  const float imuValues[characteristicNum] = {
    a.acceleration.x,
    a.acceleration.y,
    a.acceleration.z,
    g.gyro.x,
    g.gyro.y,
    g.gyro.z
  };

  for (int i = 0; i < characteristicNum; i++) {
    char buffer[16];
    dtostrf(imuValues[i], 8, 3, buffer);
    characteristics[i]->setValue((uint8_t *)buffer, strlen(buffer));
    characteristics[i]->notify();
  }

  delay(10);
}