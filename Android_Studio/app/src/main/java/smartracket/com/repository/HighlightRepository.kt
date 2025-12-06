package smartracket.com.repository

import kotlinx.coroutines.flow.Flow
import smartracket.com.db.SmartRacketDatabase
import smartracket.com.model.*
import java.util.concurrent.ConcurrentLinkedDeque
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for highlight clip management.
 *
 * Manages:
 * - Circular buffer for last 3 minutes of motion data
 * - Highlight clip creation (manual and automatic)
 * - Highlight storage and retrieval
 */
@Singleton
class HighlightRepository @Inject constructor(
    private val database: SmartRacketDatabase
) {
    private val highlightClipDao = database.highlightClipDao()

    companion object {
        private const val BUFFER_DURATION_MS = 3 * 60 * 1000L  // 3 minutes
        private const val AUTO_SAVE_SCORE_THRESHOLD = 8
        private const val CLIP_DURATION_MS = 10 * 1000L  // 10 seconds per clip
    }

    // Circular buffer for motion data (thread-safe)
    private val motionBuffer = ConcurrentLinkedDeque<HighlightBufferEntry>()

    // ============= Buffer Management =============

    /**
     * Add motion data to the circular buffer.
     */
    fun addToBuffer(
        motionData: MotionData,
        heartRate: Int? = null,
        strokeInfo: StrokeBufferInfo? = null
    ) {
        val entry = HighlightBufferEntry(
            timestamp = System.currentTimeMillis(),
            motionData = motionData,
            heartRate = heartRate,
            strokeInfo = strokeInfo
        )

        motionBuffer.addLast(entry)

        // Remove old entries (older than 3 minutes)
        val cutoffTime = System.currentTimeMillis() - BUFFER_DURATION_MS
        while (motionBuffer.peekFirst()?.timestamp ?: Long.MAX_VALUE < cutoffTime) {
            motionBuffer.pollFirst()
        }
    }

    /**
     * Check if a stroke qualifies for auto-save.
     */
    fun shouldAutoSave(score: Int): Boolean {
        return score >= AUTO_SAVE_SCORE_THRESHOLD
    }

    /**
     * Clear the motion buffer.
     */
    fun clearBuffer() {
        motionBuffer.clear()
    }

    /**
     * Get buffer entries around a timestamp.
     */
    private fun getBufferEntriesAround(timestamp: Long): List<HighlightBufferEntry> {
        val halfDuration = CLIP_DURATION_MS / 2
        val startTime = timestamp - halfDuration
        val endTime = timestamp + halfDuration

        return motionBuffer.filter { it.timestamp in startTime..endTime }
    }

    // ============= Highlight Creation =============

    /**
     * Create a highlight clip for a stroke.
     *
     * @param sessionId The current session ID
     * @param strokeInfo Information about the stroke
     * @param isAutoSaved Whether this was auto-saved or manual
     */
    suspend fun createHighlight(
        sessionId: Long,
        strokeInfo: StrokeBufferInfo,
        heartRate: Int? = null,
        isAutoSaved: Boolean = false
    ): HighlightClip {
        val currentTime = System.currentTimeMillis()
        val clipStartTime = currentTime - (CLIP_DURATION_MS / 2)
        val clipEndTime = currentTime + (CLIP_DURATION_MS / 2)

        // Get buffer entries for motion summary
        val bufferEntries = getBufferEntriesAround(currentTime)
        val motionSummary = createMotionSummary(bufferEntries)

        val metadata = HighlightMetadata(
            score = strokeInfo.score,
            strokeType = strokeInfo.strokeType,
            confidence = strokeInfo.confidence,
            heartRate = heartRate,
            feedback = strokeInfo.feedback,
            motionSummary = motionSummary
        )

        val clip = HighlightClip(
            sessionId = sessionId,
            clipStartTime = clipStartTime,
            clipEndTime = clipEndTime,
            metadata = metadata,
            isAutoSaved = isAutoSaved,
            title = generateHighlightTitle(strokeInfo)
        )

        val clipId = highlightClipDao.insert(clip)
        return clip.copy(clipId = clipId)
    }

    /**
     * Create motion summary from buffer entries.
     */
    private fun createMotionSummary(entries: List<HighlightBufferEntry>): MotionSummary? {
        if (entries.isEmpty()) return null

        var peakAccel = 0f
        var totalAngularVel = 0f
        val keyPoints = mutableListOf<MotionKeyPoint>()

        entries.forEach { entry ->
            val motionData = entry.motionData

            // Calculate peak acceleration
            motionData.accelX.indices.forEach { i ->
                val ax = motionData.accelX.getOrElse(i) { 0f }
                val ay = motionData.accelY.getOrElse(i) { 0f }
                val az = motionData.accelZ.getOrElse(i) { 0f }
                val accel = kotlin.math.sqrt(ax * ax + ay * ay + az * az)
                if (accel > peakAccel) peakAccel = accel
            }

            // Calculate angular velocity
            motionData.gyroX.indices.forEach { i ->
                val gx = motionData.gyroX.getOrElse(i) { 0f }
                val gy = motionData.gyroY.getOrElse(i) { 0f }
                val gz = motionData.gyroZ.getOrElse(i) { 0f }
                totalAngularVel += kotlin.math.sqrt(gx * gx + gy * gy + gz * gz)
            }

            // Add key point (peak moment)
            if (motionData.accelX.isNotEmpty()) {
                val peakIndex = motionData.accelX.indices.maxByOrNull { i ->
                    val ax = motionData.accelX.getOrElse(i) { 0f }
                    val ay = motionData.accelY.getOrElse(i) { 0f }
                    val az = motionData.accelZ.getOrElse(i) { 0f }
                    ax * ax + ay * ay + az * az
                } ?: 0

                keyPoints.add(MotionKeyPoint(
                    timestamp = entry.timestamp,
                    x = motionData.accelX.getOrElse(peakIndex) { 0f },
                    y = motionData.accelY.getOrElse(peakIndex) { 0f },
                    z = motionData.accelZ.getOrElse(peakIndex) { 0f }
                ))
            }
        }

        val totalSamples = entries.sumOf { it.motionData.gyroX.size }
        val avgAngularVel = if (totalSamples > 0) totalAngularVel / totalSamples else 0f

        val duration = if (entries.isNotEmpty()) {
            entries.last().timestamp - entries.first().timestamp
        } else 0L

        return MotionSummary(
            peakAcceleration = peakAccel,
            avgAngularVelocity = avgAngularVel,
            duration = duration,
            keyPoints = keyPoints.take(10)  // Limit key points
        )
    }

    /**
     * Generate a title for the highlight.
     */
    private fun generateHighlightTitle(strokeInfo: StrokeBufferInfo): String {
        val strokeType = StrokeType.fromString(strokeInfo.strokeType)
        return "${strokeType.displayName} - Score ${strokeInfo.score}"
    }

    // ============= Highlight Queries =============

    /**
     * Get all highlights.
     */
    fun getAllHighlightsFlow(): Flow<List<HighlightClip>> {
        return highlightClipDao.getAllFlow()
    }

    /**
     * Get recent highlights.
     */
    fun getRecentHighlightsFlow(limit: Int = 20): Flow<List<HighlightClip>> {
        return highlightClipDao.getRecentFlow(limit)
    }

    /**
     * Get highlights for a session.
     */
    fun getHighlightsForSessionFlow(sessionId: Long): Flow<List<HighlightClip>> {
        return highlightClipDao.getBySessionIdFlow(sessionId)
    }

    /**
     * Get highlights by date range.
     */
    fun getHighlightsByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<HighlightClip>> {
        return highlightClipDao.getByDateRangeFlow(startDate, endDate)
    }

    /**
     * Get a specific highlight.
     */
    suspend fun getHighlight(clipId: Long): HighlightClip? {
        return highlightClipDao.getById(clipId)
    }

    /**
     * Get auto-saved highlights.
     */
    fun getAutoSavedHighlightsFlow(): Flow<List<HighlightClip>> {
        return highlightClipDao.getByAutoSaveStatusFlow(true)
    }

    // ============= Highlight Updates =============

    /**
     * Update highlight title.
     */
    suspend fun updateHighlightTitle(clipId: Long, title: String) {
        highlightClipDao.updateTitle(clipId, title)
    }

    /**
     * Mark highlight as shared.
     */
    suspend fun markAsShared(clipId: Long) {
        highlightClipDao.updateSharedStatus(clipId, true)
    }

    /**
     * Update highlight thumbnail.
     */
    suspend fun updateThumbnail(clipId: Long, thumbnailUri: String) {
        highlightClipDao.updateThumbnail(clipId, thumbnailUri)
    }

    /**
     * Delete a highlight.
     */
    suspend fun deleteHighlight(clipId: Long) {
        highlightClipDao.deleteById(clipId)
    }

    // ============= Statistics =============

    /**
     * Get total highlight count.
     */
    suspend fun getTotalHighlightCount(): Int {
        return highlightClipDao.getTotalCount()
    }

    /**
     * Get highlight count for a session.
     */
    suspend fun getHighlightCountForSession(sessionId: Long): Int {
        return highlightClipDao.getCountBySessionId(sessionId)
    }

    /**
     * Get auto-saved highlight count.
     */
    suspend fun getAutoSavedCount(): Int {
        return highlightClipDao.getAutoSavedCount()
    }
}

