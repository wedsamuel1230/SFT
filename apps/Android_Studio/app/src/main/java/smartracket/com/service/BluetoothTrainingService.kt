package smartracket.com.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import smartracket.com.MainActivity
import smartracket.com.R
import smartracket.com.model.BluetoothConnectionState
import smartracket.com.repository.BluetoothRepository
import javax.inject.Inject

/**
 * Foreground service for maintaining Bluetooth connection during training.
 *
 * Runs as a foreground service to:
 * - Keep the Bluetooth connection alive even when the app is in background
 * - Show a persistent notification with connection status
 * - Handle reconnection attempts
 */
@AndroidEntryPoint
class BluetoothTrainingService : Service() {

    companion object {
        const val CHANNEL_ID = "smartracket_bluetooth_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.smartracket.action.START_BLUETOOTH_SERVICE"
        const val ACTION_STOP = "com.smartracket.action.STOP_BLUETOOTH_SERVICE"
    }

    @Inject
    lateinit var bluetoothRepository: BluetoothRepository

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundService()
            ACTION_STOP -> stopService()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification("Connecting...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Observe connection state and update notification
        serviceScope.launch {
            bluetoothRepository.connectionState.collectLatest { state ->
                val message = when (state) {
                    is BluetoothConnectionState.Connected ->
                        "Connected to ${state.device.deviceName}"
                    is BluetoothConnectionState.Connecting -> "Connecting to ${state.deviceName}..."
                    BluetoothConnectionState.Scanning -> "Scanning for devices..."
                    BluetoothConnectionState.Disconnected -> "Disconnected"
                    is BluetoothConnectionState.Error -> "Error: ${state.message}"
                }
                updateNotification(message)
            }
        }
    }

    private fun stopService() {
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SmartRacket Bluetooth",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows Bluetooth connection status"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(message: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SmartRacket Coach")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

