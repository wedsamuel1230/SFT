package smartracket.com.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import smartracket.com.db.SmartRacketDatabase
import smartracket.com.model.Stroke
import smartracket.com.model.TrainingSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync state exposed to the UI layer.
 */
sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data class Success(val sessionsSynced: Int) : SyncState()
    data class Error(val message: String) : SyncState()
    data object FirebaseUnavailable : SyncState()
}

/**
 * Repository managing bidirectional sync between Room (local cache)
 * and Firebase Firestore (cloud storage).
 *
 * Architecture:
 * - Room remains the single source of truth for the phone app.
 * - Firestore stores session summaries and strokes
 *   so the Galaxy Watch companion app can read them.
 * - Sync is session-granular: when a completed session is synced,
 *   all its strokes are pushed together.
 * - Uses anonymous Firebase Auth for per-device user identity.
 *
 * Firestore schema:
 *   users/{uid}/sessions/{sessionId}          — session doc
 *   users/{uid}/sessions/{sessionId}/strokes   — subcollection
 */
@Singleton
class FirebaseSyncRepository @Inject constructor(
    private val database: SmartRacketDatabase
) {
    companion object {
        private const val TAG = "FirebaseSyncRepo"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_SESSIONS = "sessions"
        private const val COLLECTION_STROKES = "strokes"
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val sessionDao get() = database.trainingSessionDao()
    private val strokeDao get() = database.strokeDao()

    /**
     * Whether Firebase is properly configured and available.
     * Returns false if google-services.json is a placeholder.
     */
    private fun isFirebaseAvailable(): Boolean {
        return try {
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            // Basic sanity check — project ID shouldn't be a placeholder
            val projectId = firestore.app.options.projectId
            projectId != null && !projectId.contains("placeholder")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase not available: ${e.message}")
            false
        }
    }

    /**
     * Ensure the user is authenticated (anonymous auth).
     * Returns the UID or null if authentication fails.
     */
    private suspend fun ensureAuthenticated(): String? {
        return try {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            if (currentUser != null) {
                return currentUser.uid
            }
            val result = auth.signInAnonymously().await()
            result.user?.uid
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous auth failed", e)
            null
        }
    }

    /**
     * Sync all unsynced completed sessions to Firestore.
     * This is the main entry point called by SyncWorker and manual triggers.
     *
     * @return number of sessions successfully synced
     */
    suspend fun syncUnsyncedSessions(): Int {
        if (!isFirebaseAvailable()) {
            _syncState.value = SyncState.FirebaseUnavailable
            Log.w(TAG, "Firebase not configured — skipping sync")
            return 0
        }

        _syncState.value = SyncState.Syncing

        val uid = ensureAuthenticated()
        if (uid == null) {
            _syncState.value = SyncState.Error("Authentication failed")
            return 0
        }

        return try {
            val unsyncedSessions = sessionDao.getUnsyncedSessions()
            if (unsyncedSessions.isEmpty()) {
                _syncState.value = SyncState.Success(0)
                return 0
            }

            Log.d(TAG, "Syncing ${unsyncedSessions.size} sessions for user $uid")
            var successCount = 0

            for (session in unsyncedSessions) {
                try {
                    syncSession(uid, session)
                    sessionDao.markAsSynced(session.sessionId)
                    successCount++
                    Log.d(TAG, "Synced session ${session.sessionId}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync session ${session.sessionId}", e)
                    // Continue with next session — don't abort the batch
                }
            }

            _syncState.value = SyncState.Success(successCount)
            Log.d(TAG, "Sync complete: $successCount/${unsyncedSessions.size} sessions")
            successCount
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            0
        }
    }

    /**
     * Push a single session with all its strokes to Firestore.
     */
    private suspend fun syncSession(uid: String, session: TrainingSession) {
        val firestore = FirebaseFirestore.getInstance()
        val sessionRef = firestore
            .collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_SESSIONS)
            .document(session.sessionId.toString())

        // 1. Write session document
        val sessionMap = sessionToMap(session)
        sessionRef.set(sessionMap, SetOptions.merge()).await()

        // 2. Write strokes as subcollection (batch for efficiency)
        val strokes = strokeDao.getBySessionId(session.sessionId)
        if (strokes.isNotEmpty()) {
            // Firestore batch limit is 500 — chunk if needed
            strokes.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                for (stroke in chunk) {
                    val strokeRef = sessionRef
                        .collection(COLLECTION_STROKES)
                        .document(stroke.strokeId.toString())
                    batch.set(strokeRef, strokeToMap(stroke), SetOptions.merge())
                }
                batch.commit().await()
            }
        }
    }

    /**
     * Get sync statistics for UI display.
     */
    suspend fun getSyncStats(): SyncStats {
        val unsyncedCount = sessionDao.getUnsyncedCount()
        val syncedCount = sessionDao.getSyncedCount()
        val totalCount = sessionDao.getTotalCount()
        return SyncStats(
            totalSessions = totalCount,
            syncedSessions = syncedCount,
            pendingSessions = unsyncedCount,
            isFirebaseConfigured = isFirebaseAvailable()
        )
    }

    // ============= Entity → Map Converters =============

    private fun sessionToMap(session: TrainingSession): Map<String, Any?> = mapOf(
        "sessionId" to session.sessionId,
        "startTime" to session.startTime,
        "sport" to session.sport.name,
        "endTime" to session.endTime,
        "totalDuration" to session.totalDuration,
        "avgScore" to session.avgScore.toDouble(),
        "totalStrokes" to session.totalStrokes,
        "heartRateData" to session.heartRateData.map { mapOf("timestamp" to it.timestamp, "bpm" to it.bpm) },
        "avgHeartRate" to session.avgHeartRate,
        "maxHeartRate" to session.maxHeartRate,
        "caloriesBurned" to session.caloriesBurned?.toDouble(),
        "notes" to session.notes,
        "warmUpState" to session.warmUpState.name,
        "warmUpDurationMs" to session.warmUpDurationMs,
        "restReminderIntervalMs" to session.restReminderIntervalMs,
        "restReminderCount" to session.restReminderCount,
        "syncedAt" to System.currentTimeMillis()
    )

    private fun strokeToMap(stroke: Stroke): Map<String, Any?> = mapOf(
        "strokeId" to stroke.strokeId,
        "sessionId" to stroke.sessionId,
        "timestamp" to stroke.timestamp,
        "strokeType" to stroke.strokeType,
        "score" to stroke.score,
        "feedback" to stroke.feedback,
        "confidence" to stroke.confidence.toDouble(),
        "peakAcceleration" to stroke.peakAcceleration?.toDouble(),
        "strokeDuration" to stroke.strokeDuration,
        // MotionData stored as nested map for Firestore
        "motionData" to mapOf(
            "accelX" to stroke.motionData.accelX.map { it.toDouble() },
            "accelY" to stroke.motionData.accelY.map { it.toDouble() },
            "accelZ" to stroke.motionData.accelZ.map { it.toDouble() },
            "gyroX" to stroke.motionData.gyroX.map { it.toDouble() },
            "gyroY" to stroke.motionData.gyroY.map { it.toDouble() },
            "gyroZ" to stroke.motionData.gyroZ.map { it.toDouble() },
            "timestamps" to stroke.motionData.timestamps
        )
    )
}

/**
 * Data class for sync statistics displayed in the UI.
 */
data class SyncStats(
    val totalSessions: Int,
    val syncedSessions: Int,
    val pendingSessions: Int,
    val isFirebaseConfigured: Boolean
)
