package smartracket.com.util

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feature flags for controlling rollout and A/B testing of experimental features.
 *
 * Phase E: Rollout guardrails for merged stats visibility feature.
 * When disabled, stats screens fall back to local-only queries for safety.
 */
@Singleton
class FeatureFlags @Inject constructor(
    private val prefs: SharedPreferences
) {

    companion object {
        private const val PREF_MERGED_STATS_ENABLED = "feature_merged_stats_enabled"
        private const val DEFAULT_MERGED_STATS_ENABLED = true // Enabled by default for Phase D
    }

    /**
     * Whether to use merged local + Firebase stats for Home and Analytics screens.
     *
     * When enabled: Stats queries merge Room data with Firestore cloud data.
     * When disabled: Falls back to Room-only queries for stability.
     */
    fun isMergedStatsEnabled(): Boolean {
        return prefs.getBoolean(PREF_MERGED_STATS_ENABLED, DEFAULT_MERGED_STATS_ENABLED)
    }

    /**
     * Set merged stats feature flag (primarily for testing and diagnostics).
     */
    fun setMergedStatsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_MERGED_STATS_ENABLED, enabled).apply()
    }

    /**
     * Reset feature flag to default (used in diagnostics/testing).
     */
    fun resetToDefaults() {
        setMergedStatsEnabled(DEFAULT_MERGED_STATS_ENABLED)
    }
}
