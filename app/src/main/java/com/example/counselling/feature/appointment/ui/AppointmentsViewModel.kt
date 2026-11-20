// Module 2 | Owner: Beh
// Shared list logic for students, counsellors and admins. RLS decides which
// appointment rows each role actually receives.
package com.example.counselling.feature.appointment.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.domain.AppointmentStatus
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.appointment.domain.Appointment
import com.example.counselling.feature.appointment.domain.AppointmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class AppointmentsViewModel(
    private val repository: AppointmentRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AppointmentsUiState())
    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()

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

    fun declineBooking(appointmentId: String) {
        if (_uiState.value.isMutating) return
        performMutation(appointmentId, "Booking declined and slot released.") {
            repository.updateStatus(appointmentId, AppointmentStatus.Cancelled)
        }
    }

    fun cancelConfirmedSession(appointmentId: String) {
        if (_uiState.value.isMutating) return
        performMutation(appointmentId, "Session cancelled and slot released.") {
            repository.updateStatus(appointmentId, AppointmentStatus.Cancelled)
        }
    }

    fun updateStatus(appointmentId: String, newStatus: AppointmentStatus) {
        if (_uiState.value.isMutating) return
        val message = when (newStatus) {
            AppointmentStatus.Confirmed -> "Appointment confirmed."
            AppointmentStatus.Completed -> "Session marked as completed."
            AppointmentStatus.NoShow -> "Session marked as no-show."
            AppointmentStatus.Cancelled -> "Appointment cancelled."
            AppointmentStatus.Pending -> "Appointment updated."
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
                postSuccess(successMessage)
                load()
            }.onFailure {
                postError(it.message ?: "Could not update the appointment.")
            }
        }
    }

    companion object {
        fun factory(repository: AppointmentRepository) = viewModelFactory {
            initializer { AppointmentsViewModel(repository) }
        }
    }
}
