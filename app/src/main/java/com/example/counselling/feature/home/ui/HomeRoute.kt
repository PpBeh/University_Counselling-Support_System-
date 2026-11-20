// Home | Owner: shared (Beh, Jeremy, Michael)
package com.example.counselling.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.domain.User
import com.example.counselling.feature.appointment.domain.AppointmentRepository
import com.example.counselling.feature.records.domain.SessionRecordRepository

@Composable
fun HomeRoute(
    user: User,
    appointmentRepository: AppointmentRepository,
    recordRepository: SessionRecordRepository,
    onNavigate: (String) -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(user, appointmentRepository, recordRepository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    HomeScreen(
        user = user,
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
