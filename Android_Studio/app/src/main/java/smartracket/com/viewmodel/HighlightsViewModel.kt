package smartracket.com.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import smartracket.com.model.HighlightClip
import smartracket.com.model.StrokeType
import smartracket.com.repository.HighlightRepository
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for the Highlights Gallery Screen.
 *
 * Provides:
 * - List of saved highlights
 * - Filtering by type/date
 * - Share and delete functionality
 */
@HiltViewModel
class HighlightsViewModel @Inject constructor(
    private val highlightRepository: HighlightRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())

    // All highlights
    private val _allHighlights = MutableStateFlow<List<HighlightItemUi>>(emptyList())

    // Filtered highlights
    private val _highlights = MutableStateFlow<List<HighlightItemUi>>(emptyList())
    val highlights: StateFlow<List<HighlightItemUi>> = _highlights.asStateFlow()

    // Selected filter
    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    // Selected highlight for detail view
    private val _selectedHighlight = MutableStateFlow<HighlightItemUi?>(null)
    val selectedHighlight: StateFlow<HighlightItemUi?> = _selectedHighlight.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadHighlights()

        // Apply filter when it changes
        viewModelScope.launch {
            combine(_allHighlights, _selectedFilter) { highlights, filter ->
                applyFilter(highlights, filter)
            }.collect { filtered ->
                _highlights.value = filtered
            }
        }
    }

    /**
     * Load all highlights.
     */
    private fun loadHighlights() {
        viewModelScope.launch {
            _isLoading.value = true

            highlightRepository.getAllHighlightsFlow().collect { clips ->
                _allHighlights.value = clips.map { it.toUiModel() }
                _isLoading.value = false
            }
        }
    }

    /**
     * Set filter.
     */
    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    /**
     * Select a highlight for detail view.
     */
    fun selectHighlight(clipId: Long) {
        _selectedHighlight.value = _allHighlights.value.find { it.clipId == clipId }
    }

    /**
     * Clear selected highlight.
     */
    fun clearSelectedHighlight() {
        _selectedHighlight.value = null
    }

    /**
     * Delete a highlight.
     */
    fun deleteHighlight(clipId: Long) {
        viewModelScope.launch {
            highlightRepository.deleteHighlight(clipId)
        }
    }

    /**
     * Share a highlight.
     */
    fun shareHighlight(clipId: Long) {
        val highlight = _allHighlights.value.find { it.clipId == clipId } ?: return

        val shareText = buildString {
            append("🏓 SmartRacket Highlight!\n\n")
            append("Stroke: ${highlight.strokeType}\n")
            append("Score: ${highlight.score}/10\n")
            append("Date: ${highlight.dateFormatted}\n")
            highlight.feedback?.let {
                append("\nTip: $it")
            }
            append("\n\n#SmartRacket #TableTennis")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        context.startActivity(Intent.createChooser(intent, "Share Highlight").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    private fun applyFilter(highlights: List<HighlightItemUi>, filter: String): List<HighlightItemUi> {
        return when (filter) {
            "Auto-saved" -> highlights.filter { it.isAutoSaved }
            "Manual" -> highlights.filter { !it.isAutoSaved }
            "High Score" -> highlights.filter { it.score >= 9 }
            else -> highlights
        }
    }

    private fun HighlightClip.toUiModel(): HighlightItemUi {
        return HighlightItemUi(
            clipId = clipId,
            title = title ?: "${StrokeType.fromString(metadata.strokeType).displayName} - Score ${metadata.score}",
            strokeType = StrokeType.fromString(metadata.strokeType).displayName,
            score = metadata.score,
            confidence = metadata.confidence,
            heartRate = metadata.heartRate,
            feedback = metadata.feedback,
            dateFormatted = dateFormatter.format(Date(createdAt)),
            isAutoSaved = isAutoSaved,
            thumbnailUri = thumbnailUri
        )
    }
}

/**
 * UI model for highlight list items.
 */
data class HighlightItemUi(
    val clipId: Long,
    val title: String,
    val strokeType: String,
    val score: Int,
    val confidence: Float,
    val heartRate: Int?,
    val feedback: String?,
    val dateFormatted: String,
    val isAutoSaved: Boolean,
    val thumbnailUri: String?
)

