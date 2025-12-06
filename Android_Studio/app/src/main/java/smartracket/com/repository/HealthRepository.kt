package smartracket.com.repository

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import smartracket.com.model.HeartRateReading
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Health Connect integration.
 * 
 * Provides access to:
 * - Heart rate data from Samsung Health / Health Connect
 * - Calorie burn data
 * - Exercise session sync
 */
@Singleton
class HealthRepository @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "HealthRepository"
        
        // Required permissions
        val REQUIRED_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(ExerciseSessionRecord::class)
        )
    }
    
    private var healthConnectClient: HealthConnectClient? = null
    
    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()
    
    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()
    
    private val _currentHeartRate = MutableStateFlow<Int?>(null)
    val currentHeartRate: StateFlow<Int?> = _currentHeartRate.asStateFlow()
    
    /**
     * Initialize Health Connect client.
     */
    suspend fun initialize(): Boolean {
        return try {
            val status = HealthConnectClient.getSdkStatus(context)
            _isAvailable.value = status == HealthConnectClient.SDK_AVAILABLE
            
            if (_isAvailable.value) {
                healthConnectClient = HealthConnectClient.getOrCreate(context)
                checkPermissions()
                Log.d(TAG, "Health Connect initialized successfully")
                true
            } else {
                Log.w(TAG, "Health Connect not available (status: $status)")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Health Connect", e)
            _isAvailable.value = false
            false
        }
    }
    
    /**
     * Check if all required permissions are granted.
     */
    suspend fun checkPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            _hasPermissions.value = REQUIRED_PERMISSIONS.all { it in granted }
            _hasPermissions.value
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check permissions", e)
            _hasPermissions.value = false
            false
        }
    }
    
    /**
     * Get heart rate readings for a time range.
     */
    suspend fun getHeartRateReadings(
        startTime: Instant,
        endTime: Instant
    ): List<HeartRateReading> {
        val client = healthConnectClient ?: return emptyList()
        
        return try {
            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            
            val response = client.readRecords(request)
            
            response.records.flatMap { record ->
                record.samples.map { sample ->
                    HeartRateReading(
                        timestamp = sample.time.toEpochMilli(),
                        bpm = sample.beatsPerMinute.toInt()
                    )
                }
            }.sortedBy { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read heart rate", e)
            emptyList()
        }
    }
    
    /**
     * Get the most recent heart rate reading.
     */
    suspend fun getLatestHeartRate(): Int? {
        val endTime = Instant.now()
        val startTime = endTime.minusSeconds(300)  // Last 5 minutes
        
        val readings = getHeartRateReadings(startTime, endTime)
        val latest = readings.lastOrNull()?.bpm
        
        _currentHeartRate.value = latest
        return latest
    }
    
    /**
     * Get total calories burned for a time range.
     */
    suspend fun getCaloriesBurned(
        startTime: Instant,
        endTime: Instant
    ): Float {
        val client = healthConnectClient ?: return 0f
        
        return try {
            val request = ReadRecordsRequest(
                recordType = TotalCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            
            val response = client.readRecords(request)
            
            response.records.sumOf { record ->
                record.energy.inKilocalories
            }.toFloat()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read calories", e)
            0f
        }
    }
    
    /**
     * Get heart rate statistics for a session.
     */
    suspend fun getSessionHeartRateStats(
        startTime: Long,
        endTime: Long
    ): HeartRateStats? {
        val readings = getHeartRateReadings(
            Instant.ofEpochMilli(startTime),
            Instant.ofEpochMilli(endTime)
        )
        
        if (readings.isEmpty()) return null
        
        val bpmValues = readings.map { it.bpm }
        return HeartRateStats(
            average = bpmValues.average().toInt(),
            max = bpmValues.max(),
            min = bpmValues.min(),
            readings = readings
        )
    }
    
    /**
     * Record an exercise session to Health Connect.
     */
    suspend fun recordExerciseSession(
        title: String,
        startTime: Long,
        endTime: Long,
        notes: String? = null
    ): Boolean {
        val client = healthConnectClient ?: return false
        
        return try {
            val exerciseSession = ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(startTime),
                startZoneOffset = ZonedDateTime.now().offset,
                endTime = Instant.ofEpochMilli(endTime),
                endZoneOffset = ZonedDateTime.now().offset,
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS,
                title = title,
                notes = notes
            )
            
            client.insertRecords(listOf(exerciseSession))
            Log.d(TAG, "Exercise session recorded")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record exercise session", e)
            false
        }
    }
    
    /**
     * Get recent exercise sessions.
     */
    suspend fun getRecentExerciseSessions(daysBack: Int = 7): List<ExerciseSessionInfo> {
        val client = healthConnectClient ?: return emptyList()
        
        return try {
            val endTime = Instant.now()
            val startTime = endTime.minusSeconds(daysBack.toLong() * 24 * 60 * 60)
            
            val request = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            
            val response = client.readRecords(request)
            
            response.records
                .filter { it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS }
                .map { record ->
                    ExerciseSessionInfo(
                        id = record.metadata.id,
                        title = record.title ?: "Table Tennis Session",
                        startTime = record.startTime.toEpochMilli(),
                        endTime = record.endTime.toEpochMilli(),
                        notes = record.notes
                    )
                }
                .sortedByDescending { it.startTime }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read exercise sessions", e)
            emptyList()
        }
    }
    
    /**
     * Update current heart rate from external source (e.g., Galaxy Watch).
     */
    fun updateCurrentHeartRate(bpm: Int) {
        _currentHeartRate.value = bpm
    }
}

/**
 * Heart rate statistics for a session.
 */
data class HeartRateStats(
    val average: Int,
    val max: Int,
    val min: Int,
    val readings: List<HeartRateReading>
)

/**
 * Exercise session information.
 */
data class ExerciseSessionInfo(
    val id: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val notes: String?
)

