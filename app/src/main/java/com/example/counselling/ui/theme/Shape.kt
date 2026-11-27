// UI theme | Owner: shared (Beh, Jeremy, Michael)
// Shapes are no longer one radius for everything: list rows are square,
// hero surfaces use 24dp and bottom sheets use 28dp top corners.
package com.example.counselling.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CounsellingShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
