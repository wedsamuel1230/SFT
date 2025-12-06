package smartracket.com.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import smartracket.com.db.SmartRacketDatabase
import smartracket.com.model.BluetoothConnectionState
import smartracket.com.model.DevicePairing
import smartracket.com.repository.BluetoothRepository
import smartracket.com.repository.HealthRepository
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// DataStore extension
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * ViewModel for the Settings Screen.
 *
 * Manages:
 * - Bluetooth device pairing
 * - Health Connect permissions
 * - App preferences
 * - Language settings
 * - Data management
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothRepository: BluetoothRepository,
    private val healthRepository: HealthRepository,
    private val database: SmartRacketDatabase
) : ViewModel() {

    companion object {
        private val AUTO_SAVE_THRESHOLD = intPreferencesKey("auto_save_threshold")
        private val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        private val CHINESE_LANGUAGE = booleanPreferencesKey("chinese_language")
    }

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Bluetooth state
    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothRepository.connectionState

    // Paired devices
    private val _pairedDevices = MutableStateFlow<List<PairedDeviceUi>>(emptyList())
    val pairedDevices: StateFlow<List<PairedDeviceUi>> = _pairedDevices.asStateFlow()

    // Health Connect state
    val isHealthConnectAvailable: StateFlow<Boolean> = healthRepository.isAvailable
    val hasHealthPermissions: StateFlow<Boolean> = healthRepository.hasPermissions

    // Settings
    private val _autoSaveThreshold = MutableStateFlow(8)
    val autoSaveThreshold: StateFlow<Int> = _autoSaveThreshold.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(true)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    // Language settings
    private val _isChineseLanguage = MutableStateFlow(false)
    val isChineseLanguage: StateFlow<Boolean> = _isChineseLanguage.asStateFlow()

    // Clear data dialog
    private val _showClearDataDialog = MutableStateFlow(false)
    val showClearDataDialog: StateFlow<Boolean> = _showClearDataDialog.asStateFlow()

    init {
        loadSettings()
        loadPairedDevices()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            context.dataStore.data.collect { preferences ->
                _autoSaveThreshold.value = preferences[AUTO_SAVE_THRESHOLD] ?: 8
                _keepScreenOn.value = preferences[KEEP_SCREEN_ON] ?: true
                _vibrationEnabled.value = preferences[VIBRATION_ENABLED] ?: true
                _isChineseLanguage.value = preferences[CHINESE_LANGUAGE] ?: false
            }
        }
    }

    private fun loadPairedDevices() {
        viewModelScope.launch {
            database.devicePairingDao().getAllFlow().collect { devices ->
                _pairedDevices.value = devices.map { it.toUi() }
            }
        }
    }

    // ============= Bluetooth =============

    fun startScan() {
        bluetoothRepository.startScan()
    }

    fun stopScan() {
        bluetoothRepository.stopScan()
    }

    fun connectToDevice(address: String) {
        bluetoothRepository.connect(address)
    }

    fun disconnectDevice() {
        bluetoothRepository.disconnect()
    }

    // ============= Health Connect =============

    fun requestHealthPermissions() {
        // In a real app, this would launch the Health Connect permission UI
        viewModelScope.launch {
            healthRepository.checkPermissions()
        }
    }

    // ============= Settings =============

    fun setAutoSaveThreshold(value: Int) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[AUTO_SAVE_THRESHOLD] = value
            }
        }
    }

    fun setKeepScreenOn(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[KEEP_SCREEN_ON] = value
            }
        }
    }

    fun setVibrationEnabled(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[VIBRATION_ENABLED] = value
            }
        }
    }

    fun setChineseLanguage(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[CHINESE_LANGUAGE] = value
            }
        }
    }

    // ============= App Info =============

    fun openPrivacyPolicy() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://smartracket.com/privacy")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openTerms() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://smartracket.com/terms")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun sendFeedback() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:feedback@smartracket.com")
            putExtra(Intent.EXTRA_SUBJECT, "SmartRacket Coach Feedback")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "Send Feedback").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    // ============= Data Management =============

    fun showClearDataDialog() {
        _showClearDataDialog.value = true
    }

    fun dismissClearDataDialog() {
        _showClearDataDialog.value = false
    }

    fun clearAllData() {
        viewModelScope.launch {
            database.clearAllTables()
            _showClearDataDialog.value = false
        }
    }

    private fun DevicePairing.toUi(): PairedDeviceUi {
        return PairedDeviceUi(
            address = bluetoothMacAddress,
            deviceName = deviceName,
            lastConnectedFormatted = lastConnected?.let { dateFormatter.format(Date(it)) } ?: "Never"
        )
    }
}

/**
 * UI model for paired devices.
 */
data class PairedDeviceUi(
    val address: String,
    val deviceName: String,
    val lastConnectedFormatted: String
)

