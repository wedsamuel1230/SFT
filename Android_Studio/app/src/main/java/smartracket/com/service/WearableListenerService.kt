package smartracket.com.service

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Service for receiving messages and data from Galaxy Watch.
 *
 * Handles:
 * - Heart rate data from watch
 * - Training control commands from watch
 * - Data sync requests
 */
@AndroidEntryPoint
class WearableListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearableListenerService"

        const val PATH_HEART_RATE = "/smartracket/heartrate"
        const val PATH_TRAINING_START = "/smartracket/training/start"
        const val PATH_TRAINING_STOP = "/smartracket/training/stop"
        const val PATH_SYNC_REQUEST = "/smartracket/sync"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        Log.d(TAG, "Message received: ${messageEvent.path}")

        when (messageEvent.path) {
            PATH_HEART_RATE -> handleHeartRateMessage(messageEvent.data)
            PATH_TRAINING_START -> handleTrainingStartMessage()
            PATH_TRAINING_STOP -> handleTrainingStopMessage()
            PATH_SYNC_REQUEST -> handleSyncRequest()
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        for (event in dataEvents) {
            Log.d(TAG, "Data changed: ${event.dataItem.uri.path}")
            // Handle data changes from watch
        }
    }

    private fun handleHeartRateMessage(data: ByteArray) {
        serviceScope.launch {
            try {
                if (data.isNotEmpty()) {
                    val heartRate = data[0].toInt() and 0xFF
                    Log.d(TAG, "Heart rate received: $heartRate BPM")
                    // Update heart rate in repository
                    // healthRepository.updateWatchHeartRate(heartRate)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse heart rate", e)
            }
        }
    }

    private fun handleTrainingStartMessage() {
        serviceScope.launch {
            Log.d(TAG, "Training start requested from watch")
            // Start training session
            // Could broadcast an intent to start training in the app
        }
    }

    private fun handleTrainingStopMessage() {
        serviceScope.launch {
            Log.d(TAG, "Training stop requested from watch")
            // Stop training session
        }
    }

    private fun handleSyncRequest() {
        serviceScope.launch {
            Log.d(TAG, "Sync requested from watch")
            // Sync current session data to watch
            // sendSessionDataToWatch()
        }
    }
}

