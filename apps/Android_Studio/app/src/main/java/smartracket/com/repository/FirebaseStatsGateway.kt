package smartracket.com.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import smartracket.com.model.Stroke
import smartracket.com.model.TrainingSession
import smartracket.com.model.Sport
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Stats Read Gateway (Phase B)
 *
 * Provides read-only access to training sessions and strokes stored in Firestore.
 * The gateway enables stats screens to access uploaded data even after local deletion.
 *
 * ## Architecture
 *
 * - Reads from: `users/{uid}/sessions/{sessionId}` and nested `strokes` subcollection
 * - No write operations (read-only gateway)
 * - Non-blocking: returns empty list on auth/network failures rather than crashing
 * - Used by: HomeViewModel (all-time stats), AnalyticsViewModel (date-range queries)
 *
 * ## Error Handling
 *
 * All methods return empty lists on error and log the failure. This provides graceful
 * degradation: if Firebase is unavailable, the app falls back to local Room data.
 *
 * ## Feature Flag
 *
 * Phase B onward should be controlled via a feature flag using BuildConfig or
 * SharedPreferences to ensure safe rollout and easy disable if issues arise.
 */
@Singleton
class FirebaseStatsGateway @Inject constructor(
    // Note: Firebase instances are obtained via getInstance() to simplify Hilt binding
) {
    companion object {
        private const val TAG = "FirebaseStatsGateway"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_SESSIONS = "sessions"
        private const val COLLECTION_STROKES = "strokes"
    }

        private val auth: FirebaseAuth
            get() = FirebaseAuth.getInstance()

        private val firestore: FirebaseFirestore
            get() = FirebaseFirestore.getInstance()

    /**
     * Query all user sessions from Firestore (all-time, no date filter).
     *
     * ## Failure Modes
     * - Auth not available → returns empty list
     * - Firestore unavailable → returns empty list
     * - Index missing → returns empty list (with log warning)
     * - Network error → returns empty list (with log warning)
     *
     * @return List of TrainingSession objects from Firestore, or empty if unavailable.
     */
    suspend fun getCloudSessions(): List<TrainingSession> {
        return try {
            val uid = getCurrentUserId() ?: run {
                Log.w(TAG, "User not authenticated; cannot query cloud sessions")
                return emptyList()
            }

            val docs = firestore
                .collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_SESSIONS)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .await()

            docs.map { doc ->
                doc.toTrainingSession()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch cloud sessions: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Query user sessions from Firestore within a date range.
     *
     * ## Firestore Query
     *
     * Requires index on (uid, startTime) for performance.
     * See: https://firebase.google.com/docs/firestore/indexes/manage-indexes
     *
     * @param startDate Unix timestamp (inclusive)
     * @param endDate Unix timestamp (inclusive)
     * @return Sessions with startTime in [startDate, endDate], or empty on error
     */
    suspend fun getCloudSessionsByDateRange(
        startDate: Long,
        endDate: Long
    ): List<TrainingSession> {
        return try {
            val uid = getCurrentUserId() ?: return emptyList()

            val docs = firestore
                .collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_SESSIONS)
                .whereGreaterThanOrEqualTo("startTime", startDate)
                .whereLessThanOrEqualTo("startTime", endDate)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .await()

            docs.map { doc ->
                doc.toTrainingSession()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch cloud sessions by date range: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetch strokes for a specific session from Firestore.
     *
     * Used internally by merge service if need to reconstruct full session with strokes.
     * For stats purposes, session-level aggregates (totalStrokes, avgScore) are sufficient.
     *
     * @param sessionId The session to fetch strokes for
     * @return List of Stroke objects, or empty if unavailable
     */
    suspend fun getCloudStrokesForSession(sessionId: Long): List<Stroke> {
        return try {
            val uid = getCurrentUserId() ?: return emptyList()

            val docs = firestore
                .collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_SESSIONS)
                .document(sessionId.toString())
                .collection(COLLECTION_STROKES)
                .get()
                .await()

            docs.map { doc ->
                doc.toStroke()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch strokes for session $sessionId: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Check if Firebase is configured and reachable.
     * Returns false silently (does not throw) for safe fallback.
     */
    fun isAvailable(): Boolean {
        return try {
            val projectId = firestore.app.options.projectId
            projectId != null && !projectId.contains("placeholder")
        } catch (e: Exception) {
            Log.d(TAG, "Firebase not available: ${e.message}")
            false
        }
    }

    /* ========== Helpers ========== */

    private fun getCurrentUserId(): String? {
        return try {
            auth.currentUser?.uid
        } catch (e: Exception) {
            Log.w(TAG, "Could not get current user: ${e.message}")
            null
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toTrainingSession(): TrainingSession {
        return TrainingSession(
            sessionId = getLong("sessionId")?.toLong() ?: 0L,
            startTime = getLong("startTime") ?: System.currentTimeMillis(),
            sport = (getString("sport") ?: "TABLE_TENNIS").let { Sport.valueOf(it) },
            endTime = getLong("endTime"),
            totalDuration = getLong("totalDuration") ?: 0L,
            avgScore = getDouble("avgScore")?.toFloat() ?: 0f,
            totalStrokes = getLong("totalStrokes")?.toInt() ?: 0,
            heartRateData = emptyList(), // Not stored in detail at session level
            avgHeartRate = getLong("avgHeartRate")?.toInt(),
            maxHeartRate = getLong("maxHeartRate")?.toInt(),
            caloriesBurned = getDouble("caloriesBurned")?.toFloat(),
            notes = getString("notes"),
            isSynced = getBoolean("isSynced") ?: false,
            warmUpState = (getString("warmUpState") ?: "NOT_STARTED").let {
                smartracket.com.model.WarmUpState.valueOf(it)
            },
            warmUpDurationMs = getLong("warmUpDurationMs") ?: 0L,
            restReminderIntervalMs = getLong("restReminderIntervalMs") ?: 60000L,
            restReminderCount = getLong("restReminderCount")?.toInt() ?: 0
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toStroke(): Stroke {
        return Stroke(
            strokeId = getLong("strokeId")?.toLong() ?: 0L,
            sessionId = getLong("sessionId") ?: 0L,
            timestamp = getLong("timestamp") ?: System.currentTimeMillis(),
            strokeType = getString("strokeType") ?: "unknown",
            score = getLong("score")?.toInt() ?: 0,
            motionData = smartracket.com.model.MotionData.empty(),
            feedback = getString("feedback") ?: "",
            confidence = getDouble("confidence")?.toFloat() ?: 0f,
            peakAcceleration = getDouble("peakAcceleration")?.toFloat(),
            strokeDuration = getLong("strokeDuration")
        )
    }
}
