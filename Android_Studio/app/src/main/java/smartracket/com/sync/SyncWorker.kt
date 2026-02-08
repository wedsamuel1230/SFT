package smartracket.com.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import smartracket.com.repository.FirebaseSyncRepository

/**
 * WorkManager worker that periodically syncs unsynced training sessions
 * from Room to Firebase Firestore.
 *
 * Scheduling:
 * - Periodic: every 15 minutes (WorkManager minimum)
 * - One-shot: triggered when a training session ends
 * - Constraints: requires network connectivity
 *
 * Retry policy: exponential backoff on failure.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val firebaseSyncRepository: FirebaseSyncRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "SyncWorker"
        const val WORK_NAME_PERIODIC = "smart_racket_periodic_sync"
        const val WORK_NAME_ONE_SHOT = "smart_racket_one_shot_sync"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync work (attempt $runAttemptCount)")

        return try {
            val syncedCount = firebaseSyncRepository.syncUnsyncedSessions()
            Log.d(TAG, "Sync completed: $syncedCount sessions synced")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync worker failed", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
