// UI components | Owner: shared (Beh, Jeremy, Michael)
// Mobile interaction primitives shared by every feature: edge-to-edge list
// rows, bottom sheets, pinned primary actions and plain section labels.
package com.example.counselling.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Edge-to-edge list row: full-width ripple, 64dp minimum height and an inset
 * divider. Use inside a LazyColumn instead of a card per item.
 */
@Composable
fun AppListRow(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
) {
    val hasLeading = leadingContent != null || leadingIcon != null
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(Modifier.width(16.dp))
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (trailingContent != null) {
                Spacer(Modifier.width(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    content = trailingContent,
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (hasLeading) 56.dp else 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

/**
 * Standard bottom sheet: drag handle, 28dp top corners and consistent padding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            content()
        }
    }
}

/**
 * Surface pinned to the bottom of the screen holding one primary action. It
 * rides above the keyboard so the action stays reachable while typing.
 */
@Composable
fun StickyBottomAction(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            content = content,
        )
    }
}

/**
 * Plain section title on the page background. No card, no eyebrow label.
 */
@Composable
fun SectionLabel(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

/**
 * Opaque section header for `LazyColumn.stickyHeader`. Keeps scrolled rows from
 * showing through while the label stays pinned.
 */
@Composable
fun StickyListHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
        )
    }
}

/**
 * Drag-to-reveal row. The content stays in front and the action strip sits
 * behind it on the trailing edge. A visible action always remains in the
 * row's detail sheet, so gestures are never the only way to act.
 */
@Composable
fun SwipeRevealRow(
    modifier: Modifier = Modifier,
    revealWidth: Dp = 96.dp,
    actions: @Composable RowScope.(close: () -> Unit) -> Unit,
    content: @Composable () -> Unit,
) {
    val revealPx = with(LocalDensity.current) { revealWidth.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val close = {
        scope.launch { offset.animateTo(0f) }
        Unit
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions(close)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(revealPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                offset.animateTo(
                                    if (offset.value < -revealPx / 2f) -revealPx else 0f,
                                )
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offset.snapTo(
                                    (offset.value + dragAmount).coerceIn(-revealPx, 0f),
                                )
                            }
                        },
                    )
                },
        ) {
            content()
        }
    }
}

/**
 * Single square action shown behind a [SwipeRevealRow].
 */
@Composable
fun RowScope.SwipeAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    val container = if (destructive) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val content = if (destructive) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(88.dp)
            .fillMaxHeight(),
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
