package smartracket.com.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import smartracket.com.model.*
import smartracket.com.viewmodel.HighlightSaveState
import smartracket.com.viewmodel.TrainingViewModel

/**
 * Training Screen - Real-time stroke classification and feedback.
 *
 * Displays:
 * - Large score display
 * - Live feedback tips
 * - Elapsed time and stroke count
 * - Heart rate indicator
 * - Highlight save button
 * - Recent strokes list
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrainingViewModel = hiltViewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val currentScore by viewModel.currentScore.collectAsState()
    val currentFeedback by viewModel.currentFeedback.collectAsState()
    val strokeCount by viewModel.strokeCount.collectAsState()
    val averageScore by viewModel.averageScore.collectAsState()
    val lastStroke by viewModel.lastStroke.collectAsState()
    val recentStrokes by viewModel.recentStrokes.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val currentHeartRate by viewModel.currentHeartRate.collectAsState()
    val highlightSaveState by viewModel.highlightSaveState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()

    // Bluetooth permissions
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionState = rememberMultiplePermissionsState(bluetoothPermissions)

    // Request permissions on first launch
    LaunchedEffect(Unit) {
        if (!permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Battery level indicator
                    batteryLevel?.let { level ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    level > 80 -> Icons.Default.BatteryFull
                                    level > 50 -> Icons.Default.Battery5Bar
                                    level > 20 -> Icons.Default.Battery2Bar
                                    else -> Icons.Default.BatteryAlert
                                },
                                contentDescription = "Battery",
                                tint = if (level > 20) MaterialTheme.colorScheme.onSurface
                                       else Color.Red
                            )
                            Text(
                                text = "$level%",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Connection status
            if (connectionState !is BluetoothConnectionState.Connected) {
                ConnectionPromptCard(
                    connectionState = connectionState,
                    hasPermissions = permissionState.allPermissionsGranted,
                    onRequestPermissions = { permissionState.launchMultiplePermissionRequest() },
                    onStartScan = { viewModel.startBluetoothScan() },
                    onStopScan = { viewModel.stopBluetoothScan() },
                    discoveredDevices = viewModel.discoveredDevices.collectAsState().value,
                    onConnectDevice = { viewModel.connectToDevice(it) }
                )
            } else {
                // Training content
                when (sessionState) {
                    SessionState.IDLE -> {
                        IdleStateContent(
                            connectionState = connectionState,
                            onStartSession = { viewModel.startSession() }
                        )
                    }
                    SessionState.STARTING -> {
                        CircularProgressIndicator()
                        Text("Starting session...")
                    }
                    SessionState.ACTIVE, SessionState.PAUSED -> {
                        ActiveTrainingContent(
                            isPaused = sessionState == SessionState.PAUSED,
                            elapsedTime = elapsedTime,
                            currentScore = currentScore,
                            currentFeedback = currentFeedback,
                            strokeCount = strokeCount,
                            averageScore = averageScore,
                            lastStroke = lastStroke,
                            currentHeartRate = currentHeartRate,
                            highlightSaveState = highlightSaveState,
                            recentStrokes = recentStrokes,
                            onPause = { viewModel.pauseSession() },
                            onResume = { viewModel.resumeSession() },
                            onStop = { viewModel.stopSession() },
                            onSaveHighlight = { viewModel.saveHighlightManually() }
                        )
                    }
                    SessionState.STOPPING -> {
                        CircularProgressIndicator()
                        Text("Saving session...")
                    }
                    SessionState.COMPLETED -> {
                        SessionCompleteContent(
                            strokeCount = strokeCount,
                            averageScore = averageScore,
                            duration = elapsedTime,
                            onDismiss = { viewModel.resetSession() }
                        )
                    }
                }
            }
        }
    }

    // Error snackbar
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            // Auto-dismiss after 3 seconds
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
}

@Composable
private fun ConnectionPromptCard(
    connectionState: BluetoothConnectionState,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    discoveredDevices: List<DiscoveredDevice>,
    onConnectDevice: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.BluetoothSearching,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Connect Your SmartRacket",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                !hasPermissions -> {
                    Text(
                        text = "Bluetooth permissions are required to connect to your paddle.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRequestPermissions) {
                        Text("Grant Permissions")
                    }
                }
                connectionState is BluetoothConnectionState.Scanning -> {
                    Text(
                        text = "Scanning for devices...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Show discovered devices
                    if (discoveredDevices.isNotEmpty()) {
                        Text(
                            text = "Found Devices:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        discoveredDevices.forEach { device ->
                            DeviceListItem(
                                device = device,
                                onClick = { onConnectDevice(device.address) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onStopScan) {
                        Text("Stop Scanning")
                    }
                }
                connectionState is BluetoothConnectionState.Connecting -> {
                    Text(
                        text = "Connecting...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
                connectionState is BluetoothConnectionState.Error -> {
                    Text(
                        text = connectionState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onStartScan) {
                        Text("Try Again")
                    }
                }
                else -> {
                    Text(
                        text = "Make sure your SmartRacket paddle is powered on and nearby.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onStartScan) {
                        Icon(Icons.Default.BluetoothSearching, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan for Devices")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceListItem(
    device: DiscoveredDevice,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (device.isSmartRacketDevice)
                    Icons.Default.SportsTennis
                else Icons.Default.Bluetooth,
                contentDescription = null,
                tint = if (device.isSmartRacketDevice)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            // Signal strength indicator
            Text(
                text = "${device.rssi} dBm",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun IdleStateContent(
    connectionState: BluetoothConnectionState,
    onStartSession: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Connected device info
        if (connectionState is BluetoothConnectionState.Connected) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BluetoothConnected,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connected to ${connectionState.device.deviceName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Icon(
            imageVector = Icons.Default.SportsTennis,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Ready to Train",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your paddle is connected and ready for action!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onStartSession,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Training", fontSize = 18.sp)
        }
    }
}

@Composable
private fun ActiveTrainingContent(
    isPaused: Boolean,
    elapsedTime: Long,
    currentScore: Int,
    currentFeedback: String,
    strokeCount: Int,
    averageScore: Float,
    lastStroke: Stroke?,
    currentHeartRate: Int?,
    highlightSaveState: HighlightSaveState,
    recentStrokes: List<Stroke>,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSaveHighlight: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Timer and stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Timer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatTime(elapsedTime),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Stroke count
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$strokeCount",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Strokes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Average score
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (averageScore > 0) String.format("%.1f", averageScore) else "-",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Avg Score",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Heart rate
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currentHeartRate?.toString() ?: "-",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "BPM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large score display with animation
        AnimatedScoreDisplay(
            score = currentScore,
            strokeType = lastStroke?.strokeType
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback text
        AnimatedVisibility(
            visible = currentFeedback.isNotEmpty(),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = currentFeedback,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Highlight save button
        HighlightSaveButton(
            state = highlightSaveState,
            enabled = lastStroke != null && !isPaused,
            onClick = onSaveHighlight
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Control buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isPaused) {
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resume")
                }
            } else {
                OutlinedButton(onClick = onPause) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pause")
                }
            }

            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent strokes list
        if (recentStrokes.isNotEmpty()) {
            Text(
                text = "Recent Strokes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentStrokes) { stroke ->
                    RecentStrokeItem(stroke = stroke)
                }
            }
        }
    }
}

@Composable
private fun AnimatedScoreDisplay(
    score: Int,
    strokeType: String?
) {
    val animatedScale = remember { Animatable(1f) }

    LaunchedEffect(score) {
        if (score > 0) {
            animatedScale.animateTo(
                targetValue = 1.2f,
                animationSpec = tween(100)
            )
            animatedScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(200)
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Stroke type label
        strokeType?.let { type ->
            val displayName = StrokeType.fromString(type).displayName
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Score number
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(animatedScale.value)
                .clip(CircleShape)
                .background(getScoreColor(score).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (score > 0) score.toString() else "-",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = getScoreColor(score)
            )
        }
    }
}

@Composable
private fun HighlightSaveButton(
    state: HighlightSaveState,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val buttonColor = when (state) {
        is HighlightSaveState.Saved -> Color(0xFF4CAF50)
        is HighlightSaveState.Error -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.secondary
    }

    val buttonText = when (state) {
        HighlightSaveState.Idle -> "Save Highlight"
        HighlightSaveState.Saving -> "Saving..."
        is HighlightSaveState.Saved -> "Saved!"
        is HighlightSaveState.Error -> "Failed"
    }

    val buttonIcon = when (state) {
        HighlightSaveState.Idle -> Icons.Default.Stars
        HighlightSaveState.Saving -> Icons.Default.Sync
        is HighlightSaveState.Saved -> Icons.Default.Check
        is HighlightSaveState.Error -> Icons.Default.Error
    }

    FilledTonalButton(
        onClick = onClick,
        enabled = enabled && state is HighlightSaveState.Idle,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = buttonColor.copy(alpha = 0.2f),
            contentColor = buttonColor
        )
    ) {
        Icon(buttonIcon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(buttonText)
    }
}

@Composable
private fun RecentStrokeItem(stroke: Stroke) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(getScoreColor(stroke.score).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stroke.score.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = getScoreColor(stroke.score)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = StrokeType.fromString(stroke.strokeType).displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stroke.feedback,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }

            // Confidence badge
            Text(
                text = "${(stroke.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun SessionCompleteContent(
    strokeCount: Int,
    averageScore: Float,
    duration: Long,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color(0xFFFFD700)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Session Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Stats summary
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$strokeCount",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Strokes",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%.1f", averageScore),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = getScoreColor(averageScore.toInt())
                        )
                        Text(
                            text = "Avg Score",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatTime(duration),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Duration",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Done")
        }
    }
}

private fun getScoreColor(score: Int): Color {
    return when {
        score >= 8 -> Color(0xFF4CAF50)
        score >= 6 -> Color(0xFF8BC34A)
        score >= 4 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    val hours = ms / (1000 * 60 * 60)

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

