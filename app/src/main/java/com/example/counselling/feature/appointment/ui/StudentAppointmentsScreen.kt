// Module 2 | Owner: Beh
// Student tabs: Upcoming / Past / Cancelled, with status chips and safe actions.
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.showAppMessage

private enum class StudentAppointmentTab(val label: String) {
    UPCOMING("Upcoming"),
    PAST("Past"),
    CANCELLED("Cancelled"),
}

@Composable
fun StudentAppointmentsRoute(
    repository: AppointmentRepository,
    onBook: () -> Unit,
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

    // Returning from booking/reschedule should show the new state immediately.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.load()
    }

    StudentAppointmentsScreen(
        state = state,
        snackbarHostState = snackbarState,
        onBook = onBook,
        onReschedule = onReschedule,
        onCancel = viewModel::cancelAppointment,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentAppointmentsScreen(
    state: AppointmentsUiState,
    snackbarHostState: SnackbarHostState,
    onBook: () -> Unit,
    onReschedule: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedStatusLabel by rememberSaveable { mutableStateOf("All") }
    var appointmentToCancel by remember { mutableStateOf<Appointment?>(null) }

    val tabs = StudentAppointmentTab.entries
    val selectedTab = tabs[selectedTabIndex]

    val chipOptions = when (selectedTab) {
        StudentAppointmentTab.UPCOMING -> listOf("All", "Pending", "Confirmed")
        StudentAppointmentTab.PAST -> listOf("All", "Completed", "No show")
        StudentAppointmentTab.CANCELLED -> listOf("All")
    }

    LaunchedEffect(selectedTabIndex) {
        selectedStatusLabel = "All"
    }

    val appointments = remember(state.appointments, selectedTab, selectedStatusLabel) {
        val tabRows = when (selectedTab) {
            StudentAppointmentTab.UPCOMING -> state.appointments.filter {
                it.status in listOf("pending", "confirmed")
            }.sortedBy { it.slotStart }

            StudentAppointmentTab.PAST -> state.appointments.filter {
                it.status in listOf("completed", "no_show")
            }.sortedByDescending { it.slotStart }

            StudentAppointmentTab.CANCELLED -> state.appointments.filter {
                it.status == "cancelled"
            }.sortedByDescending { it.slotStart }
        }
        val status = selectedStatusLabel.toApiStatus()
        if (status == null) tabRows else tabRows.filter { it.status == status }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = "Appointments")

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = "Your counselling sessions",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Private, calm and at your pace.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                PrimaryButton(
                    text = "Book a session",
                    onClick = onBook,
                    icon = Icons.Outlined.CalendarMonth,
                )
                Spacer(Modifier.height(14.dp))
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(tab.label) },
                    )
                }
            }

            FilterChipRow(
                options = chipOptions,
                selected = selectedStatusLabel,
                onSelect = { selectedStatusLabel = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
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
                                title = when (selectedTab) {
                                    StudentAppointmentTab.UPCOMING -> "No upcoming sessions"
                                    StudentAppointmentTab.PAST -> "No past sessions"
                                    StudentAppointmentTab.CANCELLED -> "No cancelled sessions"
                                },
                                message = when (selectedTab) {
                                    StudentAppointmentTab.UPCOMING ->
                                        "When you book a counselling session it will appear here."
                                    StudentAppointmentTab.PAST ->
                                        "Completed sessions will collect here for your reference."
                                    StudentAppointmentTab.CANCELLED ->
                                        "Appointments you cancel will remain listed here."
                                },
                                actionLabel = if (selectedTab == StudentAppointmentTab.UPCOMING) {
                                    "Book a session"
                                } else {
                                    null
                                },
                                onAction = if (selectedTab == StudentAppointmentTab.UPCOMING) {
                                    onBook
                                } else {
                                    null
                                },
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
                                val canChange = appointment.status in listOf("pending", "confirmed")
                                AppointmentCard(
                                    appointment = appointment,
                                    viewerRole = UserRole.STUDENT,
                                    isBusy = state.isMutating,
                                    onReschedule = if (
                                        canChange &&
                                        DateTimeUtils.canReschedule(appointment.slotStart)
                                    ) {
                                        { onReschedule(appointment.id) }
                                    } else {
                                        null
                                    },
                                    onCancel = if (canChange) {
                                        { appointmentToCancel = appointment }
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

    appointmentToCancel?.let { appointment ->
        ConfirmDialog(
            title = "Cancel appointment?",
            message = "The slot will be released and the counsellor will see the cancellation. This cannot be undone.",
            confirmLabel = "Cancel appointment",
            onConfirm = { onCancel(appointment.id) },
            onDismiss = { appointmentToCancel = null },
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
