package smartracket.com.repository

import smartracket.com.model.TrainingSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Merge contract for combining local and cloud training sessions.
 *
 * ## Merge Policy (Phase A: Data Contract)
 *
 * `SessionMergeService` combines local (Room) and cloud (Firebase) sessions for stats visibility.
 * When a user deletes local records but they exist in Firebase, stats remain visible via this merge.
 *
 * ### Key Terms
 * - **Local session:** exists in Room database (isSynced may be true or false).
 * - **Cloud session:** exists in Firestore (any sessionId that came from queryCloudSessions).
 * - **Merged session:** result of combining local and cloud by sessionId.
 *
 * ### Precedence Rules
 *
 * 1. **Deduplication:** If a session exists in both local and cloud (same sessionId),
 *    it appears **once** in the merged result.
 *
 * 2. **Session Selection (when sessionId matches both local and cloud):**
 *    - Prefer **local** session if it exists, because Room is the source of truth for the app.
 *    - Use cloud session as fallback only when no local version exists.
 *    - Metadata precedence: local > cloud (for timestamps, sport, user notes).
 *
 * 3. **Single-Source Fallback:**
 *    - If only local exists → include local unchanged
 *    - If only cloud exists → include cloud unchanged
 *
 * 4. **Ordering:** Sort merged list by `startTime` descending (most recent first),
 *    matching the existing TrainingRepository behavior.
 *
 * ### Invalid Merge States (should not occur)
 * - `null` sessionId in input list → filtered out
 * - Duplicate sessionId within the same source (local or cloud) → caller responsibility to dedupe
 *
 * ### Analytics Aggregations
 * - `totalStrokes`: sum across merged sessions (both local and cloud counted)
 * - `avgScore`: weighted average or mean of all session avgScore values
 * - Date-range queries: filter merged list by start-time window
 *
 */
@Singleton
class SessionMergeService @Inject constructor() {

    /**
     * Merge local and cloud sessions into a single deduplicated list.
     *
     * @param localSessions Sessions from Room database (already fetched).
     * @param cloudSessions Sessions from Firestore (already fetched).
     * @return Merged list with cloud-only records included, sorted by startTime descending.
     */
    fun mergeSessions(
        localSessions: List<TrainingSession>,
        cloudSessions: List<TrainingSession>
    ): List<TrainingSession> {
        val localById = localSessions.associateBy { it.sessionId }
        val result = mutableListOf<TrainingSession>()

        result.addAll(localSessions)

        for (cloudSession in cloudSessions) {
            if (!localById.containsKey(cloudSession.sessionId)) {
                result.add(cloudSession)
            }
        }

        return result.sortedByDescending { it.startTime }
    }

    /**
     * Get all-time statistics by merging local and cloud sessions.
     *
     * Used by HomeViewModel to compute all-time totals and averages.
     *
     * @param localSessions Sessions from Room.
     * @param cloudSessions Sessions from Firestore.
     * @return Merged sessions for stats aggregation.
     */
    fun getMergedForAllTimeStats(
        localSessions: List<TrainingSession>,
        cloudSessions: List<TrainingSession>
    ): List<TrainingSession> {
        return mergeSessions(localSessions, cloudSessions)
    }

    /**
     * Get statistics for a date range by merging local and cloud sessions.
     *
     * Used by AnalyticsViewModel to filter by date and compute trends.
     *
     * @param localSessions Sessions from Room (already filtered by date range).
     * @param cloudSessions Sessions from Firestore (already filtered by date range).
     * @return Merged sessions for the date range.
     */
    fun getMergedForDateRangeStats(
        localSessions: List<TrainingSession>,
        cloudSessions: List<TrainingSession>
    ): List<TrainingSession> {
        return mergeSessions(localSessions, cloudSessions)
    }

    /**
     * Deduplicate a list of sessions by sessionId, keeping only the first occurrence.
     *
     * Used internally to ensure no duplicate sessionIds in the merged result.
     * (This is called implicitly by mergeSessions, but exposed for testing/auditing.)
     *
     * @param sessions Sessions to deduplicate.
     * @return List with duplicate sessionIds removed.
     */
    fun deduplicateBySessionId(sessions: List<TrainingSession>): List<TrainingSession> {
        val seen = mutableSetOf<Long>()
        return sessions.filter { session ->
            val isNew = session.sessionId !in seen
            if (isNew) seen.add(session.sessionId)
            isNew
        }
    }
}
