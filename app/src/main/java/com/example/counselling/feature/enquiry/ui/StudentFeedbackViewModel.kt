// Module 6 | Owner: Michael
package com.example.counselling.feature.enquiry.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.domain.AppointmentStatus
import com.example.counselling.core.domain.Rating
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.appointment.domain.Appointment
import com.example.counselling.feature.appointment.domain.AppointmentRepository
import com.example.counselling.feature.enquiry.domain.Feedback
import com.example.counselling.feature.enquiry.domain.FeedbackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentFeedbackUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val completedAppointments: List<Appointment> = emptyList(),
    val feedback: List<Feedback> = emptyList(),
    val isSubmitting: Boolean = false,
    val busyFeedbackId: String? = null,
)

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class StudentFeedbackViewModel(
    private val feedbackRepository: FeedbackRepository,
    private val appointmentRepository: AppointmentRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(StudentFeedbackUiState())
    val uiState: StateFlow<StudentFeedbackUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() = load(refresh = true)
    fun retry() = load()

    fun submitRating(appointmentId: String, rating: Int, comment: String) {
        if (_uiState.value.isSubmitting) return
        val ratingValue = try {
            Rating.of(rating)
        } catch (_: Exception) {
            postError("Choose a rating from 1 to 5 stars.")
            return
        }
        if (comment.length > 500) {
            postError("Comment must be 500 characters or fewer.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            feedbackRepository.submitFeedback(appointmentId, ratingValue, comment)
                .onSuccess {
                    postSuccess("Thank you for your feedback.")
                    load()
                }
                .onFailure {
                    postError(it.message ?: "Could not submit feedback.")
                }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun deleteRating(feedback: Feedback) {
        if (_uiState.value.busyFeedbackId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(busyFeedbackId = feedback.id) }
            feedbackRepository.deleteFeedback(feedback.id)
                .onSuccess {
                    postSuccess("Feedback removed.")
                    load()
                }
                .onFailure {
                    postError(it.message ?: "Could not remove feedback.")
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
            val feedback = feedbackRepository.getFeedback()
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
                        .filter { it.status == AppointmentStatus.Completed }
                        .sortedByDescending { it.slot.start },
                    feedback = feedback.getOrThrow(),
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
