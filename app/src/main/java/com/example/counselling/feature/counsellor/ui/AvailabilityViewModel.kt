// Module 3 | Owner: Jeremy
// Shared availability manager for a counsellor (own slots) and an admin
// (slots for the selected counsellor).
package com.example.counselling.feature.counsellor.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.core.domain.AvailabilitySlot
import com.example.counselling.core.domain.TimeSlot
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.counsellor.domain.CounsellorRepository
import com.example.counselling.feature.counsellor.domain.WeeklyAvailabilityPattern
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AvailabilityUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val loadError: String? = null,
    val counsellorId: String? = null,
    val counsellorName: String = "",
    val slots: List<AvailabilitySlot> = emptyList(),
    val showEditor: Boolean = false,
    val editingSlot: AvailabilitySlot? = null,
    val selectedDateMillis: Long? = null,
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 10,
    val endMinute: Int = 0,
    val repeatWeeks: Int = 1,
    val dateError: String? = null,
    val timeError: String? = null,
    val repeatError: String? = null,
    val isSaving: Boolean = false,
    val deletingSlotId: String? = null,
)

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class AvailabilityViewModel(
    private val requestedCounsellorId: String?,
    private val currentAuthUserId: String,
    private val repository: CounsellorRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AvailabilityUiState())
    val uiState: StateFlow<AvailabilityUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()
    fun refresh() = load(refresh = true)

    fun openAddForm() {
        val tomorrow = DateTimeUtils.nowInKualaLumpur().plusDays(1)
        _uiState.update {
            it.copy(
                showEditor = true,
                editingSlot = null,
                selectedDateMillis = DateTimeUtils.datePickerMillis(tomorrow.toLocalDate()),
                startHour = 9,
                startMinute = 0,
                endHour = 10,
                endMinute = 0,
                repeatWeeks = 1,
                dateError = null,
                timeError = null,
                repeatError = null,
            )
        }
    }

    fun openEditForm(slot: AvailabilitySlot) {
        if (!slot.isEditable()) return
        val start = slot.timeSlot.start
        val end = slot.timeSlot.end
        val zone = ZoneId.of("Asia/Kuala_Lumpur")
        val startLocal = start.atZone(zone).toLocalTime()
        val endLocal = end.atZone(zone).toLocalTime()
        _uiState.update {
            it.copy(
                showEditor = true,
                editingSlot = slot,
                selectedDateMillis = DateTimeUtils.datePickerMillis(start.atZone(zone).toLocalDate()),
                startHour = startLocal.hour,
                startMinute = startLocal.minute,
                endHour = endLocal.hour,
                endMinute = endLocal.minute,
                repeatWeeks = 1,
                dateError = null,
                timeError = null,
                repeatError = null,
            )
        }
    }

    fun closeForm() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(showEditor = false, editingSlot = null) }
    }

    fun setDate(dateMillis: Long) {
        _uiState.update { it.copy(selectedDateMillis = dateMillis, dateError = null) }
    }

    fun setStartTime(hour: Int, minute: Int) {
        _uiState.update { it.copy(startHour = hour, startMinute = minute, timeError = null) }
    }

    fun setEndTime(hour: Int, minute: Int) {
        _uiState.update { it.copy(endHour = hour, endMinute = minute, timeError = null) }
    }

    fun setRepeat(weeks: Int) {
        _uiState.update { it.copy(repeatWeeks = weeks, repeatError = null) }
    }

    fun save() {
        val state = _uiState.value
        val counsellorId = state.counsellorId
        val date = state.selectedDateMillis
        val dateError = if (date == null) "Choose a date." else null
        val startLocal = LocalTime.of(state.startHour, state.startMinute)
        val endLocal = LocalTime.of(state.endHour, state.endMinute)
        val startInstant = date?.let {
            Instant.parse(DateTimeUtils.toApiTimestamp(it, state.startHour, state.startMinute))
        }
        val endInstant = date?.let {
            Instant.parse(DateTimeUtils.toApiTimestamp(it, state.endHour, state.endMinute))
        }

        val baseTimeError = when {
            startInstant == null || endInstant == null -> "Choose the start and end times."
            !endLocal.isAfter(startLocal) -> "The end time must be after the start time."
            !startInstant.isAfter(Instant.now()) -> "Availability cannot be created in the past."
            else -> null
        }
        val overlaps = if (startInstant == null || endInstant == null) {
            false
        } else {
            state.slots.any { existing ->
                if (existing.id == state.editingSlot?.id) return@any false
                startInstant < existing.timeSlot.end && endInstant > existing.timeSlot.start
            }
        }
        val timeError = baseTimeError ?: if (overlaps) {
            "This overlaps an existing slot for this counsellor."
        } else {
            null
        }
        val repeatError = if (state.editingSlot == null && state.repeatWeeks !in 1..8) {
            "Choose between 1 and 8 weeks."
        } else {
            null
        }
        _uiState.update {
            it.copy(dateError = dateError, timeError = timeError, repeatError = repeatError)
        }
        if (counsellorId == null || dateError != null || timeError != null || repeatError != null) {
            return
        }
        if (state.isSaving || startInstant == null || endInstant == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val baseSlot = try {
                TimeSlot.of(startInstant, endInstant)
            } catch (error: Exception) {
                postError(error.message ?: "Choose a valid time.")
                _uiState.update { it.copy(isSaving = false) }
                return@launch
            }
            val result = if (state.editingSlot != null) {
                repository.updateSlot(state.editingSlot.id, baseSlot)
            } else {
                val slots = WeeklyAvailabilityPattern(baseSlot, state.repeatWeeks).generate()
                repository.createSlots(counsellorId, slots)
            }
            _uiState.update {
                it.copy(
                    isSaving = false,
                    showEditor = if (result.isSuccess) false else it.showEditor,
                    editingSlot = if (result.isSuccess) null else it.editingSlot,
                )
            }
            result.onSuccess {
                postSuccess(
                    if (state.editingSlot != null) "Availability updated." else "Availability added.",
                )
                load(refresh = true)
            }.onFailure {
                postError(it.message ?: "Could not save availability.")
            }
        }
    }

    fun deleteSlot(slot: AvailabilitySlot) {
        if (!slot.isEditable() || _uiState.value.deletingSlotId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(deletingSlotId = slot.id) }
            repository.deleteSlot(slot.id)
                .onSuccess {
                    postSuccess("Availability slot deleted.")
                    load(refresh = true)
                }
                .onFailure {
                    postError(it.message ?: "Could not delete the slot.")
                }
            _uiState.update { it.copy(deletingSlotId = null) }
        }
    }

    private fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.slots.isEmpty(),
                    isRefreshing = refresh,
                    loadError = null,
                )
            }
            if (requestedCounsellorId == null && _uiState.value.counsellorId == null) {
                repository.getMyCounsellor(currentAuthUserId)
                    .onSuccess { counsellor ->
                        if (counsellor == null) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    loadError = "Your counsellor directory entry could not be found.",
                                )
                            }
                            return@launch
                        }
                        _uiState.update {
                            it.copy(
                                counsellorId = counsellor.id,
                                counsellorName = counsellor.fullName,
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                isRefreshing = false,
                                loadError = error.message
                                    ?: "Could not load your counsellor profile.",
                            )
                        }
                        return@launch
                    }
            }

            val id = _uiState.value.counsellorId ?: requestedCounsellorId ?: return@launch
            repository.getCounsellor(id)
                .onSuccess { counsellor ->
                    if (counsellor == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                loadError = "That counsellor could not be found.",
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                loadError = null,
                                counsellorId = counsellor.id,
                                counsellorName = counsellor.fullName,
                                slots = counsellor.availability,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            loadError = error.message ?: "Could not load availability.",
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(
            counsellorId: String?,
            currentAuthUserId: String,
            repository: CounsellorRepository,
        ) = viewModelFactory {
            initializer {
                AvailabilityViewModel(counsellorId, currentAuthUserId, repository)
            }
        }
    }
}
