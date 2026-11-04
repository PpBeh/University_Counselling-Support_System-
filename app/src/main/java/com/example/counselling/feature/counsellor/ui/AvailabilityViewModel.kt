// Module 3 | Owner: Jeremy
// Shared availability manager for a counsellor (own slots) and an admin
// (slots for the selected counsellor).
package com.example.counselling.feature.counsellor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.core.MessageKind
import com.example.counselling.core.UiMessage
import com.example.counselling.feature.counsellor.data.CounsellorRepository
import com.example.counselling.feature.counsellor.model.AvailabilitySlot
import com.example.counselling.feature.counsellor.model.buildWeeklySlots
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId

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

class AvailabilityViewModel(
    private val requestedCounsellorId: String?,
    private val currentUserId: String,
    private val repository: CounsellorRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AvailabilityUiState())
    val uiState: StateFlow<AvailabilityUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

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
        if (!slot.isEditable) return
        val parsed = DateTimeUtils.parse(slot.startAt) ?: return
        val start = parsed.toLocalTime()
        val end = DateTimeUtils.parse(slot.endAt)?.toLocalTime() ?: start.plusHours(1)
        _uiState.update {
            it.copy(
                showEditor = true,
                editingSlot = slot,
                selectedDateMillis = DateTimeUtils.datePickerMillis(parsed.toLocalDate()),
                startHour = start.hour,
                startMinute = start.minute,
                endHour = end.hour,
                endMinute = end.minute,
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
        val startAt = date?.let {
            DateTimeUtils.toApiTimestamp(it, state.startHour, state.startMinute)
        }
        val endAt = date?.let {
            DateTimeUtils.toApiTimestamp(it, state.endHour, state.endMinute)
        }
        val baseTimeError = when {
            startAt == null || endAt == null -> "Choose the start and end times."
            !endLocal.isAfter(startLocal) -> "The end time must be after the start time."
            DateTimeUtils.isPast(startAt) -> "Availability cannot be created in the past."
            else -> null
        }
        val startInstant = startAt?.let { DateTimeUtils.parse(it)?.toInstant() }
        val endInstant = endAt?.let { DateTimeUtils.parse(it)?.toInstant() }
        val overlaps = if (startInstant == null || endInstant == null) {
            false
        } else {
            state.slots.any { existing ->
                if (existing.id == state.editingSlot?.id) return@any false
                val existingStart = DateTimeUtils.parse(existing.startAt)?.toInstant()
                val existingEnd = DateTimeUtils.parse(existing.endAt)?.toInstant()
                existingStart != null && existingEnd != null &&
                    startInstant < existingEnd && endInstant > existingStart
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
        if (state.isSaving || startAt == null || endAt == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = if (state.editingSlot != null) {
                repository.updateSlot(state.editingSlot.id, startAt, endAt)
            } else {
                val zone = ZoneId.of("Asia/Kuala_Lumpur")
                val dateValue = DateTimeUtils.localDate(startAt) ?: return@launch
                val baseStart = dateValue.atTime(state.startHour, state.startMinute)
                    .atZone(zone).toInstant()
                val baseEnd = dateValue.atTime(state.endHour, state.endMinute)
                    .atZone(zone).toInstant()
                val slots = buildWeeklySlots(baseStart, baseEnd, state.repeatWeeks)
                    .map { (start, end) -> start.toString() to end.toString() }
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
                _messages.send(
                    UiMessage(
                        if (state.editingSlot != null) "Availability updated." else "Availability added.",
                        MessageKind.SUCCESS,
                    ),
                )
                load(refresh = true)
            }.onFailure {
                _messages.send(
                    UiMessage(
                        it.message ?: "Could not save availability.",
                        MessageKind.ERROR,
                    ),
                )
            }
        }
    }

    fun deleteSlot(slot: AvailabilitySlot) {
        if (!slot.isEditable || _uiState.value.deletingSlotId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(deletingSlotId = slot.id) }
            repository.deleteSlot(slot.id)
                .onSuccess {
                    _messages.send(UiMessage("Availability slot deleted.", MessageKind.SUCCESS))
                    load(refresh = true)
                }
                .onFailure {
                    _messages.send(
                        UiMessage(it.message ?: "Could not delete the slot.", MessageKind.ERROR),
                    )
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
                repository.getMyCounsellor(currentUserId)
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
            val detailResult = repository.getCounsellor(id)
            detailResult.onSuccess { counsellor ->
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
                            slots = counsellor.slots,
                        )
                    }
                }
            }.onFailure { error ->
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
            currentUserId: String,
            repository: CounsellorRepository,
        ) = viewModelFactory {
            initializer {
                AvailabilityViewModel(counsellorId, currentUserId, repository)
            }
        }
    }
}
