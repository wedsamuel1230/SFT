package smartracket.com.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import smartracket.com.db.Converters

/**
 * Represents a single stroke during a training session.
 *
 * Contains classification results from the TensorFlow Lite model,
 * raw motion data, and performance feedback.
 */
@Entity(
    tableName = "strokes",
    foreignKeys = [
        ForeignKey(
            entity = TrainingSession::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
@TypeConverters(Converters::class)
data class Stroke(
    @PrimaryKey(autoGenerate = true)
    val strokeId: Long = 0,

    /** Reference to the parent training session */
    val sessionId: Long,

    /** Unix timestamp when the stroke was detected */
    val timestamp: Long,

    /** Classified stroke type (e.g., "forehand_loop", "backhand_block", "chop") */
    val strokeType: String,

    /** Performance score from 1-10 */
    val score: Int,

    /** Raw IMU data: [accelX, accelY, accelZ, gyroX, gyroY, gyroZ] readings */
    val motionData: MotionData,

    /** AI-generated feedback tip for improvement */
    val feedback: String,

    /** Confidence score from the ML model (0.0 - 1.0) */
    val confidence: Float = 0f,

    /** Peak acceleration magnitude during the stroke */
    val peakAcceleration: Float? = null,

    /** Stroke duration in milliseconds */
    val strokeDuration: Long? = null
)

/**
 * Motion data from IMU sensor.
 * Contains accelerometer and gyroscope readings.
 */
data class MotionData(
    /** Accelerometer X-axis readings (m/s²) */
    val accelX: List<Float>,

    /** Accelerometer Y-axis readings (m/s²) */
    val accelY: List<Float>,

    /** Accelerometer Z-axis readings (m/s²) */
    val accelZ: List<Float>,

    /** Gyroscope X-axis readings (rad/s) */
    val gyroX: List<Float>,

    /** Gyroscope Y-axis readings (rad/s) */
    val gyroY: List<Float>,

    /** Gyroscope Z-axis readings (rad/s) */
    val gyroZ: List<Float>,

    /** Timestamps for each reading (relative to stroke start) */
    val timestamps: List<Long> = emptyList()
) {
    companion object {
        fun empty() = MotionData(
            accelX = emptyList(),
            accelY = emptyList(),
            accelZ = emptyList(),
            gyroX = emptyList(),
            gyroY = emptyList(),
            gyroZ = emptyList()
        )
    }
}

/**
 * Enumeration of supported stroke types.
 */
enum class StrokeType(val displayName: String, val description: String) {
    FOREHAND_LOOP("Forehand Loop", "Topspin attack with forward swing"),
    FOREHAND_DRIVE("Forehand Drive", "Fast flat forehand shot"),
    FOREHAND_FLICK("Forehand Flick", "Quick wrist flip over the table"),
    BACKHAND_LOOP("Backhand Loop", "Topspin from backhand side"),
    BACKHAND_DRIVE("Backhand Drive", "Flat backhand attack"),
    BACKHAND_FLICK("Backhand Flick", "Quick backhand flip"),
    FOREHAND_BLOCK("Forehand Block", "Defensive forehand return"),
    BACKHAND_BLOCK("Backhand Block", "Defensive backhand return"),
    FOREHAND_CHOP("Forehand Chop", "Backspin defensive shot"),
    BACKHAND_CHOP("Backhand Chop", "Backspin from backhand"),
    FOREHAND_PUSH("Forehand Push", "Short backspin return"),
    BACKHAND_PUSH("Backhand Push", "Short backspin from backhand"),
    SERVE("Serve", "Service stroke"),
    UNKNOWN("Unknown", "Unclassified stroke");

    companion object {
        fun fromString(value: String): StrokeType {
            return entries.find {
                it.name.equals(value, ignoreCase = true) ||
                it.displayName.equals(value, ignoreCase = true)
            } ?: UNKNOWN
        }
    }
}

/**
 * Result from stroke classification.
 */
data class StrokeClassificationResult(
    val strokeType: StrokeType,
    val confidence: Float,
    val score: Int,
    val feedback: String,
    val allProbabilities: Map<StrokeType, Float> = emptyMap()
)

