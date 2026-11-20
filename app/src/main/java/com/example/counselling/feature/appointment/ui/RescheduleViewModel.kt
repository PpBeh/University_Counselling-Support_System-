// Module 2 | Owner: Beh
// Students keep the 12-hour notice rule. A counsellor may reschedule one of
// their own confirmed sessions without that notice rule for an emergency.
package com.example.counselling.feature.appointment.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.domain.AppointmentStatus
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.appointment.domain.Appointment
import com.example.counselling.feature.appointment.domain.AppointmentRepository
import com.example.counselling.feature.appointment.domain.AvailableSlot
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RescheduleUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val appointment: Appointment? = null,
    val slots: List<AvailableSlot> = emptyList(),
    val selectedSlotId: String? = null,
    val tooLateToChange: Boolean = false,
    val isSubmitting: Boolean = false,
    val success: Boolean = false,
)

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class RescheduleViewModel(
    private val appointmentId: String,
    private val repository: AppointmentRepository,
    private val isCounsellor: Boolean,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(RescheduleUiState())
    val uiState: StateFlow<RescheduleUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun chooseSlot(slotId: String) {
        _uiState.update { it.copy(selectedSlotId = slotId) }
    }

    fun clearSlot() {
        _uiState.update { it.copy(selectedSlotId = null) }
    }

    fun submit() {
        val current = _uiState.value
        val appointment = current.appointment ?: return
        val slotId = current.selectedSlotId ?: return
        if (current.isSubmitting || current.tooLateToChange) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val result = if (isCounsellor) {
                repository.counsellorRescheduleAppointment(appointment.id, slotId)
            } else {
                repository.rescheduleAppointment(appointment.id, slotId)
            }
            _uiState.update { it.copy(isSubmitting = false) }
            result.onSuccess {
                _uiState.update { it.copy(success = true) }
                postSuccess(if (isCounsellor) "Session rescheduled." else "Appointment rescheduled.")
            }.onFailure { error ->
                postError(error.message ?: "Could not reschedule the appointment.")
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

            val actor = if (isCounsellor) appointment.counsellor else appointment.student
            val now = Instant.now()
            val allowed = appointment.canBeRescheduledBy(actor, now)
            if (!allowed) {
                val message = when {
                    isCounsellor && appointment.status != AppointmentStatus.Confirmed ->
                        "Only confirmed sessions can be rescheduled by the counsellor."
                    isCounsellor ->
                        "This session has already started and cannot be rescheduled."
                    appointment.status !in setOf(AppointmentStatus.Pending, AppointmentStatus.Confirmed) ->
                        "Only pending or confirmed appointments can be rescheduled."
                    else ->
                        "Appointments can only be rescheduled at least 12 hours before the session."
                }
                _uiState.value = RescheduleUiState(
                    isLoading = false,
                    appointment = appointment,
                    tooLateToChange = true,
                    error = message,
                )
                return@launch
            }

            repository.getAvailableSlots()
                .onSuccess { allSlots ->
                    val ownSlots = allSlots
                        .filter { it.counsellorId == appointment.counsellor.id }
                        .sortedBy { it.timeSlot.start }
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
            isCounsellor: Boolean,
        ) = viewModelFactory {
            initializer {
                RescheduleViewModel(appointmentId, repository, isCounsellor)
            }
        }
    }
}
