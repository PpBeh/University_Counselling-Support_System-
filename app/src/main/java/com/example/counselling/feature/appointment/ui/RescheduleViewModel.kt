// Module 2 | Owner: Beh
// Reschedule keeps the counsellor and concern, changes only the time slot and
// relies on the 12-hour notice rule enforced by the database function.
package com.example.counselling.feature.appointment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.core.MessageKind
import com.example.counselling.core.UiMessage
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.appointment.model.Appointment
import com.example.counselling.feature.appointment.model.AvailableSlotRow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RescheduleUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val appointment: Appointment? = null,
    val slots: List<AvailableSlotRow> = emptyList(),
    val selectedSlotId: String? = null,
    val tooLateToChange: Boolean = false,
    val isSubmitting: Boolean = false,
    val success: Boolean = false,
)

class RescheduleViewModel(
    private val appointmentId: String,
    private val repository: AppointmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RescheduleUiState())
    val uiState: StateFlow<RescheduleUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        load()
    }

    fun retry() = load()

    fun chooseSlot(slotId: String) {
        _uiState.update { it.copy(selectedSlotId = slotId) }
    }

    fun submit() {
        val current = _uiState.value
        val appointment = current.appointment ?: return
        val slotId = current.selectedSlotId ?: return
        if (current.isSubmitting || current.tooLateToChange) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val result = repository.rescheduleAppointment(appointment.id, slotId)
            _uiState.update { it.copy(isSubmitting = false) }
            result.onSuccess {
                _uiState.update { it.copy(success = true) }
                _messages.send(
                    UiMessage("Appointment rescheduled.", MessageKind.SUCCESS),
                )
            }.onFailure { error ->
                _messages.send(
                    UiMessage(
                        error.message ?: "Could not reschedule the appointment.",
                        MessageKind.ERROR,
                    ),
                )
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = RescheduleUiState(isLoading = true)
            val appointmentResult = repository.getAppointments()
            val appointment = appointmentResult.getOrNull()?.firstOrNull { it.id == appointmentId }
            if (appointment == null) {
                val message = appointmentResult.exceptionOrNull()?.message
                    ?: "That appointment could not be found."
                _uiState.value = RescheduleUiState(isLoading = false, error = message)
                return@launch
            }

            if (!DateTimeUtils.canReschedule(appointment.slotStart)) {
                _uiState.value = RescheduleUiState(
                    isLoading = false,
                    appointment = appointment,
                    tooLateToChange = true,
                    error = "Appointments can only be rescheduled at least 12 hours before the session.",
                )
                return@launch
            }

            repository.getAvailableSlots()
                .onSuccess { allSlots ->
                    val ownSlots = allSlots
                        .filter { it.counsellorId == appointment.counsellorId }
                        .sortedBy { it.startAt }
                    _uiState.value = RescheduleUiState(
                        isLoading = false,
                        appointment = appointment,
                        slots = ownSlots,
                    )
                }
                .onFailure { error ->
                    _uiState.value = RescheduleUiState(
                        isLoading = false,
                        appointment = appointment,
                        error = error.message ?: "Could not load available slots.",
                    )
                }
        }
    }

    companion object {
        fun factory(
            appointmentId: String,
            repository: AppointmentRepository,
        ) = viewModelFactory {
            initializer { RescheduleViewModel(appointmentId, repository) }
        }
    }
}
