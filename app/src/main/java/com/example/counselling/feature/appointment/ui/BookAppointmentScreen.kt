// Module 2 | Owner: Beh
// Single booking flow: concern -> Smart Match counsellors -> free slot -> RPC.
// Each step is a full screen with one decision and a pinned primary action.
package com.example.counselling.feature.appointment.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.AppConstants
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.core.SessionManager
import com.example.counselling.feature.appointment.domain.AppointmentRepository
import com.example.counselling.feature.appointment.domain.AvailableSlot
import com.example.counselling.feature.appointment.domain.CounsellorMatch
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.SegmentedChoiceField
import com.example.counselling.ui.components.StickyBottomAction
import com.example.counselling.ui.components.showAppMessage
import java.time.format.TextStyle
import java.util.Locale

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

    // Local choices let each step show a disabled Continue until the user has
    // made the single decision for that screen.
    var selectedConcern by remember(state.step) { mutableStateOf(state.concern) }
    var selectedMatchId by remember(state.step) {
        mutableStateOf(state.selectedMatch?.counsellorId)
    }

    val progress = when (state.step) {
        BookingStep.CONCERN -> 0.25f
        BookingStep.MATCHES -> 0.5f
        BookingStep.SLOTS -> 0.75f
        BookingStep.SUCCESS -> 1f
    }

    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = when (state.step) {
                    BookingStep.CONCERN -> "Book session"
                    BookingStep.MATCHES -> "Counsellor match"
                    BookingStep.SLOTS -> "Choose a time"
                    BookingStep.SUCCESS -> "Request sent"
                },
                onBack = onBack,
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (state.step) {
                    BookingStep.CONCERN -> ConcernStep(
                        selected = selectedConcern,
                        onSelect = { selectedConcern = it },
                    )

                    BookingStep.MATCHES -> MatchStep(
                        state = state,
                        selectedMatchId = selectedMatchId,
                        onSelect = { selectedMatchId = it.counsellorId },
                        onRetry = viewModel::retryMatches,
                        onChangeConcern = viewModel::backToConcern,
                    )

                    BookingStep.SLOTS -> SlotStep(
                        state = state,
                        onChooseSlot = viewModel::chooseSlot,
                        onClearSlot = viewModel::clearSlot,
                        onRetrySlots = viewModel::retrySlots,
                        onModeChange = viewModel::setMode,
                        onNoteChange = viewModel::setNote,
                    )

                    BookingStep.SUCCESS -> SuccessStep(state = state)
                }
            }

            when (state.step) {
                BookingStep.CONCERN -> StickyBottomAction {
                    PrimaryButton(
                        text = "Continue",
                        enabled = selectedConcern != null,
                        onClick = { selectedConcern?.let(viewModel::chooseConcern) },
                    )
                }

                BookingStep.MATCHES -> StickyBottomAction {
                    val selectedMatch = (state.matches + state.otherCounsellors).firstOrNull {
                        it.counsellorId == selectedMatchId
                    }
                    PrimaryButton(
                        text = "Continue",
                        enabled = selectedMatch != null,
                        onClick = { selectedMatch?.let(viewModel::chooseCounsellor) },
                    )
                }

                BookingStep.SLOTS -> StickyBottomAction {
                    PrimaryButton(
                        text = "Confirm appointment",
                        enabled = state.selectedSlotId != null,
                        loading = state.isBooking,
                        onClick = viewModel::book,
                    )
                }

                BookingStep.SUCCESS -> StickyBottomAction {
                    PrimaryButton(text = "View my appointments", onClick = onClose)
                }
            }
        }

        AppSnackbarHost(
            hostState = snackbarState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
        )
    }
}

@Composable
private fun ConcernStep(
    selected: String?,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "What would you like support with?",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "Choose one concern. It only decides which counsellors are matched to you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )

        AppConstants.concernCategories.forEach { concern ->
            val isSelected = concern == selected
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clickable { onSelect(concern) },
                shape = MaterialTheme.shapes.medium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                },
                border = if (isSelected) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = concern,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    RadioButton(selected = isSelected, onClick = null)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MatchStep(
    state: BookAppointmentUiState,
    selectedMatchId: String?,
    onSelect: (CounsellorMatch) -> Unit,
    onRetry: () -> Unit,
    onChangeConcern: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Best matches for ${state.concern ?: "your concern"}",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "Ranked by the earliest free slot. Choose one counsellor to see their times.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
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
                message = if (state.otherCounsellors.isEmpty()) {
                    "No counsellor lists this concern. Try another concern."
                } else {
                    "No counsellor lists this concern as a specialisation, " +
                        "but you can still choose someone below."
                },
                actionLabel = if (state.otherCounsellors.isEmpty()) "Change concern" else null,
                onAction = if (state.otherCounsellors.isEmpty()) onChangeConcern else null,
            )

            else -> state.matches.forEachIndexed { index, match ->
                MatchRow(
                    rank = index + 1,
                    match = match,
                    selected = match.counsellorId == selectedMatchId,
                    onClick = { onSelect(match) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        if (state.otherCounsellors.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Other counsellors",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Not a specialist in ${state.concern ?: "this concern"}, " +
                    "but they have free times you can book.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            state.otherCounsellors.forEach { match ->
                MatchRow(
                    rank = 0,
                    match = match,
                    selected = match.counsellorId == selectedMatchId,
                    onClick = { onSelect(match) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MatchRow(
    rank: Int,
    match: CounsellorMatch,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val reason = buildString {
        append("Specialises in ")
        append(match.specialisationLabels.joinToString(", "))
        if (match.languages.isNotEmpty()) {
            append(" · Speaks ")
            append(match.languages.joinToString(", "))
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        border = if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (rank == 1) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (rank <= 0) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        } else {
                            Text(
                                text = rank.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (rank == 1) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = match.counsellorName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (rank == 1) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Text(
                                    text = "Best match",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        text = "Next free: ${DateTimeUtils.formatDateTime(match.nextSlotStart)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                RadioButton(selected = selected, onClick = null)
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
        }
    }
}

@Composable
private fun SlotStep(
    state: BookAppointmentUiState,
    onChooseSlot: (String) -> Unit,
    onClearSlot: () -> Unit,
    onRetrySlots: () -> Unit,
    onModeChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
) {
    val match = state.selectedMatch ?: return
    if (state.isLoadingSlots) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "Loading ${match.counsellorName}'s timetable…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 14.dp),
            )
            LoadingShimmer(rows = 4)
        }
        return
    }
    if (state.slotsError != null) {
        ErrorState(message = state.slotsError, onRetry = onRetrySlots)
        return
    }

    val grouped = state.slots.groupBy { DateTimeUtils.formatDate(it.startAt) }
    var selectedDate by remember(match.counsellorId) {
        mutableStateOf(grouped.keys.firstOrNull())
    }
    val activeDate = selectedDate?.takeIf { grouped.containsKey(it) }
        ?: grouped.keys.firstOrNull()
    val visibleSlots = grouped[activeDate].orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Choose a time with ${match.counsellorName}",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "All times are in Kuala Lumpur time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )

        if (grouped.isEmpty()) {
            InfoCard(
                title = "No free slots right now",
                body = "This counsellor has no upcoming free slots. Go back and choose another match.",
            )
            Spacer(Modifier.height(16.dp))
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            grouped.forEach { (date, slots) ->
                val start = slots.firstOrNull()?.startAt?.let(DateTimeUtils::parse)
                DateStripItem(
                    day = start?.dayOfWeek
                        ?.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                        .orEmpty(),
                    date = start?.dayOfMonth?.toString().orEmpty(),
                    freeCount = slots.count { !it.isBooked },
                    selected = date == activeDate,
                    onClick = {
                        if (selectedDate != date) {
                            selectedDate = date
                            onClearSlot()
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Available times",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))

        val freeOnDate = visibleSlots.count { !it.isBooked }
        if (visibleSlots.isEmpty()) {
            InfoCard(
                title = "No times on this date",
                body = "Choose another date above.",
            )
        } else {
            if (freeOnDate == 0) {
                InfoCard(
                    title = "This day is fully booked",
                    body = "Every time on this day already has an appointment. Choose another date above.",
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visibleSlots.chunked(3).forEach { rowSlots ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowSlots.forEach { slot ->
                            TimeSlotChip(
                                slot = slot,
                                selected = !slot.isBooked &&
                                    state.selectedSlotId == slot.slotId,
                                onClick = { onChooseSlot(slot.slotId) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - rowSlots.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Session details",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(10.dp))
        SegmentedChoiceField(
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
        Spacer(Modifier.height(12.dp))
        AppTextField(
            value = state.note,
            onValueChange = onNoteChange,
            label = "Anything the counsellor should know? (optional)",
            singleLine = false,
            maxLines = 4,
            error = state.noteError,
            supportingText = "Maximum 1,000 characters.",
            enabled = !state.isBooking,
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun DateStripItem(
    day: String,
    date: String,
    freeCount: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(68.dp)
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        border = if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = day,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = date,
                style = MaterialTheme.typography.titleLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = if (freeCount == 0) "Full" else "$freeCount free",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TimeSlotChip(
    slot: AvailableSlot,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = if (slot.isBooked) modifier else modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = when {
            slot.isBooked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            selected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = 52.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = DateTimeUtils.formatTime(slot.startAt),
                style = MaterialTheme.typography.titleSmall,
                color = when {
                    selected -> MaterialTheme.colorScheme.onPrimary
                    slot.isBooked -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = if (slot.isBooked) {
                    "Booked"
                } else {
                    "– ${DateTimeUtils.formatTime(slot.endAt)}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    selected -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun SuccessStep(state: BookAppointmentUiState) {
    val haptic = LocalHapticFeedback.current
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }
    var showSummary by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        )
        showSummary = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .size(88.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = showSummary,
            enter = fadeIn(tween(300)) +
                slideInVertically(tween(300)) { height -> height / 5 },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Appointment confirmed",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 18.dp),
                )
                Text(
                    text = "Your session is booked. The counsellor can still cancel it if needed.",
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
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
