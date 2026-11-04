// Home | Owner: shared (Beh, Jeremy, Michael)
// Appointment snapshot for every role and aggregate-only stats for admins.
package com.example.counselling.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.UserRole
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.appointment.model.Appointment
import com.example.counselling.feature.records.data.SessionRecordRepository
import com.example.counselling.feature.records.model.AdminDashboardStats
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

class HomeViewModel(
    private val role: UserRole,
    private val appointmentRepository: AppointmentRepository,
    private val recordRepository: SessionRecordRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(isLoadingStats = role == UserRole.ADMIN),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        loadAppointments()
        if (role == UserRole.ADMIN) loadAdminStats()
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
            role: UserRole,
            appointmentRepository: AppointmentRepository,
            recordRepository: SessionRecordRepository,
        ) = viewModelFactory {
            initializer {
                HomeViewModel(role, appointmentRepository, recordRepository)
            }
        }
    }
}
