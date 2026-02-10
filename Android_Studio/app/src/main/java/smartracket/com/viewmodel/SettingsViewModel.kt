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
import smartracket.com.model.Language
import smartracket.com.model.ThemeMode
import smartracket.com.repository.BluetoothRepository
import smartracket.com.repository.HealthRepository
import smartracket.com.repository.SyncState
import smartracket.com.repository.SyncStats
import smartracket.com.sync.SyncManager
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
    private val database: SmartRacketDatabase,
    private val syncManager: SyncManager
) : ViewModel() {

    companion object {
        private val AUTO_SAVE_THRESHOLD = intPreferencesKey("auto_save_threshold")
        private val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        private val LANGUAGE_CODE = stringPreferencesKey("language_code")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
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

    // Samsung Health state
    val isSamsungHealthConnected: StateFlow<Boolean> = healthRepository.isSamsungHealthConnected

    // Settings
    private val _autoSaveThreshold = MutableStateFlow(8)
    val autoSaveThreshold: StateFlow<Int> = _autoSaveThreshold.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(true)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    // Language settings
    private val _language = MutableStateFlow(Language.ENGLISH)
    val language: StateFlow<Language> = _language.asStateFlow()

    // Theme mode settings
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // Clear data dialog
    private val _showClearDataDialog = MutableStateFlow(false)
    val showClearDataDialog: StateFlow<Boolean> = _showClearDataDialog.asStateFlow()

    // Remove device dialog
    private val _deviceToRemove = MutableStateFlow<PairedDeviceUi?>(null)
    val deviceToRemove: StateFlow<PairedDeviceUi?> = _deviceToRemove.asStateFlow()

    // Cloud Sync state
    private val _cloudSyncEnabled = MutableStateFlow(false)
    val cloudSyncEnabled: StateFlow<Boolean> = _cloudSyncEnabled.asStateFlow()

    val syncState: StateFlow<SyncState> = syncManager.syncState

    private val _syncStats = MutableStateFlow(SyncStats(0, 0, 0, false))
    val syncStats: StateFlow<SyncStats> = _syncStats.asStateFlow()

    init {
        loadSettings()
        loadPairedDevices()
        loadSyncStats()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            context.dataStore.data.collect { preferences ->
                _autoSaveThreshold.value = preferences[AUTO_SAVE_THRESHOLD] ?: 8
                _keepScreenOn.value = preferences[KEEP_SCREEN_ON] ?: true
                _vibrationEnabled.value = preferences[VIBRATION_ENABLED] ?: true
                _language.value = Language.fromCode(preferences[LANGUAGE_CODE] ?: "en")
                _themeMode.value = ThemeMode.fromCode(preferences[THEME_MODE] ?: "system")
                _cloudSyncEnabled.value = preferences[CLOUD_SYNC_ENABLED] ?: false
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

    // ============= Device Management =============

    fun setPrimaryDevice(address: String) {
        viewModelScope.launch {
            database.devicePairingDao().clearPrimaryDevice()
            database.devicePairingDao().setPrimaryDevice(address)
        }
    }

    fun showRemoveDeviceDialog(device: PairedDeviceUi) {
        _deviceToRemove.value = device
    }

    fun dismissRemoveDeviceDialog() {
        _deviceToRemove.value = null
    }

    fun confirmRemoveDevice() {
        val device = _deviceToRemove.value ?: return
        viewModelScope.launch {
            database.devicePairingDao().deleteById(device.address)
            _deviceToRemove.value = null
        }
    }

    // ============= Health Connect =============

    fun requestHealthPermissions() {
        // In a real app, this would launch the Health Connect permission UI
        viewModelScope.launch {
            healthRepository.checkPermissions()
        }
    }

    // ============= Samsung Health =============

    fun connectSamsungHealth() {
        viewModelScope.launch {
            healthRepository.connectSamsungHealth()
        }
    }

    fun disconnectSamsungHealth() {
        viewModelScope.launch {
            healthRepository.disconnectSamsungHealth()
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

    fun setLanguage(language: Language) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[LANGUAGE_CODE] = language.code
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[THEME_MODE] = mode.code
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

    // ============= Cloud Sync =============

    private fun loadSyncStats() {
        viewModelScope.launch {
            _syncStats.value = syncManager.getSyncStats()
        }
    }

    fun setCloudSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[CLOUD_SYNC_ENABLED] = enabled
            }
            if (enabled) {
                syncManager.enablePeriodicSync()
            } else {
                syncManager.disablePeriodicSync()
            }
        }
    }

    fun syncNow() {
        syncManager.triggerImmediateSync()
        // Refresh stats after a short delay to reflect changes
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _syncStats.value = syncManager.getSyncStats()
        }
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
            lastConnectedFormatted = lastConnected?.let { dateFormatter.format(Date(it)) } ?: "Never",
            isPrimary = isPrimary
        )
    }
}

/**
 * UI model for paired devices.
 */
data class PairedDeviceUi(
    val address: String,
    val deviceName: String,
    val lastConnectedFormatted: String,
    val isPrimary: Boolean = false
)

