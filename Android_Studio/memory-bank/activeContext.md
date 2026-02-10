# Active Context

## Current Focus
All milestones (1–5) COMPLETE. MCU migration applied. UI Polished for Samsung One UI.
- **MCU Migration:** Stroke classification moved to MCU (Edge Impulse, 3 classes: forehand/backhand/drive); `McuModelOutput` model computes `score` from `conf`; `BluetoothRepository` forwards MCU model outputs via BLE JSON (`ts/stroke/conf/peak`); `TrainingRepository.recordStrokeFromMcu()` records strokes; `StrokeClassifier` replaced with deprecated stub.
- **BLE Protocol:** test.ino UUIDs aligned with `BleDeviceProfile.DEFAULT`; JSON payload sends `ts/stroke/conf/peak` (no `score` — calculated on Android).
- **StrokeType Enum:** Reduced to `FOREHAND`, `BACKHAND`, `DRIVE`, `UNKNOWN` for prototype.
- **Health & Wellness:** New `HealthWellnessCard` on HomeScreen showing heart rate and blood pressure; BPM removed from `TodaySummaryCard`.
- **Health Alerts:** Real-time alerts during training when HR≥180 or BP≥140/90; pause-for-rest functionality in TrainingScreen dialog.
- **Samsung Health SDK:** Connection management in Settings, state tracked in `HealthRepository`, exposed via `SettingsViewModel` and `HomeViewModel`.
- Layout Refactor: `TrainingScreen` now strictly follows One UI "Viewing Area" (Top 35%) vs "Interaction Area" (Bottom 65%) split.
- Theme: Samsung Blue palette (#1428A0/#A6ADDB) enforced across Compose and XML.
- Hybrid Firebase integration: Room local cache + Firestore cloud sync for Galaxy Watch.
- Full i18n coverage (EN/ZH-CN/ZH-TW) across all screens including health & Samsung Health strings.

## Open Questions / Blockers
- User must replace `app/google-services.json` placeholder with real Firebase project config.
- Samsung Health SDK actual dependency not yet added to `build.gradle.kts` (placeholder integration).
- Galaxy Watch companion app (Wear OS) not yet built — Firestore data ready for it.
- Git not available in terminal — push changes to GitHub via Android Studio VCS or install Git.

## Notes
- `TrainingScreen.kt`: `ActiveTrainingContent` rewritten to use weighted Column split. Top=Stats/Score, Bottom=List/Controls. Added missing `recentStrokes` list.
- One UI Layout Rules: Top 30-40% for viewing, Bottom 60-70% for interaction.
- Firestore schema: users/{uid}/sessions/{sessionId} + /strokes + /highlights subcollections.
- Sync is session-granular: completed sessions with isSynced=false get pushed with all strokes/highlights.
- Strokes batched in chunks of 400 (Firestore batch limit is 500).
- Firebase unavailability handled gracefully (placeholder google-services.json won't crash).
- WearableListenerService handles /smartracket/sync path → triggers immediate sync.
- Theme.kt: Dynamic color DISABLED to preserve Samsung Blue identity.
- Color.kt: Centralized file with SmartRacketColors object + scoreColor() helper.
- BLE: Added BleOperationQueue (FIFO), device cache, MTU/priority request, status handling.

---
*Last Updated: 2026-02-08*
