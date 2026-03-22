package smartracket.com.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import smartracket.com.model.Sport
import smartracket.com.model.TrainingSession

/**
 * RED TEST: Validates that merged stats include both local and cloud sessions.
 * 
 * Scenario: User uploads session, then deletes local copy.
 * Expected: Stats screens still show the session from Firebase.
 */
class SessionMergeServiceTest {

    private lateinit var mergeService: SessionMergeService

    @Before
    fun setup() {
        mergeService = SessionMergeService()
    }

    /**
     * RED: Cloud-only session appears in merged results after local deletion.
     */
    @Test
    fun testMerge_CloudOnlySessionIncluded() {
        val cloudSession = TrainingSession(
            sessionId = 1L,
            startTime = System.currentTimeMillis() - 86400000, // yesterday
            sport = Sport.TABLE_TENNIS,
            totalStrokes = 50,
            avgScore = 8.5f,
            isSynced = true
        )

        val localSessions = emptyList<TrainingSession>()

        val merged = mergeService.mergeSessions(localSessions, listOf(cloudSession))

        assertEquals(1, merged.size)
        assertEquals(cloudSession.sessionId, merged[0].sessionId)
        assertEquals(50, merged[0].totalStrokes)
    }

    /**
     * RED: When both local and cloud sessions exist with same sessionId,
     * deduplicate and prefer local metadata for in-progress fields,
     * latest analytics for completed fields.
     */
    @Test
    fun testMerge_DeduplicateBySessionId() {
        val localSession = TrainingSession(
            sessionId = 1L,
            startTime = System.currentTimeMillis() - 3600000, // 1 hour ago
            sport = Sport.TABLE_TENNIS,
            totalStrokes = 45,
            avgScore = 8.0f,
            isSynced = true,
            endTime = System.currentTimeMillis() - 1800000 // ended
        )

        val cloudSession = TrainingSession(
            sessionId = 1L,
            startTime = System.currentTimeMillis() - 3600000,
            sport = Sport.TABLE_TENNIS,
            totalStrokes = 48, // cloud has slightly different total
            avgScore = 8.2f,
            isSynced = true,
            endTime = System.currentTimeMillis() - 1800000
        )

        val merged = mergeService.mergeSessions(listOf(localSession), listOf(cloudSession))

        assertEquals(1, merged.size)
        // Should appear only once (deduped)
        assertEquals(localSession.sessionId, merged[0].sessionId)
    }

    /**
     * RED: Local-only session returns unmodified.
     */
    @Test
    fun testMerge_LocalOnlySessionIncluded() {
        val localSession = TrainingSession(
            sessionId = 2L,
            startTime = System.currentTimeMillis(),
            sport = Sport.TABLE_TENNIS,
            totalStrokes = 30,
            avgScore = 7.5f,
            isSynced = false // not yet synced
        )

        val cloudSessions = emptyList<TrainingSession>()

        val merged = mergeService.mergeSessions(listOf(localSession), cloudSessions)

        assertEquals(1, merged.size)
        assertEquals(localSession.sessionId, merged[0].sessionId)
    }

    /**
     * RED: Merged sessions are ordered by startTime descending (most recent first).
     */
    @Test
    fun testMerge_OrderedByStartTimeDescending() {
        val sessionA = TrainingSession(
            sessionId = 1L,
            startTime = System.currentTimeMillis() - 172800000, // 2 days ago
            sport = Sport.TABLE_TENNIS
        )
        val sessionB = TrainingSession(
            sessionId = 2L,
            startTime = System.currentTimeMillis() - 86400000, // 1 day ago
            sport = Sport.TABLE_TENNIS
        )
        val sessionC = TrainingSession(
            sessionId = 3L,
            startTime = System.currentTimeMillis() - 3600000, // 1 hour ago
            sport = Sport.TABLE_TENNIS
        )

        val merged = mergeService.mergeSessions(
            localSessions = listOf(sessionA, sessionC),
            cloudSessions = listOf(sessionB)
        )

        assertEquals(3, merged.size)
        assertEquals(sessionC.sessionId, merged[0].sessionId)
        assertEquals(sessionB.sessionId, merged[1].sessionId)
        assertEquals(sessionA.sessionId, merged[2].sessionId)
    }

    /**
     * RED: All-time aggregation correctly sums cloud-only sessions.
     */
    @Test
    fun testAggregateAllTime_IncludesCloudOnly() {
        val cloudOnlySession = TrainingSession(
            sessionId = 100L,
            startTime = System.currentTimeMillis() - 2592000000, // 30 days ago
            totalStrokes = 100,
            avgScore = 9.0f
        )
        val localSession = TrainingSession(
            sessionId = 101L,
            startTime = System.currentTimeMillis(),
            totalStrokes = 50,
            avgScore = 8.0f
        )

        val merged = mergeService.mergeSessions(
            localSessions = listOf(localSession),
            cloudSessions = listOf(cloudOnlySession)
        )

        // Both sessions should be included in aggregation
        assertEquals(2, merged.size)
        val totalStrokes = merged.sumOf { it.totalStrokes }
        assertEquals(150, totalStrokes)
    }
}
