
/* Edge Impulse ingestion SDK
 * Copyright (c) 2022 EdgeImpulse Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/* Includes ---------------------------------------------------------------- */
#include <motion_detection_inferencing.h>
#include <Adafruit_MPU6050.h>
#include <Wire.h>

/* MPU6050 Sensor Setup ---------------------------------------------------- */
Adafruit_MPU6050 mpu;
sensors_event_t accel_event, gyro_event, temp_event;

/* Constant defines -------------------------------------------------------- */
#define CONVERT_G_TO_MS2    9.80665f
#define DEG_TO_RAD          0.017453292519943295f
/**
 * Edge Impulse model trained with:
 * - Accelerometer: ±2G range (RAW 16-bit values: ±32768)
 * - Gyroscope: ±250°/s range (RAW 16-bit values: ±32768)
 * - Sample rate: 100 Hz (10ms interval)
 * - Window: 1000ms (100 samples)
 * - Input axes: 6 (ax, ay, az, gx, gy, gz)
 * - Data format: Raw ADC integer values (not converted to physical units)
 */
#define MAX_ACCEPTED_RANGE  2.0f  // Accelerometer ±2G
#define MAX_GYRO_RANGE      250.0f  // Gyroscope ±250°/s

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
static uint32_t run_inference_every_ms = 200;
static TaskHandle_t inference_task_handle = NULL; // ESP32 FreeRTOS task
static float buffer[EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE] = { 0 };
static float inference_buffer[EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE];

/* Helper function to read raw 16-bit values from MPU6050 */
void readRawMPU6050(int16_t* accel_x, int16_t* accel_y, int16_t* accel_z,
                    int16_t* gyro_x, int16_t* gyro_y, int16_t* gyro_z) {
    Wire.beginTransmission(0x68);
    Wire.write(0x3B);  // Starting register for accel data
    Wire.endTransmission(false);
    Wire.requestFrom(0x68, 14, true);  // Request 14 bytes (accel + temp + gyro)
    
    *accel_x = (Wire.read() << 8 | Wire.read());
    *accel_y = (Wire.read() << 8 | Wire.read());
    *accel_z = (Wire.read() << 8 | Wire.read());
    Wire.read(); Wire.read();  // Skip temperature registers
    *gyro_x = (Wire.read() << 8 | Wire.read());
    *gyro_y = (Wire.read() << 8 | Wire.read());
    *gyro_z = (Wire.read() << 8 | Wire.read());
}

/* Forward declaration */
void run_inference_background(void *pvParameters);

/**
* @brief      Arduino setup function
*/
void setup()
{
    // put your setup code here, to run once:
    Serial.begin(115200);
    // comment out the below line to cancel the wait for USB connection (needed for native USB)
    while (!Serial);
    Serial.println("Edge Impulse Inferencing Demo");

    // Initialize I2C bus with explicit pins (ESP32 defaults: SDA=21, SCL=22)
    Wire.begin(21, 22);  // Explicit pin assignment for ESP32
    delay(100);  // Allow I2C bus to stabilize
    
    // Scan I2C bus for devices
    ei_printf("Scanning I2C bus...\r\n");
    byte i2c_devices = 0;
    for (byte addr = 1; addr < 127; addr++) {
        Wire.beginTransmission(addr);
        byte error = Wire.endTransmission();
        if (error == 0) {
            ei_printf("I2C device found at address 0x%02X\r\n", addr);
            i2c_devices++;
        }
    }
    if (i2c_devices == 0) {
        ei_printf("ERROR: No I2C devices found! Check wiring:\r\n");
        ei_printf("  ESP32 GPIO21 (SDA) -> MPU6050 SDA\r\n");
        ei_printf("  ESP32 GPIO22 (SCL) -> MPU6050 SCL\r\n");
        ei_printf("  ESP32 3.3V -> MPU6050 VCC\r\n");
        ei_printf("  ESP32 GND -> MPU6050 GND\r\n");
        while (1) { delay(1000); }
    }
    
    // Initialize MPU6050 sensor
    if (!mpu.begin(0x68, &Wire)) {  // Explicit I2C address
        ei_printf("Failed to initialize MPU6050! Check wiring.\r\n");
        while (1) {
            delay(10);
        }
    }
    ei_printf("MPU6050 initialized successfully\r\n");
    
    // Configure accelerometer range to ±2G (matches Edge Impulse model training)
    mpu.setAccelerometerRange(MPU6050_RANGE_2_G);
    
    // Set bandwidth to 21 Hz for noise filtering
    mpu.setFilterBandwidth(MPU6050_BAND_21_HZ);
    
    ei_printf("Accelerometer range: ±2G\r\n");
    
    // Configure gyroscope range to ±250°/s (matches Edge Impulse model training)
    mpu.setGyroRange(MPU6050_RANGE_250_DEG);
    ei_printf("Gyroscope range: ±250°/s\r\n");
    
    // Test sensor readings
    sensors_event_t test_accel, test_gyro, test_temp;
    mpu.getEvent(&test_accel, &test_gyro, &test_temp);
    ei_printf("Initial sensor test:\r\n");
    ei_printf("  Accel: X=%.2f Y=%.2f Z=%.2f m/s²\r\n", 
        test_accel.acceleration.x, 
        test_accel.acceleration.y, 
        test_accel.acceleration.z);
    ei_printf("  Gyro: X=%.2f Y=%.2f Z=%.2f rad/s\r\n", 
        test_gyro.gyro.x, 
        test_gyro.gyro.y, 
        test_gyro.gyro.z);
    
    // Sanity check: Z-axis should be ~9.8 m/s² (gravity) if board is flat
    if (fabs(test_accel.acceleration.z) < 5.0f) {
        ei_printf("WARNING: Z-axis gravity reading low (%.2f m/s²). Expected ~9.8 m/s²\r\n", 
            test_accel.acceleration.z);
        ei_printf("  Check sensor orientation or calibration.\r\n");
    }

    if (EI_CLASSIFIER_RAW_SAMPLES_PER_FRAME != 6) {
        ei_printf("ERR: EI_CLASSIFIER_RAW_SAMPLES_PER_FRAME should be equal to 6 (3 accel + 3 gyro axes)\n");
        return;
    }

    // Create FreeRTOS task for inference (ESP32)
    xTaskCreatePinnedToCore(
        run_inference_background,
        "inference_task",
        8192,  // Stack size
        NULL,
        1,     // Priority
        &inference_task_handle,
        0      // Core 0
    );
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
 * @brief      Run inferencing in the background (FreeRTOS task for ESP32).
 */
void run_inference_background(void *pvParameters)
{
    // wait until we have a full buffer
    delay((EI_CLASSIFIER_INTERVAL_MS * EI_CLASSIFIER_RAW_SAMPLE_COUNT) + 100);

    // This is a structure that smoothens the output result
    // With the default settings 70% of readings should be the same before classifying.
    ei_classifier_smooth_t smooth;
    ei_classifier_smooth_init(&smooth, 10 /* no. of readings */, 7 /* min. readings the same */, 0.8 /* min. confidence */, 0.3 /* max anomaly */);

    while (1) {
        // copy the buffer
        memcpy(inference_buffer, buffer, EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE * sizeof(float));

        // Turn the raw buffer in a signal which we can the classify
        signal_t signal;
        int err = numpy::signal_from_buffer(inference_buffer, EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE, &signal);
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
        ei_printf(": ");

        // ei_classifier_smooth_update yields the predicted label
        const char *prediction = ei_classifier_smooth_update(&smooth, &result);
        ei_printf("%s ", prediction);
        
        // print confidence values for all classes
        ei_printf("[ ");
        for (size_t ix = 0; ix < EI_CLASSIFIER_LABEL_COUNT; ix++) {
            ei_printf("%s: %.2f%%", result.classification[ix].label, result.classification[ix].value * 100.0f);
            if (ix != EI_CLASSIFIER_LABEL_COUNT - 1) {
                ei_printf(", ");
            }
        }
        ei_printf(" ]");
        
        // print the cumulative smoothing results
        ei_printf(" smooth:[ ");
        for (size_t ix = 0; ix < smooth.count_size; ix++) {
            ei_printf("%u", smooth.count[ix]);
            if (ix != smooth.count_size + 1) {
                ei_printf(", ");
            }
            else {
              ei_printf(" ");
            }
        }
        ei_printf("]\n");

        delay(run_inference_every_ms);
    }

    ei_classifier_smooth_free(&smooth);
    vTaskDelete(NULL); // Clean up FreeRTOS task
}

/**
* @brief      Get data and run inferencing
*
* @param[in]  debug  Get debug info if true
*/
void loop()
{
    while (1) {
        // Determine the next tick (and then sleep later)
        uint64_t next_tick = micros() + (EI_CLASSIFIER_INTERVAL_MS * 1000);

        // roll the buffer -6 points so we can overwrite the last one (6 axes: ax,ay,az,gx,gy,gz)
        numpy::roll(buffer, EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE, -6);

        // Read RAW 16-bit values from MPU6050 (matches Edge Impulse training data format)
        int16_t raw_ax, raw_ay, raw_az, raw_gx, raw_gy, raw_gz;
        readRawMPU6050(&raw_ax, &raw_ay, &raw_az, &raw_gx, &raw_gy, &raw_gz);
        
        // DEBUG: Print raw sensor values every 1 second (100 samples * 10ms = 1000ms)
        static int debug_counter = 0;
        if (debug_counter++ % 100 == 0) {
            ei_printf("RAW SENSOR: ax=%d ay=%d az=%d | gx=%d gy=%d gz=%d\r\n",
                raw_ax, raw_ay, raw_az, raw_gx, raw_gy, raw_gz);
        }
        
        // Store raw accelerometer data (16-bit integers as floats)
        buffer[EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE - 6] = (float)raw_ax;
        buffer[EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE - 5] = (float)raw_ay;
        buffer[EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE - 4] = (float)raw_az;
        
        // Store raw gyroscope data (16-bit integers as floats)
        buffer[EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE - 3] = (float)raw_gx;
        buffer[EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE - 2] = (float)raw_gy;
        buffer[EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE - 1] = (float)raw_gz;

        // No clamping needed - raw values are already constrained to ±32768 (16-bit signed)

        // and wait for next tick
        uint64_t time_to_wait = next_tick - micros();
        delay((int)floor((float)time_to_wait / 1000.0f));
        delayMicroseconds(time_to_wait % 1000);
    }
}
