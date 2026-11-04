// Module 2 | Owner: Beh
// Shared list logic for students, counsellors and admins. RLS decides which
// appointment rows each role actually receives.
package com.example.counselling.feature.appointment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.MessageKind
import com.example.counselling.core.UiMessage
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.appointment.model.Appointment
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppointmentsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isMutating: Boolean = false,
    val busyAppointmentId: String? = null,
    val appointments: List<Appointment> = emptyList(),
    val error: String? = null,
)

class AppointmentsViewModel(
    private val repository: AppointmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentsUiState())
    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()

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
                    isLoading = !refresh && it.appointments.isEmpty(),
                    isRefreshing = refresh,
                    error = null,
                )
            }
            repository.getAppointments()
                .onSuccess { appointments ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            appointments = appointments,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error.message ?: "Could not load appointments.",
                        )
                    }
                }
        }
    }

    fun cancelAppointment(appointmentId: String) {
        if (_uiState.value.isMutating) return
        performMutation(appointmentId, "Appointment cancelled.") {
            repository.cancelAppointment(appointmentId)
        }
    }

    fun updateStatus(appointmentId: String, newStatus: String) {
        if (_uiState.value.isMutating) return
        val message = when (newStatus) {
            "confirmed" -> "Appointment confirmed."
            "completed" -> "Session marked as completed."
            "no_show" -> "Session marked as no-show."
            else -> "Appointment updated."
        }
        performMutation(appointmentId, message) {
            repository.updateStatus(appointmentId, newStatus)
        }
    }

    private fun performMutation(
        appointmentId: String,
        successMessage: String,
        action: suspend () -> Result<Unit>,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, busyAppointmentId = appointmentId) }
            val result = action()
            _uiState.update { it.copy(isMutating = false, busyAppointmentId = null) }
            result.onSuccess {
                _messages.send(UiMessage(successMessage, MessageKind.SUCCESS))
                load()
            }.onFailure {
                _messages.send(
                    UiMessage(
                        it.message ?: "Could not update the appointment.",
                        MessageKind.ERROR,
                    ),
                )
            }
        }
    }

    companion object {
        fun factory(repository: AppointmentRepository) = viewModelFactory {
            initializer { AppointmentsViewModel(repository) }
        }
    }
}
