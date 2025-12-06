package smartracket.com.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import smartracket.com.model.BluetoothConnectionState
import smartracket.com.viewmodel.HomeViewModel

/**
 * Home Screen - Today's quick summary and quick actions.
 *
 * Displays:
 * - Connection status
 * - Today's training summary (strokes, avg score)
 * - Live heart rate (if available)
 * - Quick action buttons
 */
@Composable
fun HomeScreen(
    onStartTraining: () -> Unit,
    onViewAnalytics: () -> Unit,
    onViewHighlights: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val todaySummary by viewModel.todaySummary.collectAsState()
    val currentHeartRate by viewModel.currentHeartRate.collectAsState()
    val allTimeStats by viewModel.allTimeStats.collectAsState()
    val recentSessions by viewModel.recentSessions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome header
        Text(
            text = "SmartRacket Coach",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Connection status card
        ConnectionStatusCard(
            connectionState = connectionState,
            onConnectClick = { viewModel.startScan() }
        )

        // Today's summary card
        TodaySummaryCard(
            totalStrokes = todaySummary.totalStrokes,
            avgScore = todaySummary.avgScore,
            sessionsCount = todaySummary.sessionsCount,
            currentHeartRate = currentHeartRate
        )

        // Quick action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.FitnessCenter,
                label = "Start Training",
                onClick = onStartTraining,
                isPrimary = true
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Analytics,
                label = "Analytics",
                onClick = onViewAnalytics
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Stars,
                label = "Highlights",
                onClick = onViewHighlights
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.History,
                label = "History",
                onClick = onViewAnalytics
            )
        }

        // All-time stats
        AllTimeStatsCard(stats = allTimeStats)

        // Recent sessions
        if (recentSessions.isNotEmpty()) {
            Text(
                text = "Recent Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            recentSessions.take(3).forEach { session ->
                RecentSessionCard(session = session)
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: BluetoothConnectionState,
    onConnectClick: () -> Unit
) {
    val (statusColor, statusIcon, statusText) = when (connectionState) {
        is BluetoothConnectionState.Connected -> Triple(
            Color(0xFF4CAF50),
            Icons.Default.BluetoothConnected,
            "Connected to ${connectionState.device.deviceName}"
        )
        is BluetoothConnectionState.Connecting -> Triple(
            Color(0xFFFF9800),
            Icons.Default.BluetoothSearching,
            "Connecting to ${connectionState.deviceName}..."
        )
        BluetoothConnectionState.Scanning -> Triple(
            Color(0xFF2196F3),
            Icons.Default.BluetoothSearching,
            "Scanning for devices..."
        )
        BluetoothConnectionState.Disconnected -> Triple(
            Color(0xFF9E9E9E),
            Icons.Default.BluetoothDisabled,
            "Not connected"
        )
        is BluetoothConnectionState.Error -> Triple(
            Color(0xFFF44336),
            Icons.Default.Error,
            connectionState.message
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Paddle Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            if (connectionState is BluetoothConnectionState.Disconnected ||
                connectionState is BluetoothConnectionState.Error) {
                FilledTonalButton(onClick = onConnectClick) {
                    Text("Connect")
                }
            }
        }
    }
}

@Composable
private fun TodaySummaryCard(
    totalStrokes: Int,
    avgScore: Float,
    sessionsCount: Int,
    currentHeartRate: Int?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Today's Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = totalStrokes.toString(),
                    label = "Strokes",
                    icon = Icons.Default.SportsTennis
                )
                StatItem(
                    value = if (avgScore > 0) String.format("%.1f", avgScore) else "-",
                    label = "Avg Score",
                    icon = Icons.Default.Star
                )
                StatItem(
                    value = sessionsCount.toString(),
                    label = "Sessions",
                    icon = Icons.Default.Timer
                )
                StatItem(
                    value = currentHeartRate?.toString() ?: "-",
                    label = "BPM",
                    icon = Icons.Default.Favorite,
                    iconColor = Color(0xFFE91E63)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isPrimary: Boolean = false
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = modifier.height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label)
        }
    }
}

@Composable
private fun AllTimeStatsCard(stats: AllTimeStatsUi) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "All-Time Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${stats.totalSessions}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total Sessions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${stats.totalStrokes}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total Strokes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (stats.avgScore > 0) String.format("%.1f", stats.avgScore) else "-",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Avg Score",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDuration(stats.totalTrainingTimeMs),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Training Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSessionCard(session: RecentSessionUi) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(getScoreColor(session.avgScore).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("%.1f", session.avgScore),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = getScoreColor(session.avgScore)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.dateFormatted,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${session.totalStrokes} strokes • ${session.durationFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

private fun getScoreColor(score: Float): Color {
    return when {
        score >= 8 -> Color(0xFF4CAF50)
        score >= 6 -> Color(0xFF8BC34A)
        score >= 4 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

private fun formatDuration(ms: Long): String {
    val hours = ms / (1000 * 60 * 60)
    val minutes = (ms / (1000 * 60)) % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

// UI state data classes
data class AllTimeStatsUi(
    val totalSessions: Int = 0,
    val totalStrokes: Int = 0,
    val avgScore: Float = 0f,
    val totalTrainingTimeMs: Long = 0
)

data class RecentSessionUi(
    val sessionId: Long,
    val dateFormatted: String,
    val totalStrokes: Int,
    val avgScore: Float,
    val durationFormatted: String
)

data class TodaySummaryUi(
    val totalStrokes: Int = 0,
    val avgScore: Float = 0f,
    val sessionsCount: Int = 0
)

