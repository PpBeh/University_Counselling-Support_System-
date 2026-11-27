// Module 4 / Unique Feature 2 | Owner: Jeremy
// Records list, completed-session intake and the urgency triage board. Rows
// open a detail sheet; the triage buckets are sticky headers.
package com.example.counselling.feature.records.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.feature.appointment.domain.Appointment
import com.example.counselling.feature.appointment.domain.AppointmentRepository
import com.example.counselling.feature.records.domain.SessionRecord
import com.example.counselling.feature.records.domain.SessionRecordRepository
import com.example.counselling.feature.records.domain.TriageBucket
import com.example.counselling.feature.records.domain.TriageBoard
import com.example.counselling.ui.components.AppBottomSheet
import com.example.counselling.ui.components.AppListRow
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.ConfirmDialog
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.RiskBadge
import com.example.counselling.ui.components.StatusChip
import com.example.counselling.ui.components.StickyListHeader
import com.example.counselling.ui.components.showAppMessage
import com.example.counselling.ui.theme.LocalSemanticColors

@Composable
fun SessionRecordsRoute(
    repository: SessionRecordRepository,
    appointmentRepository: AppointmentRepository,
    onAddRecord: (String) -> Unit,
    onEditRecord: (String) -> Unit,
    onScheduleFollowUp: (String) -> Unit,
) {
    val viewModel: SessionRecordsViewModel = viewModel(
        factory = SessionRecordsViewModel.factory(repository, appointmentRepository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.load()
    }

    SessionRecordsScreen(
        state = state,
        snackbarHostState = snackbarState,
        onAddRecord = onAddRecord,
        onEditRecord = onEditRecord,
        onScheduleFollowUp = onScheduleFollowUp,
        onArchive = viewModel::setArchived,
        onDeleteRecord = viewModel::deleteRecord,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SessionRecordsScreen(
    state: SessionRecordsUiState,
    snackbarHostState: SnackbarHostState,
    onAddRecord: (String) -> Unit,
    onEditRecord: (String) -> Unit,
    onScheduleFollowUp: (String) -> Unit,
    onArchive: (SessionRecord, Boolean) -> Unit,
    onDeleteRecord: (SessionRecord) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var recordToView by remember { mutableStateOf<SessionRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<SessionRecord?>(null) }
    // SessionRecord equality is id-only; recompute instead of remember(list)
    // so archive/edit refreshes are visible immediately.
    val triage = TriageBoard().group(state.records)
    val visibleRecords = state.records.filter { showArchived || !it.isArchived }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = "Private notes and follow-up care",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Private notes stay with you. Students only see the public summary.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Records") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        val count = triage.values.sumOf { it.size }
                        Text(if (count == 0) "Triage board" else "Triage board ($count)")
                    },
                )
            }

            if (selectedTab == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Show archived",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = showArchived, onCheckedChange = { showArchived = it })
                }
            }

            when {
                state.isLoading -> LoadingShimmer(
                    modifier = Modifier.padding(20.dp),
                    rows = 4,
                )

                state.error != null && state.records.isEmpty() -> ErrorState(
                    message = state.error,
                    onRetry = onRetry,
                )

                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (selectedTab == 0) {
                        RecordsTab(
                            state = state,
                            records = visibleRecords,
                            onAddRecord = onAddRecord,
                            onViewRecord = { recordToView = it },
                        )
                    } else {
                        TriageTab(
                            triage = triage,
                            onViewRecord = { recordToView = it },
                        )
                    }
                }
            }
        }

        AppSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }

    recordToView?.let { record ->
        RecordDetailSheet(
            record = record,
            isBusy = state.busyRecordId == record.id,
            onDismiss = { recordToView = null },
            onEdit = {
                recordToView = null
                onEditRecord(record.id)
            },
            onScheduleFollowUp = {
                recordToView = null
                onScheduleFollowUp(record.id)
            },
            onArchive = {
                recordToView = null
                onArchive(record, !record.isArchived)
            },
            onDelete = if (record.isArchived) {
                {
                    recordToView = null
                    recordToDelete = record
                }
            } else {
                null
            },
        )
    }

    recordToDelete?.let { record ->
        ConfirmDialog(
            title = "Delete archived record permanently?",
            message = "This permanently removes ${record.studentName}'s private notes and public summary for this session. It cannot be undone and the student's summary will disappear.",
            confirmLabel = "Delete forever",
            onConfirm = { onDeleteRecord(record) },
            onDismiss = { recordToDelete = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordsTab(
    state: SessionRecordsUiState,
    records: List<SessionRecord>,
    onAddRecord: (String) -> Unit,
    onViewRecord: (SessionRecord) -> Unit,
) {
    if (state.eligibleAppointments.isEmpty() && records.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            EmptyState(
                icon = Icons.Outlined.Description,
                title = "No completed sessions yet",
                message = "Completed appointments will appear here so you can add a private note and public summary.",
            )
        }
        return
    }

    val active = records.filter { !it.isArchived }
    val archived = records.filter { it.isArchived }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        if (state.eligibleAppointments.isNotEmpty()) {
            stickyHeader(key = "header-ready") {
                StickyListHeader(text = "Ready for notes")
            }
            items(state.eligibleAppointments, key = { "ready-${it.id}" }) { appointment ->
                EligibleAppointmentRow(
                    appointment = appointment,
                    onAdd = { onAddRecord(appointment.id) },
                )
            }
        }
        if (active.isNotEmpty()) {
            stickyHeader(key = "header-active") {
                StickyListHeader(text = "Active records")
            }
            items(active, key = { it.id }) { record ->
                RecordRow(
                    record = record,
                    onClick = { onViewRecord(record) },
                )
            }
        }
        if (archived.isNotEmpty()) {
            stickyHeader(key = "header-archived") {
                StickyListHeader(text = "Archived")
            }
            items(archived, key = { it.id }) { record ->
                RecordRow(
                    record = record,
                    onClick = { onViewRecord(record) },
                )
            }
        }
        if (records.isEmpty()) {
            item(key = "empty-records") {
                EmptyState(
                    icon = Icons.Outlined.Description,
                    title = "No saved records yet",
                    message = "Saved records and their follow-up dates will appear here.",
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TriageTab(
    triage: Map<TriageBucket, List<SessionRecord>>,
    onViewRecord: (SessionRecord) -> Unit,
) {
    val total = triage.values.sumOf { it.size }
    if (total == 0) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            EmptyState(
                icon = Icons.Outlined.Description,
                title = "Triage board is clear",
                message = "High-risk or follow-up-needed records will be grouped into Overdue, Due soon and Later.",
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        TriageBucket.entries.forEach { bucket ->
            val bucketRecords = triage[bucket].orEmpty()
            if (bucketRecords.isNotEmpty()) {
                stickyHeader(key = "header-${bucket.name}") {
                    StickyListHeader(text = "${bucket.label} (${bucketRecords.size})")
                }
                items(bucketRecords, key = { "triage-${it.id}" }) { record ->
                    RecordRow(
                        record = record,
                        onClick = { onViewRecord(record) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EligibleAppointmentRow(
    appointment: Appointment,
    onAdd: () -> Unit,
) {
    AppListRow(
        title = appointment.studentName,
        supportingText = "${DateTimeUtils.formatDate(appointment.slotStart)} · " +
            appointment.concernCategory,
        leadingIcon = Icons.Outlined.Add,
        trailingContent = {
            Text(
                text = "Add",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        onClick = onAdd,
    )
}

@Composable
private fun RecordRow(
    record: SessionRecord,
    onClick: () -> Unit,
) {
    AppListRow(
        title = record.studentName,
        supportingText = "${DateTimeUtils.formatDate(record.sessionStart)} · " +
            record.concernCategory,
        leadingIcon = Icons.Outlined.Description,
        trailingContent = {
            if (record.isArchived) {
                StatusChip("archived")
            } else {
                RiskBadge(record.riskLevel)
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun RecordDetailSheet(
    record: SessionRecord,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onScheduleFollowUp: () -> Unit,
    onArchive: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    AppBottomSheet(
        onDismissRequest = onDismiss,
        title = record.studentName,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${DateTimeUtils.formatDate(record.sessionStart)} · " +
                    record.concernCategory,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            RiskBadge(record.riskLevel)
        }
        if (record.isArchived) {
            StatusChip(
                status = "archived",
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Text(
            text = "Public summary",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = record.publicSummary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = "Private notes",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = record.privateNotes,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (record.followUpNeeded) {
            Text(
                text = "Follow-up due: " +
                    (record.followUpDue?.let { DateTimeUtils.formatDateTime(it) } ?: "Not set"),
                style = MaterialTheme.typography.labelMedium,
                color = LocalSemanticColors.current.warningContent,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        if (record.isHighRisk) {
            Text(
                text = "High risk — prioritise review.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (!record.canEdit && !record.isArchived) {
            Text(
                text = "Content is read-only after 7 days. Archive/restore is still available.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        if (record.canEdit && !record.isArchived) {
            Button(
                onClick = onEdit,
                enabled = !isBusy,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("Edit record")
            }
            Spacer(Modifier.height(8.dp))
        }
        if (!record.isArchived) {
            OutlinedButton(
                onClick = onScheduleFollowUp,
                enabled = !isBusy,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("Schedule follow-up")
            }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(
            onClick = onArchive,
            enabled = !isBusy,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(if (record.isArchived) "Restore record" else "Archive record")
        }
        if (onDelete != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDelete,
                enabled = !isBusy,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("Delete forever", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
