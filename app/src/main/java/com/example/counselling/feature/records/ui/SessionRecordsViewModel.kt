// Module 4 / Unique Feature 2 | Owner: Jeremy
package com.example.counselling.feature.records.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.domain.AppointmentStatus
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.appointment.domain.Appointment
import com.example.counselling.feature.appointment.domain.AppointmentRepository
import com.example.counselling.feature.records.domain.SessionRecord
import com.example.counselling.feature.records.domain.SessionRecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionRecordsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val records: List<SessionRecord> = emptyList(),
    val eligibleAppointments: List<Appointment> = emptyList(),
    val busyRecordId: String? = null,
    // SessionRecord equality is id-only, so a status-only reload compares equal
    // and Compose would skip the recomposition. Bumping this forces the screen
    // to redraw after archive/restore.
    val contentVersion: Long = 0,
)

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class SessionRecordsViewModel(
    private val recordRepository: SessionRecordRepository,
    private val appointmentRepository: AppointmentRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SessionRecordsUiState())
    val uiState: StateFlow<SessionRecordsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() = load(refresh = true)
    fun retry() = load()

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.records.isEmpty(),
                    isRefreshing = refresh,
                    error = null,
                )
            }
            val appointmentsResult = appointmentRepository.getAppointments()
            val recordsResult = recordRepository.getRecords()

            val appointments = appointmentsResult.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message ?: "Could not load completed appointments.",
                    )
                }
                return@launch
            }
            val records = recordsResult.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message ?: "Could not load session records.",
                    )
                }
                return@launch
            }
            val recordedAppointmentIds = records.map { it.appointmentId }.toSet()
            val eligible = appointments.filter {
                it.status == AppointmentStatus.Completed && it.id !in recordedAppointmentIds
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    records = records,
                    eligibleAppointments = eligible,
                    contentVersion = it.contentVersion + 1,
                )
            }
        }
    }

    fun setArchived(record: SessionRecord, archived: Boolean) {
        if (_uiState.value.busyRecordId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(busyRecordId = record.id) }
            recordRepository.setArchived(record.id, archived)
                .onSuccess {
                    postSuccess(
                        if (archived) "Session record archived." else "Session record restored.",
                    )
                    load()
                }
                .onFailure {
                    postError(it.message ?: "Could not update the archive state.")
                }
            _uiState.update { it.copy(busyRecordId = null) }
        }
    }

    fun deleteRecord(record: SessionRecord) {
        if (_uiState.value.busyRecordId != null) return
        if (!record.isArchived) {
            postError("Archive the record before deleting it permanently.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busyRecordId = record.id) }
            recordRepository.deleteRecord(record.id)
                .onSuccess {
                    postSuccess("Archived session record permanently deleted.")
                    load()
                }
                .onFailure {
                    postError(it.message ?: "Could not delete the record.")
                }
            _uiState.update { it.copy(busyRecordId = null) }
        }
    }

    companion object {
        fun factory(
            recordRepository: SessionRecordRepository,
            appointmentRepository: AppointmentRepository,
        ) = viewModelFactory {
            initializer {
                SessionRecordsViewModel(recordRepository, appointmentRepository)
            }
        }
    }
}
