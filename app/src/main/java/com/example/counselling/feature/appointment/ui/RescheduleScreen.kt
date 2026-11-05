// Module 2 | Owner: Beh
package com.example.counselling.feature.appointment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.SecondaryButton
import com.example.counselling.ui.components.SectionHeader
import com.example.counselling.ui.components.showAppMessage

@Composable
fun RescheduleRoute(
    appointmentId: String,
    repository: AppointmentRepository,
    isCounsellor: Boolean = false,
    onBack: () -> Unit,
) {
    val viewModel: RescheduleViewModel = viewModel(
        factory = RescheduleViewModel.factory(appointmentId, repository, isCounsellor),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = if (isCounsellor) "Reschedule session" else "Reschedule",
                onBack = onBack,
            )

            when {
                state.isLoading -> LoadingShimmer(
                    modifier = Modifier.padding(20.dp),
                    rows = 4,
                )

                state.appointment == null -> ErrorState(
                    message = state.error ?: "Could not load the appointment.",
                    onRetry = viewModel::retry,
                )

                state.tooLateToChange -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                ) {
                    InfoCard(
                        title = if (isCounsellor) {
                            "Cannot reschedule this session"
                        } else {
                            "Too late to reschedule"
                        },
                        body = state.error
                            ?: "This session cannot be rescheduled here.",
                    )
                    Spacer(Modifier.height(16.dp))
                    SecondaryButton(
                        text = if (isCounsellor) "Back to schedule" else "Back to appointments",
                        onClick = onBack,
                    )
                }

                state.success -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(82.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp),
                            )
                        }
                    }
                    Text(
                        text = if (isCounsellor) {
                            "Session rescheduled"
                        } else {
                            "Appointment rescheduled"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                    Text(
                        text = if (isCounsellor) {
                            "The student will see the confirmed session at its new time."
                        } else {
                            "Your new time is waiting in Appointments."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Spacer(Modifier.height(22.dp))
                    PrimaryButton(
                        text = if (isCounsellor) "Back to schedule" else "Back to appointments",
                        onClick = onBack,
                    )
                }

                else -> {
                    val appointment = checkNotNull(state.appointment)
                    val grouped = state.slots.groupBy { DateTimeUtils.formatDate(it.startAt) }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp),
                    ) {
                        InfoCard(
                            title = "Current appointment",
                            body = if (isCounsellor) {
                                "${appointment.slotStart.let { DateTimeUtils.formatDateTime(it) }} " +
                                    "with ${appointment.studentName}"
                            } else {
                                "${appointment.slotStart.let { DateTimeUtils.formatDateTime(it) }} " +
                                    "with ${appointment.counsellorName}"
                            },
                        )
                        Spacer(Modifier.height(16.dp))
                        SectionHeader(
                            title = "Choose a new time",
                            subtitle = if (isCounsellor) {
                                "Emergency reschedule: choose a future free slot with the same counsellor. The session stays confirmed."
                            } else {
                                "Only free slots with the same counsellor are shown."
                            },
                        )
                        Spacer(Modifier.height(8.dp))

                        if (grouped.isEmpty()) {
                            InfoCard(
                                title = "No free slots right now",
                                body = "There are no other free slots with this counsellor. Please try again later or keep your current appointment.",
                            )
                        } else {
                            grouped.forEach { (date, slots) ->
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    slots.forEach { slot ->
                                        FilterChip(
                                            selected = state.selectedSlotId == slot.slotId,
                                            onClick = { viewModel.chooseSlot(slot.slotId) },
                                            label = {
                                                Text(
                                                    "${DateTimeUtils.formatTime(slot.startAt)} – " +
                                                        DateTimeUtils.formatTime(slot.endAt),
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            PrimaryButton(
                                text = "Confirm new time",
                                onClick = viewModel::submit,
                                enabled = state.selectedSlotId != null,
                                loading = state.isSubmitting,
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }

        AppSnackbarHost(
            hostState = snackbarState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}
