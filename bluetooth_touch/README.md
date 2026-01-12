# Bluetooth Touch - XIAO ESP32-S3 MPU6050

Arduino/PlatformIO project for Seeed Studio XIAO ESP32-S3 with MPU6050 IMU and capacitive touch input, streaming data via Bluetooth Serial.

## Features

- **MPU6050 IMU**: 6-axis accelerometer + gyroscope data
- **Bluetooth Serial**: Classic SPP for easy terminal connection
- **Touch1 Input**: Capacitive touch sensing on GPIO1
- **Real-time streaming**: 10Hz data output (configurable)

## Hardware Requirements

| Component | Description |
|-----------|-------------|
| Seeed XIAO ESP32-S3 | Main microcontroller |
| MPU6050 | 6-axis IMU (I2C) |
| Jumper wires | For connections |

## Wiring Diagram

```
XIAO ESP32-S3          MPU6050
--------------         -------
3.3V           ───────  VCC
GND            ───────  GND
GPIO5 (D4)     ───────  SDA
GPIO6 (D5)     ───────  SCL

Touch1: GPIO1 (D0) - Built-in capacitive touch pad
```

### Pin Reference (XIAO ESP32-S3)

| Label | GPIO | Function |
|-------|------|----------|
| D0 | GPIO1 | Touch1 (capacitive) |
| D4 | GPIO5 | I2C SDA |
| D5 | GPIO6 | I2C SCL |

## Software Setup

### Prerequisites

1. Install [PlatformIO](https://platformio.org/install)
2. Install VS Code with PlatformIO extension (recommended)

### Build & Upload

```bash
# Clone/navigate to project directory
cd bluetooth_touch

# Build the project
pio run

# Upload to board (connect via USB-C)
pio run -t upload

# Monitor serial output (USB)
pio device monitor
```

## Usage

### 1. Power On
Connect the XIAO ESP32-S3 via USB-C. The onboard LED will indicate startup.

### 2. Connect via Bluetooth
1. Open Bluetooth settings on your phone/PC
2. Search for device: **XIAO_MPU6050_Touch**
3. Pair (no PIN required for SPP)
4. Open a Bluetooth Serial Terminal app:
   - Android: "Serial Bluetooth Terminal"
   - iOS: "Bluetooth Terminal" or "nRF Toolbox"
   - PC: Use a COM port terminal (PuTTY, screen)

### 3. Data Format

The device streams data at 10Hz:

```
MPU,ax,ay,az,gx,gy,gz,temp
TOUCH1,value,state
```

**Example output:**
```
MPU,0.12,-0.05,9.81,0.02,-0.01,0.00,25.3
TOUCH1,45000,RELEASED
MPU,0.15,-0.03,9.79,0.01,-0.02,0.01,25.4
TOUCH1,12000,TOUCHED
```

| Field | Description | Unit |
|-------|-------------|------|
| ax, ay, az | Acceleration X/Y/Z | m/s² |
| gx, gy, gz | Gyroscope X/Y/Z | rad/s |
| temp | Temperature | °C |
| value | Touch raw value | - |
| state | TOUCHED/RELEASED | - |

## Configuration

Edit `src/main.cpp` to customize:

```cpp
#define TOUCH_THRESHOLD     40000   // Touch sensitivity (lower = more sensitive)
#define BT_DEVICE_NAME      "XIAO_MPU6050_Touch"
#define SEND_INTERVAL_MS    100     // Data rate (100ms = 10Hz)
```

## Troubleshooting

### MPU6050 not detected
- Check wiring (SDA to GPIO5, SCL to GPIO6)
- Verify 3.3V power supply
- Try I2C scanner to confirm address (0x68)

### Bluetooth not connecting
- Ensure Classic Bluetooth is enabled on your device
- Some devices require unpairing and re-pairing
- Check that no other device is connected

### Touch not responsive
- Adjust `TOUCH_THRESHOLD` value
- ESP32-S3 touch values are typically 10000-70000
- Lower value = touch detected

### Watchdog reset
- Ensure `delay(1)` in loop for yield
- Check for blocking operations

## License

MIT License - Feel free to use and modify.

## References

- [XIAO ESP32-S3 Wiki](https://wiki.seeedstudio.com/xiao_esp32s3_getting_started/)
- [Adafruit MPU6050 Library](https://github.com/adafruit/Adafruit_MPU6050)
- [ESP32 Touch Sensor](https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/api-reference/peripherals/touch_pad.html)
