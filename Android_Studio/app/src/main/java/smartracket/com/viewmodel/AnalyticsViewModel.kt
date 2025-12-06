package smartracket.com.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import smartracket.com.model.Stroke
import smartracket.com.model.StrokeType
import smartracket.com.model.TrainingSession
import smartracket.com.repository.TrainingRepository
import smartracket.com.ui.screens.DateFilterOption
import smartracket.com.ui.screens.ScoreTrendPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for the Analytics Screen.
 *
 * Provides:
 * - Session history with filtering
 * - Stroke distribution statistics
 * - Score trends over time
 * - Detailed session analytics
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository
) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val shortDateFormatter = SimpleDateFormat("MM/dd", Locale.getDefault())

    // Sessions list
    private val _sessions = MutableStateFlow<List<SessionDetailUi>>(emptyList())
    val sessions: StateFlow<List<SessionDetailUi>> = _sessions.asStateFlow()

    // Selected session for detail view
    private val _selectedSession = MutableStateFlow<SessionDetailUi?>(null)
    val selectedSession: StateFlow<SessionDetailUi?> = _selectedSession.asStateFlow()

    // Stroke distribution across all sessions
    private val _strokeDistribution = MutableStateFlow<List<StrokeDistributionItem>>(emptyList())
    val strokeDistribution: StateFlow<List<StrokeDistributionItem>> = _strokeDistribution.asStateFlow()

    // Score trend over time
    private val _scoreTrend = MutableStateFlow<List<ScoreTrendPoint>>(emptyList())
    val scoreTrend: StateFlow<List<ScoreTrendPoint>> = _scoreTrend.asStateFlow()

    // Date filter
    private val _dateFilter = MutableStateFlow(DateFilterOption.ALL)
    val dateFilter: StateFlow<DateFilterOption> = _dateFilter.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()

        // React to date filter changes
        viewModelScope.launch {
            _dateFilter.collect {
                loadData()
            }
        }
    }

    /**
     * Load sessions based on current filter.
     */
    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true

            val (startDate, endDate) = getDateRange(_dateFilter.value)

            trainingRepository.getSessionsByDateRangeFlow(startDate, endDate).collect { sessionList ->
                _sessions.value = sessionList.map { session ->
                    val strokes = trainingRepository.getStrokesForSession(session.sessionId)
                    session.toDetailUi(strokes)
                }

                // Calculate stroke distribution
                calculateStrokeDistribution(sessionList)

                // Calculate score trend
                calculateScoreTrend(sessionList)

                _isLoading.value = false
            }
        }
    }

    /**
     * Select a session for detail view.
     */
    fun selectSession(sessionId: Long) {
        viewModelScope.launch {
            val session = trainingRepository.getSession(sessionId)
            session?.let {
                val strokes = trainingRepository.getStrokesForSession(sessionId)
                _selectedSession.value = it.toDetailUi(strokes)
            }
        }
    }

    /**
     * Clear selected session.
     */
    fun clearSelectedSession() {
        _selectedSession.value = null
    }

    /**
     * Set date filter.
     */
    fun setDateFilter(filter: DateFilterOption) {
        _dateFilter.value = filter
    }

    /**
     * Delete a session.
     */
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            trainingRepository.deleteSession(sessionId)
        }
    }

    private fun getDateRange(filter: DateFilterOption): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis

        val startDate = when (filter) {
            DateFilterOption.ALL -> 0L
            DateFilterOption.WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.timeInMillis
            }
            DateFilterOption.MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.timeInMillis
            }
            DateFilterOption.THREE_MONTHS -> {
                calendar.add(Calendar.MONTH, -3)
                calendar.timeInMillis
            }
        }

        return startDate to endDate
    }

    private suspend fun calculateStrokeDistribution(sessions: List<TrainingSession>) {
        val allStrokes = mutableMapOf<String, Int>()

        sessions.forEach { session ->
            val distribution = trainingRepository.getStrokeDistribution(session.sessionId)
            distribution.forEach { (type, count) ->
                allStrokes[type] = (allStrokes[type] ?: 0) + count
            }
        }

        val total = allStrokes.values.sum().toFloat()

        _strokeDistribution.value = allStrokes.map { (type, count) ->
            StrokeDistributionItem(
                strokeType = StrokeType.fromString(type).displayName,
                count = count,
                percentage = if (total > 0) (count / total * 100) else 0f
            )
        }.sortedByDescending { it.count }
    }

    private fun calculateScoreTrend(sessions: List<TrainingSession>) {
        _scoreTrend.value = sessions
            .filter { it.avgScore > 0 }
            .sortedBy { it.startTime }
            .takeLast(20)
            .map { session ->
                ScoreTrendPoint(
                    dateLabel = shortDateFormatter.format(Date(session.startTime)),
                    score = session.avgScore
                )
            }
    }

    private fun TrainingSession.toDetailUi(strokes: List<Stroke>): SessionDetailUi {
        val durationMinutes = totalDuration / (1000 * 60)
        val distribution = strokes.groupBy { it.strokeType }
            .mapValues { it.value.size }
            .mapKeys { StrokeType.fromString(it.key).displayName }

        return SessionDetailUi(
            sessionId = sessionId,
            dateFormatted = dateFormatter.format(Date(startTime)),
            totalStrokes = totalStrokes,
            avgScore = avgScore,
            durationFormatted = "${durationMinutes}min",
            avgHeartRate = avgHeartRate,
            strokeDistribution = distribution
        )
    }
}

/**
 * UI model for session details.
 */
data class SessionDetailUi(
    val sessionId: Long,
    val dateFormatted: String,
    val totalStrokes: Int,
    val avgScore: Float,
    val durationFormatted: String,
    val avgHeartRate: Int? = null,
    val strokeDistribution: Map<String, Int> = emptyMap()
)

/**
 * Stroke distribution item for charts.
 */
data class StrokeDistributionItem(
    val strokeType: String,
    val count: Int,
    val percentage: Float
)

