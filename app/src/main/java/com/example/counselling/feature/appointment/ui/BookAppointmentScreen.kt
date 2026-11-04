// Module 2 | Owner: Beh
// Single booking flow: concern -> Smart Match counsellors -> free slot -> RPC.
package com.example.counselling.feature.appointment.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.AppConstants
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.core.SessionManager
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.appointment.model.CounsellorMatch
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.DropdownField
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.SecondaryButton
import com.example.counselling.ui.components.SectionHeader
import com.example.counselling.ui.components.showAppMessage
import com.example.counselling.ui.theme.TealPrimaryContainer
import com.example.counselling.ui.theme.TealOnPrimaryContainer
import java.time.LocalDate

@Composable
fun BookAppointmentRoute(
    repository: AppointmentRepository,
    sessionManager: SessionManager,
    initialConcern: String? = null,
    onClose: () -> Unit,
) {
    val viewModel: BookAppointmentViewModel = viewModel(
        factory = BookAppointmentViewModel.factory(
            repository,
            sessionManager,
            initialConcern,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }

    val onBack: () -> Unit = {
        when (state.step) {
            BookingStep.CONCERN -> onClose()
            BookingStep.MATCHES -> viewModel.backToConcern()
            BookingStep.SLOTS -> viewModel.backToMatches()
            BookingStep.SUCCESS -> onClose()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = when (state.step) {
                    BookingStep.CONCERN -> "Book a session"
                    BookingStep.MATCHES -> "Smart counsellor match"
                    BookingStep.SLOTS -> "Choose a time"
                    BookingStep.SUCCESS -> "Request sent"
                },
                onBack = onBack,
            )

            when (state.step) {
                BookingStep.CONCERN -> ConcernStep(
                    onChoose = viewModel::chooseConcern,
                )

                BookingStep.MATCHES -> MatchStep(
                    state = state,
                    onRetry = viewModel::retryMatches,
                    onChoose = viewModel::chooseCounsellor,
                    onChangeConcern = viewModel::backToConcern,
                )

                BookingStep.SLOTS -> SlotStep(
                    state = state,
                    onChooseSlot = viewModel::chooseSlot,
                    onModeChange = viewModel::setMode,
                    onNoteChange = viewModel::setNote,
                    onBook = viewModel::book,
                )

                BookingStep.SUCCESS -> SuccessStep(
                    state = state,
                    onDone = onClose,
                )
            }
        }

        AppSnackbarHost(
            hostState = snackbarState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp),
        )
    }
}

@Composable
private fun ConcernStep(onChoose: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "What would you like support with?",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "We use this only to match you with counsellors who list this concern as a specialisation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
        )

        AppConstants.concernCategories.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { concern ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onChoose(concern) },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Text(
                            text = concern,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        InfoCard(
            title = "Not sure what to choose?",
            body = "Choose the closest concern. You can talk about anything in the session, and you can contact the counselling office if you need help.",
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MatchStep(
    state: BookAppointmentUiState,
    onRetry: () -> Unit,
    onChoose: (CounsellorMatch) -> Unit,
    onChangeConcern: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Best matches for ${state.concern ?: "your concern"}",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "Ranked by the earliest free slot. The first counsellor is your best match.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )

        when {
            state.isLoadingMatches -> LoadingShimmer(rows = 3)
            state.loadError != null -> ErrorState(
                message = state.loadError,
                onRetry = onRetry,
            )
            state.matches.isEmpty() -> EmptyState(
                icon = Icons.Outlined.Person,
                title = "No counsellor matches yet",
                message = "No active counsellor currently lists this concern. Try another concern or contact the counselling office.",
                actionLabel = "Change concern",
                onAction = onChangeConcern,
            )
            else -> state.matches.forEachIndexed { index, match ->
                MatchCard(
                    match = match,
                    isBestMatch = index == 0,
                    onClick = { onChoose(match) },
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MatchCard(
    match: CounsellorMatch,
    isBestMatch: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (isBestMatch) {
            TealPrimaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (isBestMatch) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = match.counsellorName
                                .split(" ")
                                .filter { it.isNotBlank() }
                                .takeLast(1)
                                .joinToString("")
                                .take(1)
                                .uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = match.counsellorName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = match.specialisations.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (match.languages.isNotEmpty()) {
                        Text(
                            text = "Speaks ${match.languages.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                if (isBestMatch) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = "Best match",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = TealOnPrimaryContainer,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = "Next free: ${DateTimeUtils.formatDateTime(match.nextSlotStart)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            if (match.bio.isNotBlank()) {
                Text(
                    text = match.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Text(
                text = "Tap to choose a time",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun SlotStep(
    state: BookAppointmentUiState,
    onChooseSlot: (String) -> Unit,
    onModeChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onBook: () -> Unit,
) {
    val match = state.selectedMatch ?: return
    val grouped = remember(match) {
        match.slots.groupBy { DateTimeUtils.formatDate(it.startAt) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Choose a time with ${match.counsellorName}",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "All times are shown in Kuala Lumpur time. Your concern: ${state.concern}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )

        grouped.forEach { (date, slots) ->
            SectionHeader(title = date)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                slots.forEach { slot ->
                    FilterChip(
                        selected = state.selectedSlotId == slot.slotId,
                        onClick = { onChooseSlot(slot.slotId) },
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
            Spacer(Modifier.height(14.dp))
        }

        DropdownField(
            label = "Session mode",
            options = AppConstants.sessionModes.map {
                AppConstants.sessionModeLabels[it] ?: it
            },
            selected = AppConstants.sessionModeLabels[state.mode] ?: state.mode,
            onSelect = { label ->
                val mode = AppConstants.sessionModeLabels.entries
                    .firstOrNull { it.value == label }
                    ?.key
                    ?: "in_person"
                onModeChange(mode)
            },
            enabled = !state.isBooking,
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = state.note,
            onValueChange = onNoteChange,
            label = "Anything you would like the counsellor to know? (optional)",
            singleLine = false,
            maxLines = 4,
            error = state.noteError,
            supportingText = "Maximum 1,000 characters.",
            enabled = !state.isBooking,
        )
        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            text = "Request appointment",
            onClick = onBook,
            enabled = state.selectedSlotId != null,
            loading = state.isBooking,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SuccessStep(
    state: BookAppointmentUiState,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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
            text = "Appointment requested",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            text = "Your request is pending until the counsellor confirms it. It will appear under Appointments.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (state.selectedMatch != null) {
            Spacer(Modifier.height(16.dp))
            InfoCard(
                title = state.selectedMatch.counsellorName,
                body = state.concern ?: "",
            )
        }
        Spacer(Modifier.height(22.dp))
        PrimaryButton(text = "View my appointments", onClick = onDone)
    }
}
