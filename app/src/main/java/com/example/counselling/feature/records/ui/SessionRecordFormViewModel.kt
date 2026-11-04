// Module 4 | Owner: Jeremy
package com.example.counselling.feature.records.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.core.MessageKind
import com.example.counselling.core.UiMessage
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.appointment.model.Appointment
import com.example.counselling.feature.records.data.SessionRecordRepository
import com.example.counselling.feature.records.model.SessionRecord
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionRecordFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val readOnly: Boolean = false,
    val appointment: Appointment? = null,
    val privateNotes: String = "",
    val publicSummary: String = "",
    val riskLevel: String = "",
    val followUpNeeded: Boolean = false,
    val followUpDueMillis: Long? = null,
    val privateNotesError: String? = null,
    val publicSummaryError: String? = null,
    val riskError: String? = null,
    val followUpDateError: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
)

class SessionRecordFormViewModel(
    private val recordId: String?,
    private val appointmentId: String?,
    private val recordRepository: SessionRecordRepository,
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SessionRecordFormUiState(isEdit = recordId != null),
    )
    val uiState: StateFlow<SessionRecordFormUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        load()
    }

    fun retry() = load()

    fun onPrivateNotesChange(value: String) =
        _uiState.update { it.copy(privateNotes = value, privateNotesError = null) }

    fun onPublicSummaryChange(value: String) =
        _uiState.update { it.copy(publicSummary = value, publicSummaryError = null) }

    fun onRiskChange(value: String) =
        _uiState.update { it.copy(riskLevel = value, riskError = null) }

    fun onFollowUpNeededChange(value: Boolean) = _uiState.update {
        it.copy(
            followUpNeeded = value,
            followUpDateError = null,
            followUpDueMillis = if (value) it.followUpDueMillis else null,
        )
    }

    fun onFollowUpDateChange(value: Long) =
        _uiState.update { it.copy(followUpDueMillis = value, followUpDateError = null) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.readOnly) return
        val appointment = state.appointment
        if (appointment == null) {
            _messages.trySend(UiMessage("That appointment could not be loaded.", MessageKind.ERROR))
            return
        }
        val notesError = when {
            state.privateNotes.trim().length < 20 ->
                "Private notes must be at least 20 characters."
            state.privateNotes.trim().length > 5000 ->
                "Private notes must be 5,000 characters or fewer."
            else -> null
        }
        val summaryError = when {
            state.publicSummary.trim().isEmpty() -> "Add a public summary."
            state.publicSummary.trim().length > 2000 ->
                "Public summary must be 2,000 characters or fewer."
            else -> null
        }
        val riskError = if (state.riskLevel !in listOf("low", "medium", "high")) {
            "Choose a risk level."
        } else {
            null
        }
        val due = state.followUpDueMillis?.let {
            DateTimeUtils.toApiTimestamp(it, 9, 0)
        }
        val dueError = if (state.followUpNeeded) {
            when {
                due == null -> "Choose a follow-up due date."
                DateTimeUtils.isPast(due) -> "The follow-up due date must be in the future."
                else -> null
            }
        } else {
            null
        }
        _uiState.update {
            it.copy(
                privateNotesError = notesError,
                publicSummaryError = summaryError,
                riskError = riskError,
                followUpDateError = dueError,
            )
        }
        if (listOf(notesError, summaryError, riskError, dueError).any { it != null }) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = if (recordId == null) {
                recordRepository.createRecord(
                    appointmentId = appointment.id,
                    privateNotes = state.privateNotes,
                    publicSummary = state.publicSummary,
                    riskLevel = state.riskLevel,
                    followUpNeeded = state.followUpNeeded,
                    followUpDue = due,
                )
            } else {
                recordRepository.updateRecord(
                    recordId = recordId,
                    privateNotes = state.privateNotes,
                    publicSummary = state.publicSummary,
                    riskLevel = state.riskLevel,
                    followUpNeeded = state.followUpNeeded,
                    followUpDue = due,
                )
            }
            _uiState.update { it.copy(isSaving = false) }
            result.onSuccess {
                _uiState.update { it.copy(saveSuccess = true) }
                _messages.send(
                    UiMessage(
                        if (recordId == null) "Session record created." else "Session record updated.",
                        MessageKind.SUCCESS,
                    ),
                )
            }.onFailure {
                _messages.send(
                    UiMessage(
                        it.message ?: "Could not save the session record.",
                        MessageKind.ERROR,
                    ),
                )
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            if (recordId != null) {
                val records = recordRepository.getRecords()
                val record = records.getOrNull()?.firstOrNull { it.id == recordId }
                if (record == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadError = records.exceptionOrNull()?.message
                                ?: "That session record could not be found.",
                        )
                    }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        readOnly = !record.canEdit,
                        appointment = record.appointment,
                        privateNotes = record.privateNotes,
                        publicSummary = record.publicSummary,
                        riskLevel = record.riskLevel,
                        followUpNeeded = record.followUpNeeded,
                        followUpDueMillis = DateTimeUtils.localDate(record.followUpDue)?.let {
                            DateTimeUtils.datePickerMillis(it)
                        },
                    )
                }
            } else {
                val appointments = appointmentRepository.getAppointments()
                val appointment = appointments.getOrNull()
                    ?.firstOrNull { it.id == appointmentId }
                if (appointment == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadError = appointments.exceptionOrNull()?.message
                                ?: "That completed appointment could not be found.",
                        )
                    }
                    return@launch
                }
                if (appointment.status != "completed") {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadError = "Session records can only be added for completed appointments.",
                        )
                    }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        appointment = appointment,
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            recordId: String?,
            appointmentId: String?,
            recordRepository: SessionRecordRepository,
            appointmentRepository: AppointmentRepository,
        ) = viewModelFactory {
            initializer {
                SessionRecordFormViewModel(
                    recordId,
                    appointmentId,
                    recordRepository,
                    appointmentRepository,
                )
            }
        }
    }
}
