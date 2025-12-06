package smartracket.com.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import smartracket.com.model.TrainingSession

/**
 * Data Access Object for TrainingSession entities.
 *
 * Provides CRUD operations and queries for training session data.
 */
@Dao
interface TrainingSessionDao {

    // ============= Insert Operations =============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: TrainingSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<TrainingSession>): List<Long>

    // ============= Update Operations =============

    @Update
    suspend fun update(session: TrainingSession)

    @Query("UPDATE training_sessions SET endTime = :endTime, totalDuration = :duration, avgScore = :avgScore, totalStrokes = :totalStrokes WHERE sessionId = :sessionId")
    suspend fun updateSessionEnd(sessionId: Long, endTime: Long, duration: Long, avgScore: Float, totalStrokes: Int)

    @Query("UPDATE training_sessions SET avgHeartRate = :avgHr, maxHeartRate = :maxHr, caloriesBurned = :calories WHERE sessionId = :sessionId")
    suspend fun updateHealthData(sessionId: Long, avgHr: Int?, maxHr: Int?, calories: Float?)

    @Query("UPDATE training_sessions SET notes = :notes WHERE sessionId = :sessionId")
    suspend fun updateNotes(sessionId: Long, notes: String?)

    // ============= Delete Operations =============

    @Delete
    suspend fun delete(session: TrainingSession)

    @Query("DELETE FROM training_sessions WHERE sessionId = :sessionId")
    suspend fun deleteById(sessionId: Long)

    @Query("DELETE FROM training_sessions WHERE startTime < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)

    // ============= Query Operations =============

    @Query("SELECT * FROM training_sessions WHERE sessionId = :sessionId")
    suspend fun getById(sessionId: Long): TrainingSession?

    @Query("SELECT * FROM training_sessions WHERE sessionId = :sessionId")
    fun getByIdFlow(sessionId: Long): Flow<TrainingSession?>

    @Query("SELECT * FROM training_sessions ORDER BY startTime DESC")
    fun getAllFlow(): Flow<List<TrainingSession>>

    @Query("SELECT * FROM training_sessions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentFlow(limit: Int): Flow<List<TrainingSession>>

    @Query("SELECT * FROM training_sessions ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<TrainingSession>

    @Query("SELECT * FROM training_sessions WHERE startTime BETWEEN :startDate AND :endDate ORDER BY startTime DESC")
    fun getByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<TrainingSession>>

    @Query("SELECT * FROM training_sessions WHERE startTime BETWEEN :startDate AND :endDate ORDER BY startTime DESC")
    suspend fun getByDateRange(startDate: Long, endDate: Long): List<TrainingSession>

    @Query("SELECT * FROM training_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): TrainingSession?

    @Query("SELECT * FROM training_sessions WHERE endTime IS NULL")
    fun getActiveSessionFlow(): Flow<TrainingSession?>

    // ============= Aggregation Queries =============

    @Query("SELECT COUNT(*) FROM training_sessions")
    suspend fun getTotalCount(): Int

    @Query("SELECT SUM(totalStrokes) FROM training_sessions")
    suspend fun getTotalStrokesAllTime(): Int?

    @Query("SELECT AVG(avgScore) FROM training_sessions WHERE avgScore > 0")
    suspend fun getAverageScoreAllTime(): Float?

    @Query("SELECT SUM(totalDuration) FROM training_sessions")
    suspend fun getTotalTrainingTimeMs(): Long?

    @Query("""
        SELECT * FROM training_sessions 
        WHERE DATE(startTime / 1000, 'unixepoch', 'localtime') = DATE(:dateMs / 1000, 'unixepoch', 'localtime')
        ORDER BY startTime DESC
    """)
    suspend fun getSessionsForDate(dateMs: Long): List<TrainingSession>

    @Query("""
        SELECT SUM(totalStrokes) FROM training_sessions 
        WHERE DATE(startTime / 1000, 'unixepoch', 'localtime') = DATE(:dateMs / 1000, 'unixepoch', 'localtime')
    """)
    suspend fun getTotalStrokesForDate(dateMs: Long): Int?

    @Query("""
        SELECT AVG(avgScore) FROM training_sessions 
        WHERE avgScore > 0 AND DATE(startTime / 1000, 'unixepoch', 'localtime') = DATE(:dateMs / 1000, 'unixepoch', 'localtime')
    """)
    suspend fun getAverageScoreForDate(dateMs: Long): Float?
}

