package smartracket.com.utils

import android.content.Context
import android.util.Log
import smartracket.com.model.StrokeClassificationResult
import smartracket.com.model.StrokeType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DEPRECATED: Stroke classification is now performed on the MCU (Edge Impulse).
 *
 * This class is kept as a stub for future fallback if on-device classification
 * is needed. The MCU sends classification results via BLE JSON payloads
 * (stroke type, confidence, peak acceleration), which are parsed by
 * BluetoothManager and processed by TrainingRepository.
 *
 * For prototype: MCU classifies 3 stroke types — forehand, backhand, drive.
 * Score is calculated from confidence on the Android side (McuModelOutput.score).
 */
@Deprecated("Classification moved to MCU. Use McuModelOutput from BLE instead.")
@Singleton
class StrokeClassifier @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "StrokeClassifier"
    }

    fun isReady(): Boolean = false

    fun cleanup() {
        Log.d(TAG, "StrokeClassifier is deprecated — classification runs on MCU")
    }
}

