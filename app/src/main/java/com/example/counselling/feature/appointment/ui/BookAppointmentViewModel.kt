// Module 2 | Owner: Beh
// Booking + Unique Feature 1 (Smart Counsellor Match).
package com.example.counselling.feature.appointment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.ErrorMapper
import com.example.counselling.core.MessageKind
import com.example.counselling.core.SessionManager
import com.example.counselling.core.UiMessage
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.appointment.model.CounsellorMatch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BookingStep {
    CONCERN,
    MATCHES,
    SLOTS,
    SUCCESS,
}

data class BookAppointmentUiState(
    val step: BookingStep = BookingStep.CONCERN,
    val concern: String? = null,
    val matches: List<CounsellorMatch> = emptyList(),
    val selectedMatch: CounsellorMatch? = null,
    val selectedSlotId: String? = null,
    val mode: String = "in_person",
    val note: String = "",
    val noteError: String? = null,
    val isLoadingMatches: Boolean = false,
    val loadError: String? = null,
    val isBooking: Boolean = false,
)

class BookAppointmentViewModel(
    private val repository: AppointmentRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    private val preferredMode = sessionManager.currentProfile?.preferredSessionMode
        ?.takeIf { it in listOf("in_person", "online") }
        ?: "in_person"

    private val _uiState = MutableStateFlow(BookAppointmentUiState(mode = preferredMode))
    val uiState: StateFlow<BookAppointmentUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun chooseConcern(concern: String) {
        _uiState.update {
            it.copy(
                concern = concern,
                step = BookingStep.MATCHES,
                matches = emptyList(),
                selectedMatch = null,
                selectedSlotId = null,
                loadError = null,
            )
        }
        loadMatches()
    }

    fun retryMatches() = loadMatches()

    fun chooseCounsellor(match: CounsellorMatch) {
        _uiState.update {
            it.copy(
                selectedMatch = match,
                selectedSlotId = null,
                step = BookingStep.SLOTS,
            )
        }
    }

    fun chooseSlot(slotId: String) {
        _uiState.update { it.copy(selectedSlotId = slotId) }
    }

    fun setMode(mode: String) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun setNote(note: String) {
        _uiState.update { it.copy(note = note, noteError = null) }
    }

    fun backToConcern() {
        _uiState.update {
            it.copy(
                step = BookingStep.CONCERN,
                matches = emptyList(),
                selectedMatch = null,
                selectedSlotId = null,
                loadError = null,
            )
        }
    }

    fun backToMatches() {
        _uiState.update {
            it.copy(
                step = BookingStep.MATCHES,
                selectedMatch = null,
                selectedSlotId = null,
            )
        }
    }

    fun book() {
        val state = _uiState.value
        val slotId = state.selectedSlotId
        val concern = state.concern
        val match = state.selectedMatch

        if (slotId == null || concern == null || match == null) {
            _messages.trySend(UiMessage("Please choose a time slot.", MessageKind.ERROR))
            return
        }
        if (state.note.length > 1000) {
            _uiState.update { it.copy(noteError = "Your note must be 1,000 characters or fewer.") }
            return
        }
        if (state.isBooking) return

        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true) }
            val result = repository.bookAppointment(
                slotId = slotId,
                mode = state.mode,
                concernCategory = concern,
                note = state.note.takeIf { it.isNotBlank() },
            )
            _uiState.update { it.copy(isBooking = false) }
            result.onSuccess {
                _uiState.update { it.copy(step = BookingStep.SUCCESS) }
                _messages.send(
                    UiMessage(
                        "Appointment requested with ${match.counsellorName}.",
                        MessageKind.SUCCESS,
                    ),
                )
            }.onFailure { error ->
                _messages.send(
                    UiMessage(
                        error.message ?: "Could not book the appointment.",
                        MessageKind.ERROR,
                    ),
                )
            }
        }
    }

    private fun loadMatches() {
        val concern = _uiState.value.concern ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMatches = true, loadError = null) }
            repository.getSmartMatches(concern)
                .onSuccess { matches ->
                    _uiState.update {
                        it.copy(
                            isLoadingMatches = false,
                            matches = matches,
                            loadError = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingMatches = false,
                            loadError = error.message ?: "Could not load counsellors.",
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(
            repository: AppointmentRepository,
            sessionManager: SessionManager,
        ) = viewModelFactory {
            initializer { BookAppointmentViewModel(repository, sessionManager) }
        }
    }
}
