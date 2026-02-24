package smartracket.com.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import smartracket.com.model.DevicePairing

/**
 * Data Access Object for DevicePairing entities.
 *
 * Provides CRUD operations and queries for paired Bluetooth devices.
 */
@Dao
interface DevicePairingDao {

    // ============= Insert Operations =============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DevicePairing): Long

    // ============= Update Operations =============

    @Update
    suspend fun update(device: DevicePairing)

    @Query("UPDATE device_pairings SET lastConnected = :timestamp WHERE deviceId = :deviceId")
    suspend fun updateLastConnected(deviceId: String, timestamp: Long)

    @Query("UPDATE device_pairings SET batteryLevel = :level WHERE deviceId = :deviceId")
    suspend fun updateBatteryLevel(deviceId: String, level: Int)

    @Query("UPDATE device_pairings SET isPrimary = 0")
    suspend fun clearPrimaryDevice()

    @Query("UPDATE device_pairings SET isPrimary = 1 WHERE deviceId = :deviceId")
    suspend fun setPrimaryDevice(deviceId: String)

    // ============= Delete Operations =============

    @Delete
    suspend fun delete(device: DevicePairing)

    @Query("DELETE FROM device_pairings WHERE deviceId = :deviceId")
    suspend fun deleteById(deviceId: String)

    @Query("DELETE FROM device_pairings")
    suspend fun deleteAll()

    // ============= Query Operations =============

    @Query("SELECT * FROM device_pairings WHERE deviceId = :deviceId")
    suspend fun getById(deviceId: String): DevicePairing?

    @Query("SELECT * FROM device_pairings WHERE bluetoothMacAddress = :macAddress")
    suspend fun getByMacAddress(macAddress: String): DevicePairing?

    @Query("SELECT * FROM device_pairings ORDER BY lastConnected DESC")
    fun getAllFlow(): Flow<List<DevicePairing>>

    @Query("SELECT * FROM device_pairings ORDER BY lastConnected DESC")
    suspend fun getAll(): List<DevicePairing>

    @Query("SELECT * FROM device_pairings WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryDevice(): DevicePairing?

    @Query("SELECT * FROM device_pairings WHERE isPrimary = 1 LIMIT 1")
    fun getPrimaryDeviceFlow(): Flow<DevicePairing?>

    @Query("SELECT * FROM device_pairings ORDER BY lastConnected DESC LIMIT 1")
    suspend fun getLastConnectedDevice(): DevicePairing?

    // ============= Aggregation Queries =============

    @Query("SELECT COUNT(*) FROM device_pairings")
    suspend fun getCount(): Int
}

