// Home | Owner: shared (Beh, Jeremy, Michael)
// Appointment snapshot for every signed-in User and aggregate-only stats for
// administrators, selected through permissions rather than a role string.
package com.example.counselling.feature.home.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.domain.User
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.appointment.domain.Appointment
import com.example.counselling.feature.appointment.domain.AppointmentRepository
import com.example.counselling.feature.records.domain.SessionRecordRepository
import com.example.counselling.feature.records.domain.AdminDashboardStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val appointments: List<Appointment> = emptyList(),
    val adminStats: AdminDashboardStats? = null,
    val isLoadingStats: Boolean = false,
    val statsError: String? = null,
)

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class HomeViewModel(
    private val user: User,
    private val appointmentRepository: AppointmentRepository,
    private val recordRepository: SessionRecordRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(isLoadingStats = user.permissions.canViewAllAppointments()),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        loadAppointments()
        if (user.permissions.canViewAllAppointments()) loadAdminStats()
    }

    fun retryStats() = loadAdminStats()

    private fun loadAppointments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.appointments.isEmpty(), error = null) }
            appointmentRepository.getAppointments()
                .onSuccess { appointments ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            appointments = appointments,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Could not load appointments.",
                        )
                    }
                }
        }
    }

    private fun loadAdminStats() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoadingStats = it.adminStats == null, statsError = null)
            }
            recordRepository.getAdminStats()
                .onSuccess { stats ->
                    _uiState.update {
                        it.copy(
                            isLoadingStats = false,
                            statsError = null,
                            adminStats = stats,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingStats = false,
                            statsError = error.message ?: "Could not load statistics.",
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(
            user: User,
            appointmentRepository: AppointmentRepository,
            recordRepository: SessionRecordRepository,
        ) = viewModelFactory {
            initializer {
                HomeViewModel(user, appointmentRepository, recordRepository)
            }
        }
    }
}
