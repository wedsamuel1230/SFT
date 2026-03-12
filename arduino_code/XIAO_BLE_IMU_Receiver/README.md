# XIAO BLE IMU Receiver

This sketch runs on the **receiving** `Seeed Studio XIAO nRF52840 Sense` board.

## What it does

- Scans for the BLE IMU sender
- Connects to the sender advertising as `SmartRacket` and subscribes to its IMU characteristic
- Receives one IMU sample per BLE notification
- Prints `ax, ay, az, gx, gy, gz` over USB serial at `115200` baud

## Typical use

This is the board you connect to your computer when collecting wireless IMU data for **Edge Impulse Data Forwarder**.

Recommended workflow:

1. Flash `../XIAO_BLE_IMU_Sender/XIAO_BLE_IMU_Sender.ino` to the moving sensor board
2. Flash this receiver sketch to a second XIAO board
3. Connect the receiver board to your computer with USB
4. Open the receiver serial port at `115200` if you want to inspect the stream manually
5. Point Edge Impulse Data Forwarder at the **receiver board**, not the sender

## What you will see on serial

On startup, the receiver prints connection and discovery messages such as:

- `XIAO BLE IMU Receiver`
- `Scanning for SmartRacket sender`
- `Connected to sender`
- `Subscribed to IMU notifications`

After the BLE link is established, it prints IMU rows in this form:

- `ax, ay, az, gx, gy, gz`

## Why this board is useful for Edge Impulse

The sender board can stay attached to the moving object, while this receiver board stays connected to the host computer.

That makes this pair useful as a simple **wireless IMU bridge** for dataset collection.

## Board and libraries

- Board: `Seeeduino:nrf52:xiaonRF52840Sense`
- Required libraries:
  - `bluefruit`

## Upload

1. Open `XIAO_BLE_IMU_Receiver.ino` in Arduino IDE
2. Select **Seeed XIAO nRF52840 Sense**
3. Upload to the USB-connected receiver board
4. Open Serial Monitor at `115200` to confirm the stream

For packet details and throughput notes, see `../README.md`.
