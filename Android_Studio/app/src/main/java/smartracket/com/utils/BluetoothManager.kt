package smartracket.com.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import smartracket.com.model.BleDeviceProfile
import smartracket.com.model.BluetoothConnectionState
import smartracket.com.model.DevicePairing
import smartracket.com.model.DiscoveredDevice
import smartracket.com.model.ImuDataPacket
import smartracket.com.model.McuModelOutput
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bluetooth Low Energy manager for ESP32 paddle communication.
 *
 * Handles:
 * - Device scanning and discovery
 * - Connection management with auto-reconnect
 * - IMU data stream reception and parsing
 * - Error handling and state management
 *
 * ESP32 Communication Protocol:
 * UUIDs are provided by [BleDeviceProfile] at runtime so the
 * app can support different hardware revisions or third-party paddles.
 */
@Singleton
class BluetoothManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "BluetoothManager"

        // Scanning constants
        private const val SCAN_TIMEOUT_MS = 15000L
        private const val RECONNECT_DELAY_MS = 2000L
        private const val MAX_RECONNECT_ATTEMPTS = 5

        private const val DESIRED_MTU = 517
    }

    /**
     * Active BLE device profile.
     * Defaults to the original ESP32 SmartRacket UUIDs.
     * Call [setDeviceProfile] before scanning to switch hardware variants.
     */
    private var profile: BleDeviceProfile = BleDeviceProfile.DEFAULT

    /** Replace the active BLE profile (e.g. when the user picks a different paddle model). */
    fun setDeviceProfile(newProfile: BleDeviceProfile) {
        profile = newProfile
    }

    /** Returns the currently active profile for external inspection. */
    fun getDeviceProfile(): BleDeviceProfile = profile

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        manager?.adapter
    }

    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var imuCharacteristic: BluetoothGattCharacteristic? = null
    private var controlCharacteristic: BluetoothGattCharacteristic? = null

    private val operationQueue = BleOperationQueue()
    private val discoveredDeviceCache: MutableMap<String, BluetoothDevice> = mutableMapOf()

    private var reconnectAttempts = 0
    private var shouldReconnect = true
    private var currentDeviceAddress: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // State flows
    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Disconnected)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _imuDataFlow = MutableSharedFlow<ImuDataPacket>(
        replay = 0,
        extraBufferCapacity = 100
    )
    val imuDataFlow: SharedFlow<ImuDataPacket> = _imuDataFlow.asSharedFlow()

    private val _modelOutputFlow = MutableSharedFlow<McuModelOutput>(
        replay = 0,
        extraBufferCapacity = 50
    )
    val modelOutputFlow: SharedFlow<McuModelOutput> = _modelOutputFlow.asSharedFlow()

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // ============= Permission Checking =============

    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    // ============= Scanning =============

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasBluetoothPermissions()) {
            _connectionState.value = BluetoothConnectionState.Error("Bluetooth permissions not granted", 1)
            return
        }

        if (!isBluetoothEnabled()) {
            _connectionState.value = BluetoothConnectionState.Error("Bluetooth is not enabled", 2)
            return
        }

        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            _connectionState.value = BluetoothConnectionState.Error("BLE Scanner not available", 3)
            return
        }

        _discoveredDevices.value = emptyList()
        _connectionState.value = BluetoothConnectionState.Scanning
        _isScanning.value = true

        val scanFilters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(profile.serviceUuid))
                .build()
        )

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .build()

        try {
            bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)

            // Auto-stop scan after timeout
            handler.postDelayed({
                stopScan()
            }, SCAN_TIMEOUT_MS)

            Log.d(TAG, "BLE scan started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start scan", e)
            _connectionState.value = BluetoothConnectionState.Error("Scan failed: ${e.message}", 4)
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (_isScanning.value) {
            try {
                bluetoothLeScanner?.stopScan(scanCallback)
                Log.d(TAG, "BLE scan stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop scan", e)
            }
            _isScanning.value = false

            bluetoothLeScanner = null

            if (_connectionState.value is BluetoothConnectionState.Scanning) {
                _connectionState.value = BluetoothConnectionState.Disconnected
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceName = device.name ?: "Unknown Device"
            val isSmartRacket = deviceName.startsWith(profile.deviceNamePrefix) ||
                               result.scanRecord?.serviceUuids?.any { it.uuid == profile.serviceUuid } == true

            discoveredDeviceCache[device.address] = device

            val discoveredDevice = DiscoveredDevice(
                address = device.address,
                name = deviceName,
                rssi = result.rssi,
                isSmartRacketDevice = isSmartRacket
            )

            // Add to list if not already present
            val currentList = _discoveredDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.address == device.address }
            if (existingIndex >= 0) {
                currentList[existingIndex] = discoveredDevice
            } else {
                currentList.add(discoveredDevice)
            }
            _discoveredDevices.value = currentList.sortedByDescending { it.rssi }

            Log.d(TAG, "Discovered device: $deviceName (${device.address}), RSSI: ${result.rssi}")
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code: $errorCode")
            _connectionState.value = BluetoothConnectionState.Error("Scan failed (code: $errorCode)", errorCode)
            _isScanning.value = false
        }
    }

    // ============= Connection =============

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        if (!hasBluetoothPermissions()) {
            _connectionState.value = BluetoothConnectionState.Error("Bluetooth permissions not granted", 1)
            return
        }

        stopScan()

        val device = discoveredDeviceCache[address] ?: bluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            _connectionState.value = BluetoothConnectionState.Error("Device not found", 5)
            return
        }

        currentDeviceAddress = address
        shouldReconnect = true
        reconnectAttempts = 0

        _connectionState.value = BluetoothConnectionState.Connecting(device.name ?: "SmartRacket")

        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        Log.d(TAG, "Connecting to ${device.name} ($address)")
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        shouldReconnect = false
        operationQueue.clear()
        bluetoothGatt?.let { gatt ->
            gatt.disconnect()
            gatt.close()
        }
        bluetoothGatt = null
        imuCharacteristic = null
        controlCharacteristic = null
        currentDeviceAddress = null
        _connectionState.value = BluetoothConnectionState.Disconnected
        Log.d(TAG, "Disconnected")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "GATT error on connection change: status=$status, state=$newState")
                handleGattDisconnect(gatt, status)
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    reconnectAttempts = 0

                    handler.post {
                        enqueueImmediateOperation("connectionPriority") {
                            gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                        }
                        enqueueOperation("requestMtu") {
                            gatt.requestMtu(DESIRED_MTU)
                        }
                        enqueueOperation("discoverServices") {
                            gatt.discoverServices()
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server (status: $status)")
                    imuCharacteristic = null
                    controlCharacteristic = null

                    handleGattDisconnect(gatt, status)
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            operationQueue.onOperationComplete()
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")

                val service = gatt.getService(profile.serviceUuid)
                if (service == null) {
                    Log.e(TAG, "SmartRacket service not found")
                    _connectionState.value = BluetoothConnectionState.Error("SmartRacket service not found", 7)
                    return
                }

                imuCharacteristic = service.getCharacteristic(profile.imuCharacteristicUuid)
                controlCharacteristic = service.getCharacteristic(profile.controlCharacteristicUuid)

                if (imuCharacteristic == null) {
                    Log.e(TAG, "IMU characteristic not found")
                    _connectionState.value = BluetoothConnectionState.Error("IMU characteristic not found", 8)
                    return
                }

                // Enable notifications for IMU data
                enableNotifications(gatt, imuCharacteristic!!)

                val devicePairing = DevicePairing(
                    deviceId = gatt.device.address,
                    deviceName = gatt.device.name ?: "SmartRacket",
                    bluetoothMacAddress = gatt.device.address,
                    lastConnected = System.currentTimeMillis()
                )
                _connectionState.value = BluetoothConnectionState.Connected(devicePairing)

                Log.d(TAG, "Successfully connected and configured")
            } else {
                Log.e(TAG, "Service discovery failed with status: $status")
                _connectionState.value = BluetoothConnectionState.Error("Service discovery failed", status)
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == profile.imuCharacteristicUuid) {
                val data = characteristic.value
                parseImuData(data)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == profile.imuCharacteristicUuid) {
                parseImuData(value)
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            operationQueue.onOperationComplete()
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == profile.batteryCharacteristicUuid) {
                val battery = characteristic.value?.firstOrNull()?.toInt()?.and(0xFF)
                _batteryLevel.value = battery
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            operationQueue.onOperationComplete()
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Characteristic write failed: $status")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            operationQueue.onOperationComplete()
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Descriptor write successful: ${descriptor.uuid}")
            } else {
                Log.e(TAG, "Descriptor write failed: $status")
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            operationQueue.onOperationComplete()
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "MTU updated to $mtu")
            } else {
                Log.e(TAG, "MTU request failed: $status")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        enqueueOperation("enableNotifications") {
            gatt.setCharacteristicNotification(characteristic, true)

            val descriptor = characteristic.getDescriptor(BleDeviceProfile.CCCD_UUID) ?: return@enqueueOperation false
            val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(
                    descriptor,
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
            if (started) {
                Log.d(TAG, "Enabled notifications for ${characteristic.uuid}")
            }
            started
        }
    }

    private fun parseImuData(data: ByteArray) {
        if (data.isEmpty()) return

        // MCU model output is sent as JSON on the IMU characteristic
        val payload = kotlin.runCatching { String(data, Charsets.UTF_8).trim() }.getOrNull()
        if (!payload.isNullOrEmpty() && payload.firstOrNull() == '{') {
            val parsed = parseModelOutputJson(payload)
            if (parsed != null) {
                coroutineScope.launch {
                    _modelOutputFlow.emit(parsed)
                }
                return
            }
        }

        // Fallback to legacy IMU binary packet parsing
        val packet = ImuDataPacket.fromBytes(data)
        if (packet != null) {
            coroutineScope.launch {
                _imuDataFlow.emit(packet)
            }
        } else {
            Log.w(TAG, "Failed to parse IMU packet (size: ${data.size})")
        }
    }

    private fun parseModelOutputJson(json: String): McuModelOutput? {
        return try {
            val obj = JSONObject(json)
            McuModelOutput(
                ts = obj.optLong("ts", System.currentTimeMillis()),
                stroke = obj.optString("stroke", "unknown"),
                conf = obj.optDouble("conf", 0.0).toFloat(),
                peak = obj.optDouble("peak", 0.0).toFloat()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Invalid model output JSON: $json", e)
            null
        }
    }

    // ============= Control Commands =============

    @SuppressLint("MissingPermission")
    fun sendCommand(command: ByteArray): Boolean {
        val gatt = bluetoothGatt ?: return false
        val characteristic = controlCharacteristic ?: return false

        enqueueOperation("sendCommand") {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(
                        characteristic,
                        command,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    ) == BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    characteristic.value = command
                    @Suppress("DEPRECATION")
                    gatt.writeCharacteristic(characteristic)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send command", e)
                false
            }
        }

        return true
    }

    /**
     * Request battery level from device.
     */
    @SuppressLint("MissingPermission")
    fun requestBatteryLevel() {
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(profile.serviceUuid) ?: return
        val batteryChar = service.getCharacteristic(profile.batteryCharacteristicUuid) ?: return
        enqueueOperation("readBattery") {
            gatt.readCharacteristic(batteryChar)
        }
    }

    /**
     * Clean up resources.
     */
    fun cleanup() {
        stopScan()
        disconnect()
        coroutineScope.cancel()
        handler.removeCallbacksAndMessages(null)
        discoveredDeviceCache.clear()
        operationQueue.clear()
    }

    private fun enqueueOperation(name: String, operation: () -> Boolean) {
        operationQueue.enqueue(object : BleOperationQueue.BleOperation {
            override val name: String = name
            override fun execute(): Boolean = operation()
        })
    }

    private fun enqueueImmediateOperation(name: String, operation: () -> Boolean) {
        operationQueue.enqueue(object : BleOperationQueue.BleOperation {
            override val name: String = name
            override fun execute(): Boolean {
                operation()
                return false
            }
        })
    }

    private fun handleGattDisconnect(gatt: BluetoothGatt, status: Int) {
        operationQueue.clear()
        imuCharacteristic = null
        controlCharacteristic = null

        if (shouldReconnect && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            Log.d(TAG, "Attempting reconnect ($reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)")
            _connectionState.value = BluetoothConnectionState.Connecting("Reconnecting...")

            handler.postDelayed({
                currentDeviceAddress?.let { address ->
                    gatt.close()
                    bluetoothGatt = null
                    connect(address)
                }
            }, RECONNECT_DELAY_MS)
        } else {
            gatt.close()
            bluetoothGatt = null
            _connectionState.value = if (shouldReconnect) {
                BluetoothConnectionState.Error("Connection lost after $MAX_RECONNECT_ATTEMPTS attempts", status)
            } else {
                BluetoothConnectionState.Disconnected
            }
        }
    }
}

