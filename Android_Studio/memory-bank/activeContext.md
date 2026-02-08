# Active Context

## Current Focus
All milestones (1–5) COMPLETE. UI Polished for Samsung One UI.
- Layout Refactor: `TrainingScreen` now strictly follows One UI "Viewing Area" (Top 35%) vs "Interaction Area" (Bottom 65%) split.
- Theme: Samsung Blue palette (#1428A0/#A6ADDB) enforced across Compose and XML.
- Hybrid Firebase integration: Room local cache + Firestore cloud sync for Galaxy Watch.
- FirebaseSyncRepository: anonymous auth, session-granular push to Firestore.
- SyncWorker: WorkManager periodic (15min) + one-shot sync with network constraints.
- SyncManager: coordinates scheduling, exposes sync state to UI.
- Cloud Sync section added to SettingsScreen with toggle, status display, and Sync Now button.
- ThemeMode enum (System/Light/Dark) persisted via DataStore.
- BleDeviceProfile replaces all hardcoded UUIDs in BluetoothManager.
- Full i18n coverage (EN/ZH-CN/ZH-TW) across all screens including cloud sync strings.

## Open Questions / Blockers
- User must replace `app/google-services.json` placeholder with real Firebase project config.
- Locate exact BLE MCU button event parsing in BluetoothManager to emit highlight trigger.
- Galaxy Watch companion app (Wear OS) not yet built — Firestore data ready for it.

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
