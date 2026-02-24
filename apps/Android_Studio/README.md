# SmartRacket Android App — Setup & User Guide

This document is a focused, step-by-step guide to set up, build, run, and debug the SmartRacket Android application contained in this repository. It assumes you have the project checked out and that firmware is handled separately.

## Overview

The Android app connects to SmartRacket hardware over Bluetooth to receive real-time sensor data and display analytics. This guide covers environment setup, importing the project into Android Studio, building and running on a device, CLI commands, and troubleshooting.

---

## Prerequisites

- macOS (or Windows/Linux) with sufficient disk space
- Android Studio (Arctic Fox or later recommended)
- JDK compatible with Android Studio (bundled with Android Studio)
- Physical Android device with USB debugging and Bluetooth
- USB cable (data-capable)

Optional tools:

- Android SDK Platform Tools (adb)
- Git (to clone repository)

---

## 1 — Clone the repository

If you haven't already, clone the repo locally:

```bash
git clone <your-repo-url>
cd SFT/apps/Android_Studio/
```

---

## 2 — Open the project in Android Studio

1. Launch Android Studio.
2. Choose "Open" and select the `apps/Android_Studio/` folder.
3. Allow Gradle to sync. If prompted to update any Gradle plugin or SDK components, follow the prompts.

Notes:

- If Gradle sync fails, open the Gradle Console to inspect errors. Common fixes: install missing SDK components, accept SDK licenses, or increase the Gradle heap size in `gradle.properties`.

---

## 3 — Configure Google Services (if applicable)

If the app requires Google services (Firebase, Google Sign-In), ensure you have the `google-services.json` file in `app/` (it may already be present). If not, add it to `app/` and sync Gradle.

---

## 4 — Set up your Android device

1. Enable Developer Options: Settings > About phone > Tap Build number 7 times.
2. Enable USB Debugging: Developer Options > USB debugging.
3. Enable necessary runtime permissions (Bluetooth, Location) when the app prompts you.

Optional: Use an Android emulator with Bluetooth emulation is limited — a physical device is recommended.

---

## 5 — Build and Run from Android Studio

1. Select the `app` module configuration.
2. Choose your connected device in the run target drop-down.
3. Click Run (green triangle). Android Studio will build the app and install it on your device.

On first run, grant runtime permissions for Bluetooth and Location.

---

## 6 — Command-line build & install

From the `apps/Android_Studio/` folder you can use Gradle wrapper:

```bash
./gradlew assembleDebug      # build debug APK
./gradlew installDebug       # build and install to connected device
./gradlew assembleRelease    # build release APK (requires signingConfig)
```

You can also install a built APK via adb:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 7 — App Configuration & Build Variants

- Check `app/build.gradle.kts` for build flavors and signing configs.
- If building a release, configure `signingConfigs` and add keystore info to `gradle.properties` or use Android Studio's signing dialog.

---

## 8 — Connecting the App to SmartRacket Hardware

1. Power on the SmartRacket device and ensure firmware is running.
2. Open the SmartRacket app.
3. In the app, go to the "Connect" screen and scan for nearby Bluetooth devices.
4. Select the device that corresponds to your SmartRacket (device name typically includes "SmartRacket" or the board name).
5. Accept any pairing request on both the phone and the hardware if prompted.
6. Once connected, the app should display live sensor data.

Notes:

- Ensure Bluetooth and Location permissions are granted (Android requires Location permission for Bluetooth scanning on some versions).
- Keep the phone and hardware within a few meters during setup.

---

## 9 — Debugging & Logs

- Use Logcat in Android Studio to view runtime logs and errors.
- Add log statements in code to trace connection/state changes.
- Use `adb logcat` from the command line:

```bash
adb logcat -s SmartRacket:* *:S
```

Adjust the tag to match the app's logging tags.

---

## 10 — Common Issues & Fixes

- App fails to install: ensure device has "Install via USB" enabled and you have accepted debug prompts.
- Gradle sync problems: update SDK/NDK or accept licenses with `sdkmanager --licenses`.
- Bluetooth/Scan returns no devices: ensure firmware is advertising, and Location permission is granted.
- App crashes on startup: check Logcat for stack traces; missing resources or invalid manifest entries are common causes.

---

## 11 — Building a Release APK

1. Set up a signing key (keystore) and configure `signingConfigs` in `app/build.gradle.kts`.
1. Run:

```bash
./gradlew assembleRelease
```

1. The signed APK will be in `app/build/outputs/apk/release/` if configured.

---

## 12 — Where to look next

- App source: `apps/Android_Studio/app/src/main/java` and `res` for layouts.
- For firmware pairing details, consult `arduino code/XIAOEI/README.md` and `XIAOEI.ino`.
- Project-wide docs: `doc/` folder.