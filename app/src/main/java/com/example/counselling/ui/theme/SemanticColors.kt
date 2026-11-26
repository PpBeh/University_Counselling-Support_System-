// UI theme | Owner: shared (Beh, Jeremy, Michael)
// Semantic colours that Material 3 does not expose directly (success/warning).
// They have explicit light and dark variants so cards stay readable in both.
package com.example.counselling.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class SemanticColors(
    val successContainer: Color,
    val successContent: Color,
    val warningContainer: Color,
    val warningContent: Color,
)

val LightSemanticColors = SemanticColors(
    successContainer = SuccessContainer,
    successContent = SuccessGreen,
    warningContainer = WarningContainer,
    warningContent = WarningAmber,
)

val DarkSemanticColors = SemanticColors(
    successContainer = Color(0xFF1D3A29),
    successContent = Color(0xFFA9D8B2),
    warningContainer = Color(0xFF4A3A12),
    warningContent = Color(0xFFFFD98A),
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }
