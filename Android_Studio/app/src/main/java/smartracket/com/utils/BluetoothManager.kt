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
import smartracket.com.model.BluetoothConnectionState
import smartracket.com.model.DevicePairing
import smartracket.com.model.DiscoveredDevice
import smartracket.com.model.ImuDataPacket
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
 * - Service UUID: 4fafc201-1fb5-459e-8fcc-c5c9c331914b
 * - IMU Characteristic: beb5483e-36e1-4688-b7f5-ea07361b26a8 (Notify)
 * - Control Characteristic: beb5483e-36e1-4688-b7f5-ea07361b26a9 (Write)
 */
@Singleton
class BluetoothManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "BluetoothManager"

        // ESP32 SmartRacket BLE Service and Characteristic UUIDs
        val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val IMU_CHARACTERISTIC_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
        val CONTROL_CHARACTERISTIC_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9")
        val BATTERY_CHARACTERISTIC_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26aa")

        // Client Characteristic Configuration Descriptor (for enabling notifications)
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // Scanning constants
        private const val SCAN_TIMEOUT_MS = 15000L
        private const val RECONNECT_DELAY_MS = 2000L
        private const val MAX_RECONNECT_ATTEMPTS = 5

        // Device name prefix for SmartRacket paddles
        private const val DEVICE_NAME_PREFIX = "SmartRacket"
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        manager?.adapter
    }

    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var imuCharacteristic: BluetoothGattCharacteristic? = null
    private var controlCharacteristic: BluetoothGattCharacteristic? = null

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
                .setServiceUuid(ParcelUuid(SERVICE_UUID))
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
            val isSmartRacket = deviceName.startsWith(DEVICE_NAME_PREFIX) ||
                               result.scanRecord?.serviceUuids?.any { it.uuid == SERVICE_UUID } == true

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

        val device = bluetoothAdapter?.getRemoteDevice(address)
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
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    reconnectAttempts = 0
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server (status: $status)")
                    imuCharacteristic = null
                    controlCharacteristic = null

                    if (shouldReconnect && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                        reconnectAttempts++
                        Log.d(TAG, "Attempting reconnect ($reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)")
                        _connectionState.value = BluetoothConnectionState.Connecting("Reconnecting...")

                        handler.postDelayed({
                            currentDeviceAddress?.let { address ->
                                gatt.close()
                                connect(address)
                            }
                        }, RECONNECT_DELAY_MS)
                    } else {
                        gatt.close()
                        bluetoothGatt = null
                        _connectionState.value = if (shouldReconnect) {
                            BluetoothConnectionState.Error("Connection lost after $MAX_RECONNECT_ATTEMPTS attempts", 6)
                        } else {
                            BluetoothConnectionState.Disconnected
                        }
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")

                val service = gatt.getService(SERVICE_UUID)
                if (service == null) {
                    Log.e(TAG, "SmartRacket service not found")
                    _connectionState.value = BluetoothConnectionState.Error("SmartRacket service not found", 7)
                    return
                }

                imuCharacteristic = service.getCharacteristic(IMU_CHARACTERISTIC_UUID)
                controlCharacteristic = service.getCharacteristic(CONTROL_CHARACTERISTIC_UUID)

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
            if (characteristic.uuid == IMU_CHARACTERISTIC_UUID) {
                val data = characteristic.value
                parseImuData(data)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == IMU_CHARACTERISTIC_UUID) {
                parseImuData(value)
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == BATTERY_CHARACTERISTIC_UUID) {
                val battery = characteristic.value?.firstOrNull()?.toInt()?.and(0xFF)
                _batteryLevel.value = battery
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Descriptor write successful: ${descriptor.uuid}")
            } else {
                Log.e(TAG, "Descriptor write failed: $status")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
            Log.d(TAG, "Enabled notifications for ${characteristic.uuid}")
        }
    }

    private fun parseImuData(data: ByteArray) {
        val packet = ImuDataPacket.fromBytes(data)
        if (packet != null) {
            coroutineScope.launch {
                _imuDataFlow.emit(packet)
            }
        } else {
            Log.w(TAG, "Failed to parse IMU packet (size: ${data.size})")
        }
    }

    // ============= Control Commands =============

    @SuppressLint("MissingPermission")
    fun sendCommand(command: ByteArray): Boolean {
        val gatt = bluetoothGatt ?: return false
        val characteristic = controlCharacteristic ?: return false

        return try {
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

    /**
     * Request battery level from device.
     */
    @SuppressLint("MissingPermission")
    fun requestBatteryLevel() {
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(SERVICE_UUID) ?: return
        val batteryChar = service.getCharacteristic(BATTERY_CHARACTERISTIC_UUID) ?: return
        gatt.readCharacteristic(batteryChar)
    }

    /**
     * Clean up resources.
     */
    fun cleanup() {
        stopScan()
        disconnect()
        coroutineScope.cancel()
        handler.removeCallbacksAndMessages(null)
    }
}

