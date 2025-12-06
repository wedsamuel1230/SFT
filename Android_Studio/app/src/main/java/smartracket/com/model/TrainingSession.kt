package smartracket.com.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import smartracket.com.db.Converters

/**
 * Represents a complete training session.
 *
 * Contains metadata about the session including timing, performance metrics,
 * and aggregated health data from connected devices.
 */
@Entity(tableName = "training_sessions")
@TypeConverters(Converters::class)
data class TrainingSession(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0,

    /** Unix timestamp when the session started */
    val startTime: Long,

    /** Unix timestamp when the session ended (null if still active) */
    val endTime: Long? = null,

    /** Total duration in milliseconds */
    val totalDuration: Long = 0,

    /** Average score across all strokes (1-10) */
    val avgScore: Float = 0f,

    /** Total number of strokes recorded */
    val totalStrokes: Int = 0,

    /** List of heart rate readings during the session (BPM values) */
    val heartRateData: List<HeartRateReading> = emptyList(),

    /** Average heart rate during the session */
    val avgHeartRate: Int? = null,

    /** Maximum heart rate during the session */
    val maxHeartRate: Int? = null,

    /** Estimated calories burned */
    val caloriesBurned: Float? = null,

    /** User notes about the session */
    val notes: String? = null,

    /** Whether the session was synced to cloud */
    val isSynced: Boolean = false
)

/**
 * Heart rate reading with timestamp.
 */
data class HeartRateReading(
    val timestamp: Long,
    val bpm: Int
)

/**
 * Session state for UI representation.
 */
enum class SessionState {
    IDLE,
    STARTING,
    ACTIVE,
    PAUSED,
    STOPPING,
    COMPLETED
}

/**
 * Summary statistics for a training session.
 */
data class SessionSummary(
    val sessionId: Long,
    val date: Long,
    val duration: Long,
    val totalStrokes: Int,
    val avgScore: Float,
    val avgHeartRate: Int?,
    val strokeDistribution: Map<String, Int>
)

