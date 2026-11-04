// Module 6 | Owner: Michael
package com.example.counselling.feature.enquiry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.MessageKind
import com.example.counselling.core.UiMessage
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.appointment.model.Appointment
import com.example.counselling.feature.enquiry.data.FeedbackRepository
import com.example.counselling.feature.enquiry.model.FeedbackRow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentFeedbackUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val completedAppointments: List<Appointment> = emptyList(),
    val feedback: List<FeedbackRow> = emptyList(),
    val isSubmitting: Boolean = false,
    val busyFeedbackId: String? = null,
)

class StudentFeedbackViewModel(
    private val feedbackRepository: FeedbackRepository,
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentFeedbackUiState())
    val uiState: StateFlow<StudentFeedbackUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        load()
    }

    fun refresh() = load(refresh = true)
    fun retry() = load()

    fun submitRating(appointmentId: String, rating: Int, comment: String) {
        if (_uiState.value.isSubmitting) return
        if (rating !in 1..5) {
            _messages.trySend(UiMessage("Choose a rating from 1 to 5 stars.", MessageKind.ERROR))
            return
        }
        if (comment.length > 500) {
            _messages.trySend(UiMessage("Comment must be 500 characters or fewer.", MessageKind.ERROR))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            feedbackRepository.submitFeedback(appointmentId, rating, comment)
                .onSuccess {
                    _messages.send(UiMessage("Thank you for your feedback.", MessageKind.SUCCESS))
                    load()
                }
                .onFailure {
                    _messages.send(
                        UiMessage(it.message ?: "Could not submit feedback.", MessageKind.ERROR),
                    )
                }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun deleteRating(feedback: FeedbackRow) {
        if (_uiState.value.busyFeedbackId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(busyFeedbackId = feedback.id) }
            feedbackRepository.deleteFeedback(feedback.id)
                .onSuccess {
                    _messages.send(UiMessage("Feedback removed.", MessageKind.SUCCESS))
                    load()
                }
                .onFailure {
                    _messages.send(
                        UiMessage(it.message ?: "Could not remove feedback.", MessageKind.ERROR),
                    )
                }
            _uiState.update { it.copy(busyFeedbackId = null) }
        }
    }

    private fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.completedAppointments.isEmpty(),
                    isRefreshing = refresh,
                    error = null,
                )
            }
            val appointments = appointmentRepository.getAppointments()
            val feedback = feedbackRepository.getFeedbackWithAppointments()
            appointments.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message ?: "Could not load finished sessions.",
                    )
                }
                return@launch
            }
            feedback.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message ?: "Could not load feedback.",
                    )
                }
                return@launch
            }
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    completedAppointments = appointments.getOrThrow()
                        .filter { it.status == "completed" }
                        .sortedByDescending { it.slotStart },
                    feedback = feedback.getOrThrow().map { it.feedback },
                )
            }
        }
    }

    companion object {
        fun factory(
            feedbackRepository: FeedbackRepository,
            appointmentRepository: AppointmentRepository,
        ) = viewModelFactory {
            initializer {
                StudentFeedbackViewModel(feedbackRepository, appointmentRepository)
            }
        }
    }
}
