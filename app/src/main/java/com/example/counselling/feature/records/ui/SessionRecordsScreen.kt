// Module 4 / Unique Feature 2 | Owner: Jeremy
// Records list, completed-session intake and the urgency triage board.
package com.example.counselling.feature.records.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.appointment.model.Appointment
import com.example.counselling.feature.records.data.SessionRecordRepository
import com.example.counselling.feature.records.model.SessionRecord
import com.example.counselling.feature.records.model.TriageBucket
import com.example.counselling.feature.records.model.triageRecords
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.RiskBadge
import com.example.counselling.ui.components.SectionHeader
import com.example.counselling.ui.components.StatusChip
import com.example.counselling.ui.components.showAppMessage
import com.example.counselling.ui.theme.WarningAmber

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
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionRecordsScreen(
    state: SessionRecordsUiState,
    snackbarHostState: SnackbarHostState,
    onAddRecord: (String) -> Unit,
    onEditRecord: (String) -> Unit,
    onScheduleFollowUp: (String) -> Unit,
    onArchive: (SessionRecord, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var recordToView by remember { mutableStateOf<SessionRecord?>(null) }
    val triage = remember(state.records) { state.records.triageRecords() }
    val visibleRecords = remember(state.records, showArchived) {
        state.records.filter { showArchived || !it.isArchived }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = "Session records")
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
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
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
                        onEditRecord = onEditRecord,
                        onScheduleFollowUp = onScheduleFollowUp,
                        onViewRecord = { recordToView = it },
                        onArchive = onArchive,
                    )
                } else {
                    TriageTab(
                        triage = triage,
                        onEditRecord = onEditRecord,
                        onScheduleFollowUp = onScheduleFollowUp,
                        onViewRecord = { recordToView = it },
                        onArchive = onArchive,
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
        RecordDetailDialog(record = record, onDismiss = { recordToView = null })
    }
}

@Composable
private fun RecordDetailDialog(record: SessionRecord, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(record.studentName)
                Text(
                    text = "${DateTimeUtils.formatDate(record.sessionStart)} · " +
                        record.concernCategory,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RiskBadge(record.riskLevel)
                    if (record.isArchived) StatusChip("archived")
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
                            (record.followUpDue?.let { DateTimeUtils.formatDateTime(it) }
                                ?: "Not set"),
                        style = MaterialTheme.typography.labelMedium,
                        color = WarningAmber,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
private fun RecordsTab(
    state: SessionRecordsUiState,
    records: List<SessionRecord>,
    onAddRecord: (String) -> Unit,
    onEditRecord: (String) -> Unit,
    onScheduleFollowUp: (String) -> Unit,
    onViewRecord: (SessionRecord) -> Unit,
    onArchive: (SessionRecord, Boolean) -> Unit,
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.eligibleAppointments.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Completed sessions ready for notes",
                    subtitle = "One record per completed appointment.",
                )
            }
            items(state.eligibleAppointments, key = { "ready-${it.id}" }) { appointment ->
                EligibleAppointmentCard(
                    appointment = appointment,
                    onAdd = { onAddRecord(appointment.id) },
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
        if (records.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Description,
                    title = if (state.eligibleAppointments.isEmpty()) {
                        "No records to show"
                    } else {
                        "No saved records yet"
                    },
                    message = "Saved records and their follow-up dates will appear here.",
                )
            }
        } else {
            items(records, key = { it.id }) { record ->
                RecordCard(
                    record = record,
                    isBusy = state.busyRecordId == record.id,
                    onView = { onViewRecord(record) },
                    onEdit = { onEditRecord(record.id) },
                    onScheduleFollowUp = { onScheduleFollowUp(record.id) },
                    onArchive = { onArchive(record, !record.isArchived) },
                )
            }
        }
    }
}

@Composable
private fun TriageTab(
    triage: Map<TriageBucket, List<SessionRecord>>,
    onEditRecord: (String) -> Unit,
    onScheduleFollowUp: (String) -> Unit,
    onViewRecord: (SessionRecord) -> Unit,
    onArchive: (SessionRecord, Boolean) -> Unit,
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TriageBucket.entries.forEach { bucket ->
            val bucketRecords = triage[bucket].orEmpty()
            if (bucketRecords.isNotEmpty()) {
                item(key = "header-${bucket.name}") {
                    SectionHeader(
                        title = "${bucket.label} (${bucketRecords.size})",
                        subtitle = when (bucket) {
                            TriageBucket.OVERDUE -> "Follow-up date has passed."
                            TriageBucket.DUE_SOON -> "Due within the next seven days or high-risk review."
                            TriageBucket.LATER -> "Scheduled for later follow-up."
                        },
                    )
                }
                items(bucketRecords, key = { "triage-${it.id}" }) { record ->
                    RecordCard(
                        record = record,
                        isBusy = false,
                        onView = { onViewRecord(record) },
                        onEdit = { onEditRecord(record.id) },
                        onScheduleFollowUp = { onScheduleFollowUp(record.id) },
                        onArchive = { onArchive(record, true) },
                    )
                }
                item(key = "divider-${bucket.name}") {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun EligibleAppointmentCard(
    appointment: Appointment,
    onAdd: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appointment.studentName,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${DateTimeUtils.formatDate(appointment.slotStart)} · " +
                        appointment.concernCategory,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onAdd) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                )
                Text("Add record")
            }
        }
    }
}

@Composable
fun RecordCard(
    record: SessionRecord,
    isBusy: Boolean,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onScheduleFollowUp: () -> Unit,
    onArchive: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.studentName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${DateTimeUtils.formatDate(record.sessionStart)} · ${record.concernCategory}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    RiskBadge(record.riskLevel)
                    if (record.isArchived) {
                        Spacer(Modifier.height(4.dp))
                        StatusChip("archived")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = record.publicSummary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (record.followUpNeeded) {
                Text(
                    text = "Follow-up due: " +
                        (record.followUpDue?.let { DateTimeUtils.formatDateTime(it) } ?: "Not set"),
                    style = MaterialTheme.typography.labelMedium,
                    color = WarningAmber,
                    modifier = Modifier.padding(top = 8.dp),
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

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onView, enabled = !isBusy) {
                    Text("View")
                }
                if (record.canEdit && !record.isArchived) {
                    TextButton(onClick = onEdit, enabled = !isBusy) {
                        Text("Edit")
                    }
                }
                if (!record.isArchived) {
                    TextButton(onClick = onScheduleFollowUp, enabled = !isBusy) {
                        Text("Follow up")
                    }
                }
                TextButton(onClick = onArchive, enabled = !isBusy) {
                    Text(if (record.isArchived) "Restore" else "Archive")
                }
            }
        }
    }
}
