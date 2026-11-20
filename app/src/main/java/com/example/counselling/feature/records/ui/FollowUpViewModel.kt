// Module 4 / Unique Feature 2 | Owner: Jeremy
package com.example.counselling.feature.records.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.appointment.domain.AvailableSlot
import com.example.counselling.feature.records.domain.SessionRecord
import com.example.counselling.feature.records.domain.SessionRecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FollowUpUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val record: SessionRecord? = null,
    val slots: List<AvailableSlot> = emptyList(),
    val selectedSlotId: String? = null,
    val isSubmitting: Boolean = false,
    val success: Boolean = false,
)

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class FollowUpViewModel(
    private val recordId: String,
    private val repository: SessionRecordRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(FollowUpUiState())
    val uiState: StateFlow<FollowUpUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun chooseSlot(slotId: String) {
        _uiState.update { it.copy(selectedSlotId = slotId) }
    }

    fun submit() {
        val state = _uiState.value
        val slotId = state.selectedSlotId ?: return
        if (state.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            repository.scheduleFollowUp(recordId, slotId)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false, success = true) }
                    postSuccess("Follow-up appointment created for the student.")
                }
                .onFailure {
                    _uiState.update { it.copy(isSubmitting = false) }
                    postError(it.message ?: "Could not schedule the follow-up.")
                }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = FollowUpUiState(isLoading = true)
            val records = repository.getRecords()
            val record = records.getOrNull()?.firstOrNull { it.id == recordId }
            if (record == null) {
                _uiState.value = FollowUpUiState(
                    isLoading = false,
                    error = records.exceptionOrNull()?.message
                        ?: "That session record could not be found.",
                )
                return@launch
            }
            if (record.isArchived) {
                _uiState.value = FollowUpUiState(
                    isLoading = false,
                    record = record,
                    error = "Archived session records cannot be used to schedule follow-ups.",
                )
                return@launch
            }
            repository.getFreeSlotsForCounsellor(record.counsellorId)
                .onSuccess { slots ->
                    _uiState.value = FollowUpUiState(
                        isLoading = false,
                        record = record,
                        slots = slots,
                    )
                }
                .onFailure {
                    _uiState.value = FollowUpUiState(
                        isLoading = false,
                        record = record,
                        error = it.message ?: "Could not load free slots.",
                    )
                }
        }
    }

    companion object {
        fun factory(recordId: String, repository: SessionRecordRepository) =
            viewModelFactory {
                initializer { FollowUpViewModel(recordId, repository) }
            }
    }
}
