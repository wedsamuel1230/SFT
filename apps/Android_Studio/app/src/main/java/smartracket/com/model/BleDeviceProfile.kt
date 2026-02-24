package smartracket.com.model

import java.util.UUID

/**
 * BLE device profile describing the GATT services and characteristics
 * of a SmartRacket paddle.
 *
 * The default values match the original ESP32 firmware. Users can
 * register custom profiles if they build a different paddle variant.
 */
data class BleDeviceProfile(
    /** Human-readable label, e.g. "SmartRacket v1" */
    val label: String,

    /** Device name prefix used for BLE scan filtering (e.g. "SmartRacket") */
    val deviceNamePrefix: String,

    /** GATT service UUID that groups all paddle characteristics */
    val serviceUuid: UUID,

    /** Characteristic that delivers IMU data via Notify */
    val imuCharacteristicUuid: UUID,

    /** Characteristic used to send control commands via Write */
    val controlCharacteristicUuid: UUID,

    /** Characteristic that reports battery level via Read */
    val batteryCharacteristicUuid: UUID
) {
    companion object {
        /**
         * The factory-default profile for the ESP32 SmartRacket paddle.
         *
         * This was previously hard-coded in [smartracket.com.utils.BluetoothManager].
         */
        val DEFAULT = BleDeviceProfile(
            label = "SmartRacket v1",
            deviceNamePrefix = "SmartRacket",
            serviceUuid = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b"),
            imuCharacteristicUuid = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8"),
            controlCharacteristicUuid = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9"),
            batteryCharacteristicUuid = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26aa")
        )

        /** Client Characteristic Configuration Descriptor (standard BLE UUID). */
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
