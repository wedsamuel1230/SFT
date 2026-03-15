# Active Context

## Agent Context

- Request workflow is running under `.github/prompts/request.prompt.md`.
- Memory bank was bootstrapped on 2026-03-10 because it did not exist.

## Current Task

Make warm-up skipping persist for the rest of a live BLE connection and keep the Start training button disabled until the full warm-up completes unless that connection-wide skip is active.

## Pending

- Perform runtime/manual validation that tapping Skip warm-up once causes later same-connection sessions to bypass the timed warm-up and show an immediate Start training action
- Perform runtime/manual validation that repeated sessions on the same connection skip the sport selector and go straight to warm-up
- Perform runtime/manual validation that disconnecting and reconnecting requires sport confirmation again, with the remembered default preselected
- Verify the scrollable warm-up layout feels correct on smaller screens and does not hide the action buttons
- Verify the new sport selection icons feel visually balanced in selected and unselected card states on device/emulator
- Verify the foreground-service rest reminder behavior end to end during pause/resume transitions
- Decide whether to add Compose/instrumentation coverage for the new preparation flow

## Completed This Session

- Added RED coverage for connection-wide warm-up skip persistence and Start training gating
- Added a connection-scoped warm-up requirement flag so Skip warm-up applies to later same-connection sessions until disconnect/reconnect
- Updated the warm-up reset path so skipped connections return to an idle Start training state instead of restarting the timer
- Disabled the Start training warm-up action until the timer fully completes unless the connection-wide skip is active
- Added unit coverage for connection-scoped preparation rules and warm-up demo progression
- Updated the training flow so sport selection appears once per BLE connection and repeated sessions on the same connection resume at warm-up
- Added local warm-up illustration assets and surfaced them in the warm-up screen as demo guidance
- Replaced the generic selector glyph with seven dedicated local sport drawables modeled on the provided sport PNG set
- Tightened the selector layout with a shared spec for icon size, grid spacing, and card padding
- Removed the warm-up guiding image block and made the warm-up content area scrollable
- Switched the chooser to the exact provided PNG sport assets after normalizing the Android resource filenames
- Added per-step warm-up descriptions and active-step highlighting
- Fixed same-connection session resets so warm-up restarts in `WARMING_UP` instead of getting stuck on the second run
- Re-ran the targeted preparation-flow tests and the full debug unit suite successfully

## Recovery Instructions

1. Read `projectbrief.md`, `activeContext.md`, `SESSION.md`, and `master-plan.md`.
2. Start with the runtime/manual validation items in `## Pending`, especially connection-wide Skip warm-up persistence and same-connection preparation behavior on device.
3. Keep the phase ledger aligned with actual proof from tests and file changes.

---
Last Updated: 2026-03-12
