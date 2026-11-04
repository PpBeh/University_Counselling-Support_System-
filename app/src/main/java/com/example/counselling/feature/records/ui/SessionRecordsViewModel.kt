// Module 4 / Unique Feature 2 | Owner: Jeremy
package com.example.counselling.feature.records.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.MessageKind
import com.example.counselling.core.UiMessage
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.records.data.SessionRecordRepository
import com.example.counselling.feature.records.model.SessionRecord
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionRecordsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val records: List<SessionRecord> = emptyList(),
    val eligibleAppointments: List<com.example.counselling.feature.appointment.model.Appointment> =
        emptyList(),
    val busyRecordId: String? = null,
)

class SessionRecordsViewModel(
    private val recordRepository: SessionRecordRepository,
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionRecordsUiState())
    val uiState: StateFlow<SessionRecordsUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

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
                it.status == "completed" && it.id !in recordedAppointmentIds
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    records = records,
                    eligibleAppointments = eligible,
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
                    _messages.send(
                        UiMessage(
                            if (archived) "Session record archived." else "Session record restored.",
                            MessageKind.SUCCESS,
                        ),
                    )
                    load()
                }
                .onFailure {
                    _messages.send(
                        UiMessage(
                            it.message ?: "Could not update the archive state.",
                            MessageKind.ERROR,
                        ),
                    )
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
