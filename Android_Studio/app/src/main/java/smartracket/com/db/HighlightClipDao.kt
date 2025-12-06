package smartracket.com.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import smartracket.com.model.HighlightClip

/**
 * Data Access Object for HighlightClip entities.
 *
 * Provides CRUD operations and queries for saved highlight clips.
 */
@Dao
interface HighlightClipDao {

    // ============= Insert Operations =============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(clip: HighlightClip): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clips: List<HighlightClip>): List<Long>

    // ============= Update Operations =============

    @Update
    suspend fun update(clip: HighlightClip)

    @Query("UPDATE highlight_clips SET title = :title WHERE clipId = :clipId")
    suspend fun updateTitle(clipId: Long, title: String)

    @Query("UPDATE highlight_clips SET isShared = :isShared WHERE clipId = :clipId")
    suspend fun updateSharedStatus(clipId: Long, isShared: Boolean)

    @Query("UPDATE highlight_clips SET thumbnailUri = :thumbnailUri WHERE clipId = :clipId")
    suspend fun updateThumbnail(clipId: Long, thumbnailUri: String)

    // ============= Delete Operations =============

    @Delete
    suspend fun delete(clip: HighlightClip)

    @Query("DELETE FROM highlight_clips WHERE clipId = :clipId")
    suspend fun deleteById(clipId: Long)

    @Query("DELETE FROM highlight_clips WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)

    // ============= Query Operations =============

    @Query("SELECT * FROM highlight_clips WHERE clipId = :clipId")
    suspend fun getById(clipId: Long): HighlightClip?

    @Query("SELECT * FROM highlight_clips WHERE clipId = :clipId")
    fun getByIdFlow(clipId: Long): Flow<HighlightClip?>

    @Query("SELECT * FROM highlight_clips ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<HighlightClip>>

    @Query("SELECT * FROM highlight_clips ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentFlow(limit: Int): Flow<List<HighlightClip>>

    @Query("SELECT * FROM highlight_clips ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<HighlightClip>

    @Query("SELECT * FROM highlight_clips WHERE sessionId = :sessionId ORDER BY clipStartTime ASC")
    fun getBySessionIdFlow(sessionId: Long): Flow<List<HighlightClip>>

    @Query("SELECT * FROM highlight_clips WHERE sessionId = :sessionId ORDER BY clipStartTime ASC")
    suspend fun getBySessionId(sessionId: Long): List<HighlightClip>

    @Query("SELECT * FROM highlight_clips WHERE createdAt BETWEEN :startDate AND :endDate ORDER BY createdAt DESC")
    fun getByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<HighlightClip>>

    @Query("SELECT * FROM highlight_clips WHERE createdAt BETWEEN :startDate AND :endDate ORDER BY createdAt DESC")
    suspend fun getByDateRange(startDate: Long, endDate: Long): List<HighlightClip>

    // ============= Filter Queries =============

    @Query("SELECT * FROM highlight_clips WHERE isAutoSaved = :isAutoSaved ORDER BY createdAt DESC")
    fun getByAutoSaveStatusFlow(isAutoSaved: Boolean): Flow<List<HighlightClip>>

    @Query("SELECT * FROM highlight_clips WHERE isShared = 1 ORDER BY createdAt DESC")
    fun getSharedClipsFlow(): Flow<List<HighlightClip>>

    // ============= Aggregation Queries =============

    @Query("SELECT COUNT(*) FROM highlight_clips")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM highlight_clips WHERE sessionId = :sessionId")
    suspend fun getCountBySessionId(sessionId: Long): Int

    @Query("SELECT COUNT(*) FROM highlight_clips WHERE isAutoSaved = 1")
    suspend fun getAutoSavedCount(): Int
}

