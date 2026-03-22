# Session Log

## 2026-03-10 — v0.1.0

**Objective:** Start implementation for sport selection, warm-up, and recurring rest reminders.

**Actions:**

- Loaded implementation workflow skills and request prompt requirements
- Bootstrapped repository memory-bank files and kept `memory-bank/` tracked in git
- Added RED tests for training metadata and training flow API, then implemented the required model, repository, viewmodel, screen, and service changes to make them pass
- Added sport selection, warm-up flow, and recurring rest reminder support across persistence, UI, and foreground service layers
- Repaired local verification blockers by adding `settings.gradle.kts`, configuring `local.properties`, and pinning `buildToolsVersion = "35.0.0"`
- Ran targeted RED/GREEN verification and a final full `testDebugUnitTest` pass

**Status:** ✅ Implementation complete, runtime validation pending

---

## 2026-03-12 — v0.1.1

**Objective:** Refine the sport-selection behavior so it is tied to BLE connection boundaries and add warm-up demo media.

**Actions:**

- Added RED coverage for connection-scoped preparation rules and warm-up demo progression
- Implemented a preparation-flow rule set so a fresh connection requires sport confirmation, while repeated sessions on the same connection reopen at warm-up
- Kept the remembered default sport preselected on reconnect and preserved the chosen sport between sessions on the same live connection
- Added local warm-up illustration assets and displayed the current demo image and caption in the warm-up screen
- Ran targeted RED/GREEN verification and a final full `:app:testDebugUnitTest` pass

**Status:** ✅ Code change complete, runtime/manual validation pending

---

## 2026-03-12 — v0.1.2

**Objective:** Replace the generic sport selector glyph with dedicated sport-specific assets based on the provided chooser images.

**Actions:**

- Added RED coverage requiring a dedicated icon resource for each sport in the selector
- Created seven local sport drawables and mapped them by sport through `SportSelectionIconLibrary`
- Updated the `SportCard` UI to render the sport-specific asset instead of the single `SportsTennis` icon
- Re-ran targeted RED/GREEN verification and a final full `:app:testDebugUnitTest` pass

**Status:** ✅ Asset swap complete, manual visual validation pending

---

## 2026-03-12 — v0.1.3

**Objective:** Tighten the sport selector layout now that the new assets are in place.

**Actions:**

- Added RED coverage for a shared selector layout spec
- Created `SportSelectionLayoutSpec` to centralize icon size, card padding, content spacing, grid spacing, and top spacing
- Updated the sport selection grid and card UI to use the tighter shared layout spec
- Re-ran targeted RED/GREEN verification and a final full `:app:testDebugUnitTest` pass

**Status:** ✅ Layout tightening complete, exact PNG swap still blocked on missing resource files

---

## 2026-03-12 — v0.1.4

**Objective:** Make the warm-up section scrollable and remove the guiding image block.

**Actions:**

- Added RED coverage for the warm-up layout spec
- Added `WarmUpLayoutSpec` to define the new behavior explicitly
- Removed the warm-up guiding image block and made the upper warm-up content area vertically scrollable while keeping the action buttons fixed
- Re-ran targeted RED/GREEN verification and a final full `:app:testDebugUnitTest` pass

**Status:** ✅ Warm-up layout refinement complete, manual screen-size validation pending

---

## 2026-03-12 — v0.1.5

**Objective:** Replace the recreated chooser icons with the exact provided PNGs, improve the sport chooser presentation, and fix the warm-up stall on the second same-connection run.

**Actions:**

- Normalized the incoming PNG resource filenames for Android and mapped the chooser to the exact provided assets
- Redesigned the chooser cards with larger centered imagery, better row balance, and safe bottom spacing for the continue button
- Added per-step warm-up descriptions and active-step highlighting in the warm-up screen
- Fixed `resetSession()` so the same-connection warm-up path re-enters `WARMING_UP` and restarts the warm-up timer instead of idling
- Re-ran targeted RED/GREEN verification and a final full `:app:testDebugUnitTest` pass

**Status:** ✅ Final chooser and warm-up fixes complete, manual runtime validation pending

---

## 2026-03-12 — v0.1.6

**Objective:** Make Skip warm-up persist for the rest of a live connection and keep Start training disabled until the timed warm-up is actually complete.

**Actions:**

- Added RED coverage for connection-wide warm-up skip persistence and Start training gating
- Added a connection-scoped warm-up requirement flag that resets on disconnect/reconnect and is cleared when the user taps Skip warm-up
- Updated the reset flow so same-connection sessions stay idle after a connection-wide skip instead of restarting the warm-up timer automatically
- Routed the warm-up primary action through a gating rule so Start training stays disabled until the full timed warm-up completes unless the connection-wide skip is active
- Re-ran targeted RED/GREEN verification and a final full `:app:testDebugUnitTest` pass

**Status:** ✅ Connection-wide warm-up skip and button gating complete, runtime/manual validation pending

---

## 2026-03-22 — v0.2.0

**Objective:** Produce a concrete implementation plan to keep stats visible after Firebase upload even when local records are cleaned.

**Actions:**

- Loaded required request workflow skills and memory-bank context
- Validated current behavior: sync marks sessions as synced, while stats screens read Room-only sources
- Captured product decision to support local cleanup with Firebase-backed stats visibility
- Authored implementation plan at `memory-bank/plans/2026-03-22-firebase-merged-stats-visibility.md`
- Updated project brief, active context, and master plan for implementation handoff

**Status:** ✅ Planning complete, implementation queued

---
