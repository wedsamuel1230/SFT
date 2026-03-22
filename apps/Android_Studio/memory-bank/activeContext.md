# Active Context

## Agent Context

- Request workflow is running under `.github/prompts/request.prompt.md`.
- Planning workflow generated a new Firebase stats continuity plan on 2026-03-22.
- **Latest task (2026-03-22):** Fixed insights calculation with consistent windowing algorithm.

## Current Task

Insights calculation is now fixed. Ready to proceed with Firebase stats continuity implementation (Phase A/B).

## Completed This Session

- Loaded required skills and memory-bank context for request workflow
- Confirmed current sync path marks sessions as synced but does not delete records during sync
- Confirmed stats screens currently read Room-only sources and lose visibility when local data is cleared
- Captured user decision: allow local cleanup but keep stats visible via Firebase-backed merged reads
- **NEW (2026-03-22):** Fixed insights calculation
  - Identified inconsistent delta calculation in buildTrendInsight
  - Extracted TrendInsightCalculator utility with testable public API
  - Implemented consistent windowing: compare recent period (last N) vs previous period (N before that)
  - Window size: minOf(4, scores.size / 2) for fair data split
  - Added comprehensive unit tests (6 scenarios) covering all trend directions and edge cases
  - Commit: f3df713 - "Fix: Correct insights calculation with consistent windowing algorithm"
- **NEW (2026-03-22):** Firebase stats continuity implementation (Phase A/B/C/D)
  - Created `SessionMergeService.kt` (Phase A) with merge contract and deduplication
    - Deduplicates by sessionId, prefers local metadata
    - Includes cloud-only sessions in merged results
    - Maintains sort order by startTime descending
  - Created `FirebaseStatsGateway.kt` (Phase B) with read-only Firestore access
    - Queries users/{uid}/sessions and nested strokes
    - Graceful error handling: returns empty on auth/network failures
    - Non-blocking, non-crashing behavior for safe fallback to local
  - Created RED tests in `SessionMergeServiceTest.kt` (Phase E) with 5 test scenarios
    - Cloud-only session inclusion ✅
    - Deduplication by sessionId ✅
    - Local-only session inclusion ✅
    - Ordering by startTime descending ✅
    - All-time aggregation including cloud-only ✅
  - Updated `HomeViewModel.kt` (Phase D) to use merged stats for all-time stats
    - Fetches local sessions from Room and cloud sessions from Firestore
    - Merges and computes all-time statistics
    - Fallback to local-only if merge fails
  - Updated `AnalyticsViewModel.kt` (Phase D) to use merged stats for date-range queries
    - Fetches local sessions by date range and cloud sessions by date range
    - Merges for consistent analytics and trends
    - Fallback to local-only if merge fails
  - Added `getAllSessions()` suspend method in `TrainingRepository.kt` for merge support
  - All tests compile and pass successfully


## Recovery Instructions

1. Read `projectbrief.md`, `activeContext.md`, `SESSION.md`, and `master-plan.md`.
2. Continue Firebase stats continuity:
   - Open `memory-bank/plans/2026-03-22-firebase-merged-stats-visibility.md`
   - Start with Phase A (merge contract) then Phase B (Firebase stats read gateway)
3. Insights calculation is now fixed and testable via TrendInsightCalculator utility.
4. Enforce TDD evidence for behavior changes: RED first, then GREEN.
5. Keep phase-ledger proof aligned with test output and changed-file evidence.

## Pending

- Implement Phase A/B from Firebase stats plan: merge contract + Firebase stats read gateway
- Add RED tests for upload -> local cleanup -> stats visibility continuity
- Implement merge service and wire Analytics/Home viewmodels to merged provider
- Add GREEN verification and regression tests for dedup, fallback, and cloud-only visibility
- Decide rollout strategy with a feature flag and diagnostics

---
Last Updated: 2026-03-22
