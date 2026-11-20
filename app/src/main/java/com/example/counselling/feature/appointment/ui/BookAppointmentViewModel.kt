// Module 2 | Owner: Beh
// Booking + Unique Feature 1 (Smart Counsellor Match).
package com.example.counselling.feature.appointment.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.AppConstants
import com.example.counselling.core.SessionManager
import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.SessionMode
import com.example.counselling.core.domain.Student
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.appointment.domain.AppointmentRepository
import com.example.counselling.feature.appointment.domain.CounsellorMatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class BookAppointmentViewModel(
    private val repository: AppointmentRepository,
    sessionManager: SessionManager,
    initialConcern: String?,
) : BaseViewModel() {

    private val preferredMode = (sessionManager.currentUser as? Student)
        ?.preferredMode
        ?.code
        ?: SessionMode.IN_PERSON.code

    private val _uiState = MutableStateFlow(BookAppointmentUiState(mode = preferredMode))
    val uiState: StateFlow<BookAppointmentUiState> = _uiState.asStateFlow()

    init {
        if (initialConcern != null && initialConcern in AppConstants.concernCategories) {
            _uiState.update {
                it.copy(concern = initialConcern, step = BookingStep.MATCHES)
            }
            loadMatches()
        }
    }

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
            postError("Please choose a time slot.")
            return
        }
        val concernCategory = ConcernCategory.fromWire(concern)
        val mode = SessionMode.fromWire(state.mode)
        if (concernCategory == null || mode == null) {
            postError("Please choose a valid concern and session mode.")
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
                mode = mode,
                concern = concernCategory,
                note = state.note.takeIf { it.isNotBlank() },
            )
            _uiState.update { it.copy(isBooking = false) }
            result.onSuccess {
                _uiState.update { it.copy(step = BookingStep.SUCCESS) }
                postSuccess("Appointment requested with ${match.counsellorName}.")
            }.onFailure { error ->
                postError(error.message ?: "Could not book the appointment.")
            }
        }
    }

    private fun loadMatches() {
        val concern = _uiState.value.concern ?: return
        val category = ConcernCategory.fromWire(concern) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMatches = true, loadError = null) }
            repository.getSmartMatches(category)
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
            initialConcern: String?,
        ) = viewModelFactory {
            initializer {
                BookAppointmentViewModel(repository, sessionManager, initialConcern)
            }
        }
    }
}
