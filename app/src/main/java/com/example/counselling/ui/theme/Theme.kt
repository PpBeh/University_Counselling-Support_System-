// UI theme | Owner: shared (Beh, Jeremy, Michael)
package com.example.counselling.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealOnPrimaryContainer,
    inversePrimary = TealDarkPrimary,
    secondary = LavenderSecondary,
    onSecondary = LavenderOnSecondary,
    secondaryContainer = LavenderSecondaryContainer,
    onSecondaryContainer = LavenderOnSecondaryContainer,
    tertiary = Color(0xFF537A50),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD8EBD2),
    onTertiaryContainer = Color(0xFF10280F),
    background = WarmBackground,
    onBackground = SoftInk,
    surface = WarmSurface,
    onSurface = SoftInk,
    surfaceVariant = SoftSurfaceVariant,
    onSurfaceVariant = MutedInk,
    surfaceTint = TealPrimary,
    inverseSurface = Color(0xFF2D3533),
    inverseOnSurface = Color(0xFFEDF5F3),
    error = SoftError,
    onError = Color.White,
    errorContainer = SoftErrorContainer,
    onErrorContainer = Color(0xFF6B1717),
    outline = SoftOutline,
    outlineVariant = Color(0xFFE1E9E7),
    scrim = Color.Black,
)

private val DarkColors = darkColorScheme(
    primary = TealDarkPrimary,
    onPrimary = TealDarkOnPrimary,
    primaryContainer = TealDarkPrimaryContainer,
    onPrimaryContainer = TealDarkOnPrimaryContainer,
    inversePrimary = TealPrimary,
    secondary = LavenderDarkSecondary,
    onSecondary = LavenderDarkOnSecondary,
    secondaryContainer = LavenderDarkSecondaryContainer,
    onSecondaryContainer = LavenderDarkOnSecondaryContainer,
    tertiary = Color(0xFFB8D6B1),
    onTertiary = Color(0xFF23391F),
    tertiaryContainer = Color(0xFF3A5035),
    onTertiaryContainer = Color(0xFFD8EBD2),
    background = NightBackground,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightMutedInk,
    surfaceTint = TealDarkPrimary,
    inverseSurface = Color(0xFFE5F0ED),
    inverseOnSurface = Color(0xFF27312F),
    error = NightError,
    onError = Color(0xFF690005),
    errorContainer = NightErrorContainer,
    onErrorContainer = Color(0xFFFFDAD6),
    outline = NightOutline,
    outlineVariant = Color(0xFF3E4D49),
    scrim = Color.Black,
)

@Composable
fun CounsellingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = CounsellingTypography,
        shapes = CounsellingShapes,
        content = content,
    )
}
