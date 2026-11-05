// Module 2 | Owner: Beh
// Shared appointment summary card. Actions are supplied by the signed-in role
// so admins never see write controls.
package com.example.counselling.feature.appointment.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.counselling.core.AppConstants
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.core.UserRole
import com.example.counselling.feature.appointment.model.Appointment
import com.example.counselling.ui.components.StatusChip
import com.example.counselling.ui.theme.WarningContainer
import com.example.counselling.ui.theme.WarningAmber

@Composable
fun AppointmentCard(
    appointment: Appointment,
    viewerRole: UserRole,
    modifier: Modifier = Modifier,
    isBusy: Boolean = false,
    onReschedule: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    cancelLabel: String = "Cancel",
    onConfirm: (() -> Unit)? = null,
    onDecline: (() -> Unit)? = null,
    onComplete: (() -> Unit)? = null,
    onNoShow: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment.concernCategory,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = when (viewerRole) {
                            UserRole.STUDENT -> "with ${appointment.counsellorName}"
                            UserRole.COUNSELLOR -> "Student: ${appointment.studentName}"
                            UserRole.ADMIN -> "${appointment.studentName} with ${appointment.counsellorName}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusChip(status = appointment.status)
            }

            Spacer(Modifier.height(12.dp))
            DetailRow(
                icon = Icons.Outlined.CalendarMonth,
                text = DateTimeUtils.formatDate(appointment.slotStart),
            )
            DetailRow(
                icon = Icons.Outlined.AccessTime,
                text = "${DateTimeUtils.formatTime(appointment.slotStart)} – " +
                    DateTimeUtils.formatTime(appointment.slotEnd),
            )
            DetailRow(
                icon = Icons.Outlined.LocationOn,
                text = AppConstants.sessionModeLabels[appointment.mode] ?: appointment.mode,
            )
            if (viewerRole == UserRole.ADMIN && appointment.studentCode != null) {
                DetailRow(
                    icon = Icons.Outlined.Person,
                    text = "Student ID: ${appointment.studentCode}",
                )
            }

            if (!appointment.note.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = appointment.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (appointment.isFollowUp) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = WarningContainer),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.height(15.dp),
                            )
                            Text(
                                text = "Follow-up",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarningAmber,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
            }

            val hasTimeActions = onReschedule != null || onCancel != null
            val hasSessionActions = onConfirm != null || onDecline != null ||
                onComplete != null || onNoShow != null
            val hasActions = hasTimeActions || hasSessionActions
            if (hasActions) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (hasTimeActions) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            if (onReschedule != null) {
                                TextButton(onClick = onReschedule, enabled = !isBusy) {
                                    Text("Reschedule")
                                }
                            }
                            if (onCancel != null) {
                                TextButton(onClick = onCancel, enabled = !isBusy) {
                                    Text(cancelLabel, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    if (hasSessionActions) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            if (onConfirm != null) {
                                TextButton(onClick = onConfirm, enabled = !isBusy) {
                                    Text("Confirm")
                                }
                            }
                            if (onDecline != null) {
                                TextButton(onClick = onDecline, enabled = !isBusy) {
                                    Text("Decline", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            if (onComplete != null) {
                                TextButton(onClick = onComplete, enabled = !isBusy) {
                                    Text("Complete")
                                }
                            }
                            if (onNoShow != null) {
                                TextButton(onClick = onNoShow, enabled = !isBusy) {
                                    Text("No-show", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.height(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
