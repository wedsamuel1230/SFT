/* Edge Impulse Arduino examples
 * Copyright (c) 2021 EdgeImpulse Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/* Includes ---------------------------------------------------------------- */
#include <SFT_inferencing.h>
#include <LSM6DS3.h>
#include <Wire.h>
#include <bluefruit.h>
#include <nrf_power.h>
#include <nrf_gpio.h>
#include <cstring>
/* Constant defines -------------------------------------------------------- */
#define CONVERT_G_TO_MS2    9.80665f
#define MAX_ACCEPTED_RANGE  2.0f        // starting 03/2022, models are generated setting range to +-2, but this example use Arudino library which set range to +-4g. If you are using an older model, ignore this value and use 4.0f instead

#define RED_LED 11
#define GREEN_LED 12
#define BLUE_LED 13

// Sleep mode configuration
#define SLEEP_PIN 2  // D0 is GPIO 2 on XIAO nRF52840 (P0.02)

/*
 ** NOTE: If you run into TFLite arena allocation issue.
 **
 ** This may be due to may dynamic memory fragmentation.
 ** Try defining "-DEI_CLASSIFIER_ALLOCATION_STATIC" in boards.local.txt (create
 ** if it doesn't exist) and copy this file to
 ** `<ARDUINO_CORE_INSTALL_PATH>/arduino/hardware/<mbed_core>/<core_version>/`.
 **
 ** See
 ** (https://support.arduino.cc/hc/en-us/articles/360012076960-Where-are-the-installed-cores-located-)
 ** to find where Arduino installs cores on your machine.
 **
 ** If the problem persists then there's not enough memory for this model and application.
 */

/* Private variables ------------------------------------------------------- */
static bool debug_nn = false; // Set this to true to see e.g. features generated from the raw signal
LSM6DS3 myIMU(I2C_MODE, 0x6A);

#define SERVICE_UUID        "4fafc201-1fb5-4599-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

BLEService        eiservice(SERVICE_UUID);
BLECharacteristic eicharacteristic(CHARACTERISTIC_UUID);
bool deviceConnected = false;
bool oldDeviceConnected = false;

// Sleep mode variables
bool sleepMode = false;
bool lastPinState = HIGH;

void connect_callback(uint16_t conn_handle) {
    deviceConnected = true;
    Serial.println("Connected");
    // Flash green on connect
    setRGB(false, true, false);
    delay(500);
    setRGB(false, false, false);
    // Then solid green if connected
    if (deviceConnected) setRGB(false, true, false);
}

void disconnect_callback(uint16_t conn_handle, uint8_t reason) {
    deviceConnected = false;
    Serial.println("Disconnected");
    // Flash red on disconnect
    setRGB(true, false, false);
    delay(500);
    setRGB(false, false, false);
    // Then solid red if disconnected
    if (!deviceConnected) setRGB(true, false, false);
}

void setRGB(bool r, bool g, bool b) {
    digitalWrite(RED_LED, r ? LOW : HIGH);
    digitalWrite(GREEN_LED, g ? LOW : HIGH);
    digitalWrite(BLUE_LED, b ? LOW : HIGH);
}

/**
* @brief      Arduino setup function
*/

void setup()
{
    // put your setup code here, to run once:
    Serial.begin(115200);
    Serial.println("Edge Impulse Inferencing Demo");

    // Initialize LED pins (active-low)
    pinMode(RED_LED, OUTPUT);
    pinMode(GREEN_LED, OUTPUT);
    pinMode(BLUE_LED, OUTPUT);
    // Initially disconnected: RED on, others off
    setRGB(true, false, false);

    // Initialize sleep pin
    pinMode(SLEEP_PIN, INPUT_PULLUP);

    //if (!IMU.begin()) {
      if (!myIMU.begin()) {
        ei_printf("Failed to initialize IMU!\r\n");
    }
    else {
        ei_printf("IMU initialized\r\n");
    }

    if (EI_CLASSIFIER_RAW_SAMPLES_PER_FRAME != 3) {
        ei_printf("ERR: EI_CLASSIFIER_RAW_SAMPLES_PER_FRAME should be equal to 3 (the 3 sensor axes)\n");
        return;
    }

    // Initialize Bluefruit
    Bluefruit.begin();
    Bluefruit.setName("XIAO-EI");

    // Set up callbacks
    Bluefruit.Periph.setConnectCallback(connect_callback);
    Bluefruit.Periph.setDisconnectCallback(disconnect_callback);

    // Configure and start the service
    eiservice.begin();

    // Configure and start the characteristic
    eicharacteristic.setProperties(CHR_PROPS_READ | CHR_PROPS_WRITE | CHR_PROPS_NOTIFY);
    eicharacteristic.setPermission(SECMODE_OPEN, SECMODE_OPEN);
    eicharacteristic.setFixedLen(20);
    eicharacteristic.begin();

    // Set up advertising
    Bluefruit.Advertising.addFlags(BLE_GAP_ADV_FLAGS_LE_ONLY_GENERAL_DISC_MODE);
    Bluefruit.Advertising.addTxPower();
    Bluefruit.Advertising.addService(eiservice);
    Bluefruit.Advertising.addName();
    Bluefruit.Advertising.restartOnDisconnect(true);
    Bluefruit.Advertising.setInterval(32, 244); // 20ms to 152.5ms
    Bluefruit.Advertising.setFastTimeout(30); // 30 seconds fast advertising
    Bluefruit.Advertising.start(0);

    Serial.println("Waiting a client connection to notify...");
}

/**
 * @brief Return the sign of the number
 * 
 * @param number 
 * @return int 1 if positive (or 0) -1 if negative
 */
float ei_get_sign(float number) {
    return (number >= 0.0) ? 1.0 : -1.0;
}

/**
* @brief      Get data and run inferencing
*
* @param[in]  debug  Get debug info if true
*/
void loop()
{
    // Check for sleep mode toggle
    bool currentPinState = digitalRead(SLEEP_PIN);
    if (currentPinState == LOW && lastPinState == HIGH) {
        sleepMode = !sleepMode;
        Serial.print("Sleep mode toggled to: ");
        Serial.println(sleepMode ? "ON" : "OFF");
    }
    lastPinState = currentPinState;
    
    if (sleepMode) {
        // Enter deep sleep
        Serial.println("Entering deep sleep...");
        nrf_gpio_cfg_sense_input(SLEEP_PIN, NRF_GPIO_PIN_PULLUP, NRF_GPIO_PIN_SENSE_LOW);
        sd_power_system_off();
    }

    // disconnecting
    if (!deviceConnected && oldDeviceConnected) {
        delay(500); // give the bluetooth stack the chance to get things ready
        Bluefruit.Advertising.start(); // restart advertising
        Serial.println("start advertising");
        oldDeviceConnected = deviceConnected;
    }
    // connecting
    if (deviceConnected && !oldDeviceConnected) {
        // do stuff here on connecting
        oldDeviceConnected = deviceConnected;
    }
    
    ei_printf("\nStarting inferencing in 2 seconds...\n");

    delay(2000);

    ei_printf("Sampling...\n");

    // Allocate a buffer here for the values we'll read from the IMU
    float buffer[EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE] = { 0 };

    for (size_t ix = 0; ix < EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE; ix += 3) {
        // Determine the next tick (and then sleep later)
        uint64_t next_tick = micros() + (EI_CLASSIFIER_INTERVAL_MS * 1000);

buffer[ix] = myIMU.readFloatAccelX();
buffer[ix+1] = myIMU.readFloatAccelY();
buffer[ix+2] = myIMU.readFloatAccelZ();

//buffer[ix] = myIMU.readFloatGyroX();
//buffer[ix+1] = myIMU.readFloatGyroY();
//buffer[ix+2] = myIMU.readFloatGyroZ();

        for (int i = 0; i < 3; i++) {
            if (fabs(buffer[ix + i]) > MAX_ACCEPTED_RANGE) {
                buffer[ix + i] = ei_get_sign(buffer[ix + i]) * MAX_ACCEPTED_RANGE;
            }
        }

        buffer[ix + 0] *= CONVERT_G_TO_MS2;
        buffer[ix + 1] *= CONVERT_G_TO_MS2;
        buffer[ix + 2] *= CONVERT_G_TO_MS2;

        delayMicroseconds(next_tick - micros());
    }

    // Turn the raw buffer in a signal which we can the classify
    signal_t signal;
    int err = numpy::signal_from_buffer(buffer, EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE, &signal);
    if (err != 0) {
        ei_printf("Failed to create signal from buffer (%d)\n", err);
        return;
    }

    // Run the classifier
    ei_impulse_result_t result = { 0 };

    err = run_classifier(&signal, &result, debug_nn);
    if (err != EI_IMPULSE_OK) {
        ei_printf("ERR: Failed to run classifier (%d)\n", err);
        return;
    }

    // print the predictions
    ei_printf("Predictions ");
    ei_printf("(DSP: %d ms., Classification: %d ms., Anomaly: %d ms.)",
        result.timing.dsp, result.timing.classification, result.timing.anomaly);
    ei_printf(": \n");
    for (size_t ix = 0; ix < EI_CLASSIFIER_LABEL_COUNT; ix++) {
        ei_printf("    %s: %.5f\n", result.classification[ix].label, result.classification[ix].value);
    }
#if EI_CLASSIFIER_HAS_ANOMALY == 1
    ei_printf("    anomaly score: %.3f\n", result.anomaly);
#endif

    // Find the prediction with highest confidence
    size_t best_ix = 0;
    float best_value = 0.0;
    for (size_t ix = 0; ix < EI_CLASSIFIER_LABEL_COUNT; ix++) {
        if (result.classification[ix].value > best_value) {
            best_value = result.classification[ix].value;
            best_ix = ix;
        }
    }

    // Set LED color based on inference result
    if (strcmp(result.classification[best_ix].label, "idle") == 0) {
        setRGB(false, false, true); // Blue for idle
    } else if (strcmp(result.classification[best_ix].label, "walking") == 0) {
        setRGB(false, true, false); // Green for walking
    } else if (strcmp(result.classification[best_ix].label, "running") == 0) {
        setRGB(true, true, false); // Yellow for running (red + green)
    } else {
        setRGB(true, false, true); // Magenta for unknown/other (red + blue)
    }

    // Format data: label:confidence (ensure <20 bytes)
    char data[20];
    snprintf(data, sizeof(data), "%s:%.2f", result.classification[best_ix].label, best_value);

    // Send via BLE if connected
    if (deviceConnected) {
        eicharacteristic.write((uint8_t*)data, strlen(data));
        eicharacteristic.notify((uint8_t*)data, strlen(data));
        Serial.printf("Sent: %s\n", data);
    }
}
