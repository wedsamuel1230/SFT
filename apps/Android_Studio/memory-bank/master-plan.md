# Master Plan

## Milestones

1. [x] Add sport domain and persistence metadata
2. [x] Add sport selection and warm-up flow
3. [x] Add recurring rest reminders during active training
4. [ ] Verify with tests and manual lifecycle checks

## Upcoming Work

- [ ] Manually validate that tapping Skip warm-up once causes later sessions on the same connection to bypass the timed warm-up and present an immediate Start training action
- [ ] Manually validate that the first session after a new connection shows sport selection with the remembered default preselected
- [ ] Manually validate that subsequent sessions on the same connection start at warm-up without showing sport selection again
- [ ] Manually validate the scrollable warm-up layout on device/emulator, especially on smaller screens
- [ ] Manually validate the seven new sport selector icons in selected and unselected card states
- [ ] Manually validate recurring reminder notifications across active and paused states
- [ ] Decide whether to add instrumentation or Compose UI tests for the new flow

## Completed

- [x] Created implementation plan for the requested features
- [x] Bootstrapped repository memory-bank structure
- [x] Added sport metadata persistence and Room schema updates
- [x] Added sport selection and warm-up training subflows
- [x] Added recurring rest reminder service and UI handling
- [x] Passed targeted RED/GREEN tests and full debug unit tests
- [x] Scoped sport selection to new BLE connections instead of every session reset
- [x] Added warm-up demo illustration assets to the training flow
- [x] Replaced the generic sport selector glyph with dedicated sport-specific assets
- [x] Tightened the selector icon sizing and card/grid spacing with a shared layout spec
- [x] Removed the warm-up guiding image block and made the warm-up section scrollable
- [x] Swapped the chooser to the exact provided PNG sport assets
- [x] Added step descriptions and active-step highlighting to the warm-up screen
- [x] Fixed same-connection warm-up restarts so the second run does not stall
- [x] Made Skip warm-up persist for the rest of the live connection and gated Start training until timed warm-up completion

---
Last Updated: 2026-03-12
