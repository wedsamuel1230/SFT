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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import smartracket.com.R
import smartracket.com.model.*
import smartracket.com.ui.i18n.LocalAppStrings
import smartracket.com.ui.theme.SmartRacketColors
import smartracket.com.ui.theme.scoreColor
import smartracket.com.viewmodel.TrainingViewModel

/**
 * Training Screen - Real-time stroke classification and feedback.
 *
 * Displays:
 * - Large score display
 * - Live feedback tips
 * - Elapsed time and stroke count
 * - Heart rate indicator
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
    val errorMessage by viewModel.errorMessage.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val healthAlert by viewModel.showHealthAlert.collectAsState()
    val strings = LocalAppStrings.current

    // Health alert dialog
    healthAlert?.let { alert ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissHealthAlert() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = strings.healthAlertTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = when (alert.type) {
                            smartracket.com.repository.HealthAlertType.HEART_RATE_HIGH,
                            smartracket.com.repository.HealthAlertType.HEART_RATE_DANGER ->
                                strings.heartRateTooHigh
                            smartracket.com.repository.HealthAlertType.BLOOD_PRESSURE_HIGH ->
                                strings.bloodPressureTooHigh
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.takeARestMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.pauseForRest() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(strings.restNow)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissHealthAlert() }) {
                    Text(strings.stayActive)
                }
            }
        )
    }

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
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        strings.trainingTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    // Battery level indicator
                    batteryLevel?.let { level ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    level > 80 -> Icons.Default.BatteryFull
                                    level > 50 -> Icons.Default.Battery5Bar
                                    level > 20 -> Icons.Default.Battery2Bar
                                    else -> Icons.Default.BatteryAlert
                                },
                                contentDescription = strings.battery,
                                tint = if (level > 20) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$level%",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(strings.startingSession)
                            }
                        }
                    }
                    SessionState.ACTIVE, SessionState.PAUSED -> {
                        ActiveTrainingContent(
                            isPaused = sessionState == SessionState.PAUSED,
                            isConnected = connectionState is BluetoothConnectionState.Connected,
                            elapsedTime = elapsedTime,
                            currentScore = currentScore,
                            currentFeedback = currentFeedback,
                            strokeCount = strokeCount,
                            averageScore = averageScore,
                            lastStroke = lastStroke,
                            currentHeartRate = currentHeartRate,
                            recentStrokes = recentStrokes,
                            onPause = { viewModel.pauseSession() },
                            onResume = { viewModel.resumeSession() },
                            onStop = { viewModel.stopSession() }
                        )
                    }
                    SessionState.STOPPING -> {
                         Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(strings.savingSession)
                            }
                        }
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

    // Error snackbar ...existing code...
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
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large, // Squircle
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.BluetoothSearching,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = strings.connectSmartRacket,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                !hasPermissions -> {
                    Text(
                        text = strings.btPermissionsRequired,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onRequestPermissions, shape = CircleShape) {
                        Text(strings.grantPermissions)
                    }
                }
                connectionState is BluetoothConnectionState.Scanning -> {
                    Text(
                        text = strings.scanningForDevices,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Show discovered devices
                    if (discoveredDevices.isNotEmpty()) {
                        Text(
                            text = "${strings.foundDevices}:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                             color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        discoveredDevices.forEach { device ->
                            DeviceListItem(
                                device = device,
                                onClick = { onConnectDevice(device.address) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onStopScan) {
                        Text(strings.stopScanning)
                    }
                }
                connectionState is BluetoothConnectionState.Connecting -> {
                    Text(
                        text = strings.connecting,
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
                    Button(onClick = onStartScan, shape = CircleShape) {
                        Text(strings.tryAgain)
                    }
                }
                else -> {
                    Text(
                        text = strings.makeSurePaddleOn,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onStartScan, shape = CircleShape) {
                        Icon(Icons.Default.BluetoothSearching, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.scanForDevices)
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
    val strings = LocalAppStrings.current
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val deviceIcon = if (device.isSmartRacketDevice) {
                painterResource(R.drawable.ic_table_tennis)
            } else {
                rememberVectorPainter(Icons.Default.Bluetooth)
            }
            Icon(
                painter = deviceIcon,
                contentDescription = null,
                modifier = if (device.isSmartRacketDevice) Modifier.size(16.dp) else Modifier,
                tint = if (device.isSmartRacketDevice)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: strings.unknownDevice,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Signal strength indicator
            Text(
                text = "${device.rssi} dBm",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IdleStateContent(
    connectionState: BluetoothConnectionState,
    onStartSession: () -> Unit
) {
    val strings = LocalAppStrings.current
    // One UI: Content centered, action at bottom
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 80.dp), // Shift visual up slightly
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Connected device info
            if (connectionState is BluetoothConnectionState.Connected) {
                Surface(
                    shape = CircleShape,
                    color = SmartRacketColors.StatusConnected.copy(alpha = 0.1f),
                    contentColor = SmartRacketColors.StatusConnected
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothConnected,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = connectionState.device.deviceName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Main Visual
            Icon(
                painter = painterResource(R.drawable.ic_table_tennis),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = strings.readyToTrain,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                 color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.paddleReadyAction,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Action Button at bottom
        Button(
            onClick = onStartSession,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(56.dp),
            shape = MaterialTheme.shapes.extraLarge // One UI fully rounded
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.startTrainingBtn, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActiveTrainingContent(
    isPaused: Boolean,
    isConnected: Boolean,
    elapsedTime: Long,
    currentScore: Int,
    currentFeedback: String,
    strokeCount: Int,
    averageScore: Float,
    lastStroke: Stroke?,
    currentHeartRate: Int?,
    recentStrokes: List<Stroke>,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val strings = LocalAppStrings.current

    // One UI Layout Strategy:
    // Top 35%: Viewing Area (Stats, Live Score)
    // Bottom 65%: Interaction Area (List, Controls)
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Viewing Area (Top 35%) ─────────────────────────────────
        Column(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Timer and stats items in a single row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(formatTime(elapsedTime), strings.duration)
                StatColumn("$strokeCount", strings.strokesLabel)
                StatColumn(if (averageScore > 0) String.format("%.1f", averageScore) else "-", strings.avgScoreLabel)

                // Heart rate indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = SmartRacketColors.HeartRatePink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentHeartRate?.toString() ?: "-",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = strings.bpmLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Score Display
            AnimatedScoreDisplay(
                score = currentScore,
                strokeType = lastStroke?.strokeType
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Feedback Toast
            AnimatedVisibility(
                visible = currentFeedback.isNotEmpty(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium, // Squircle 16dp
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = currentFeedback,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // ── Interaction Area (Bottom 65%) ──────────────────────────
        Surface(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), // One UI sheet style
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) // Subtle separation
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Recent Strokes List Handled Here
                // This fills the space between Viewing Area and Bottom Buttons
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Recent Strokes", // TODO: i18n
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentStrokes) { stroke ->
                        RecentStrokeItem(stroke)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action area at the very bottom
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    // Main Control Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Pause/Resume (Primary action, larger)
                        Button(
                            onClick = if (isPaused) onResume else onPause,
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp), // Tall target for easy reach
                            shape = MaterialTheme.shapes.extraLarge, // Squircle 28dp
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPaused) SmartRacketColors.StatusConnected else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, 
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isPaused) strings.resume else strings.pause,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Stop (Secondary action)
                        FilledTonalButton(
                            onClick = onStop,
                            modifier = Modifier
                                .height(64.dp)
                                .width(80.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = strings.stop)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                .background(scoreColor(score).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (score > 0) score.toString() else "-",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = scoreColor(score)
            )
        }
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
                    .background(scoreColor(stroke.score).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stroke.score.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor(stroke.score)
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
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = SmartRacketColors.TrophyGold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = strings.sessionComplete,
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
                            text = strings.strokesLabel,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%.1f", averageScore),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor(averageScore.toInt())
                        )
                        Text(
                            text = strings.avgScoreLabel,
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
                            text = strings.duration,
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
            Text(strings.done)
        }
    }
}

private fun getScoreColor(score: Int): Color = scoreColor(score)

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

