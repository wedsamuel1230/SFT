package smartracket.com.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import smartracket.com.db.Converters

/**
 * Represents a saved highlight clip from a training session.
 *
 * Highlights are captured when a stroke exceeds the score threshold
 * or when the user manually taps the save button.
 */
@Entity(
    tableName = "highlight_clips",
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
data class HighlightClip(
    @PrimaryKey(autoGenerate = true)
    val clipId: Long = 0,

    /** Reference to the parent training session */
    val sessionId: Long,

    /** Unix timestamp when the clip capture started */
    val clipStartTime: Long,

    /** Unix timestamp when the clip capture ended */
    val clipEndTime: Long,

    /** URI or path to the thumbnail image */
    val thumbnailUri: String? = null,

    /** JSON metadata containing score, stroke type, etc. */
    val metadata: HighlightMetadata,

    /** Whether this was auto-saved (score threshold) or manual */
    val isAutoSaved: Boolean = false,

    /** User-provided title for the highlight */
    val title: String? = null,

    /** Whether this highlight has been shared */
    val isShared: Boolean = false,

    /** Creation timestamp */
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Metadata associated with a highlight clip.
 */
data class HighlightMetadata(
    /** Score of the highlighted stroke */
    val score: Int,

    /** Type of stroke that triggered the highlight */
    val strokeType: String,

    /** Confidence of the classification */
    val confidence: Float,

    /** Heart rate at the moment (if available) */
    val heartRate: Int? = null,

    /** Feedback provided for the stroke */
    val feedback: String? = null,

    /** Motion data summary for replay */
    val motionSummary: MotionSummary? = null
)

/**
 * Summarized motion data for highlight replay.
 */
data class MotionSummary(
    /** Peak acceleration magnitude */
    val peakAcceleration: Float,

    /** Average angular velocity */
    val avgAngularVelocity: Float,

    /** Stroke duration in milliseconds */
    val duration: Long,

    /** Key motion points for visualization */
    val keyPoints: List<MotionKeyPoint> = emptyList()
)

/**
 * Key point in motion trajectory for visualization.
 */
data class MotionKeyPoint(
    val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float
)

/**
 * Circular buffer entry for highlight capture.
 * Stores recent motion data for the last 3 minutes.
 */
data class HighlightBufferEntry(
    val timestamp: Long,
    val motionData: MotionData,
    val heartRate: Int? = null,
    val strokeInfo: StrokeBufferInfo? = null
)

/**
 * Stroke information stored in the buffer.
 */
data class StrokeBufferInfo(
    val strokeType: String,
    val score: Int,
    val confidence: Float,
    val feedback: String
)

