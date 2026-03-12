# Arduino Firmware Guide

This folder contains the Arduino sketches for SmartRacket-related hardware experiments and firmware.

## Available sketches

- `XIAOEI/XIAOEI.ino` — existing SmartRacket firmware with Edge Impulse integration.
- `XIAO_BLE_IMU_Sender/XIAO_BLE_IMU_Sender.ino` — BLE peripheral that reads `LSM6DS3` IMU data, packs one full IMU sample into a single BLE notification, and reports link stats once per second.
- `XIAO_BLE_IMU_Receiver/XIAO_BLE_IMU_Receiver.ino` — BLE central that receives the single-notification samples, prints `ax, ay, az, gx, gy, gz` to Serial, and reports packet-gap stats.

## Board and library requirements

- Board: **Seeed Studio XIAO nRF52840 Sense**
- Board package: `Seeeduino:nrf52:xiaonRF52840Sense`
- Required libraries:
  - `bluefruit`
  - `LSM6DS3` (sender sketch)
  - `Wire` (sender sketch)

## BLE IMU sender/receiver quick start

1. Open `XIAO_BLE_IMU_Sender/XIAO_BLE_IMU_Sender.ino` in Arduino IDE and upload it to the transmitting XIAO board.
2. Open `XIAO_BLE_IMU_Receiver/XIAO_BLE_IMU_Receiver.ino` in Arduino IDE and upload it to the receiving XIAO board.
3. Open Serial Monitor on the receiver board at `115200` baud.
4. Power both boards and wait for the receiver to connect.
5. Move the sender board and watch the receiver print `ax, ay, az, gx, gy, gz` values.
6. Watch the periodic `stats ...` lines on both boards to confirm the achieved sample rate and whether packets are being dropped.

## Packet format

The sender transmits exactly **one BLE notification per IMU sample**. Each payload is a fixed `14` bytes:

- `uint16_t sequence`
- `int16_t ax`
- `int16_t ay`
- `int16_t az`
- `int16_t gx`
- `int16_t gy`
- `int16_t gz`

Each axis is multiplied by `100` before transmission and stored as a signed 16-bit integer in little-endian order. This keeps one complete sample below the default notification payload limit, so no sample is split across multiple BLE packets.

## Throughput tuning notes

- The sender targets `8000 µs` per sample (`125 Hz`) and uses `Bluefruit.configPrphBandwidth(BANDWIDTH_MAX)` plus a preferred connection interval of `7.5–10 ms`.
- The receiver uses `Bluefruit.configCentralBandwidth(BANDWIDTH_MAX)` and requests the same preferred connection interval.
- Both sketches request PHY, data length, and MTU upgrades after connect.
- Sender-side values are clipped if `value * 100` exceeds the signed 16-bit range (`±327.67`). If your gyro range is configured above roughly `±245 dps`, sustained saturation is possible and will show up in the sender stats as `clipped_last_s`.
