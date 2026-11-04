// Module 4 | Owner: Jeremy
package com.example.counselling.feature.records.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.records.data.SessionRecordRepository
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.DatePickerField
import com.example.counselling.ui.components.DropdownField
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.SectionHeader
import com.example.counselling.ui.components.showAppMessage
import kotlinx.coroutines.delay

@Composable
fun SessionRecordFormRoute(
    recordId: String?,
    appointmentId: String?,
    recordRepository: SessionRecordRepository,
    appointmentRepository: AppointmentRepository,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val viewModel: SessionRecordFormViewModel = viewModel(
        factory = SessionRecordFormViewModel.factory(
            recordId = recordId,
            appointmentId = appointmentId,
            recordRepository = recordRepository,
            appointmentRepository = appointmentRepository,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            delay(900)
            onSaved()
        }
    }

    SessionRecordFormScreen(
        state = state,
        snackbarHostState = snackbarState,
        onBack = onBack,
        onRetry = viewModel::retry,
        onPrivateNotesChange = viewModel::onPrivateNotesChange,
        onPublicSummaryChange = viewModel::onPublicSummaryChange,
        onRiskChange = viewModel::onRiskChange,
        onFollowUpNeededChange = viewModel::onFollowUpNeededChange,
        onFollowUpDateChange = viewModel::onFollowUpDateChange,
        onSave = viewModel::save,
    )
}

@Composable
private fun SessionRecordFormScreen(
    state: SessionRecordFormUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPrivateNotesChange: (String) -> Unit,
    onPublicSummaryChange: (String) -> Unit,
    onRiskChange: (String) -> Unit,
    onFollowUpNeededChange: (Boolean) -> Unit,
    onFollowUpDateChange: (Long) -> Unit,
    onSave: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = if (state.isEdit) "Edit record" else "New session record",
                onBack = onBack,
            )

            when {
                state.isLoading -> LoadingShimmer(
                    modifier = Modifier.padding(20.dp),
                    rows = 5,
                )

                state.loadError != null -> ErrorState(
                    message = state.loadError,
                    onRetry = onRetry,
                )

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = if (state.isEdit) "Session record" else "Add a private record",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        text = "This is only available after a completed session.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    )

                    state.appointment?.let { appointment ->
                        InfoCard(
                            title = appointment.studentName,
                            body = "${DateTimeUtils.formatDate(appointment.slotStart)} · " +
                                "${appointment.concernCategory} · ${appointment.studentCode ?: "Student"}",
                        )
                    }

                    if (state.readOnly) {
                        Spacer(Modifier.height(12.dp))
                        InfoCard(
                            title = "This record is read-only",
                            body = "Session record content can only be edited within 7 days of creation. You can still archive or restore it from the Records list.",
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    SectionHeader(
                        title = "Private notes",
                        subtitle = "Only the owning counsellor can read this. Minimum 20 characters.",
                    )
                    Spacer(Modifier.height(6.dp))
                    AppTextField(
                        value = state.privateNotes,
                        onValueChange = onPrivateNotesChange,
                        label = "Private notes",
                        error = state.privateNotesError,
                        enabled = !state.isSaving && !state.readOnly,
                        singleLine = false,
                        maxLines = 6,
                        supportingText = "${state.privateNotes.length}/5000",
                    )

                    Spacer(Modifier.height(16.dp))
                    SectionHeader(
                        title = "Public summary",
                        subtitle = "The student sees this in their session summary history.",
                    )
                    Spacer(Modifier.height(6.dp))
                    AppTextField(
                        value = state.publicSummary,
                        onValueChange = onPublicSummaryChange,
                        label = "Public summary",
                        error = state.publicSummaryError,
                        enabled = !state.isSaving && !state.readOnly,
                        singleLine = false,
                        maxLines = 5,
                        supportingText = "${state.publicSummary.length}/2000",
                    )

                    Spacer(Modifier.height(16.dp))
                    DropdownField(
                        label = "Risk level",
                        options = listOf("Low", "Medium", "High"),
                        selected = state.riskLevel.replaceFirstChar { it.uppercase() },
                        onSelect = { onRiskChange(it.lowercase()) },
                        enabled = !state.isSaving && !state.readOnly,
                        error = state.riskError,
                    )

                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Follow-up needed",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Adds this record to the urgency triage board.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.followUpNeeded,
                            onCheckedChange = onFollowUpNeededChange,
                            enabled = !state.isSaving && !state.readOnly,
                        )
                    }
                    if (state.followUpNeeded) {
                        Spacer(Modifier.height(10.dp))
                        DatePickerField(
                            label = "Follow-up due date",
                            selectedMillis = state.followUpDueMillis,
                            onDateSelected = onFollowUpDateChange,
                            enabled = !state.isSaving && !state.readOnly,
                            error = state.followUpDateError,
                            supportingText = "Due at 9:00 AM Kuala Lumpur time.",
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    if (!state.readOnly) {
                        PrimaryButton(
                            text = if (state.isEdit) "Save record" else "Create record",
                            onClick = {
                                focusManager.clearFocus()
                                onSave()
                            },
                            loading = state.isSaving,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
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
}
