// Module 2 (counsellor side) | Owner: Beh
// The Module 3 availability manager will extend this screen in the next phase.
package com.example.counselling.feature.appointment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.core.UserRole
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.appointment.model.Appointment
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.ConfirmDialog
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.FilterChipRow
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.SecondaryButton
import com.example.counselling.ui.components.showAppMessage

@Composable
fun CounsellorScheduleRoute(
    repository: AppointmentRepository,
    onManageAvailability: () -> Unit,
    onReschedule: (String) -> Unit,
) {
    val viewModel: AppointmentsViewModel = viewModel(
        factory = AppointmentsViewModel.factory(repository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.load()
    }

    CounsellorScheduleScreen(
        state = state,
        snackbarHostState = snackbarState,
        onManageAvailability = onManageAvailability,
        onConfirm = { id -> viewModel.updateStatus(id, "confirmed") },
        onDecline = viewModel::declineBooking,
        onComplete = { id -> viewModel.updateStatus(id, "completed") },
        onNoShow = { id -> viewModel.updateStatus(id, "no_show") },
        onCancelSession = viewModel::cancelConfirmedSession,
        onReschedule = onReschedule,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CounsellorScheduleScreen(
    state: AppointmentsUiState,
    snackbarHostState: SnackbarHostState,
    onManageAvailability: () -> Unit,
    onConfirm: (String) -> Unit,
    onDecline: (String) -> Unit,
    onComplete: (String) -> Unit,
    onNoShow: (String) -> Unit,
    onCancelSession: (String) -> Unit,
    onReschedule: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    val filters = listOf("All", "Pending", "Confirmed", "Completed", "No show", "Cancelled")
    val timeFilters = listOf("Upcoming", "This week", "All")
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedTimeFilter by remember { mutableStateOf("Upcoming") }
    var pendingNoShow by remember { mutableStateOf<Appointment?>(null) }
    var pendingDecline by remember { mutableStateOf<Appointment?>(null) }
    var pendingCancel by remember { mutableStateOf<Appointment?>(null) }

    val appointments = remember(state.appointments, selectedFilter, selectedTimeFilter) {
        val byTime = when (selectedTimeFilter) {
            "Upcoming" -> state.appointments.filter { DateTimeUtils.isFuture(it.slotStart) }
            "This week" -> state.appointments.filter {
                DateTimeUtils.isWithinNextDays(it.slotStart, 7)
            }
            else -> state.appointments
        }
        val filtered = selectedFilter.toApiStatus()?.let { status ->
            byTime.filter { it.status == status }
        } ?: byTime
        filtered.sortedWith(
            compareBy<Appointment> {
                if (it.status in listOf("pending", "confirmed")) 0 else 1
            }.thenBy { it.slotStart },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = "My schedule")

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = "Your sessions",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Confirm requests, decline what you cannot take, or manage an emergency change.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                SecondaryButton(
                    text = "Manage my availability",
                    onClick = onManageAvailability,
                )
                Spacer(Modifier.height(10.dp))
            }

            FilterChipRow(
                options = timeFilters,
                selected = selectedTimeFilter,
                onSelect = { selectedTimeFilter = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            FilterChipRow(
                options = filters,
                selected = selectedFilter,
                onSelect = { selectedFilter = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )

            when {
                state.isLoading -> LoadingShimmer(
                    modifier = Modifier.padding(20.dp),
                    rows = 4,
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
                                icon = Icons.Outlined.CalendarMonth,
                                title = if (selectedFilter == "All") {
                                    "No sessions in your schedule"
                                } else {
                                    "No $selectedFilter sessions"
                                },
                                message = "Booked student appointments will appear here. Manage your free slots from this screen.",
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
                                    viewerRole = UserRole.COUNSELLOR,
                                    isBusy = state.isMutating,
                                    onConfirm = if (appointment.status == "pending") {
                                        { onConfirm(appointment.id) }
                                    } else {
                                        null
                                    },
                                    onDecline = if (appointment.status == "pending") {
                                        { pendingDecline = appointment }
                                    } else {
                                        null
                                    },
                                    onReschedule = if (appointment.status == "confirmed") {
                                        { onReschedule(appointment.id) }
                                    } else {
                                        null
                                    },
                                    onCancel = if (appointment.status == "confirmed") {
                                        { pendingCancel = appointment }
                                    } else {
                                        null
                                    },
                                    cancelLabel = "Cancel session",
                                    onComplete = if (appointment.status == "confirmed") {
                                        { onComplete(appointment.id) }
                                    } else {
                                        null
                                    },
                                    onNoShow = if (appointment.status == "confirmed") {
                                        { pendingNoShow = appointment }
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        AppSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }

    pendingDecline?.let { appointment ->
        ConfirmDialog(
            title = "Decline this booking?",
            message = "The pending request from ${appointment.studentName} will be cancelled and the slot released. The student will see it in Cancelled.",
            confirmLabel = "Decline booking",
            onConfirm = { onDecline(appointment.id) },
            onDismiss = { pendingDecline = null },
        )
    }

    pendingCancel?.let { appointment ->
        ConfirmDialog(
            title = "Cancel this confirmed session?",
            message = "Use this for an emergency. ${appointment.studentName} will see the session as Cancelled and the slot will be released for someone else.",
            confirmLabel = "Cancel session",
            onConfirm = { onCancelSession(appointment.id) },
            onDismiss = { pendingCancel = null },
        )
    }

    pendingNoShow?.let { appointment ->
        ConfirmDialog(
            title = "Mark as no-show?",
            message = "This records that ${appointment.studentName} did not attend. It will appear in the student's past sessions.",
            confirmLabel = "Mark no-show",
            onConfirm = { onNoShow(appointment.id) },
            onDismiss = { pendingNoShow = null },
        )
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
