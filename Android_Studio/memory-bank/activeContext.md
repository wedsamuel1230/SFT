# Active Context

## Current Focus
All milestones (1–5) COMPLETE. Firebase hybrid sync implemented.
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
- Firebase deps: firebase-bom 33.7.0, firestore-ktx, auth-ktx, google-services plugin 4.4.2.
- Hilt-work 1.2.0 + hilt-compiler for @HiltWorker support.
- SmartRacketApplication implements Configuration.Provider for custom WorkManager init.
- Default WorkManager initializer disabled in AndroidManifest.xml.
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
