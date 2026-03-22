package smartracket.com.ui.screens

/**
 * Utility for calculating performance trend insights from score data.
 *
 * Provides a consistent, testable algorithm for trend detection that compares
 * recent performance against previous periods.
 */
object TrendInsightCalculator {

    /**
     * Calculate trend insight from score history.
     *
     * Uses a windowing approach to compare recent performance (last N scores)
     * against the previous period (N scores before that), ensuring consistent
     * trend detection regardless of data size.
     *
     * @param scoreTrend List of score trend points in chronological order
     * @return TrendInsight with delta, direction, and advice, or null if insufficient data
     */
    fun calculateTrendInsight(scoreTrend: List<ScoreTrendPoint>): TrendInsight? {
        if (scoreTrend.size < 2) return null

        val scores = scoreTrend.map { it.score }

        // Use consistent windowing for all cases
        val delta = calculateDelta(scores)

        val direction = when {
            delta >= 0.25f -> TrendDirection.IMPROVING
            delta <= -0.25f -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }

        val advice = when (direction) {
            TrendDirection.IMPROVING ->
                "Great progress. Keep your current drill routine and increase controlled speed by about 10% in your next sessions."

            TrendDirection.DECLINING ->
                "Refocus on consistency: slow down your swing tempo for 1-2 sessions, prioritize clean contact, then gradually rebuild speed."

            TrendDirection.STABLE ->
                "You are close to a breakthrough. Add one focused drill (backhand control or serve receive) to push scores higher."
        }

        return TrendInsight(delta = delta, direction = direction, advice = advice)
    }

    /**
     * Calculate delta by comparing recent period average to previous period average.
     *
     * For all data sizes >= 2 scores, uses a consistent windowing strategy:
     * - Calculates window size as minOf(4, scores.size / 2) to split data fairly
     * - Compares: average of recent window vs average of previous window
     * - For small datasets with just 2 scores, delta = last - first as best effort
     *
     * @param scores List of scores in chronological order
     * @return Float representing change from previous period to recent period
     */
    private fun calculateDelta(scores: List<Float>): Float {
        return when {
            scores.size < 2 -> 0f
            scores.size == 2 -> {
                // With only 2 scores, best we can do is compare them
                scores.last() - scores.first()
            }
            else -> {
                // For 3+ scores, use windowing approach
                val windowSize = minOf(4, maxOf(1, scores.size / 2))

                // Recent window: last N scores
                val recentAverage = scores.takeLast(windowSize).average().toFloat()

                // Previous window: N scores before the recent window
                val previousAverage = scores
                    .dropLast(windowSize)
                    .takeLast(windowSize)
                    .average()
                    .toFloat()

                recentAverage - previousAverage
            }
        }
    }
}

/**
 * Represents the direction of performance trend.
 */
enum class TrendDirection {
    IMPROVING,
    DECLINING,
    STABLE
}

/**
 * Insight about performance trend with actionable advice.
 */
data class TrendInsight(
    val delta: Float,
    val direction: TrendDirection,
    val advice: String
)
