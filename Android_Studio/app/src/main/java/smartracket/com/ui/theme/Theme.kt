package smartracket.com.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ── Samsung Blue – Material 3 Color Schemes ──────────────────
// Derived from the canonical token table in color.md.
// Dynamic color is intentionally DISABLED so that the Samsung
// Blue identity is always preserved across all devices.

private val DarkColorScheme = darkColorScheme(
    primary = SmartRacketColors.SamsungBlueLighter,       // #A6ADDB
    onPrimary = Color.Black,
    primaryContainer = SmartRacketColors.DarkPrimaryContainer, // #0D1A68
    onPrimaryContainer = Color.White,
    secondary = SmartRacketColors.SecondaryDark,
    onSecondary = Color.Black,
    secondaryContainer = SmartRacketColors.SecondaryContainerDark,
    onSecondaryContainer = Color.White,
    tertiary = SmartRacketColors.TertiaryDark,
    error = SmartRacketColors.Error,
    onError = SmartRacketColors.OnError,
    background = SmartRacketColors.DarkSurface,            // #121212
    onBackground = SmartRacketColors.DarkOnSurface,        // #EDEDED
    surface = SmartRacketColors.DarkSurface,               // #121212
    onSurface = SmartRacketColors.DarkOnSurface,           // #EDEDED
    surfaceVariant = SmartRacketColors.DarkSurfaceVariant,
    onSurfaceVariant = SmartRacketColors.DarkOnSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = SmartRacketColors.SamsungBlue,               // #1428A0
    onPrimary = Color.White,
    primaryContainer = SmartRacketColors.LightPrimaryContainer, // #D5D8EE
    onPrimaryContainer = Color.Black,
    secondary = SmartRacketColors.SecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = SmartRacketColors.SecondaryContainerLight,
    onSecondaryContainer = Color.Black,
    tertiary = SmartRacketColors.TertiaryLight,
    error = SmartRacketColors.Error,
    onError = SmartRacketColors.OnError,
    background = SmartRacketColors.LightSurface,           // #FFFFFF
    onBackground = SmartRacketColors.LightOnSurface,       // #1A1A1A
    surface = SmartRacketColors.LightSurface,              // #FFFFFF
    onSurface = SmartRacketColors.LightOnSurface,          // #1A1A1A
    surfaceVariant = SmartRacketColors.LightSurfaceVariant,
    onSurfaceVariant = SmartRacketColors.LightOnSurfaceVariant
)

// ── One UI "Squircle" shape system ───────────────────────────
private val SmartRacketShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * SmartRacket theme – Samsung One UI style with Material 3.
 *
 * Supports:
 * - Dark / Light mode (follows system)
 * - Samsung Blue brand colors (dynamic color disabled)
 * - One UI squircle shapes
 * - SamsungOne typography
 */
@Composable
fun SmartRacketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Edge-to-edge: tint status bar with surface for a cleaner look
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SmartRacketShapes,
        content = content
    )
}

