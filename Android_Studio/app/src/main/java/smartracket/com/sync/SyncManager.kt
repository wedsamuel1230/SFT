package smartracket.com.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import smartracket.com.repository.FirebaseSyncRepository
import smartracket.com.repository.SyncState
import smartracket.com.repository.SyncStats
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinator for all sync operations.
 *
 * Responsibilities:
 * - Schedule periodic background sync via WorkManager
 * - Trigger immediate one-shot sync (e.g., after session end)
 * - Expose sync state and statistics to the UI
 * - Cancel sync work when user disables cloud sync
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseSyncRepository: FirebaseSyncRepository
) {
    companion object {
        private const val TAG = "SyncManager"
        private const val PERIODIC_INTERVAL_MINUTES = 15L
    }

    private val workManager = WorkManager.getInstance(context)

    /** Observable sync state for UI binding. */
    val syncState: StateFlow<SyncState> = firebaseSyncRepository.syncState

    /**
     * Enable periodic background sync.
     * Call once at app startup or when user enables cloud sync in settings.
     */
    fun enablePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            PERIODIC_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(SyncWorker.TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )

        Log.d(TAG, "Periodic sync enabled (every ${PERIODIC_INTERVAL_MINUTES}min)")
    }

    /**
     * Disable periodic background sync.
     * Call when user disables cloud sync in settings.
     */
    fun disablePeriodicSync() {
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME_PERIODIC)
        Log.d(TAG, "Periodic sync disabled")
    }

    /**
     * Trigger an immediate one-shot sync.
     * Useful after a training session ends or when user taps "Sync Now".
     */
    fun triggerImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneShotRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag(SyncWorker.TAG)
            .build()

        workManager.enqueueUniqueWork(
            SyncWorker.WORK_NAME_ONE_SHOT,
            ExistingWorkPolicy.REPLACE,
            oneShotRequest
        )

        Log.d(TAG, "One-shot sync triggered")
    }

    /**
     * Observe the sync work status for UI feedback.
     */
    fun observeSyncWork(): Flow<Boolean> {
        return workManager
            .getWorkInfosByTagFlow(SyncWorker.TAG)
            .map { workInfos ->
                workInfos.any { it.state == WorkInfo.State.RUNNING }
            }
    }

    /**
     * Get current sync statistics.
     */
    suspend fun getSyncStats(): SyncStats {
        return firebaseSyncRepository.getSyncStats()
    }

    /**
     * Run sync directly (for use in service contexts like WearableListenerService).
     * Prefer [triggerImmediateSync] for UI-triggered syncs.
     */
    suspend fun syncNow(): Int {
        return firebaseSyncRepository.syncUnsyncedSessions()
    }
}
