package smartracket.com.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Samsung One UI "Samsung Blue" color palette.
 *
 * All colors are derived from the Samsung Blue brand identity.
 * See color.md at project root for the canonical token table.
 *
 * NEVER use raw Color(0xFF...) literals in screen composables.
 * Always reference [MaterialTheme.colorScheme] tokens or
 * semantic constants defined here.
 */
object SmartRacketColors {

    // ── Primary (Samsung Blue) ────────────────────────────────
    val SamsungBlue              = Color(0xFF1428A0)
    val SamsungBlueLighter       = Color(0xFFA6ADDB)
    val SamsungBlueDark          = Color(0xFF0D1A68)

    // ── Surfaces ──────────────────────────────────────────────
    val LightSurface             = Color(0xFFFFFFFF)
    val LightOnSurface           = Color(0xFF1A1A1A)
    val DarkSurface              = Color(0xFF121212)
    val DarkOnSurface            = Color(0xFFEDEDED)

    // ── Primary Container ─────────────────────────────────────
    val LightPrimaryContainer    = Color(0xFFD5D8EE)
    val DarkPrimaryContainer     = Color(0xFF0D1A68)

    // ── Secondary (Accent teal – neutral companion) ───────────
    val SecondaryLight           = Color(0xFF3A6EA5)
    val SecondaryDark            = Color(0xFF7EAAD4)
    val SecondaryContainerLight  = Color(0xFFD0E4F5)
    val SecondaryContainerDark   = Color(0xFF1A3D5C)

    // ── Tertiary ──────────────────────────────────────────────
    val TertiaryLight            = Color(0xFF5C6BC0)
    val TertiaryDark             = Color(0xFFB0BEC5)

    // ── Error / Status ────────────────────────────────────────
    val Error                    = Color(0xFFD32F2F)
    val ErrorContainer           = Color(0xFFFCE4EC)
    val OnError                  = Color(0xFFFFFFFF)

    // ── Semantic status (for score indicators, BLE state, etc.) ──
    val StatusConnected          = Color(0xFF4CAF50)
    val StatusConnecting         = Color(0xFF2196F3)
    val StatusDisconnected       = Color(0xFF9E9E9E)
    val StatusError              = Color(0xFFF44336)

    val ScoreExcellent           = Color(0xFF4CAF50)
    val ScoreGood                = Color(0xFF8BC34A)
    val ScoreAverage             = Color(0xFFFF9800)
    val ScorePoor                = Color(0xFFF44336)

    val HeartRatePink            = Color(0xFFE91E63)
    val TrophyGold               = Color(0xFFFFD700)

    // ── Chart palette ─────────────────────────────────────────
    val ChartColors = listOf(
        0xFF1428A0.toInt(), // Samsung Blue
        0xFF3A6EA5.toInt(), // Secondary
        0xFF5C6BC0.toInt(), // Tertiary
        0xFFA6ADDB.toInt(), // Blue lighter
        0xFF0D1A68.toInt(), // Blue dark
        0xFF7EAAD4.toInt(), // Secondary dark
    )

    // ── Surface variants ──────────────────────────────────────
    val LightSurfaceVariant      = Color(0xFFE8E8EE)
    val LightOnSurfaceVariant    = Color(0xFF44474F)
    val DarkSurfaceVariant       = Color(0xFF2C2C34)
    val DarkOnSurfaceVariant     = Color(0xFFCACAD0)
}

/**
 * Returns a semantic color for a numeric score (0-10 range).
 *
 * Centralised here so every screen uses the same mapping.
 */
fun scoreColor(score: Float): Color = when {
    score >= 8f -> SmartRacketColors.ScoreExcellent
    score >= 6f -> SmartRacketColors.ScoreGood
    score >= 4f -> SmartRacketColors.ScoreAverage
    else        -> SmartRacketColors.ScorePoor
}

/** Int overload for convenience. */
fun scoreColor(score: Int): Color = scoreColor(score.toFloat())
