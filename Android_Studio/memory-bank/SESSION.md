# Session Log

## 2026-02-08 — v0.1.0
**Objective:** [One sentence describing what this session accomplishes]

**Actions:**
- [Bullet point 1]
- [Bullet point 2]

**Status:** ⚠️ In Progress

---

## 2026-02-08 — v0.1.1
**Objective:** Draft implementation plan for localization, highlights, and layout fixes.

**Actions:**
- Created detailed plan in memory-bank/plans for centralized strings, CJK font, BLE-only highlights, and UI layout adjustments.
- Updated active context and master plan milestones.

**Status:** ✅ Complete

---

## 2026-02-08 — v0.2.0
**Objective:** Samsung One UI color & design alignment across all Compose UI.

**Actions:**
- Created centralized Color.kt with SmartRacketColors object (Samsung Blue palette, semantic status colors, chart colors, scoreColor() helper).
- Refactored Theme.kt: replaced green/orange scheme with Samsung Blue (#1428A0 light / #A6ADDB dark), disabled dynamic color, added One UI squircle Shapes (12/16/20/28 dp), surface-colored status bar.
- Updated colors.xml: removed legacy purple/teal, replaced green/orange with Samsung Blue tokens.
- Updated themes.xml (light + night): surface-colored bars, added windowLightStatusBar attribute.
- Eliminated ALL hardcoded Color(0x...) and parseColor() calls from 5 screen files (HomeScreen, TrainingScreen, AnalyticsScreen, HighlightsScreen, SettingsScreen) — replaced with SmartRacketColors.* and scoreColor() references.
- Replaced hardcoded RoundedCornerShape with MaterialTheme.shapes.medium / extraLarge.

**Status:** ✅ Complete

---

## 2026-02-08 — v0.3.0
**Objective:** Add dark/light mode toggle in Settings and multi-device BLE support.

**Actions:**
- Created ThemeMode enum (System/Light/Dark) with DataStore persistence.
- Added themeMode flow + setter to SettingsViewModel; wired into SmartRacketApp.
- Created BleDeviceProfile data class; refactored BluetoothManager to use dynamic profile instead of hardcoded UUIDs.
- Added Appearance section with ThemeModeSelectorItem to SettingsScreen.
- Added Devices section for paired paddle management (set primary, remove).
- Added 17 new i18n strings in EN, ZH-CN, ZH-TW.

**Status:** ✅ Complete

---

## 2026-02-08 — v0.4.1
**Objective:** Fix compilation error in TrainingScreen.kt caused by extra closing braces after refactoring ActiveTrainingContent.

**Actions:**
- Identified extra closing braces at lines 703-705 in TrainingScreen.kt.
- Removed the orphaned `}        }    }` that were left after the ActiveTrainingContent refactor.
- Verified build passes successfully.

**Status:** ✅ Complete

---

## 2026-02-08 — v0.4.0
**Objective:** Implement BLE reliability fixes (operation queue, status handling, MTU/priority).

**Actions:**
- Added BleOperationQueue with JUnit tests to serialize BLE GATT operations.
- Integrated queue into BluetoothManager for reads/writes/descriptors/MTU/discovery.
- Cached discovered BluetoothDevice instances to improve reconnect reliability.
- Added GATT status handling, connection priority, MTU request, and main-thread service discovery sequencing.

**Status:** ✅ Complete

---

## 2026-02-08 — v0.5.0
**Objective:** Plan Firebase migration for hybrid sync with Galaxy Watch support.

**Actions:**
- Reviewed Room DB and watch listener service to map current persistence and wearable flows.
- Drafted phased migration plan (Room cache + Firebase sync) with risks and dependencies.

**Status:** ✅ Complete

---

## 2026-02-08 — v0.6.0
**Objective:** Implement master-plan milestones 1–4 (i18n, CJK font, highlights restriction, label layout).

**Actions:**
- Added `lastConnected` and `viewDetails` string keys to AppStrings with EN/ZH-CN/ZH-TW translations.
- Fixed hardcoded "View details" in HomeScreen (now uses strings.viewDetails).
- Fixed hardcoded "Last connected" in SettingsScreen (now uses strings.lastConnected).
- Removed unused `autoSaveThreshold` state from SettingsScreen (auto-save already disabled in VM).
- Added maxLines=1 + TextOverflow.Ellipsis to AnalyticsScreen Tab labels for CJK safety.
- Updated Type.kt documentation to accurately describe CJK fallback behavior.
- All 4 milestones verified: BUILD SUCCESSFUL, all tests pass.

**Status:** ✅ Complete

---

## 2026-02-08 — v0.7.0
**Objective:** Implement Milestone 5 — Hybrid Firebase sync (Room cache + Firestore cloud) for Galaxy Watch.

**Actions:**
- Added Firebase deps: firebase-bom 33.7.0, firestore-ktx, auth-ktx, google-services 4.4.2 plugin.
- Added hilt-work 1.2.0 + hilt-compiler for @HiltWorker support.
- Created placeholder `app/google-services.json` (user must replace with real Firebase config).
- Added sync DAO queries to TrainingSessionDao: getUnsyncedSessions, markAsSynced, markAsUnsynced, getUnsyncedCount, getSyncedCount.
- Created FirebaseSyncRepository: anonymous auth, session-granular Firestore push, SyncState sealed class, SyncStats data class.
- Created SyncWorker (@HiltWorker): WorkManager CoroutineWorker with retry logic.
- Created SyncManager: periodic (15min) + one-shot sync scheduling, sync state exposure.
- Updated SmartRacketApplication to implement Configuration.Provider for HiltWorkerFactory.
- Disabled default WorkManager initializer in AndroidManifest.xml.
- Updated AppModule with FirebaseSyncRepository and SyncManager providers.
- Wired WearableListenerService to inject SyncManager and handle /smartracket/sync path.
- Added 13 new i18n string keys (cloudSync, syncNow, syncStatus, etc.) in EN/ZH-CN/ZH-TW.
- Added Cloud Sync section to SettingsScreen: Firebase config notice, sync toggle, sync status, Sync Now button.
- Updated SettingsViewModel with cloudSyncEnabled preference, syncNow(), setCloudSyncEnabled().
- Build: assembleDebug ✅, test ✅ (67 tasks, 0 failures).

**Status:** ✅ Complete

---

## 2026-02-08 — v0.7.0
**Objective:** Generate Technical Whitepaper and Investor Deck for SmartRacket due diligence and roadshow.

**Actions:**
- Deep dive analysis of entire codebase: verified tech stack (Kotlin/Compose/Room/Firebase/TFLite/BLE), data models (Stroke, TrainingSession, HighlightClip, McuModelOutput, BleDeviceProfile), and architecture (3-tier Edge-Mobile-Cloud).
- Created docs/technical_whitepaper.md: 6-chapter whitepaper covering System Summary, Functional Specs (6 features with Trigger/Logic/EdgeCases/DataStructure), System Architecture (3-layer diagram, BLE GATT profile, ML deployment strategy, Android app architecture, DB schema), Data & Privacy (GDPR, encryption flow, anonymous auth), and Technical Trade-offs (latency vs accuracy, IMU vs CV, Room+Firebase vs pure cloud).
- Created docs/investor_deck.md: 10-slide pitch deck with Title/Key Bullets/Speaker Notes per slide. Covers: Problem, Solution, Market, Tech Moat, Product Demo, Business Model, Competitive Advantage, Roadmap, Team, Fundraising Ask.
- Both documents are terminologically consistent and reference the same hardware (nRF52840 Sense), ML architecture (Two-Stage Pipeline), and data structures.

**Status:** ✅ Complete

---

## 2026-02-10 — v0.8.0
**Objective:** Verify and restore codebase after GitHub data loss — MCU migration scope.

**Actions:**
- Full codebase scan: 45 Kotlin source files across 11 packages confirmed present.
- Verified McuModelOutput, BluetoothRepository (MCU BLE flows), TrainingRepository (recordStrokeFromMcu), AppModule (no StrokeClassifier provider) all intact.
- StrokeClassifier.kt exists as dead code (382 lines) — kept as fallback.
- All UI screens, viewmodels, i18n (Strings.kt), theming (Color.kt/Theme.kt), Firebase sync, ic_table_tennis drawable confirmed present.
- Build: `assembleDebug` → BUILD SUCCESSFUL (42 tasks, 0 failures).
- No restoration needed — local workspace already contains all MCU migration changes.
- Updated activeContext.md with MCU migration status and Git blocker.

**Status:** ✅ Complete

---

## 2026-02-08 — v0.7.1
**Objective:** Correct ML architecture descriptions and fix PDF generation for technical whitepaper.

**Actions:**
- Corrected ML deployment strategy across both docs: changed from hybrid two-stage pipeline to MCU-only Edge Impulse inference with 3 classes (idle/forehand/backhand). Removed all references to Phone-side TFLite inference in current architecture, kept as future roadmap.
- Updated technical_whitepaper.md: Section 1 (system summary), Section 2.1 (backend logic), Section 3.1 (architecture diagram), Section 3.3 (ML strategy), Section 5.1 (trade-offs table and analysis), Section 6.2 (versions table with Edge Impulse SDK).
- Updated investor_deck.md: Slide 4 key bullets and speaker notes to reflect MCU-only Edge Impulse architecture.
- Fixed PDF generation: replaced Chinese anchor names with ASCII IDs (#system-summary, #functional-specs, etc.) to resolve LaTeX hyper reference warnings. Used XeLaTeX with ctex document class and Microsoft JhengHei font for proper Traditional Chinese rendering.

**Status:** ✅ Complete

---

## 2026-02-10 — v0.9.0
**Objective:** BLE protocol alignment, prototype stroke types, health & wellness features, Samsung Health integration.

**Actions:**
- Aligned test.ino BLE UUIDs with BleDeviceProfile.DEFAULT (service/data/ctrl UUIDs matching).
- Reduced StrokeType enum from 14 to 3+UNKNOWN (FOREHAND, BACKHAND, DRIVE) for prototype.
- Updated test.ino: stroke types, JSON format (ts/stroke/conf/peak), removed score field.
- McuModelOutput: removed `score` constructor param, added computed `score` from `conf` (conf*10, coerced 1-10).
- BluetoothManager: parseModelOutputJson updated to not parse `score` from JSON.
- HealthRepository: added blood pressure tracking, health alerts (HR≥180, BP≥140/90), Samsung Health connect/disconnect methods.
- Added 30+ i18n strings for health & wellness across EN/ZH-CN/ZH-TW.
- HomeViewModel: exposed blood pressure and Samsung Health connection state.
- HomeScreen: removed BPM from TodaySummaryCard, created HealthWellnessCard (heart rate, blood pressure, Samsung Health status).
- TrainingViewModel: added health alert observation, dismissHealthAlert(), pauseForRest().
- TrainingScreen: added health alert dialog with Rest Now / Continue buttons.
- SettingsViewModel: added Samsung Health connect/disconnect methods.
- SettingsScreen: added Samsung Health connection item in Health & Fitness section.
- StrokeClassifier: replaced 382-line dead code with deprecated stub (classification runs on MCU).
- Fixed missing ZH-CN function declaration in Strings.kt.
- Build verified: assembleDebug BUILD SUCCESSFUL.

**Status:** ✅ Complete

---
