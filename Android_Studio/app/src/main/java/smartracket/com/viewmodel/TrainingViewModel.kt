package smartracket.com.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import smartracket.com.model.*
import smartracket.com.repository.*
import javax.inject.Inject

/**
 * ViewModel for the real-time training screen.
 * 
 * Manages:
 * - Training session lifecycle
 * - Real-time stroke classification and feedback
 * - Bluetooth connection state
 * - Highlight capture
 * - Heart rate monitoring
 */
@HiltViewModel
class TrainingViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val highlightRepository: HighlightRepository,
    private val healthRepository: HealthRepository
) : ViewModel() {
    
    // ============= Session State =============
    
    private val _sessionState = MutableStateFlow(SessionState.IDLE)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    
    private val _currentSession = MutableStateFlow<TrainingSession?>(null)
    val currentSession: StateFlow<TrainingSession?> = _currentSession.asStateFlow()
    
    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()
    
    private var timerJob: Job? = null
    
    // ============= Stroke & Feedback =============
    
    private val _lastStroke = MutableStateFlow<Stroke?>(null)
    val lastStroke: StateFlow<Stroke?> = _lastStroke.asStateFlow()
    
    private val _currentScore = MutableStateFlow(0)
    val currentScore: StateFlow<Int> = _currentScore.asStateFlow()
    
    private val _currentFeedback = MutableStateFlow("")
    val currentFeedback: StateFlow<String> = _currentFeedback.asStateFlow()
    
    private val _strokeCount = MutableStateFlow(0)
    val strokeCount: StateFlow<Int> = _strokeCount.asStateFlow()
    
    private val _averageScore = MutableStateFlow(0f)
    val averageScore: StateFlow<Float> = _averageScore.asStateFlow()
    
    // Recent strokes for UI display
    private val _recentStrokes = MutableStateFlow<List<Stroke>>(emptyList())
    val recentStrokes: StateFlow<List<Stroke>> = _recentStrokes.asStateFlow()
    
    // ============= Bluetooth =============
    
    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothRepository.connectionState
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = bluetoothRepository.discoveredDevices
    val isScanning: StateFlow<Boolean> = bluetoothRepository.isScanning
    val batteryLevel: StateFlow<Int?> = bluetoothRepository.batteryLevel
    
    // ============= Health =============
    
    val currentHeartRate: StateFlow<Int?> = healthRepository.currentHeartRate
    
    // ============= Highlights =============
    
    private val _highlightSaveState = MutableStateFlow<HighlightSaveState>(HighlightSaveState.Idle)
    val highlightSaveState: StateFlow<HighlightSaveState> = _highlightSaveState.asStateFlow()
    
    // ============= Error Handling =============
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        // Initialize classifier on startup
        viewModelScope.launch {
            trainingRepository.initializeClassifier()
        }
        
        // Initialize Health Connect
        viewModelScope.launch {
            healthRepository.initialize()
        }
        
        // Listen for detected strokes
        viewModelScope.launch {
            bluetoothRepository.detectedStrokes.collect { motionData ->
                processDetectedStroke(motionData)
            }
        }
        
        // Listen for connection state changes
        viewModelScope.launch {
            connectionState.collect { state ->
                if (state is BluetoothConnectionState.Error) {
                    _errorMessage.value = state.message
                }
            }
        }
        
        // Poll heart rate periodically
        viewModelScope.launch {
            while (true) {
                if (_sessionState.value == SessionState.ACTIVE) {
                    healthRepository.getLatestHeartRate()
                }
                delay(5000)  // Every 5 seconds
            }
        }
    }
    
    // ============= Session Control =============
    
    /**
     * Start a new training session.
     */
    fun startSession() {
        if (_sessionState.value != SessionState.IDLE) return
        
        viewModelScope.launch {
            try {
                _sessionState.value = SessionState.STARTING
                
                val session = trainingRepository.startSession()
                _currentSession.value = session
                
                // Reset counters
                _strokeCount.value = 0
                _averageScore.value = 0f
                _recentStrokes.value = emptyList()
                _lastStroke.value = null
                _currentScore.value = 0
                _currentFeedback.value = "Ready! Start practicing."
                
                // Clear highlight buffer
                highlightRepository.clearBuffer()
                
                // Start timer
                startTimer()
                
                _sessionState.value = SessionState.ACTIVE
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to start session: ${e.message}"
                _sessionState.value = SessionState.IDLE
            }
        }
    }
    
    /**
     * Pause the current session.
     */
    fun pauseSession() {
        if (_sessionState.value != SessionState.ACTIVE) return
        
        timerJob?.cancel()
        _sessionState.value = SessionState.PAUSED
    }
    
    /**
     * Resume a paused session.
     */
    fun resumeSession() {
        if (_sessionState.value != SessionState.PAUSED) return
        
        startTimer()
        _sessionState.value = SessionState.ACTIVE
    }
    
    /**
     * Stop the current session.
     */
    fun stopSession() {
        if (_sessionState.value == SessionState.IDLE) return
        
        viewModelScope.launch {
            try {
                _sessionState.value = SessionState.STOPPING
                
                timerJob?.cancel()
                
                _currentSession.value?.let { session ->
                    val endedSession = trainingRepository.endSession(session.sessionId)
                    _currentSession.value = endedSession
                    
                    // Sync to Health Connect
                    endedSession?.let {
                        healthRepository.recordExerciseSession(
                            title = "Table Tennis Training",
                            startTime = it.startTime,
                            endTime = it.endTime ?: System.currentTimeMillis(),
                            notes = "Strokes: ${it.totalStrokes}, Avg Score: ${it.avgScore}"
                        )
                    }
                }
                
                _sessionState.value = SessionState.COMPLETED
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to end session: ${e.message}"
            }
        }
    }
    
    /**
     * Reset to idle state (after viewing summary).
     */
    fun resetSession() {
        _sessionState.value = SessionState.IDLE
        _currentSession.value = null
        _elapsedTime.value = 0
        _strokeCount.value = 0
        _averageScore.value = 0f
        _recentStrokes.value = emptyList()
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val startOffset = _elapsedTime.value
            val startTime = System.currentTimeMillis()
            
            while (true) {
                _elapsedTime.value = startOffset + (System.currentTimeMillis() - startTime)
                delay(100)  // Update every 100ms
            }
        }
    }
    
    // ============= Stroke Processing =============
    
    private suspend fun processDetectedStroke(motionData: MotionData) {
        val session = _currentSession.value ?: return
        if (_sessionState.value != SessionState.ACTIVE) return
        
        try {
            // Record and classify the stroke
            val stroke = trainingRepository.recordStroke(session.sessionId, motionData)
            
            // Update UI state
            _lastStroke.value = stroke
            _currentScore.value = stroke.score
            _currentFeedback.value = stroke.feedback
            _strokeCount.value = _strokeCount.value + 1
            
            // Update average score
            val totalScore = _averageScore.value * (_strokeCount.value - 1) + stroke.score
            _averageScore.value = totalScore / _strokeCount.value
            
            // Add to recent strokes (keep last 10)
            val current = _recentStrokes.value.toMutableList()
            current.add(0, stroke)
            _recentStrokes.value = current.take(10)
            
            // Add to highlight buffer
            val strokeInfo = StrokeBufferInfo(
                strokeType = stroke.strokeType,
                score = stroke.score,
                confidence = stroke.confidence,
                feedback = stroke.feedback
            )
            highlightRepository.addToBuffer(
                motionData = motionData,
                heartRate = currentHeartRate.value,
                strokeInfo = strokeInfo
            )
            
            // Check for auto-save highlight
            if (highlightRepository.shouldAutoSave(stroke.score)) {
                saveHighlight(isAutoSave = true, strokeInfo = strokeInfo)
            }
            
        } catch (e: Exception) {
            _errorMessage.value = "Stroke processing error: ${e.message}"
        }
    }
    
    // ============= Highlight Capture =============
    
    /**
     * Manually save current moment as highlight.
     */
    fun saveHighlightManually() {
        val lastStroke = _lastStroke.value ?: return
        
        val strokeInfo = StrokeBufferInfo(
            strokeType = lastStroke.strokeType,
            score = lastStroke.score,
            confidence = lastStroke.confidence,
            feedback = lastStroke.feedback
        )
        
        saveHighlight(isAutoSave = false, strokeInfo = strokeInfo)
    }
    
    private fun saveHighlight(isAutoSave: Boolean, strokeInfo: StrokeBufferInfo) {
        val session = _currentSession.value ?: return
        
        viewModelScope.launch {
            try {
                _highlightSaveState.value = HighlightSaveState.Saving
                
                val clip = highlightRepository.createHighlight(
                    sessionId = session.sessionId,
                    strokeInfo = strokeInfo,
                    heartRate = currentHeartRate.value,
                    isAutoSaved = isAutoSave
                )
                
                _highlightSaveState.value = HighlightSaveState.Saved(clip)
                
                // Reset state after delay
                delay(2000)
                _highlightSaveState.value = HighlightSaveState.Idle
                
            } catch (e: Exception) {
                _highlightSaveState.value = HighlightSaveState.Error(e.message ?: "Save failed")
                delay(2000)
                _highlightSaveState.value = HighlightSaveState.Idle
            }
        }
    }
    
    // ============= Bluetooth Control =============
    
    fun hasBluetoothPermissions(): Boolean = bluetoothRepository.hasPermissions()
    
    fun isBluetoothEnabled(): Boolean = bluetoothRepository.isBluetoothEnabled()
    
    fun startBluetoothScan() {
        bluetoothRepository.startScan()
    }
    
    fun stopBluetoothScan() {
        bluetoothRepository.stopScan()
    }
    
    fun connectToDevice(address: String) {
        bluetoothRepository.connect(address)
    }
    
    fun disconnectDevice() {
        bluetoothRepository.disconnect()
    }
    
    // ============= Error Handling =============
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    // ============= Cleanup =============
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

/**
 * State for highlight save operation.
 */
sealed class HighlightSaveState {
    data object Idle : HighlightSaveState()
    data object Saving : HighlightSaveState()
    data class Saved(val clip: HighlightClip) : HighlightSaveState()
    data class Error(val message: String) : HighlightSaveState()
}

