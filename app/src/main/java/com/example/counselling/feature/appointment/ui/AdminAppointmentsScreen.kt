// Module 2 | Owner: Beh
// Admin read-only oversight. There are deliberately no write actions here.
package com.example.counselling.feature.appointment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.UserRole
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.appointment.model.Appointment
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.FilterChipRow
import com.example.counselling.ui.components.LoadingShimmer

@Composable
fun AdminAppointmentsRoute(
    repository: AppointmentRepository,
    onBack: () -> Unit,
) {
    val viewModel: AppointmentsViewModel = viewModel(
        factory = AppointmentsViewModel.factory(repository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AdminAppointmentsScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminAppointmentsScreen(
    state: AppointmentsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    val filters = listOf("All", "Pending", "Confirmed", "Completed", "No show", "Cancelled")
    var selectedFilter by remember { mutableStateOf("All") }

    val appointments = remember(state.appointments, selectedFilter) {
        val filtered = selectedFilter.toApiStatus()?.let { status ->
            state.appointments.filter { it.status == status }
        } ?: state.appointments
        filtered.sortedByDescending { it.slotStart }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "All appointments", onBack = onBack)

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = "Service-wide oversight",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Read-only. Administrators cannot create, edit or cancel bookings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
        }

        FilterChipRow(
            options = filters,
            selected = selectedFilter,
            onSelect = { selectedFilter = it },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        when {
            state.isLoading -> LoadingShimmer(
                modifier = Modifier.padding(20.dp),
                rows = 5,
            )

            state.error != null && state.appointments.isEmpty() -> ErrorState(
                message = state.error,
                onRetry = onRetry,
            )

            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (appointments.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        EmptyState(
                            icon = Icons.Outlined.EventNote,
                            title = "No appointments",
                            message = "Appointments across the service will appear here.",
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(appointments, key = { it.id }) { appointment ->
                            AppointmentCard(
                                appointment = appointment,
                                viewerRole = UserRole.ADMIN,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun String.toApiStatus(): String? = when (this) {
    "Pending" -> "pending"
    "Confirmed" -> "confirmed"
    "Completed" -> "completed"
    "No show" -> "no_show"
    "Cancelled" -> "cancelled"
    else -> null
}
