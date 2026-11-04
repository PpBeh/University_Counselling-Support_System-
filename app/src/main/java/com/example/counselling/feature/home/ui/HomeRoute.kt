// Home | Owner: shared (Beh, Jeremy, Michael)
package com.example.counselling.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.UserRole
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.records.data.SessionRecordRepository
import com.example.counselling.feature.student.model.Profile

@Composable
fun HomeRoute(
    profile: Profile,
    appointmentRepository: AppointmentRepository,
    recordRepository: SessionRecordRepository,
    onNavigate: (String) -> Unit,
) {
    val role = profile.userRole ?: UserRole.STUDENT
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(role, appointmentRepository, recordRepository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    HomeScreen(
        profile = profile,
        onNavigate = onNavigate,
        appointments = state.appointments,
        isLoadingAppointments = state.isLoading,
        appointmentError = state.error,
        onRetryAppointments = viewModel::refresh,
        adminStats = state.adminStats,
        isLoadingStats = state.isLoadingStats,
        statsError = state.statsError,
        onRetryStats = viewModel::retryStats,
    )
}
