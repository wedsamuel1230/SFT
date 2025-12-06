package smartracket.com.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import smartracket.com.model.BluetoothConnectionState
import smartracket.com.model.TrainingSession
import smartracket.com.repository.BluetoothRepository
import smartracket.com.repository.HealthRepository
import smartracket.com.repository.TrainingRepository
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
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val healthRepository: HealthRepository
) : ViewModel() {

    // Bluetooth connection state
    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothRepository.connectionState

    // Current heart rate
    val currentHeartRate: StateFlow<Int?> = healthRepository.currentHeartRate

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

            // Load all-time stats
            val stats = trainingRepository.getAllTimeStats()
            _allTimeStats.value = AllTimeStatsUi(
                totalSessions = stats.totalSessions,
                totalStrokes = stats.totalStrokes,
                avgScore = stats.avgScore,
                totalTrainingTimeMs = stats.totalTrainingTimeMs
            )
        }
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

