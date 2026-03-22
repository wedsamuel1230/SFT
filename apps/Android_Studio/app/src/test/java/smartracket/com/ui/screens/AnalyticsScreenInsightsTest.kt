package smartracket.com.ui.screens

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for insights calculation using TrendInsightCalculator.
 *
 * Validates that the trend insight properly compares recent vs previous performance periods
 * using a consistent windowing algorithm.
 */
class AnalyticsScreenInsightsTest {

    @Test
    fun `calculateTrendInsight should compare recent vs previous periods for consistent trend detection`() {
        // Scenario: 6 scores with a clear improving trend in the recent period
        // First 3 scores (previous): 5.0, 5.1, 5.2 (avg: 5.1)
        // Last 3 scores (recent): 6.0, 6.1, 6.2 (avg: 6.1)
        // Window size = minOf(4, 6/2) = 3
        // Expected delta: 6.1 - 5.1 = 1.0 (improving significantly)
        val improvingScores = listOf(
            ScoreTrendPoint("01/01", 5.0f),
            ScoreTrendPoint("01/02", 5.1f),
            ScoreTrendPoint("01/03", 5.2f),
            ScoreTrendPoint("01/04", 6.0f),
            ScoreTrendPoint("01/05", 6.1f),
            ScoreTrendPoint("01/06", 6.2f)
        )

        val insight = TrendInsightCalculator.calculateTrendInsight(improvingScores)
        assertNotNull("Insight should be generated for 6+ scores", insight)
        insight?.let {
            assertTrue(
                "Delta should reflect recent vs previous period comparison (~1.0), was ${it.delta}",
                it.delta > 0.8f && it.delta < 1.2f
            )
            assertEquals("Direction should be IMPROVING", TrendDirection.IMPROVING, it.direction)
        }
    }

    @Test
    fun `calculateTrendInsight should use windowing for 3 scores`() {
        // Scenario: Only 3 scores
        // Window size = minOf(4, 3/2) = 1
        // Recent window (last 1): [7.0] -> avg = 7.0
        // Previous window: [6.0] -> avg = 6.0
        // Delta = 7.0 - 6.0 = 1.0 (improving)
        val threeScores = listOf(
            ScoreTrendPoint("01/01", 5.0f),
            ScoreTrendPoint("01/02", 6.0f),
            ScoreTrendPoint("01/03", 7.0f)
        )

        val insight = TrendInsightCalculator.calculateTrendInsight(threeScores)
        assertNotNull("Insight should be generated for 3 scores", insight)
        insight?.let {
            // Should recognize improvement
            assertEquals("Direction should be IMPROVING", TrendDirection.IMPROVING, it.direction)
            // For 3 scores with window size 1 and strict improvement: delta = 1.0
            assertTrue(
                "Delta should be reasonable for 3 scores, was ${it.delta}",
                it.delta > 0f
            )
        }
    }

    @Test
    fun `calculateTrendInsight should detect declining trend`() {
        // Clear declining trend: scores go from 8.0 down to 6.0+
        // Window size = minOf(4, 6/2) = 3
        // Recent: [7.5, 7.2, 6.8] avg = 7.167
        // Previous: [8.5, 8.3, 8.1] avg = 8.3
        // Delta = 7.167 - 8.3 = -1.133 (declining)
        val decliningScores = listOf(
            ScoreTrendPoint("01/01", 8.5f),
            ScoreTrendPoint("01/02", 8.3f),
            ScoreTrendPoint("01/03", 8.1f),
            ScoreTrendPoint("01/04", 7.5f),
            ScoreTrendPoint("01/05", 7.2f),
            ScoreTrendPoint("01/06", 6.8f)
        )

        val insight = TrendInsightCalculator.calculateTrendInsight(decliningScores)
        assertNotNull("Insight should be generated", insight)
        insight?.let {
            assertEquals("Direction should be DECLINING", TrendDirection.DECLINING, it.direction)
            assertTrue("Delta should be negative", it.delta < -0.25f)
        }
    }

    @Test
    fun `calculateTrendInsight should detect stable trend`() {
        // Scores hovering around 7.5 with minor variations (no significant trend)
        // Window size = 3
        // Recent: [7.5, 7.6, 7.5] avg = 7.533
        // Previous: [7.5, 7.6, 7.4] avg = 7.5
        // Delta = 7.533 - 7.5 = 0.033 (within stable range)
        val stableScores = listOf(
            ScoreTrendPoint("01/01", 7.5f),
            ScoreTrendPoint("01/02", 7.6f),
            ScoreTrendPoint("01/03", 7.4f),
            ScoreTrendPoint("01/04", 7.5f),
            ScoreTrendPoint("01/05", 7.6f),
            ScoreTrendPoint("01/06", 7.5f)
        )

        val insight = TrendInsightCalculator.calculateTrendInsight(stableScores)
        assertNotNull("Insight should be generated", insight)
        insight?.let {
            assertEquals("Direction should be STABLE", TrendDirection.STABLE, it.direction)
            assertTrue(
                "Delta should be within stable range (-0.25 to 0.25), was ${it.delta}",
                it.delta > -0.25f && it.delta < 0.25f
            )
        }
    }

    @Test
    fun `calculateTrendInsight should handle edge case with 2 scores`() {
        // With only 2 scores, best effort is last - first
        val twoScores = listOf(
            ScoreTrendPoint("01/01", 5.0f),
            ScoreTrendPoint("01/02", 6.0f)
        )

        val insight = TrendInsightCalculator.calculateTrendInsight(twoScores)
        assertNotNull("Insight should be generated for 2 scores", insight)
        insight?.let {
            assertEquals("Delta should be 1.0", 1.0f, it.delta)
            assertEquals("Direction should be IMPROVING", TrendDirection.IMPROVING, it.direction)
        }
    }

    @Test
    fun `calculateTrendInsight should return null for single score`() {
        val singleScore = listOf(ScoreTrendPoint("01/01", 5.0f))

        val insight = TrendInsightCalculator.calculateTrendInsight(singleScore)
        assertNull("Insight should be null for single score", insight)
    }
}
