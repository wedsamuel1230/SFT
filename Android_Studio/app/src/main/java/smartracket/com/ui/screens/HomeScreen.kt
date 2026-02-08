package smartracket.com.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import smartracket.com.R
import smartracket.com.model.BluetoothConnectionState
import smartracket.com.ui.i18n.LocalAppStrings
import smartracket.com.ui.theme.SmartRacketColors
import smartracket.com.ui.theme.scoreColor
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
    val strings = LocalAppStrings.current
    val greeting = remember(strings.homeGreetings) { strings.homeGreetings.random() }

    // One UI Style: Main content in a LazyColumn
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Viewing Area (Top 30-40%) ─────────────────────────────

        // Large App Header (One UI style)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)
            ) {
                Text(
                    text = strings.appName,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Connection Status
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ConnectionStatusCard(
                    connectionState = connectionState,
                    onConnectClick = { viewModel.startScan() }
                )
            }
        }

        // Today's Summary
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                TodaySummaryCard(
                    totalStrokes = todaySummary.totalStrokes,
                    avgScore = todaySummary.avgScore,
                    sessionsCount = todaySummary.sessionsCount,
                    currentHeartRate = currentHeartRate
                )
            }
        }

        // ── Interaction Area (Bottom 60-70%) ─────────────────────

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.quickActionsTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Quick Action Grid (2 Rows of 2)
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FitnessCenter,
                        label = strings.startTraining,
                        onClick = onStartTraining,
                        isPrimary = true,
                        // One UI: Primary action often has distinctive color/shape
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Analytics,
                        label = strings.analyticsLabel,
                        onClick = onViewAnalytics,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Stars,
                        label = strings.highlightsLabel,
                        onClick = onViewHighlights,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.History,
                        label = strings.history, // Assuming "history" maps to Analytics/History
                        onClick = onViewAnalytics,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Section: All Time Stats
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                AllTimeStatsCard(stats = allTimeStats)
            }
        }

        // Section: Recent Sessions
        if (recentSessions.isNotEmpty()) {
            item {
                Text(
                    text = strings.recentSessions,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(recentSessions.take(3)) { session ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    RecentSessionCard(session = session)
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: BluetoothConnectionState,
    onConnectClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val (statusColor, statusIcon, statusText) = when (connectionState) {
        is BluetoothConnectionState.Connected -> Triple(
            SmartRacketColors.StatusConnected,
            Icons.Default.BluetoothConnected,
            "${strings.connectedTo} ${connectionState.device.deviceName}"
        )
        is BluetoothConnectionState.Connecting -> Triple(
            SmartRacketColors.ScoreAverage,
            Icons.Default.BluetoothSearching,
            "${strings.connectingTo} ${connectionState.deviceName}..."
        )
        BluetoothConnectionState.Scanning -> Triple(
            SmartRacketColors.StatusConnecting,
            Icons.Default.BluetoothSearching,
            strings.scanningForDevices
        )
        BluetoothConnectionState.Disconnected -> Triple(
            SmartRacketColors.StatusDisconnected,
            Icons.Default.BluetoothDisabled,
            strings.notConnected
        )
        is BluetoothConnectionState.Error -> Triple(
            SmartRacketColors.StatusError,
            Icons.Default.Error,
            connectionState.message
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large, // Squircle 20.dp
        colors = CardDefaults.cardColors(
            containerColor = convertToBackgroundColor(statusColor, isSystemInDarkTheme())
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp), // More breathing room
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.paddleStatus,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (connectionState is BluetoothConnectionState.Disconnected ||
                connectionState is BluetoothConnectionState.Error
            ) {
                // Primary button for primary action
                Button(
                    onClick = onConnectClick,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Text(strings.connect)
                }
            }
        }
    }
}

// Helper to create a subtle background from a status color
@Composable
private fun convertToBackgroundColor(color: Color, isDark: Boolean): Color {
    // One UI often uses very subtle tinted surfaces or just Surface Variant
    return if (isDark) {
         MaterialTheme.colorScheme.surfaceVariant
    } else {
         MaterialTheme.colorScheme.surface
    }.let { base ->
         // Tint slightly if needed, or just return Surface Variant
         // Ideally use Surface Variant as base
         MaterialTheme.colorScheme.surfaceVariant
    }
    // Alternatively, just use Surface Variant for consistency
    // return MaterialTheme.colorScheme.surfaceVariant
}


@Composable
private fun TodaySummaryCard(
    totalStrokes: Int,
    avgScore: Float,
    sessionsCount: Int,
    currentHeartRate: Int?
) {
    val strings = LocalAppStrings.current
    // Primary Container used for the main summary card - High emphasis
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large, // Squircle
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = strings.todaySummary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween // Spread out
            ) {
                StatItem(
                    value = totalStrokes.toString(),
                    label = strings.strokes,
                    icon = painterResource(R.drawable.ic_table_tennis),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatItem(
                    value = if (avgScore > 0) String.format("%.1f", avgScore) else "-",
                    label = strings.avgScore,
                    icon = rememberVectorPainter(Icons.Default.Star),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatItem(
                    value = sessionsCount.toString(),
                    label = strings.sessions,
                    icon = rememberVectorPainter(Icons.Default.Timer),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatItem(
                    value = currentHeartRate?.toString() ?: "-",
                    label = strings.bpm,
                    icon = rememberVectorPainter(Icons.Default.Favorite),
                    iconColor = SmartRacketColors.HeartRatePink,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: Painter,
    iconColor: Color? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val finalIconColor = iconColor ?: contentColor
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = finalIconColor.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge, // Bigger numbers
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isPrimary: Boolean = false,
    containerColor: Color
) {
    // Using Card or Button? One UI standard buttons are capsular or rounded rectangles (squircle)
    // We use Button for interaction feedback
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp), // Taller touch target
        shape = MaterialTheme.shapes.large, // Squircle
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp) // Flat style
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun AllTimeStatsCard(stats: AllTimeStatsUi) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // Subtle
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = strings.allTimeStatistics,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Using helper function or re-writing simple columns
                StatsPair(
                     value1 = "${stats.totalSessions}", label1 = strings.totalSessions,
                     value2 = "${stats.totalStrokes}", label2 = strings.totalStrokes
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatsPair(
                    value1 = if (stats.avgScore > 0) String.format("%.1f", stats.avgScore) else "-",
                    label1 = strings.avgScore,
                    value2 = formatDuration(stats.totalTrainingTimeMs),
                    label2 = strings.trainingTime
                )
            }
        }
    }
}

@Composable
private fun StatsPair(value1: String, label1: String, value2: String, label2: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
         Column {
            Text(text = value1, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = label1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = value2, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = label2, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun RecentSessionCard(session: RecentSessionUi) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface, // Clean white/dark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // One UI favors flat or soft shadow. We'll stick to flat with border or just background contrast.
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
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
                    .background(scoreColor(session.avgScore).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("%.1f", session.avgScore),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor(session.avgScore)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.dateFormatted,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${session.totalStrokes} strokes • ${session.durationFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = strings.viewDetails,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

private fun getScoreColor(score: Float): Color = scoreColor(score)

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
