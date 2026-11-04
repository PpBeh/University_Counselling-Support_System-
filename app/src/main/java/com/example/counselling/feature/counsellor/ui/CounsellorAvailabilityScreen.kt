// Module 3 | Owner: Jeremy
// Availability list and editor for a counsellor. Admin can manage any
// counsellor; a counsellor can manage only their own slots.
package com.example.counselling.feature.counsellor.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.feature.counsellor.data.CounsellorRepository
import com.example.counselling.feature.counsellor.model.AvailabilitySlot
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
import com.example.counselling.ui.components.TimePickerField
import com.example.counselling.ui.components.showAppMessage

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = "Availability", onBack = onBack)

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
                    text = "Free slots appear in Smart Match. Booked and past slots are kept for history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                PrimaryButton(
                    text = "Add availability",
                    onClick = onAdd,
                    icon = Icons.Outlined.Add,
                )
                Spacer(Modifier.height(14.dp))
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
                    if (state.slots.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            EmptyState(
                                icon = Icons.Outlined.CalendarMonth,
                                title = "No availability yet",
                                message = "Add a free slot so students can book a session.",
                                actionLabel = "Add availability",
                                onAction = onAdd,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                bottom = 24.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(state.slots, key = { it.id }) { slot ->
                                SlotRow(
                                    slot = slot,
                                    isDeleting = state.deletingSlotId == slot.id,
                                    onEdit = { onEdit(slot) },
                                    onDelete = { slotToDelete = slot },
                                )
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
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp),
        )
    }

    if (state.showEditor) {
        AvailabilityEditorDialog(
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
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DateTimeUtils.formatDate(slot.startAt),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${DateTimeUtils.formatTime(slot.startAt)} – " +
                        DateTimeUtils.formatTime(slot.endAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(modifier = Modifier.padding(top = 6.dp)) {
                    StatusChip(
                        status = when {
                            slot.isBooked -> "booked"
                            slot.isPast -> "past"
                            else -> "free"
                        },
                    )
                }
                if (!slot.isEditable) {
                    Text(
                        text = if (slot.isBooked) {
                            "Linked to an appointment — cannot be edited or deleted."
                        } else {
                            "Past slot — kept for history."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (slot.isEditable) {
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
private fun AvailabilityEditorDialog(
    state: AvailabilityUiState,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit,
    onStartTime: (Int, Int) -> Unit,
    onEndTime: (Int, Int) -> Unit,
    onRepeat: (Int) -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (state.editingSlot == null) "Add availability" else "Edit availability")
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
            ) {
                DatePickerField(
                    label = "Date",
                    selectedMillis = state.selectedDateMillis,
                    onDateSelected = onDateSelected,
                    error = state.dateError,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        TimePickerField(
                            label = "Start",
                            hour = state.startHour,
                            minute = state.startMinute,
                            onTimeSelected = onStartTime,
                            error = state.timeError,
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        TimePickerField(
                            label = "End",
                            hour = state.endHour,
                            minute = state.endMinute,
                            onTimeSelected = onEndTime,
                        )
                    }
                }
                if (state.editingSlot == null) {
                    Spacer(Modifier.height(10.dp))
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
                } else {
                    InfoCard(
                        title = "Changing a booked slot",
                        body = "Booked and past slots cannot be edited. Creating a new slot instead keeps history clear.",
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !state.isSaving) {
                Text(if (state.isSaving) "Saving…" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) {
                Text("Cancel")
            }
        },
        shape = MaterialTheme.shapes.large,
    )
}
