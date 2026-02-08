package smartracket.com.model

/**
 * Model output received from MCU over BLE.
 *
 * Expected JSON fields:
 * - ts: timestamp (ms)
 * - stroke: stroke type string
 * - score: int score (1-10)
 * - conf: confidence (0.0-1.0)
 * - peak: peak acceleration (m/s^2)
 */
data class McuModelOutput(
    val ts: Long,
    val stroke: String,
    val score: Int,
    val conf: Float,
    val peak: Float
)