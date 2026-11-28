// Module 3 | Owner: Jeremy
// Availability list and editor for a counsellor. Admin can manage any
// counsellor; a counsellor can manage only their own slots.
package com.example.counselling.feature.counsellor.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.core.domain.AvailabilitySlot
import com.example.counselling.feature.counsellor.domain.CounsellorRepository
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.ConfirmDialog
import com.example.counselling.ui.components.DatePickerField
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.FilterChipRow
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.SectionHeader
import com.example.counselling.ui.components.StatusChip
import com.example.counselling.ui.components.showAppMessage
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun CounsellorAvailabilityRoute(
    counsellorId: String?,
    currentUserId: String,
    repository: CounsellorRepository,
    onBack: () -> Unit,
) {
    val viewModel: AvailabilityViewModel = viewModel(
        factory = AvailabilityViewModel.factory(counsellorId, currentUserId, repository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }

    CounsellorAvailabilityScreen(
        state = state,
        snackbarHostState = snackbarState,
        onBack = onBack,
        onAdd = viewModel::openAddForm,
        onEdit = viewModel::openEditForm,
        onDelete = viewModel::deleteSlot,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onCloseEditor = viewModel::closeForm,
        onDateSelected = viewModel::setDate,
        onStartTime = viewModel::setStartTime,
        onEndTime = viewModel::setEndTime,
        onRepeat = viewModel::setRepeat,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CounsellorAvailabilityScreen(
    state: AvailabilityUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (AvailabilitySlot) -> Unit,
    onDelete: (AvailabilitySlot) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onCloseEditor: () -> Unit,
    onDateSelected: (Long) -> Unit,
    onStartTime: (Int, Int) -> Unit,
    onEndTime: (Int, Int) -> Unit,
    onRepeat: (Int) -> Unit,
    onSave: () -> Unit,
) {
    var slotToDelete by remember { mutableStateOf<AvailabilitySlot?>(null) }
    var rangeFilter by rememberSaveable { mutableStateOf("Upcoming") }
    val listState = rememberLazyListState()

    // New slots are inserted in date order, so a slot earlier than the current
    // first row would otherwise stay hidden above the viewport. Jump back to
    // the top whenever the saved list changes.
    LaunchedEffect(state.slots) {
        if (state.slots.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    val visibleSlots = when (rangeFilter) {
        "This week" -> state.slots.filter {
            !it.isPast() && DateTimeUtils.isWithinNextDays(it.startAt, 7)
        }
        "All" -> state.slots
        else -> state.slots.filter { !it.isPast() }
    }
    val grouped = visibleSlots.groupBy { DateTimeUtils.formatDate(it.startAt) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = "Availability",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh availability")
                    }
                },
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = if (state.counsellorName.isBlank()) {
                        "Manage availability"
                    } else {
                        state.counsellorName
                    },
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Create bookable slots quickly. Past and booked slots are kept for history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                PrimaryButton(
                    text = "Add availability",
                    onClick = onAdd,
                    icon = Icons.Outlined.Add,
                )
                Spacer(Modifier.height(10.dp))
                FilterChipRow(
                    options = listOf("Upcoming", "This week", "All"),
                    selected = rangeFilter,
                    onSelect = { rangeFilter = it },
                )
                Spacer(Modifier.height(6.dp))
            }

            when {
                state.isLoading -> LoadingShimmer(
                    modifier = Modifier.padding(20.dp),
                    rows = 4,
                )

                state.loadError != null && state.slots.isEmpty() -> ErrorState(
                    message = state.loadError,
                    onRetry = onRetry,
                )

                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (visibleSlots.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            EmptyState(
                                icon = Icons.Outlined.CalendarMonth,
                                title = if (rangeFilter == "All") {
                                    "No availability yet"
                                } else {
                                    "No $rangeFilter availability"
                                },
                                message = "Add a free slot so students can book a session.",
                                actionLabel = "Add availability",
                                onAction = onAdd,
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                bottom = 24.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            grouped.forEach { (date, slots) ->
                                item(key = "date-$date") {
                                    SectionHeader(
                                        title = date,
                                        subtitle = "${slots.count { it.isEditable() }} editable · " +
                                            "${slots.count { it.isBooked }} booked",
                                    )
                                }
                                items(slots, key = { it.id }) { slot ->
                                    SlotRow(
                                        slot = slot,
                                        isDeleting = state.deletingSlotId == slot.id,
                                        onEdit = { onEdit(slot) },
                                        onDelete = { slotToDelete = slot },
                                    )
                                }
                                item(key = "space-$date") { Spacer(Modifier.height(4.dp)) }
                            }
                        }
                    }
                }
            }
        }

        AppSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }

    if (state.showEditor) {
        AvailabilityEditor(
            state = state,
            onDismiss = onCloseEditor,
            onDateSelected = onDateSelected,
            onStartTime = onStartTime,
            onEndTime = onEndTime,
            onRepeat = onRepeat,
            onSave = onSave,
        )
    }

    slotToDelete?.let { slot ->
        ConfirmDialog(
            title = "Delete this free slot?",
            message = "${DateTimeUtils.formatDate(slot.startAt)} · " +
                "${DateTimeUtils.formatTime(slot.startAt)} – ${DateTimeUtils.formatTime(slot.endAt)}. " +
                "This cannot be undone.",
            confirmLabel = "Delete slot",
            onConfirm = { onDelete(slot) },
            onDismiss = { slotToDelete = null },
        )
    }
}

@Composable
private fun SlotRow(
    slot: AvailabilitySlot,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${DateTimeUtils.formatTime(slot.startAt)} – " +
                        DateTimeUtils.formatTime(slot.endAt),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(modifier = Modifier.padding(top = 5.dp)) {
                    StatusChip(
                        status = when {
                            slot.isBooked -> "booked"
                            slot.isPast() -> "past"
                            else -> "free"
                        },
                    )
                    if (!slot.isEditable()) {
                        Text(
                            text = if (slot.isBooked) {
                                "Linked to an appointment"
                            } else {
                                "Past slot"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
            if (slot.isEditable()) {
                IconButton(onClick = onEdit, enabled = !isDeleting) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit slot")
                }
                IconButton(onClick = onDelete, enabled = !isDeleting) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete slot",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                Icon(
                    imageVector = if (slot.isBooked) Icons.Outlined.CalendarMonth else Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun AvailabilityEditor(
    state: AvailabilityUiState,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit,
    onStartTime: (Int, Int) -> Unit,
    onEndTime: (Int, Int) -> Unit,
    onRepeat: (Int) -> Unit,
    onSave: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) {
                AppTopBar(
                    title = if (state.editingSlot == null) "Add availability" else "Edit availability",
                    onBack = if (state.isSaving) null else onDismiss,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = if (state.editingSlot == null) {
                            "Set a day, start time and duration. Repeat it weekly if needed."
                        } else {
                            "Adjust this free slot. Booked and past slots are read-only."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                    )

                    SectionHeader(
                        title = "Day",
                        subtitle = "Tap a day or use the date picker for another date.",
                    )
                    QuickDateRow(
                        selectedMillis = state.selectedDateMillis,
                        onSelect = onDateSelected,
                    )
                    Spacer(Modifier.height(10.dp))
                    DatePickerField(
                        label = "Other date",
                        selectedMillis = state.selectedDateMillis,
                        onDateSelected = onDateSelected,
                        error = state.dateError,
                    )

                    Spacer(Modifier.height(18.dp))
                    SectionHeader(
                        title = "Start time",
                        subtitle = "Choose the time students can book.",
                    )
                    QuickStartTimeRow(
                        state = state,
                        onStartTime = onStartTime,
                        onEndTime = onEndTime,
                    )

                    Spacer(Modifier.height(18.dp))
                    SectionHeader(
                        title = "Duration",
                        subtitle = "The end time is calculated automatically.",
                    )
                    DurationRow(
                        state = state,
                        onEndTime = onEndTime,
                    )
                    Spacer(Modifier.height(8.dp))
                    InfoCard(
                        title = "Ends at " +
                            DateTimeUtils.formatTime(state.endHour, state.endMinute),
                        body = "The slot is saved as one continuous booking window.",
                    )
                    state.timeError?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }

                    if (state.editingSlot == null) {
                        Spacer(Modifier.height(18.dp))
                        SectionHeader(
                            title = "Repeat weekly",
                            subtitle = "Creates one slot per week on the same weekday.",
                        )
                        Spacer(Modifier.height(6.dp))
                        FilterChipRow(
                            options = (1..8).map {
                                "$it week" + if (it == 1) "" else "s"
                            },
                            selected = "${state.repeatWeeks} week" +
                                if (state.repeatWeeks == 1) "" else "s",
                            onSelect = { label ->
                                onRepeat(label.substringBefore(' ').toIntOrNull() ?: 1)
                            },
                        )
                        state.repeatError?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    } else {
                        Spacer(Modifier.height(14.dp))
                        InfoCard(
                            title = "Editing a free slot",
                            body = "Booked and past slots cannot be edited. Create a new slot instead to keep history clear.",
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }

                PrimaryButton(
                    text = if (state.isSaving) "Saving…" else "Save availability",
                    onClick = onSave,
                    enabled = !state.isSaving,
                    loading = state.isSaving,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                )
            }
        }
    }
}

@Composable
private fun QuickDateRow(
    selectedMillis: Long?,
    onSelect: (Long) -> Unit,
) {
    val dates = remember {
        val today = DateTimeUtils.nowInKualaLumpur().toLocalDate()
        (1..7).map { today.plusDays(it.toLong()) }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        dates.forEach { date ->
            val millis = DateTimeUtils.datePickerMillis(date)
            FilterChip(
                selected = selectedMillis == millis,
                onClick = { onSelect(millis) },
                label = {
                    Text(date.format(DateTimeFormatter.ofPattern("EEE d MMM")))
                },
            )
        }
    }
}

@Composable
private fun QuickStartTimeRow(
    state: AvailabilityUiState,
    onStartTime: (Int, Int) -> Unit,
    onEndTime: (Int, Int) -> Unit,
) {
    val duration = remember(state.startHour, state.startMinute, state.endHour, state.endMinute) {
        val start = state.startHour * 60 + state.startMinute
        val end = state.endHour * 60 + state.endMinute
        (end - start).takeIf { it >= 30 } ?: 60
    }
    val options = remember(state.startHour, state.startMinute) {
        val standard = (8..18).flatMap { hour -> listOf(hour to 0, hour to 30) }
        (standard + (state.startHour to state.startMinute)).distinct()
            .sortedWith(compareBy({ it.first }, { it.second }))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (hour, minute) ->
            FilterChip(
                selected = state.startHour == hour && state.startMinute == minute,
                onClick = {
                    onStartTime(hour, minute)
                    val end = LocalTime.of(hour, minute).plusMinutes(duration.toLong())
                    onEndTime(end.hour, end.minute)
                },
                label = { Text(DateTimeUtils.formatTime(hour, minute)) },
            )
        }
    }
}

@Composable
private fun DurationRow(
    state: AvailabilityUiState,
    onEndTime: (Int, Int) -> Unit,
) {
    val currentDuration = (state.endHour * 60 + state.endMinute) -
        (state.startHour * 60 + state.startMinute)
    val durations = remember(currentDuration) {
        (listOf(30, 45, 60, 90, 120) + currentDuration)
            .filter { it >= 30 }
            .distinct()
            .sorted()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        durations.forEach { minutes ->
            FilterChip(
                selected = currentDuration == minutes,
                onClick = {
                    val end = LocalTime.of(state.startHour, state.startMinute)
                        .plusMinutes(minutes.toLong())
                    onEndTime(end.hour, end.minute)
                },
                label = {
                    Text(
                        if (minutes < 60) {
                            "$minutes min"
                        } else {
                            "${minutes / 60} hr" + if (minutes % 60 == 0) "" else " ${minutes % 60} min"
                        },
                    )
                },
            )
        }
    }
}
