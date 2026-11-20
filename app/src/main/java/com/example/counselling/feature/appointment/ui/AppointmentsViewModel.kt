// Module 2 | Owner: Beh
// Shared list logic for students, counsellors and admins. RLS decides which
// appointment rows each role actually receives.
package com.example.counselling.feature.appointment.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.domain.AppointmentStatus
import com.example.counselling.core.domain.Rating
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.appointment.domain.Appointment
import com.example.counselling.feature.appointment.domain.AppointmentRepository
import com.example.counselling.feature.enquiry.domain.FeedbackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppointmentFeedbackUi(
    val id: String,
    val rating: Int,
    val comment: String?,
)

data class AppointmentsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isMutating: Boolean = false,
    val busyAppointmentId: String? = null,
    val appointments: List<Appointment> = emptyList(),
    val feedback: Map<String, AppointmentFeedbackUi> = emptyMap(),
    val isSubmittingFeedback: Boolean = false,
    val deletingFeedbackId: String? = null,
    val error: String? = null,
)

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class AppointmentsViewModel(
    private val repository: AppointmentRepository,
    private val feedbackRepository: FeedbackRepository? = null,
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
                    feedbackRepository?.let { loadFeedback(it) }
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

    fun confirmFollowUp(appointmentId: String) {
        if (_uiState.value.isMutating) return
        performMutation(appointmentId, "Follow-up confirmed.") {
            repository.confirmFollowUp(appointmentId)
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

    fun submitFeedback(appointmentId: String, rating: Int, comment: String) {
        val repository = feedbackRepository ?: return
        if (_uiState.value.isSubmittingFeedback) return
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
            _uiState.update { it.copy(isSubmittingFeedback = true) }
            repository.submitFeedback(appointmentId, ratingValue, comment)
                .onSuccess {
                    postSuccess("Thank you for your feedback.")
                    loadFeedback(repository)
                }
                .onFailure {
                    postError(it.message ?: "Could not submit feedback.")
                }
            _uiState.update { it.copy(isSubmittingFeedback = false) }
        }
    }

    fun deleteFeedback(feedbackId: String) {
        val repository = feedbackRepository ?: return
        if (_uiState.value.deletingFeedbackId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(deletingFeedbackId = feedbackId) }
            repository.deleteFeedback(feedbackId)
                .onSuccess {
                    postSuccess("Feedback removed.")
                    loadFeedback(repository)
                }
                .onFailure {
                    postError(it.message ?: "Could not remove feedback.")
                }
            _uiState.update { it.copy(deletingFeedbackId = null) }
        }
    }

    private suspend fun loadFeedback(repository: FeedbackRepository) {
        val feedback = repository.getFeedback().getOrNull().orEmpty()
        _uiState.update {
            it.copy(
                feedback = feedback.associate { item ->
                    item.appointmentId to AppointmentFeedbackUi(
                        id = item.id,
                        rating = item.rating.value,
                        comment = item.comment,
                    )
                },
            )
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
        fun factory(
            repository: AppointmentRepository,
            feedbackRepository: FeedbackRepository? = null,
        ) = viewModelFactory {
            initializer { AppointmentsViewModel(repository, feedbackRepository) }
        }
    }
}
