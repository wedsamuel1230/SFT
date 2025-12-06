package smartracket.com.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import smartracket.com.model.Stroke

/**
 * Data Access Object for Stroke entities.
 *
 * Provides CRUD operations and queries for individual stroke data.
 */
@Dao
interface StrokeDao {

    // ============= Insert Operations =============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stroke: Stroke): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(strokes: List<Stroke>): List<Long>

    // ============= Update Operations =============

    @Update
    suspend fun update(stroke: Stroke)

    // ============= Delete Operations =============

    @Delete
    suspend fun delete(stroke: Stroke)

    @Query("DELETE FROM strokes WHERE strokeId = :strokeId")
    suspend fun deleteById(strokeId: Long)

    @Query("DELETE FROM strokes WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)

    // ============= Query Operations =============

    @Query("SELECT * FROM strokes WHERE strokeId = :strokeId")
    suspend fun getById(strokeId: Long): Stroke?

    @Query("SELECT * FROM strokes WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getBySessionIdFlow(sessionId: Long): Flow<List<Stroke>>

    @Query("SELECT * FROM strokes WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySessionId(sessionId: Long): List<Stroke>

    @Query("SELECT * FROM strokes WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentBySessionId(sessionId: Long, limit: Int): List<Stroke>

    @Query("SELECT * FROM strokes WHERE sessionId = :sessionId AND strokeType = :strokeType ORDER BY timestamp ASC")
    suspend fun getBySessionIdAndType(sessionId: Long, strokeType: String): List<Stroke>

    @Query("SELECT * FROM strokes WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastStroke(sessionId: Long): Stroke?

    @Query("SELECT * FROM strokes WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    fun getLastStrokeFlow(sessionId: Long): Flow<Stroke?>

    // ============= Aggregation Queries =============

    @Query("SELECT COUNT(*) FROM strokes WHERE sessionId = :sessionId")
    suspend fun getCountBySessionId(sessionId: Long): Int

    @Query("SELECT AVG(score) FROM strokes WHERE sessionId = :sessionId")
    suspend fun getAverageScoreBySessionId(sessionId: Long): Float?

    @Query("SELECT strokeType, COUNT(*) as count FROM strokes WHERE sessionId = :sessionId GROUP BY strokeType")
    suspend fun getStrokeDistribution(sessionId: Long): List<StrokeTypeCount>

    @Query("SELECT MAX(score) FROM strokes WHERE sessionId = :sessionId")
    suspend fun getMaxScoreBySessionId(sessionId: Long): Int?

    @Query("SELECT * FROM strokes WHERE sessionId = :sessionId AND score >= :minScore ORDER BY score DESC")
    suspend fun getHighScoreStrokes(sessionId: Long, minScore: Int): List<Stroke>

    @Query("SELECT strokeType, AVG(score) as avgScore FROM strokes WHERE sessionId = :sessionId GROUP BY strokeType")
    suspend fun getAverageScoreByType(sessionId: Long): List<StrokeTypeScore>

    // ============= Analytics Queries =============

    @Query("""
        SELECT strokeType, COUNT(*) as count 
        FROM strokes 
        WHERE sessionId IN (SELECT sessionId FROM training_sessions WHERE startTime BETWEEN :startDate AND :endDate)
        GROUP BY strokeType
    """)
    suspend fun getStrokeDistributionByDateRange(startDate: Long, endDate: Long): List<StrokeTypeCount>

    @Query("""
        SELECT AVG(score) 
        FROM strokes 
        WHERE strokeType = :strokeType 
        AND sessionId IN (SELECT sessionId FROM training_sessions WHERE startTime BETWEEN :startDate AND :endDate)
    """)
    suspend fun getAverageScoreByTypeAndDateRange(strokeType: String, startDate: Long, endDate: Long): Float?
}

/**
 * Data class for stroke type count query results.
 */
data class StrokeTypeCount(
    val strokeType: String,
    val count: Int
)

/**
 * Data class for stroke type average score query results.
 */
data class StrokeTypeScore(
    val strokeType: String,
    val avgScore: Float
)

