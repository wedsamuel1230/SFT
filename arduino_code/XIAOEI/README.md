# XIAOEI Arduino Firmware

This folder contains the firmware for the SmartRacket hardware, written for Arduino-compatible microcontrollers.

## File Structure

- `XIAOEI.ino` — Main firmware source code for the SmartRacket device.

## How to Use

### Requirements

- Arduino IDE (latest version recommended)
- Supported microcontroller: **Seeed Studio XIAO nRF52840 Sense** (recommended)
- Required libraries:
    - **ei-sft-arduino-1.0.2.zip** (provided in this repo)
        - To install: In Arduino IDE, go to **Sketch > Include Library > Add .ZIP Library...** and select `ei-sft-arduino-1.0.2 (1).zip` from the `arduino_code` folder.
    - Other dependencies: `LSM6DS3`, `Wire`, `bluefruit`, `nrf_power`, `nrf_gpio` (install via Library Manager if needed)

### Uploading the Firmware

1. Open `XIAOEI.ino` in the Arduino IDE.
2. Connect your SmartRacket hardware to your computer via USB.
3. In the Arduino IDE:
    - Select the correct board type (**Seeed nRF52840 Boards > Seeed XIAO nRF52840 Sense**) under **Tools > Board**.
    - Select the correct port under **Tools > Port**.
    - Install any missing libraries if prompted.
4. Click the **Upload** button (right arrow icon) to flash the firmware to your device.
5. Open the Serial Monitor (**Tools > Serial Monitor**) to view debug output and confirm successful operation.

### Features

- Reads sensor data from the SmartRacket hardware.
- Communicates with the Android app via Bluetooth or serial (depending on hardware setup).
- Can be extended for additional features or sensors.

### Troubleshooting

- Ensure the correct board and port are selected.
- Install all required libraries.
- Check wiring and power supply to the hardware.
- Use Serial Monitor for debugging output.

---

For more information, see the main project `README.md` in the root directory.
