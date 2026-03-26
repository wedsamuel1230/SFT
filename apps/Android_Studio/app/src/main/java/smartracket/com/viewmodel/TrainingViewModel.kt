package smartracket.com.viewmodel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import smartracket.com.db.SmartRacketDatabase
import smartracket.com.model.*
import smartracket.com.repository.*
import smartracket.com.service.TrainingSessionService
import javax.inject.Inject

/**
 * ViewModel for the real-time training screen.
 * 
 * Manages:
 * - Training session lifecycle
 * - Real-time stroke classification and feedback
 * - Bluetooth connection state
 * - Heart rate monitoring
 */
@HiltViewModel
class TrainingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trainingRepository: TrainingRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val healthRepository: HealthRepository,
    private val database: SmartRacketDatabase
) : ViewModel() {
    
    // ============= Session State =============
    
    private val _sessionState = MutableStateFlow(SessionState.IDLE)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    
    private val _currentSession = MutableStateFlow<TrainingSession?>(null)
    val currentSession: StateFlow<TrainingSession?> = _currentSession.asStateFlow()
    
    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

    private val _selectedSport = MutableStateFlow<Sport?>(null)
    val selectedSport: StateFlow<Sport?> = _selectedSport.asStateFlow()

    private val _preparationStep = MutableStateFlow(TrainingPreparationStep.SPORT_SELECTION)
    val preparationStep: StateFlow<TrainingPreparationStep> = _preparationStep.asStateFlow()

    private val _warmUpPlan = MutableStateFlow<WarmUpPlan?>(null)
    val warmUpPlan: StateFlow<WarmUpPlan?> = _warmUpPlan.asStateFlow()

    private val _warmUpElapsedTime = MutableStateFlow(0L)
    val warmUpElapsedTime: StateFlow<Long> = _warmUpElapsedTime.asStateFlow()

    private val _warmUpRequiredForConnection = MutableStateFlow(true)
    val warmUpRequiredForConnection: StateFlow<Boolean> = _warmUpRequiredForConnection.asStateFlow()

    private val _showRestReminder = MutableStateFlow<RestReminderUiState?>(null)
    val showRestReminder: StateFlow<RestReminderUiState?> = _showRestReminder.asStateFlow()

    val availableSports: List<Sport> = Sport.entries
    
    private var timerJob: Job? = null
    private var warmUpJob: Job? = null
    private var restReminderCount = 0
    private var connectedDeviceId: String? = null
    private var sportSelectionRequiredForConnection = true
    
    // ============= Stroke & Feedback =============
    
    private val _lastStroke = MutableStateFlow<Stroke?>(null)
    val lastStroke: StateFlow<Stroke?> = _lastStroke.asStateFlow()
    
    private val _currentScore = MutableStateFlow(0)
    val currentScore: StateFlow<Int> = _currentScore.asStateFlow()
    
    private val _currentFeedback = MutableStateFlow("")
    val currentFeedback: StateFlow<String> = _currentFeedback.asStateFlow()
    
    private val _strokeCount = MutableStateFlow(0)
    val strokeCount: StateFlow<Int> = _strokeCount.asStateFlow()

    private val _strokeAnimationTick = MutableStateFlow(0L)
    val strokeAnimationTick: StateFlow<Long> = _strokeAnimationTick.asStateFlow()
    
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

    private val _showHealthAlert = MutableStateFlow<HealthAlert?>(null)
    val showHealthAlert: StateFlow<HealthAlert?> = _showHealthAlert.asStateFlow()
    
    // ============= Error Handling =============
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        // Initialize Health Connect
        viewModelScope.launch {
            healthRepository.initialize()
        }
        
        // Listen for MCU model outputs
        viewModelScope.launch {
            bluetoothRepository.modelOutputs.collect { output ->
                processMcuStroke(output)
            }
        }
        
        // Listen for connection state changes
        viewModelScope.launch {
            connectionState.collect { state ->
                when (state) {
                    is BluetoothConnectionState.Error -> {
                        _errorMessage.value = state.message
                    }
                    is BluetoothConnectionState.Connected -> {
                        val isNewConnection = connectedDeviceId != state.device.deviceId
                        if (isNewConnection) {
                            connectedDeviceId = state.device.deviceId
                            sportSelectionRequiredForConnection = true
                            _warmUpRequiredForConnection.value = true
                        }

                        if (_sessionState.value == SessionState.IDLE) {
                            syncConnectedDevice(
                                device = state.device,
                                isNewConnection = isNewConnection
                            )
                        }
                    }
                    else -> {
                        connectedDeviceId = null
                        sportSelectionRequiredForConnection = true
                        _warmUpRequiredForConnection.value = true
                        if (_sessionState.value == SessionState.IDLE) {
                            _preparationStep.value = TrainingPreparationStep.SPORT_SELECTION
                        }
                    }
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

        // Listen for health alerts during training
        viewModelScope.launch {
            healthRepository.healthAlert.collect { alert ->
                if (_sessionState.value == SessionState.ACTIVE) {
                    _showHealthAlert.value = alert
                }
            }
        }
    }

    /**
     * Dismiss the current health alert.
     */
    fun dismissHealthAlert() {
        _showHealthAlert.value = null
    }

    /**
     * Pause session due to health alert and dismiss.
     */
    fun pauseForRest() {
        _showHealthAlert.value = null
        _showRestReminder.value = null
        pauseSession()
    }

    fun dismissRestReminder() {
        _showRestReminder.value = null
    }
    
    // ============= Session Control =============
    
    /**
     * Start a new training session.
     */
    fun startSession() {
        if (_sessionState.value != SessionState.IDLE) return

        val sport = _selectedSport.value ?: Sport.TABLE_TENNIS
        startActiveSession(
            sport = sport,
            warmUpState = WarmUpState.NOT_STARTED,
            warmUpDurationMs = 0L
        )
    }

    fun selectSport(sport: Sport) {
        _selectedSport.value = sport
    }

    fun confirmSportSelection() {
        if (_sessionState.value != SessionState.IDLE) return

        val sport = _selectedSport.value ?: Sport.TABLE_TENNIS
        viewModelScope.launch {
            persistDeviceSport(sport)
        }
        sportSelectionRequiredForConnection = false
        _warmUpRequiredForConnection.value = true
        _warmUpPlan.value = WarmUpPlans.forSport(sport)
        _warmUpElapsedTime.value = 0L
        _preparationStep.value = TrainingPreparationStep.WARM_UP
        _sessionState.value = SessionState.WARMING_UP
        beginWarmUp()
    }

    fun beginWarmUp() {
        if (_sessionState.value != SessionState.WARMING_UP) return

        warmUpJob?.cancel()
        warmUpJob = viewModelScope.launch {
            val startOffset = _warmUpElapsedTime.value
            val startTime = System.currentTimeMillis()

            while (true) {
                _warmUpElapsedTime.value = startOffset + (System.currentTimeMillis() - startTime)

                val plan = _warmUpPlan.value
                if (plan != null && _warmUpElapsedTime.value >= plan.totalDurationSeconds * 1000L) {
                    completeWarmUp()
                    break
                }

                delay(100)
            }
        }
    }

    fun skipWarmUp() {
        if (_sessionState.value != SessionState.WARMING_UP) return

        warmUpJob?.cancel()
        _warmUpRequiredForConnection.value = false
        startActiveSession(
            sport = _selectedSport.value ?: Sport.TABLE_TENNIS,
            warmUpState = WarmUpState.SKIPPED,
            warmUpDurationMs = _warmUpElapsedTime.value
        )
    }

    fun startTrainingFromWarmUp() {
        if (!WarmUpActionRules.canStartTraining(
                plan = _warmUpPlan.value,
                elapsedTimeMs = _warmUpElapsedTime.value,
                warmUpRequiredForConnection = _warmUpRequiredForConnection.value
            )) {
            return
        }

        if (_warmUpRequiredForConnection.value) {
            completeWarmUp()
            return
        }

        startActiveSession(
            sport = _selectedSport.value ?: Sport.TABLE_TENNIS,
            warmUpState = WarmUpState.SKIPPED,
            warmUpDurationMs = 0L
        )
    }

    fun completeWarmUp() {
        if (_sessionState.value != SessionState.WARMING_UP) return

        warmUpJob?.cancel()
        startActiveSession(
            sport = _selectedSport.value ?: Sport.TABLE_TENNIS,
            warmUpState = WarmUpState.COMPLETED,
            warmUpDurationMs = _warmUpElapsedTime.value
        )
    }

    private fun startActiveSession(
        sport: Sport,
        warmUpState: WarmUpState,
        warmUpDurationMs: Long
    ) {
        if (_sessionState.value == SessionState.ACTIVE || _sessionState.value == SessionState.STARTING) return
        
        viewModelScope.launch {
            try {
                _sessionState.value = SessionState.STARTING
                warmUpJob?.cancel()
                
                val session = trainingRepository.startSession(
                    sport = sport,
                    warmUpState = warmUpState,
                    warmUpDurationMs = warmUpDurationMs,
                    restReminderIntervalMs = RestReminderPolicy.DEFAULT_INTERVAL_MS
                )
                _currentSession.value = session
                _selectedSport.value = sport
                _warmUpPlan.value = WarmUpPlans.forSport(sport)
                restReminderCount = session.restReminderCount
                _showRestReminder.value = null
                
                // Reset counters
                _strokeCount.value = 0
                _averageScore.value = 0f
                _recentStrokes.value = emptyList()
                _lastStroke.value = null
                _currentScore.value = 0
                _currentFeedback.value = "Ready! Start practicing."
                _elapsedTime.value = 0L
                
                // Start timer
                startTimer()
                startTrainingService(session)
                
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
        stopTrainingService()
        _sessionState.value = SessionState.PAUSED
    }
    
    /**
     * Resume a paused session.
     */
    fun resumeSession() {
        if (_sessionState.value != SessionState.PAUSED) return
        
        startTimer()
        _currentSession.value?.let { startTrainingService(it, _elapsedTime.value, restReminderCount) }
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
                warmUpJob?.cancel()
                stopTrainingService()
                
                _currentSession.value?.let { session ->
                    val endedSession = trainingRepository.endSession(session.sessionId)
                    _currentSession.value = endedSession
                    
                    // Sync to Health Connect
                    endedSession?.let {
                        healthRepository.recordExerciseSession(
                            title = it.sport.healthSessionTitle,
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
        timerJob?.cancel()
        warmUpJob?.cancel()
        stopTrainingService()
        _currentSession.value = null
        _elapsedTime.value = 0
        _strokeCount.value = 0
        _strokeAnimationTick.value = 0L
        _averageScore.value = 0f
        _recentStrokes.value = emptyList()
        _warmUpElapsedTime.value = 0L
        _showRestReminder.value = null
        restReminderCount = 0
        _warmUpPlan.value = _selectedSport.value?.let(WarmUpPlans::forSport)
        val nextPreparationStep = TrainingPreparationFlowRules.stepAfterSessionReset(
            isConnected = connectionState.value is BluetoothConnectionState.Connected,
            selectedSport = _selectedSport.value,
            selectionRequired = sportSelectionRequiredForConnection
        )
        _preparationStep.value = nextPreparationStep
        _sessionState.value = TrainingPreparationFlowRules.sessionStateAfterSessionReset(
            nextPreparationStep = nextPreparationStep,
            warmUpRequiredForConnection = _warmUpRequiredForConnection.value
        )

        if (_sessionState.value == SessionState.WARMING_UP) {
            beginWarmUp()
        }
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val startOffset = _elapsedTime.value
            val startTime = System.currentTimeMillis()
            
            while (true) {
                _elapsedTime.value = startOffset + (System.currentTimeMillis() - startTime)
                val session = _currentSession.value
                if (session != null && RestReminderPolicy.shouldTriggerReminder(
                        elapsedTimeMs = _elapsedTime.value,
                        remindersAlreadyShown = restReminderCount,
                        intervalMs = session.restReminderIntervalMs
                    )) {
                    restReminderCount += 1
                    _showRestReminder.value = RestReminderUiState(
                        reminderCount = restReminderCount,
                        elapsedTimeMs = _elapsedTime.value
                    )
                    trainingRepository.updateRestReminderCount(session.sessionId, restReminderCount)
                }
                delay(100)  // Update every 100ms
            }
        }
    }

    private suspend fun syncConnectedDevice(
        device: DevicePairing,
        isNewConnection: Boolean
    ) {
        val deviceDao = database.devicePairingDao()
        val existing = deviceDao.getByMacAddress(device.bluetoothMacAddress)
        val merged = (existing ?: device).copy(
            deviceId = device.deviceId,
            deviceName = device.deviceName,
            bluetoothMacAddress = device.bluetoothMacAddress,
            lastConnected = System.currentTimeMillis(),
            batteryLevel = batteryLevel.value ?: existing?.batteryLevel,
            firmwareVersion = device.firmwareVersion ?: existing?.firmwareVersion,
            defaultSport = existing?.defaultSport ?: device.defaultSport,
            isPrimary = existing?.isPrimary ?: device.isPrimary,
            addedAt = existing?.addedAt ?: device.addedAt
        )
        deviceDao.insert(merged)
        if (_selectedSport.value == null || isNewConnection) {
            _selectedSport.value = merged.defaultSport
        }
        _warmUpPlan.value = _selectedSport.value?.let(WarmUpPlans::forSport)
        _preparationStep.value = TrainingPreparationFlowRules.stepAfterConnection(
            isNewConnection = sportSelectionRequiredForConnection,
            selectedSport = _selectedSport.value
        )
    }

    private suspend fun persistDeviceSport(sport: Sport) {
        val device = (connectionState.value as? BluetoothConnectionState.Connected)?.device ?: return
        val deviceDao = database.devicePairingDao()
        val existing = deviceDao.getByMacAddress(device.bluetoothMacAddress)
        if (existing != null) {
            deviceDao.updateDefaultSport(existing.deviceId, sport)
        } else {
            deviceDao.insert(device.copy(defaultSport = sport, lastConnected = System.currentTimeMillis()))
        }
    }

    private fun startTrainingService(
        session: TrainingSession,
        initialElapsedMs: Long = 0L,
        initialReminderCount: Int = 0
    ) {
        if (!hasHealthForegroundServiceRuntimePermission()) {
            _errorMessage.value = "Training permissions required. Please allow Activity Recognition or Body Sensors in system settings."
            return
        }

        val intent = Intent(context, TrainingSessionService::class.java).apply {
            action = TrainingSessionService.ACTION_START
            putExtra(TrainingSessionService.EXTRA_SESSION_ID, session.sessionId)
            putExtra(TrainingSessionService.EXTRA_INITIAL_ELAPSED_MS, initialElapsedMs)
            putExtra(TrainingSessionService.EXTRA_REMINDER_INTERVAL_MS, session.restReminderIntervalMs)
            putExtra(TrainingSessionService.EXTRA_INITIAL_REMINDER_COUNT, initialReminderCount)
        }
        context.startForegroundService(intent)
    }

    private fun hasHealthForegroundServiceRuntimePermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true

        return hasPermission(Manifest.permission.ACTIVITY_RECOGNITION) ||
            hasPermission(Manifest.permission.BODY_SENSORS) ||
            hasPermission(Manifest.permission.HIGH_SAMPLING_RATE_SENSORS)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun stopTrainingService() {
        val intent = Intent(context, TrainingSessionService::class.java).apply {
            action = TrainingSessionService.ACTION_STOP
        }
        context.startService(intent)
    }
    
    // ============= Stroke Processing =============
    
    private suspend fun processMcuStroke(output: McuModelOutput) {
        val session = _currentSession.value ?: return
        if (_sessionState.value != SessionState.ACTIVE) return
        
        try {
            // Record stroke from MCU model output
            val stroke = trainingRepository.recordStrokeFromMcu(session.sessionId, output)
            
            // Update UI state
            _lastStroke.value = stroke
            _currentScore.value = stroke.score
            _currentFeedback.value = stroke.feedback
            _strokeCount.value = _strokeCount.value + 1
            _strokeAnimationTick.value = _strokeAnimationTick.value + 1
            
            // Update average score
            val totalScore = _averageScore.value * (_strokeCount.value - 1) + stroke.score
            _averageScore.value = totalScore / _strokeCount.value
            
            // Add to recent strokes (keep last 10)
            val current = _recentStrokes.value.toMutableList()
            current.add(0, stroke)
            _recentStrokes.value = current.take(10)
            
        } catch (e: Exception) {
            _errorMessage.value = "Stroke processing error: ${e.message}"
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
        warmUpJob?.cancel()
    }
}

