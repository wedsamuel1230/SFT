package smartracket.com.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import smartracket.com.model.BluetoothConnectionState
import smartracket.com.model.TrainingSession
import smartracket.com.repository.BloodPressureReading
import smartracket.com.repository.BluetoothRepository
import smartracket.com.repository.HealthRepository
import smartracket.com.repository.TrainingRepository
import smartracket.com.repository.FirebaseStatsGateway
import smartracket.com.repository.SessionMergeService
import smartracket.com.ui.screens.AllTimeStatsUi
import smartracket.com.ui.screens.RecentSessionUi
import smartracket.com.ui.screens.TodaySummaryUi
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for the Home Screen.
 *
 * Provides:
 * - Today's training summary
 * - All-time statistics
 * - Recent sessions list
 * - Bluetooth connection state
 * 
 * NOTE: Injections for firebaseStatsGateway and sessionMergeService are ready
 * for Phase D ViewModel wiring (currently using local-only stats until merged
 * stats loading is fully integrated).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val healthRepository: HealthRepository,
    private val firebaseStatsGateway: FirebaseStatsGateway,
    private val sessionMergeService: SessionMergeService
) : ViewModel() {

    // Bluetooth connection state
    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothRepository.connectionState

    // Current heart rate
    val currentHeartRate: StateFlow<Int?> = healthRepository.currentHeartRate

    // Current blood pressure
    val currentBloodPressure: StateFlow<BloodPressureReading?> = healthRepository.currentBloodPressure

    // Samsung Health connection status
    val isSamsungHealthConnected: StateFlow<Boolean> = healthRepository.isSamsungHealthConnected

    // Today's summary
    private val _todaySummary = MutableStateFlow(TodaySummaryUi())
    val todaySummary: StateFlow<TodaySummaryUi> = _todaySummary.asStateFlow()

    // All-time stats
    private val _allTimeStats = MutableStateFlow(AllTimeStatsUi())
    val allTimeStats: StateFlow<AllTimeStatsUi> = _allTimeStats.asStateFlow()

    // Recent sessions
    private val _recentSessions = MutableStateFlow<List<RecentSessionUi>>(emptyList())
    val recentSessions: StateFlow<List<RecentSessionUi>> = _recentSessions.asStateFlow()

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    init {
        loadData()

        // Observe recent sessions
        viewModelScope.launch {
            trainingRepository.getRecentSessionsFlow(5).collect { sessions ->
                _recentSessions.value = sessions.map { it.toUiModel() }
            }
        }
    }

    /**
     * Load initial data.
     * TODO: Phase D - Update to use merged stats from Firebase + local Room
     */
    private fun loadData() {
        viewModelScope.launch {
            // Load today's summary
            val summary = trainingRepository.getTodaySummary()
            _todaySummary.value = TodaySummaryUi(
                totalStrokes = summary.totalStrokes,
                avgScore = summary.avgScore,
                sessionsCount = summary.sessionsCount
            )

            // Load all-time stats (merged from local Room + cloud Firestore)
            loadMergedAllTimeStats()
        }
    }

    /**
     * Load all-time stats by merging local Room sessions with cloud Firestore sessions.
     * This enables stats visibility after local cleanup via cloud backup.
     * Called from loadData() as part of Phase D ViewModel wiring.
     */
    private suspend fun loadMergedAllTimeStats() {
        try {
            // Fetch local sessions from Room database
            val localSessions = trainingRepository.getAllSessions()
            
            // Fetch cloud sessions from Firestore (gracefully returns empty if unavailable)
            val cloudSessions = firebaseStatsGateway.getCloudSessions()
            
            // Merge: dedup by sessionId, prefer local, include cloud-only
            val mergedSessions = sessionMergeService.mergeSessions(localSessions, cloudSessions)
            
            // Compute stats from merged sessions
            val stats = computeAllTimeStatsFromSessions(mergedSessions)
            _allTimeStats.value = stats
        } catch (e: Exception) {
            // Fallback: use local stats only if merge fails
            val localStats = trainingRepository.getAllTimeStats()
            _allTimeStats.value = AllTimeStatsUi(
                totalSessions = localStats.totalSessions,
                totalStrokes = localStats.totalStrokes,
                avgScore = localStats.avgScore,
                totalTrainingTimeMs = localStats.totalTrainingTimeMs
            )
        }
    }

    /**
     * Compute all-time training statistics from a list of training sessions.
     * Used by merged stats loading to calculate stats from combined local + cloud sessions.
     */
    private fun computeAllTimeStatsFromSessions(sessions: List<TrainingSession>): AllTimeStatsUi {
        if (sessions.isEmpty()) {
            return AllTimeStatsUi()
        }
        
        val totalSessions = sessions.size
        val totalStrokes = sessions.sumOf { it.totalStrokes }
        val totalScore = sessions.sumOf { (it.avgScore * it.totalStrokes).toLong() }
        val avgScore = if (totalStrokes > 0) (totalScore.toFloat() / totalStrokes.toFloat()) else 0f
        val totalTrainingTimeMs = sessions.sumOf { it.totalDuration }
        
        return AllTimeStatsUi(
            totalSessions = totalSessions,
            totalStrokes = totalStrokes,
            avgScore = avgScore,
            totalTrainingTimeMs = totalTrainingTimeMs
        )
    }
    /**
     * Start Bluetooth scan for devices.
     */
    fun startScan() {
        bluetoothRepository.startScan()
    }

    /**
     * Refresh data.
     */
    fun refresh() {
        loadData()
    }

    private fun TrainingSession.toUiModel(): RecentSessionUi {
        val durationMinutes = totalDuration / (1000 * 60)
        return RecentSessionUi(
            sessionId = sessionId,
            dateFormatted = dateFormatter.format(Date(startTime)),
            totalStrokes = totalStrokes,
            avgScore = avgScore,
            durationFormatted = "${durationMinutes}min"
        )
    }
}

