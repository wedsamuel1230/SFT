package smartracket.com.model

import java.util.Locale

/**
 * Model output received from MCU over BLE.
 *
 * Expected JSON fields from test.ino:
 * - ts: timestamp (ms)
 * - stroke: stroke type string ("forehand", "backhand", "drive")
 * - event: optional event type (e.g., "warning")
 * - conf: confidence (0.0-1.0)
 * - peak: peak acceleration (m/s²)
 *
 * Score is calculated from confidence: score = round(conf * 10).coerceIn(1, 10)
 */
data class McuModelOutput(
    val ts: Long,
    val stroke: String,
    val event: String = "",
    val conf: Float,
    val peak: Float
) {
    companion object {
        const val HEAVY_PEAK_THRESHOLD = 300f
    }

    /** Score derived from confidence: 1–10 scale */
    val score: Int get() = (conf * 10).toInt().coerceIn(1, 10)

    /** True when firmware marks this packet as a warning or overload event. */
    val isWarning: Boolean
        get() = event.equals("warning", ignoreCase = true) ||
            stroke.startsWith("overload_", ignoreCase = true)

    /** True when peak exceeds the heavy-load safety threshold. */
    val isTooHeavy: Boolean
        get() = peak > HEAVY_PEAK_THRESHOLD

    /** Human-readable warning text for overload packets; null for normal stroke packets. */
    fun warningMessageOrNull(): String? {
        if (!isWarning) return null

        val channel = if (stroke.contains("gyro", ignoreCase = true)) "gyro" else "accel"
        return String.format(
            Locale.US,
            "Overload warning (%s): peak %.1f. Please reduce swing intensity.",
            channel,
            peak
        )
    }
}