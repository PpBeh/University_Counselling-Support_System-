// Module 2 | Owner: Beh
// Appointment summary row and the action sheet shared by every role. The row
// stays quiet; actions live in the sheet so they are reachable with one thumb.
package com.example.counselling.feature.appointment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.core.domain.User
import com.example.counselling.feature.appointment.domain.Appointment
import com.example.counselling.ui.components.AppBottomSheet
import com.example.counselling.ui.components.AppListRow
import com.example.counselling.ui.components.StarRating
import com.example.counselling.ui.components.StatusChip
import com.example.counselling.ui.theme.LocalSemanticColors

@Composable
fun AppointmentRow(
    appointment: Appointment,
    viewer: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val title = when {
        viewer.permissions.canBookAppointments() -> appointment.counsellorName
        viewer.permissions.canViewPrivateSessionNotes() -> appointment.studentName
        else -> appointment.studentName
    }
    val supporting = buildString {
        if (viewer.permissions.canViewAllAppointments()) {
            append(appointment.counsellorName)
            append(" · ")
        }
        append(appointment.concern.label)
        append(" · ")
        append(appointment.mode.label)
        if (appointment.isFollowUp) append(" · Follow-up")
    }

    AppListRow(
        title = title,
        supportingText = supporting,
        modifier = modifier,
        leadingContent = {
            Text(
                text = DateTimeUtils.formatTime(appointment.slotStart),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            StatusChip(status = appointment.status.wireValue)
        },
        onClick = onClick,
        showDivider = showDivider,
    )
}

@Composable
fun AppointmentDetailSheet(
    appointment: Appointment,
    viewer: User,
    onDismiss: () -> Unit,
    isBusy: Boolean = false,
    onReschedule: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    cancelLabel: String = "Cancel appointment",
    onConfirm: (() -> Unit)? = null,
    onDecline: (() -> Unit)? = null,
    onComplete: (() -> Unit)? = null,
    onNoShow: (() -> Unit)? = null,
    onFeedback: (() -> Unit)? = null,
    feedbackRating: Int? = null,
    feedbackComment: String? = null,
    onRemoveFeedback: (() -> Unit)? = null,
    notice: String? = null,
) {
    val semantic = LocalSemanticColors.current

    AppBottomSheet(
        onDismissRequest = onDismiss,
        title = appointment.concern.label,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DateTimeUtils.formatDate(appointment.slotStart),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${DateTimeUtils.formatTime(appointment.slotStart)} – " +
                        DateTimeUtils.formatTime(appointment.slotEnd) +
                        " · ${appointment.mode.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(0.dp))
            StatusChip(status = appointment.status.wireValue)
        }

        Text(
            text = when {
                viewer.permissions.canBookAppointments() ->
                    "With ${appointment.counsellorName}"
                viewer.permissions.canViewPrivateSessionNotes() ->
                    "Student: ${appointment.studentName}"
                else ->
                    "${appointment.studentName} with ${appointment.counsellorName}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp),
        )

        if (viewer.permissions.canViewAllAppointments()) {
            Text(
                text = "Student ID: ${appointment.studentCode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (!appointment.note.isNullOrBlank()) {
            Text(
                text = appointment.note,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        if (!notice.isNullOrBlank()) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = semantic.warningContainer,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(
                    text = notice,
                    style = MaterialTheme.typography.labelMedium,
                    color = semantic.warningContent,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        if (appointment.isFollowUp) {
            Text(
                text = "Follow-up session",
                style = MaterialTheme.typography.labelMedium,
                color = semantic.warningContent,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        if (feedbackRating != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Your rating",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StarRating(
                    rating = feedbackRating,
                    starSize = 16.dp,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            if (!feedbackComment.isNullOrBlank()) {
                Text(
                    text = feedbackComment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        if (onReschedule != null || onCancel != null || onConfirm != null ||
            onDecline != null || onComplete != null || onNoShow != null ||
            onFeedback != null || onRemoveFeedback != null
        ) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onConfirm != null) {
                    SheetButton(
                        label = "Confirm session",
                        onClick = onConfirm,
                        enabled = !isBusy,
                        primary = true,
                    )
                }
                if (onComplete != null) {
                    SheetButton(
                        label = "Mark completed",
                        onClick = onComplete,
                        enabled = !isBusy,
                        primary = true,
                    )
                }
                if (onFeedback != null) {
                    SheetButton(
                        label = if (feedbackRating == null) "Rate session" else "Update rating",
                        onClick = onFeedback,
                        enabled = !isBusy,
                        primary = true,
                    )
                }
                if (onReschedule != null) {
                    SheetButton(
                        label = "Reschedule",
                        onClick = onReschedule,
                        enabled = !isBusy,
                    )
                }
                if (onRemoveFeedback != null) {
                    SheetButton(
                        label = "Remove rating",
                        onClick = onRemoveFeedback,
                        enabled = !isBusy,
                    )
                }
                if (onDecline != null) {
                    SheetButton(
                        label = "Decline booking",
                        onClick = onDecline,
                        enabled = !isBusy,
                        destructive = true,
                    )
                }
                if (onNoShow != null) {
                    SheetButton(
                        label = "Mark no-show",
                        onClick = onNoShow,
                        enabled = !isBusy,
                        destructive = true,
                    )
                }
                if (onCancel != null) {
                    SheetButton(
                        label = cancelLabel,
                        onClick = onCancel,
                        enabled = !isBusy,
                        destructive = true,
                    )
                }
            }
        } else {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun SheetButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    primary: Boolean = false,
    destructive: Boolean = false,
) {
    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(
                text = label,
                color = if (destructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}
