# XIAO BLE IMU Sender

This sketch runs on the **transmitting** `Seeed Studio XIAO nRF52840 Sense` board.

## What it does

- Reads IMU data from the onboard `LSM6DS3`
- Packs one full IMU sample into a single BLE notification
- Advertises as `SmartRacket`
- Streams data wirelessly to the matching receiver sketch

## Typical use

Use this sketch together with `../XIAO_BLE_IMU_Receiver/XIAO_BLE_IMU_Receiver.ino` when you want to collect motion data without a USB cable attached to the moving sensor board.

A common workflow for **Edge Impulse Data Forwarder** is:

1. Flash this sender sketch to the moving XIAO board
2. Flash the receiver sketch to a second XIAO board connected to your computer by USB
3. Let the sender transmit IMU data over BLE to the receiver
4. Run Edge Impulse Data Forwarder against the **receiver board's serial port**

## Important note

The sender board is **not** the board that Edge Impulse Data Forwarder should read from.

The sender only sends IMU samples over a BLE connection. The receiver board converts that BLE stream into USB serial output for your computer.

## Board and libraries

- Board: `Seeeduino:nrf52:xiaonRF52840Sense`
- Required libraries:
  - `bluefruit`
  - `LSM6DS3`
  - `Wire`

## Upload

1. Open `XIAO_BLE_IMU_Sender.ino` in Arduino IDE
2. Select **Seeed XIAO nRF52840 Sense**
3. Upload to the transmitting board
4. Power the board and wait for the receiver to connect

For packet details and throughput notes, see `../README.md`.
