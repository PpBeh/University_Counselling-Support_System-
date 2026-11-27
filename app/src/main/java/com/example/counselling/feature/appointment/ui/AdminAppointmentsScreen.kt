// Module 2 | Owner: Beh
// Admin read-only oversight. Rows open a detail sheet; there are deliberately
// no write actions here.
package com.example.counselling.feature.appointment.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.core.domain.AppointmentStatus
import com.example.counselling.core.domain.User
import com.example.counselling.feature.appointment.domain.Appointment
import com.example.counselling.feature.appointment.domain.AppointmentRepository
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.FilterChipRow
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.StickyListHeader

@Composable
fun AdminAppointmentsRoute(
    repository: AppointmentRepository,
    viewer: User,
    onBack: () -> Unit,
) {
    val viewModel: AppointmentsViewModel = viewModel(
        factory = AppointmentsViewModel.factory(repository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    AdminAppointmentsScreen(
        state = state,
        viewer = viewer,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AdminAppointmentsScreen(
    state: AppointmentsUiState,
    viewer: User,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    val filters = listOf("All", "Pending", "Confirmed", "Completed", "No show", "Cancelled")
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }

    val appointments = run {
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
                text = "Service overview",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Read-only. Administrators cannot create, edit or cancel bookings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }

        FilterChipRow(
            options = filters,
            selected = selectedFilter,
            onSelect = { selectedFilter = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
                            icon = Icons.AutoMirrored.Outlined.EventNote,
                            title = "No appointments",
                            message = "Appointments across the service will appear here.",
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        appointments
                            .groupBy { DateTimeUtils.formatDate(it.slotStart) }
                            .forEach { (date, rows) ->
                                stickyHeader(key = "header-$date") {
                                    StickyListHeader(text = date)
                                }
                                items(rows, key = { it.uiKey() }) { appointment ->
                                    AppointmentRow(
                                        appointment = appointment,
                                        viewer = viewer,
                                        onClick = { selectedAppointment = appointment },
                                    )
                                }
                            }
                    }
                }
            }
        }
    }

    selectedAppointment?.let { appointment ->
        AppointmentDetailSheet(
            appointment = appointment,
            viewer = viewer,
            onDismiss = { selectedAppointment = null },
        )
    }
}

private fun String.toApiStatus(): AppointmentStatus? = when (this) {
    "Pending" -> AppointmentStatus.Pending
    "Confirmed" -> AppointmentStatus.Confirmed
    "Completed" -> AppointmentStatus.Completed
    "No show" -> AppointmentStatus.NoShow
    "Cancelled" -> AppointmentStatus.Cancelled
    else -> null
}
