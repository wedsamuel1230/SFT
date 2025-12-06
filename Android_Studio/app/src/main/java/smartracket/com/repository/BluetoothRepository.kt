package smartracket.com.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import smartracket.com.model.*
import smartracket.com.utils.BluetoothManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Bluetooth communication with the ESP32 paddle.
 *
 * Abstracts Bluetooth operations and provides a clean interface for:
 * - Device scanning and connection management
 * - IMU data streaming
 * - Connection state monitoring
 * - Device pairing persistence
 */
@Singleton
class BluetoothRepository @Inject constructor(
    private val bluetoothManager: BluetoothManager
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // IMU data buffer for stroke detection
    private val imuBuffer = mutableListOf<ImuDataPacket>()
    private val bufferLock = Any()

    // Stroke detection parameters
    private val strokeDetectionThreshold = 15f  // Acceleration magnitude threshold
    private val strokeCooldownMs = 500L  // Minimum time between strokes
    private var lastStrokeTime = 0L

    // Detected strokes flow
    private val _detectedStrokes = MutableSharedFlow<MotionData>(
        replay = 0,
        extraBufferCapacity = 10
    )
    val detectedStrokes: SharedFlow<MotionData> = _detectedStrokes.asSharedFlow()

    /**
     * Connection state from BluetoothManager.
     */
    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothManager.connectionState

    /**
     * Discovered devices during scanning.
     */
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = bluetoothManager.discoveredDevices

    /**
     * Raw IMU data stream.
     */
    val imuDataFlow: SharedFlow<ImuDataPacket> = bluetoothManager.imuDataFlow

    /**
     * Current battery level of connected device.
     */
    val batteryLevel: StateFlow<Int?> = bluetoothManager.batteryLevel

    /**
     * Whether currently scanning for devices.
     */
    val isScanning: StateFlow<Boolean> = bluetoothManager.isScanning

    init {
        // Process IMU data for stroke detection
        scope.launch {
            bluetoothManager.imuDataFlow.collect { packet ->
                processImuPacket(packet)
            }
        }
    }

    /**
     * Check if Bluetooth permissions are granted.
     */
    fun hasPermissions(): Boolean = bluetoothManager.hasBluetoothPermissions()

    /**
     * Check if Bluetooth is enabled.
     */
    fun isBluetoothEnabled(): Boolean = bluetoothManager.isBluetoothEnabled()

    /**
     * Check if Bluetooth is supported on this device.
     */
    fun isBluetoothSupported(): Boolean = bluetoothManager.isBluetoothSupported()

    /**
     * Start scanning for SmartRacket devices.
     */
    fun startScan() {
        bluetoothManager.startScan()
    }

    /**
     * Stop scanning.
     */
    fun stopScan() {
        bluetoothManager.stopScan()
    }

    /**
     * Connect to a device by address.
     */
    fun connect(address: String) {
        clearImuBuffer()
        bluetoothManager.connect(address)
    }

    /**
     * Disconnect from current device.
     */
    fun disconnect() {
        clearImuBuffer()
        bluetoothManager.disconnect()
    }

    /**
     * Request battery level from connected device.
     */
    fun requestBatteryLevel() {
        bluetoothManager.requestBatteryLevel()
    }

    /**
     * Send a command to the connected device.
     */
    fun sendCommand(command: ByteArray): Boolean {
        return bluetoothManager.sendCommand(command)
    }

    /**
     * Get the currently connected device info.
     */
    fun getConnectedDevice(): DevicePairing? {
        val state = connectionState.value
        return if (state is BluetoothConnectionState.Connected) {
            state.device
        } else null
    }

    /**
     * Check if currently connected to a device.
     */
    fun isConnected(): Boolean {
        return connectionState.value is BluetoothConnectionState.Connected
    }

    /**
     * Process incoming IMU packet for stroke detection.
     */
    private fun processImuPacket(packet: ImuDataPacket) {
        synchronized(bufferLock) {
            imuBuffer.add(packet)

            // Keep buffer size manageable (last 100 packets = ~5 seconds at 20Hz)
            while (imuBuffer.size > 100) {
                imuBuffer.removeAt(0)
            }
        }

        // Check for stroke detection
        val accelMag = packet.accelerationMagnitude()
        val currentTime = System.currentTimeMillis()

        if (accelMag > strokeDetectionThreshold &&
            currentTime - lastStrokeTime > strokeCooldownMs) {

            lastStrokeTime = currentTime
            val strokeData = extractStrokeData()

            scope.launch {
                _detectedStrokes.emit(strokeData)
            }
        }
    }

    /**
     * Extract motion data for the detected stroke from buffer.
     */
    private fun extractStrokeData(): MotionData {
        synchronized(bufferLock) {
            // Use last 50 packets (~2.5 seconds) for the stroke
            val strokePackets = imuBuffer.takeLast(50)

            return MotionData(
                accelX = strokePackets.map { it.accelX },
                accelY = strokePackets.map { it.accelY },
                accelZ = strokePackets.map { it.accelZ },
                gyroX = strokePackets.map { it.gyroX },
                gyroY = strokePackets.map { it.gyroY },
                gyroZ = strokePackets.map { it.gyroZ },
                timestamps = strokePackets.map { it.timestamp }
            )
        }
    }

    /**
     * Clear the IMU buffer.
     */
    private fun clearImuBuffer() {
        synchronized(bufferLock) {
            imuBuffer.clear()
        }
    }

    /**
     * Get recent motion data for highlight capture.
     *
     * @param durationMs Duration of data to retrieve in milliseconds
     */
    fun getRecentMotionData(durationMs: Long = 180000): List<ImuDataPacket> {
        synchronized(bufferLock) {
            val cutoffTime = System.currentTimeMillis() - durationMs
            return imuBuffer.filter { it.timestamp >= cutoffTime }.toList()
        }
    }

    /**
     * Clean up resources.
     */
    fun cleanup() {
        bluetoothManager.cleanup()
    }
}

