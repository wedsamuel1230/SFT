package smartracket.com.model

/**
 * Model output received from MCU over BLE.
 *
 * Expected JSON fields from test.ino:
 * - ts: timestamp (ms)
 * - stroke: stroke type string ("forehand", "backhand", "drive")
 * - conf: confidence (0.0-1.0)
 * - peak: peak acceleration (m/s²)
 *
 * Score is calculated from confidence: score = round(conf * 10).coerceIn(1, 10)
 */
data class McuModelOutput(
    val ts: Long,
    val stroke: String,
    val conf: Float,
    val peak: Float
) {
    /** Score derived from confidence: 1–10 scale */
    val score: Int get() = (conf * 10).toInt().coerceIn(1, 10)
}