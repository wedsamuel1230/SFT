# Master Plan

## Milestones

1. [x] Add sport domain and persistence metadata
2. [x] Add sport selection and warm-up flow
3. [x] Add recurring rest reminders during active training
4. [ ] Verify with tests and manual lifecycle checks
5. [ ] Implement merged local + Firebase stats visibility after local cleanup

## Upcoming Work

- [ ] Execute Phase A/B from `memory-bank/plans/2026-03-22-firebase-merged-stats-visibility.md` (merge contract + Firebase stats read gateway)
- [ ] Add RED tests for upload -> local cleanup -> stats continuity across Home and Analytics
- [ ] Implement merge provider and wire HomeViewModel + AnalyticsViewModel to merged source
- [ ] Produce GREEN verification plus regression results for cloud-only visibility and dedup behavior
- [ ] Add rollout guardrails (feature flag + diagnostics) for merged stats source

## Completed

- [x] Installed and configured follow-builders with daily Chinese digest automation and Discord webhook delivery (2026-03-27)
- [x] Added heavy-overload popup (peak > 300) and temporarily disabled heart-rate polling to avoid Health Connect permission errors (2026-03-26)
- [x] Fixed Android handling of firmware overload warning packets and verified with debug unit tests (2026-03-26)
- [x] Fixed Arduino overload warning emission path in `test_overload/test_overload.ino` and verified compile on XIAO nRF52840 Sense (2026-03-26)
- [x] Reconciled memory-bank workflow state drift and refreshed session tracking artifacts (2026-03-26)
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
- [x] Created Firebase merged-stats continuity plan at `memory-bank/plans/2026-03-22-firebase-merged-stats-visibility.md`

---
Last Updated: 2026-03-26
