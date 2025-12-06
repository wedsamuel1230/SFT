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
import smartracket.com.model.SessionState
import smartracket.com.repository.TrainingRepository
import javax.inject.Inject

/**
 * Foreground service for maintaining active training session.
 *
 * Runs as a foreground service to:
 * - Keep the training session alive during background
 * - Show a persistent notification with session stats
 * - Ensure session data is properly saved
 */
@AndroidEntryPoint
class TrainingSessionService : Service() {

    companion object {
        const val CHANNEL_ID = "smartracket_training_channel"
        const val NOTIFICATION_ID = 1002

        const val ACTION_START = "com.smartracket.action.START_TRAINING_SERVICE"
        const val ACTION_STOP = "com.smartracket.action.STOP_TRAINING_SERVICE"

        const val EXTRA_SESSION_ID = "session_id"
    }

    @Inject
    lateinit var trainingRepository: TrainingRepository

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var sessionId: Long = 0
    private var startTime: Long = 0
    private var strokeCount: Int = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                sessionId = intent.getLongExtra(EXTRA_SESSION_ID, 0)
                startTime = System.currentTimeMillis()
                startForegroundService()
            }
            ACTION_STOP -> stopService()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification(0, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Update notification periodically
        serviceScope.launch {
            while (isActive) {
                delay(1000)
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                updateNotification(elapsed.toInt(), strokeCount)
            }
        }

        // Observe stroke count
        if (sessionId > 0) {
            serviceScope.launch {
                trainingRepository.getStrokesForSessionFlow(sessionId).collectLatest { strokes ->
                    strokeCount = strokes.size
                }
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
                "Training Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows training session status"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(elapsedSeconds: Int, strokes: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Training in Progress")
            .setContentText("$timeText • $strokes strokes")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(elapsedSeconds: Int, strokes: Int) {
        val notification = createNotification(elapsedSeconds, strokes)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

