// Module 4 / Unique Feature 2 | Owner: Jeremy
package com.example.counselling.feature.records.ui

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
import androidx.compose.material.icons.outlined.Schedule
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
import com.example.counselling.feature.records.data.SessionRecordRepository
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.SecondaryButton
import com.example.counselling.ui.components.SectionHeader
import com.example.counselling.ui.components.showAppMessage

@Composable
fun FollowUpRoute(
    recordId: String,
    repository: SessionRecordRepository,
    onBack: () -> Unit,
) {
    val viewModel: FollowUpViewModel = viewModel(
        factory = FollowUpViewModel.factory(recordId, repository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = "Schedule follow-up", onBack = onBack)

            when {
                state.isLoading -> LoadingShimmer(
                    modifier = Modifier.padding(20.dp),
                    rows = 4,
                )

                state.record == null -> ErrorState(
                    message = state.error ?: "Could not load the session record.",
                    onRetry = viewModel::retry,
                )

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
                        text = "Follow-up scheduled",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                    Text(
                        text = "The student will see a pending follow-up appointment with the Follow-up tag.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Spacer(Modifier.height(22.dp))
                    PrimaryButton(text = "Back to records", onClick = onBack)
                }

                else -> {
                    val record = checkNotNull(state.record)
                    val grouped = state.slots.groupBy { DateTimeUtils.formatDate(it.startAt) }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp),
                    ) {
                        InfoCard(
                            title = record.studentName,
                            body = "${record.concernCategory} · " +
                                DateTimeUtils.formatDate(record.sessionStart),
                        )
                        if (state.error != null) {
                            Spacer(Modifier.height(12.dp))
                            InfoCard(
                                title = "Follow-up unavailable",
                                body = state.error ?: "",
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        SectionHeader(
                            title = "Choose one of your free slots",
                            subtitle = "The student's maximum-active and overlap rules still apply.",
                        )
                        Spacer(Modifier.height(8.dp))

                        if (grouped.isEmpty()) {
                            EmptyState(
                                icon = Icons.Outlined.Schedule,
                                title = "No free slots available",
                                message = "Add availability first, then come back to schedule the follow-up.",
                                actionLabel = "Retry",
                                onAction = viewModel::retry,
                            )
                        } else {
                            grouped.forEach { (date, slots) ->
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
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
                                text = "Create follow-up appointment",
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
