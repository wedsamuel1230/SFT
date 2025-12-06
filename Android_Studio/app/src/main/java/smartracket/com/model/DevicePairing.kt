package smartracket.com.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a paired Bluetooth device (ESP32 paddle).
 */
@Entity(tableName = "device_pairings")
data class DevicePairing(
    @PrimaryKey
    val deviceId: String,

    /** User-friendly device name */
    val deviceName: String,

    /** Bluetooth MAC address */
    val bluetoothMacAddress: String,

    /** Last successful connection timestamp */
    val lastConnected: Long? = null,

    /** Whether this is the primary/default device */
    val isPrimary: Boolean = false,

    /** Battery level (if reported by device) */
    val batteryLevel: Int? = null,

    /** Firmware version of the ESP32 */
    val firmwareVersion: String? = null,

    /** Device added timestamp */
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Bluetooth connection state.
 */
sealed class BluetoothConnectionState {
    data object Disconnected : BluetoothConnectionState()
    data object Scanning : BluetoothConnectionState()
    data class Connecting(val deviceName: String) : BluetoothConnectionState()
    data class Connected(val device: DevicePairing) : BluetoothConnectionState()
    data class Error(val message: String, val code: Int? = null) : BluetoothConnectionState()
}

/**
 * Bluetooth device discovered during scanning.
 */
data class DiscoveredDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val isSmartRacketDevice: Boolean = false
)

/**
 * IMU data packet received from ESP32.
 *
 * The ESP32 sends data in a specific binary format:
 * [Header(2)] [Timestamp(4)] [AccelX(4)] [AccelY(4)] [AccelZ(4)] [GyroX(4)] [GyroY(4)] [GyroZ(4)] [Checksum(1)]
 */
data class ImuDataPacket(
    /** Packet timestamp from ESP32 (milliseconds since device boot) */
    val timestamp: Long,

    /** Accelerometer X-axis (m/s²) */
    val accelX: Float,

    /** Accelerometer Y-axis (m/s²) */
    val accelY: Float,

    /** Accelerometer Z-axis (m/s²) */
    val accelZ: Float,

    /** Gyroscope X-axis (rad/s) */
    val gyroX: Float,

    /** Gyroscope Y-axis (rad/s) */
    val gyroY: Float,

    /** Gyroscope Z-axis (rad/s) */
    val gyroZ: Float,

    /** Battery level (optional, included in some packets) */
    val batteryLevel: Int? = null
) {
    /**
     * Calculate acceleration magnitude.
     */
    fun accelerationMagnitude(): Float {
        return kotlin.math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ)
    }

    /**
     * Calculate angular velocity magnitude.
     */
    fun angularVelocityMagnitude(): Float {
        return kotlin.math.sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
    }

    companion object {
        const val HEADER_BYTE_1: Byte = 0x5A
        const val HEADER_BYTE_2: Byte = 0xA5.toByte()
        const val PACKET_SIZE = 31 // Total packet size in bytes

        /**
         * Parse IMU data from raw bytes.
         */
        fun fromBytes(bytes: ByteArray): ImuDataPacket? {
            if (bytes.size < PACKET_SIZE) return null
            if (bytes[0] != HEADER_BYTE_1 || bytes[1] != HEADER_BYTE_2) return null

            // Verify checksum
            val checksum = bytes.take(PACKET_SIZE - 1).fold(0) { acc, b -> acc xor b.toInt() }
            if (checksum.toByte() != bytes[PACKET_SIZE - 1]) return null

            return try {
                val buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                buffer.position(2) // Skip header

                ImuDataPacket(
                    timestamp = buffer.int.toLong() and 0xFFFFFFFFL,
                    accelX = buffer.float,
                    accelY = buffer.float,
                    accelZ = buffer.float,
                    gyroX = buffer.float,
                    gyroY = buffer.float,
                    gyroZ = buffer.float,
                    batteryLevel = if (bytes.size > PACKET_SIZE) bytes[PACKET_SIZE].toInt() else null
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

