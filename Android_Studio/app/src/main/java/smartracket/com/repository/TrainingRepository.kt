package smartracket.com.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import smartracket.com.db.SmartRacketDatabase
import smartracket.com.db.StrokeTypeCount
import smartracket.com.model.*
import smartracket.com.utils.StrokeClassifier
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for training session and stroke data.
 *
 * Manages:
 * - Training session lifecycle (start, update, end)
 * - Stroke recording and classification
 * - Session queries and analytics
 */
@Singleton
class TrainingRepository @Inject constructor(
    private val database: SmartRacketDatabase,
    private val strokeClassifier: StrokeClassifier
) {
    private val sessionDao = database.trainingSessionDao()
    private val strokeDao = database.strokeDao()

    // Current active session
    private var activeSessionId: Long? = null

    // ============= Session Management =============

    /**
     * Start a new training session.
     */
    suspend fun startSession(): TrainingSession {
        // End any existing active session
        getActiveSession()?.let { endSession(it.sessionId) }

        val session = TrainingSession(
            startTime = System.currentTimeMillis()
        )

        val sessionId = sessionDao.insert(session)
        activeSessionId = sessionId

        return session.copy(sessionId = sessionId)
    }

    /**
     * End a training session and calculate final statistics.
     */
    suspend fun endSession(sessionId: Long): TrainingSession? {
        val session = sessionDao.getById(sessionId) ?: return null

        val endTime = System.currentTimeMillis()
        val duration = endTime - session.startTime
        val strokes = strokeDao.getBySessionId(sessionId)
        val avgScore = strokes.map { it.score }.average().toFloat()

        sessionDao.updateSessionEnd(
            sessionId = sessionId,
            endTime = endTime,
            duration = duration,
            avgScore = if (avgScore.isNaN()) 0f else avgScore,
            totalStrokes = strokes.size
        )

        activeSessionId = null

        return sessionDao.getById(sessionId)
    }

    /**
     * Get the currently active session.
     */
    suspend fun getActiveSession(): TrainingSession? {
        return sessionDao.getActiveSession()
    }

    /**
     * Get active session as a Flow.
     */
    fun getActiveSessionFlow(): Flow<TrainingSession?> {
        return sessionDao.getActiveSessionFlow()
    }

    /**
     * Get a session by ID.
     */
    suspend fun getSession(sessionId: Long): TrainingSession? {
        return sessionDao.getById(sessionId)
    }

    /**
     * Get session by ID as a Flow.
     */
    fun getSessionFlow(sessionId: Long): Flow<TrainingSession?> {
        return sessionDao.getByIdFlow(sessionId)
    }

    /**
     * Get all sessions.
     */
    fun getAllSessionsFlow(): Flow<List<TrainingSession>> {
        return sessionDao.getAllFlow()
    }

    /**
     * Get recent sessions.
     */
    fun getRecentSessionsFlow(limit: Int = 10): Flow<List<TrainingSession>> {
        return sessionDao.getRecentFlow(limit)
    }

    /**
     * Get sessions by date range.
     */
    fun getSessionsByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<TrainingSession>> {
        return sessionDao.getByDateRangeFlow(startDate, endDate)
    }

    /**
     * Update session notes.
     */
    suspend fun updateSessionNotes(sessionId: Long, notes: String?) {
        sessionDao.updateNotes(sessionId, notes)
    }

    /**
     * Update session health data.
     */
    suspend fun updateSessionHealthData(
        sessionId: Long,
        avgHeartRate: Int?,
        maxHeartRate: Int?,
        caloriesBurned: Float?
    ) {
        sessionDao.updateHealthData(sessionId, avgHeartRate, maxHeartRate, caloriesBurned)
    }

    /**
     * Delete a session.
     */
    suspend fun deleteSession(sessionId: Long) {
        sessionDao.deleteById(sessionId)
    }

    // ============= Stroke Management =============

    /**
     * Classify motion data and record the stroke.
     */
    suspend fun recordStroke(sessionId: Long, motionData: MotionData): Stroke {
        // Classify the stroke
        val classification = strokeClassifier.classify(motionData)

        val stroke = Stroke(
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            strokeType = classification.strokeType.name,
            score = classification.score,
            motionData = motionData,
            feedback = classification.feedback,
            confidence = classification.confidence,
            peakAcceleration = calculatePeakAcceleration(motionData),
            strokeDuration = calculateStrokeDuration(motionData)
        )

        val strokeId = strokeDao.insert(stroke)
        return stroke.copy(strokeId = strokeId)
    }

    /**
     * Get strokes for a session.
     */
    suspend fun getStrokesForSession(sessionId: Long): List<Stroke> {
        return strokeDao.getBySessionId(sessionId)
    }

    /**
     * Get strokes for a session as a Flow.
     */
    fun getStrokesForSessionFlow(sessionId: Long): Flow<List<Stroke>> {
        return strokeDao.getBySessionIdFlow(sessionId)
    }

    /**
     * Get the last stroke from a session.
     */
    suspend fun getLastStroke(sessionId: Long): Stroke? {
        return strokeDao.getLastStroke(sessionId)
    }

    /**
     * Get the last stroke as a Flow.
     */
    fun getLastStrokeFlow(sessionId: Long): Flow<Stroke?> {
        return strokeDao.getLastStrokeFlow(sessionId)
    }

    /**
     * Get stroke distribution for a session.
     */
    suspend fun getStrokeDistribution(sessionId: Long): Map<String, Int> {
        return strokeDao.getStrokeDistribution(sessionId)
            .associate { it.strokeType to it.count }
    }

    /**
     * Get high-score strokes from a session.
     */
    suspend fun getHighScoreStrokes(sessionId: Long, minScore: Int = 8): List<Stroke> {
        return strokeDao.getHighScoreStrokes(sessionId, minScore)
    }

    // ============= Analytics =============

    /**
     * Get today's summary statistics.
     */
    suspend fun getTodaySummary(): TodaySummary {
        val today = System.currentTimeMillis()

        return TodaySummary(
            totalStrokes = sessionDao.getTotalStrokesForDate(today) ?: 0,
            avgScore = sessionDao.getAverageScoreForDate(today) ?: 0f,
            sessionsCount = sessionDao.getSessionsForDate(today).size
        )
    }

    /**
     * Get all-time statistics.
     */
    suspend fun getAllTimeStats(): AllTimeStats {
        return AllTimeStats(
            totalSessions = sessionDao.getTotalCount(),
            totalStrokes = sessionDao.getTotalStrokesAllTime() ?: 0,
            avgScore = sessionDao.getAverageScoreAllTime() ?: 0f,
            totalTrainingTimeMs = sessionDao.getTotalTrainingTimeMs() ?: 0
        )
    }

    /**
     * Get stroke type distribution over a date range.
     */
    suspend fun getStrokeDistributionByDateRange(
        startDate: Long,
        endDate: Long
    ): Map<String, Int> {
        return strokeDao.getStrokeDistributionByDateRange(startDate, endDate)
            .associate { it.strokeType to it.count }
    }

    /**
     * Get score trend for a specific stroke type.
     */
    suspend fun getScoreTrendByStrokeType(
        strokeType: String,
        startDate: Long,
        endDate: Long
    ): Float? {
        return strokeDao.getAverageScoreByTypeAndDateRange(strokeType, startDate, endDate)
    }

    // ============= Helper Methods =============

    private fun calculatePeakAcceleration(motionData: MotionData): Float {
        if (motionData.accelX.isEmpty()) return 0f

        return motionData.accelX.indices.maxOfOrNull { i ->
            val ax = motionData.accelX.getOrElse(i) { 0f }
            val ay = motionData.accelY.getOrElse(i) { 0f }
            val az = motionData.accelZ.getOrElse(i) { 0f }
            kotlin.math.sqrt(ax * ax + ay * ay + az * az)
        } ?: 0f
    }

    private fun calculateStrokeDuration(motionData: MotionData): Long {
        if (motionData.timestamps.isEmpty()) return 0
        return motionData.timestamps.last() - motionData.timestamps.first()
    }

    /**
     * Initialize the stroke classifier.
     */
    suspend fun initializeClassifier(): Boolean {
        return strokeClassifier.initialize()
    }

    /**
     * Check if classifier is ready.
     */
    fun isClassifierReady(): Boolean {
        return strokeClassifier.isReady()
    }
}

/**
 * Today's training summary.
 */
data class TodaySummary(
    val totalStrokes: Int,
    val avgScore: Float,
    val sessionsCount: Int
)

/**
 * All-time training statistics.
 */
data class AllTimeStats(
    val totalSessions: Int,
    val totalStrokes: Int,
    val avgScore: Float,
    val totalTrainingTimeMs: Long
)

