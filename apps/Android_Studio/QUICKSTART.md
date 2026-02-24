# SmartRacket App — Quick Start

Short, actionable steps to get the SmartRacket app connected and running.

Prerequisites

- Android device with Bluetooth (Android 8.0+ recommended)
- SmartRacket hardware running the `XIAOEI` firmware on the Seeed XIAO nRF52840 Sense
- USB cable and computer only needed if you must flash firmware

1. Install the app

- Option A — From Android Studio (developer):
  - Open `apps/Android_Studio/` in Android Studio and run the `app` module on your device.

- Option B — Sideload APK (non-developer):
  - If you have a prepared APK, copy it to your phone and install, or use:

```bash
adb install -r path/to/app-debug.apk
```

2. Enable phone settings

- Enable Bluetooth.
- Grant Location permission when prompted (required on some Android versions for Bluetooth scanning).

3. Power the SmartRacket hardware

- Ensure the Seeed XIAO nRF52840 Sense is powered and running the uploaded firmware (`XIAOEI.ino`).
- Verify firmware by checking that the device is advertising over Bluetooth (LED or Serial Monitor if available).

4. Open the app and connect

- Launch the SmartRacket app on your phone.
- Allow any runtime permissions the app requests (Bluetooth, Location, Storage if present).
- Navigate to the "Connect" screen.
- Tap "Scan" — wait a few seconds for devices to appear.
- Select the device named for your SmartRacket (name often contains "SmartRacket" or the board name).
- Accept any pairing prompts on the phone and on the hardware (if shown).

5. Confirm data streaming

- Once connected the app UI should show live sensor values (accel/gyro) or a status indicator.
- If nothing appears, try restarting the app and re-scanning.

6. End session

- Use the app’s disconnect/stop button to end a session cleanly.
- Power down the hardware if finished.

Troubleshooting (quick tips)

- No devices found when scanning:
  - Confirm firmware is running and advertising.
  - Toggle Bluetooth off/on on the phone.
  - Grant Location permission and try again.

- App builds but crashes:
  - Open Logcat in Android Studio and search the stack trace for the cause.

- Firmware connection unstable:
  - Keep phone and hardware within a few meters.
  - Check battery/power and antenna/placement.

Need more help?

- See `apps/Android_Studio/README.md` for full setup, build and debug instructions.
- See `arduino code/XIAOEI/README.md` for firmware instructions.
- Open an issue in the repo with logs and device details if you’re stuck.
