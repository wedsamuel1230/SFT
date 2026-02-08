package smartracket.com.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import smartracket.com.R
import smartracket.com.ui.i18n.LocalAppStrings
import smartracket.com.ui.theme.SmartRacketColors
import smartracket.com.ui.theme.scoreColor
import smartracket.com.viewmodel.HighlightsViewModel
import smartracket.com.viewmodel.HighlightItemUi

/**
 * Highlights Gallery Screen - Saved highlight clips.
 *
 * Displays:
 * - Grid/list of saved highlights
 * - Filter by date/stroke type
 * - Sharing options
 * - Highlight details and playback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: HighlightsViewModel = hiltViewModel()
) {
    val highlights by viewModel.highlights.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedHighlight by viewModel.selectedHighlight.collectAsState()
    val strings = LocalAppStrings.current

    val filterOptions = listOf(strings.filterAll, strings.filterAutoSaved, strings.filterManual, strings.filterHighScore)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        TopAppBar(
            title = { Text(strings.highlightsTitle) },
            actions = {
                // View toggle (grid/list)
                var isGridView by remember { mutableStateOf(true) }
                IconButton(onClick = { isGridView = !isGridView }) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = strings.toggleView
                    )
                }
            }
        )

        // Filter chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { viewModel.setFilter(filter) },
                    label = {
                        Text(
                            text = filter,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        // Content
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (highlights.isEmpty()) {
            EmptyHighlightsState()
        } else {
            HighlightsGrid(
                highlights = highlights,
                onHighlightClick = { viewModel.selectHighlight(it) },
                onDeleteClick = { viewModel.deleteHighlight(it) },
                onShareClick = { viewModel.shareHighlight(it) }
            )
        }
    }

    // Highlight detail bottom sheet
    selectedHighlight?.let { highlight ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearSelectedHighlight() }
        ) {
            HighlightDetailSheet(
                highlight = highlight,
                onDismiss = { viewModel.clearSelectedHighlight() },
                onShare = { viewModel.shareHighlight(highlight.clipId) },
                onDelete = {
                    viewModel.deleteHighlight(highlight.clipId)
                    viewModel.clearSelectedHighlight()
                }
            )
        }
    }
}

@Composable
private fun EmptyHighlightsState() {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Stars,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = strings.noHighlightsYet,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = strings.noHighlightsMessage,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(onClick = { /* Navigate to training */ }) {
            Icon(Icons.Default.FitnessCenter, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.startTraining)
        }
    }
}

@Composable
private fun HighlightsGrid(
    highlights: List<HighlightItemUi>,
    onHighlightClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onShareClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(highlights) { highlight ->
            HighlightCard(
                highlight = highlight,
                onClick = { onHighlightClick(highlight.clipId) },
                onDelete = { onDeleteClick(highlight.clipId) },
                onShare = { onShareClick(highlight.clipId) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HighlightCard(
    highlight: HighlightItemUi,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val strings = LocalAppStrings.current
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(scoreColor(highlight.score).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_table_tennis),
                        contentDescription = null,
                        tint = scoreColor(highlight.score),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = highlight.score.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor(highlight.score)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = highlight.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (highlight.isAutoSaved) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = strings.autoLabel,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = highlight.strokeType,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = highlight.dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    highlight.heartRate?.let { hr ->
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = SmartRacketColors.HeartRatePink
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$hr BPM",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Menu button
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = strings.moreOptions)
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(strings.shareLabel) },
                        onClick = {
                            onShare()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.deleteLabel) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightDetailSheet(
    highlight: HighlightItemUi,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = highlight.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (highlight.isAutoSaved) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = strings.autoSavedLabel,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Motion visualization placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.motionVisualization,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = highlight.score.toString(),
                label = strings.score,
                valueColor = scoreColor(highlight.score)
            )
            StatItem(
                value = String.format("%.0f%%", highlight.confidence * 100),
                label = strings.confidence
            )
            StatItem(
                value = highlight.heartRate?.toString() ?: "-",
                label = strings.bpm
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback
        highlight.feedback?.let { feedback ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = feedback,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.deleteLabel)
            }

            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.shareLabel)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(strings.deleteHighlightTitle) },
            text = { Text(strings.deleteHighlightMessage) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun StatItem(
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

private fun getScoreColor(score: Int): Color = scoreColor(score)

