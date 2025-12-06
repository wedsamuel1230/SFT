package smartracket.com.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import smartracket.com.viewmodel.AnalyticsViewModel
import smartracket.com.viewmodel.SessionDetailUi
import smartracket.com.viewmodel.StrokeDistributionItem

/**
 * Analytics Screen - Training history and performance analytics.
 *
 * Displays:
 * - Session history list with filters
 * - Stroke distribution charts
 * - Score trends over time
 * - Performance evolution graphs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val selectedSession by viewModel.selectedSession.collectAsState()
    val strokeDistribution by viewModel.strokeDistribution.collectAsState()
    val scoreTrend by viewModel.scoreTrend.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("History", "Statistics", "Trends")

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Tab row
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> SessionHistoryTab(
                sessions = sessions,
                selectedSession = selectedSession,
                dateFilter = dateFilter,
                isLoading = isLoading,
                onSessionClick = { viewModel.selectSession(it) },
                onDateFilterChange = { viewModel.setDateFilter(it) },
                onDeleteSession = { viewModel.deleteSession(it) }
            )
            1 -> StatisticsTab(
                strokeDistribution = strokeDistribution,
                sessions = sessions
            )
            2 -> TrendsTab(
                scoreTrend = scoreTrend
            )
        }
    }

    // Session detail bottom sheet
    selectedSession?.let { session ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearSelectedSession() }
        ) {
            SessionDetailSheet(
                session = session,
                onDismiss = { viewModel.clearSelectedSession() }
            )
        }
    }
}

@Composable
private fun SessionHistoryTab(
    sessions: List<SessionDetailUi>,
    selectedSession: SessionDetailUi?,
    dateFilter: DateFilterOption,
    isLoading: Boolean,
    onSessionClick: (Long) -> Unit,
    onDateFilterChange: (DateFilterOption) -> Unit,
    onDeleteSession: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Date filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateFilterOption.entries.forEach { option ->
                FilterChip(
                    selected = dateFilter == option,
                    onClick = { onDateFilterChange(option) },
                    label = { Text(option.displayName) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (sessions.isEmpty()) {
            EmptyStateMessage(
                icon = Icons.Default.History,
                title = "No Training Sessions",
                message = "Start your first training session to see your history here."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions) { session ->
                    SessionCard(
                        session = session,
                        onClick = { onSessionClick(session.sessionId) },
                        onDelete = { onDeleteSession(session.sessionId) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionCard(
    session: SessionDetailUi,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
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
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(getScoreColor(session.avgScore).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("%.1f", session.avgScore),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = getScoreColor(session.avgScore)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.dateFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${session.totalStrokes} strokes • ${session.durationFormatted}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                session.avgHeartRate?.let { hr ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$hr BPM avg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Session?") },
            text = { Text("This will permanently delete this training session and all associated data.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatisticsTab(
    strokeDistribution: List<StrokeDistributionItem>,
    sessions: List<SessionDetailUi>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary stats
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatColumn(
                            value = "${sessions.size}",
                            label = "Sessions"
                        )
                        StatColumn(
                            value = "${sessions.sumOf { it.totalStrokes }}",
                            label = "Total Strokes"
                        )
                        StatColumn(
                            value = if (sessions.isNotEmpty())
                                String.format("%.1f", sessions.map { it.avgScore }.average())
                            else "-",
                            label = "Avg Score"
                        )
                    }
                }
            }
        }

        // Stroke distribution chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Stroke Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (strokeDistribution.isNotEmpty()) {
                        StrokeDistributionChart(
                            data = strokeDistribution,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        )
                    } else {
                        Text(
                            text = "No data available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // Stroke type breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Stroke Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    strokeDistribution.forEach { item ->
                        StrokeTypeRow(item = item)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StrokeDistributionChart(
    data: List<StrokeDistributionItem>,
    modifier: Modifier = Modifier
) {
    val barColors = listOf(
        android.graphics.Color.parseColor("#4CAF50"),
        android.graphics.Color.parseColor("#2196F3"),
        android.graphics.Color.parseColor("#FF9800"),
        android.graphics.Color.parseColor("#9C27B0"),
        android.graphics.Color.parseColor("#E91E63"),
        android.graphics.Color.parseColor("#00BCD4")
    )

    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = true
                setDrawGridBackground(false)
                setDrawBarShadow(false)
                setFitBars(true)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    valueFormatter = IndexAxisValueFormatter(data.map { it.strokeType })
                    labelRotationAngle = -45f
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    axisMinimum = 0f
                }

                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val entries = data.mapIndexed { index, item ->
                BarEntry(index.toFloat(), item.count.toFloat())
            }

            val dataSet = BarDataSet(entries, "Strokes").apply {
                colors = barColors.take(data.size)
                valueTextSize = 10f
            }

            chart.data = BarData(dataSet)
            chart.invalidate()
        },
        modifier = modifier
    )
}

@Composable
private fun StrokeTypeRow(item: StrokeDistributionItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.strokeType,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${item.count}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.width(8.dp))

        LinearProgressIndicator(
            progress = { item.percentage / 100f },
            modifier = Modifier
                .width(100.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${item.percentage.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun TrendsTab(
    scoreTrend: List<ScoreTrendPoint>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Score Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (scoreTrend.isNotEmpty()) {
                        ScoreTrendChart(
                            data = scoreTrend,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        )
                    } else {
                        EmptyStateMessage(
                            icon = Icons.Default.TrendingUp,
                            title = "Not Enough Data",
                            message = "Complete more training sessions to see your score trends."
                        )
                    }
                }
            }
        }

        // Performance insights
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Performance Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (scoreTrend.size >= 2) {
                        val trend = scoreTrend.last().score - scoreTrend.first().score
                        val trendIcon = if (trend >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown
                        val trendColor = if (trend >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        val trendText = if (trend >= 0) "improving" else "declining"

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = trendIcon,
                                contentDescription = null,
                                tint = trendColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your performance is $trendText (${if (trend >= 0) "+" else ""}${String.format("%.1f", trend)} points)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        Text(
                            text = "Complete more sessions to get insights",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreTrendChart(
    data: List<ScoreTrendPoint>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setDrawGridBackground(false)
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    valueFormatter = IndexAxisValueFormatter(data.map { it.dateLabel })
                    labelRotationAngle = -45f
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    axisMinimum = 0f
                    axisMaximum = 10f
                }

                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val entries = data.mapIndexed { index, point ->
                Entry(index.toFloat(), point.score)
            }

            val dataSet = LineDataSet(entries, "Score").apply {
                color = android.graphics.Color.parseColor("#4CAF50")
                lineWidth = 2f
                setDrawCircles(true)
                circleRadius = 4f
                setCircleColor(android.graphics.Color.parseColor("#4CAF50"))
                setDrawFilled(true)
                fillColor = android.graphics.Color.parseColor("#4CAF50")
                fillAlpha = 50
                valueTextSize = 10f
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }

            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        modifier = modifier
    )
}

@Composable
private fun SessionDetailSheet(
    session: SessionDetailUi,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = session.dateFormatted,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(value = "${session.totalStrokes}", label = "Strokes")
            StatColumn(
                value = String.format("%.1f", session.avgScore),
                label = "Avg Score",
                valueColor = getScoreColor(session.avgScore)
            )
            StatColumn(value = session.durationFormatted, label = "Duration")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stroke distribution
        Text(
            text = "Stroke Distribution",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        session.strokeDistribution.forEach { (type, count) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = type, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Close")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun EmptyStateMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
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

// Data classes for trends
data class ScoreTrendPoint(
    val dateLabel: String,
    val score: Float
)

enum class DateFilterOption(val displayName: String) {
    ALL("All"),
    WEEK("This Week"),
    MONTH("This Month"),
    THREE_MONTHS("3 Months")
}

