// Module 2 (counsellor side) | Owner: Beh
// Today-and-later sessions as date-grouped rows. Confirming, declining and
// emergency changes live in the row sheet; destructive actions still confirm.
package com.example.counselling.feature.appointment.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.counselling.core.domain.AppointmentStatus
import com.example.counselling.core.domain.User
import com.example.counselling.feature.appointment.domain.Appointment
import com.example.counselling.feature.appointment.domain.AppointmentRepository
import com.example.counselling.feature.enquiry.domain.FeedbackRepository
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.ConfirmDialog
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.FilterChipRow
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.ScreenTitle
import com.example.counselling.ui.components.SecondaryButton
import com.example.counselling.ui.components.StickyListHeader
import com.example.counselling.ui.components.SwipeAction
import com.example.counselling.ui.components.SwipeRevealRow
import com.example.counselling.ui.components.showAppMessage

@Composable
fun CounsellorScheduleRoute(
    repository: AppointmentRepository,
    feedbackRepository: FeedbackRepository,
    viewer: User,
    onManageAvailability: () -> Unit,
    onReschedule: (String) -> Unit,
) {
    val viewModel: AppointmentsViewModel = viewModel(
        factory = AppointmentsViewModel.factory(repository, feedbackRepository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }
    LaunchedEffect(Unit) {
        viewModel.load()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.load()
    }

    CounsellorScheduleScreen(
        state = state,
        viewer = viewer,
        snackbarHostState = snackbarState,
        onManageAvailability = onManageAvailability,
        onConfirm = { id -> viewModel.updateStatus(id, AppointmentStatus.Confirmed) },
        onDecline = viewModel::declineBooking,
        onComplete = { id -> viewModel.updateStatus(id, AppointmentStatus.Completed) },
        onNoShow = { id -> viewModel.updateStatus(id, AppointmentStatus.NoShow) },
        onCancelSession = viewModel::cancelConfirmedSession,
        onReschedule = onReschedule,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CounsellorScheduleScreen(
    state: AppointmentsUiState,
    viewer: User,
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
    var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }
    var pendingNoShow by remember { mutableStateOf<Appointment?>(null) }
    var pendingDecline by remember { mutableStateOf<Appointment?>(null) }
    var pendingCancel by remember { mutableStateOf<Appointment?>(null) }

    // Recompute from the state on every recomposition. Appointment equality is
    // id-only, so remember(list) would hide a status-only refresh.
    val appointments = run {
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
                if (it.status in setOf(AppointmentStatus.Pending, AppointmentStatus.Confirmed)) 0 else 1
            }.thenBy { it.slotStart },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenTitle(
                title = "Your sessions",
                subtitle = "Confirm requests, decline what you cannot take, or manage an emergency change.",
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh schedule")
                    }
                },
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
            ) {
                SecondaryButton(
                    text = "Manage my availability",
                    onClick = onManageAvailability,
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
            }

            FilterChipRow(
                options = timeFilters,
                selected = selectedTimeFilter,
                onSelect = { selectedTimeFilter = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            FilterChipRow(
                options = filters,
                selected = selectedFilter,
                onSelect = { selectedFilter = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
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
                            contentPadding = PaddingValues(bottom = 24.dp),
                        ) {
                            appointments
                                .groupBy { DateTimeUtils.formatDate(it.slotStart) }
                                .forEach { (date, rows) ->
                                    stickyHeader(key = "header-$date") {
                                        StickyListHeader(text = date)
                                    }
                                    items(
                                        rows,
                                        key = { appointment ->
                                            val feedback = state.feedback[appointment.id]
                                            "${appointment.uiKey()}:${feedback?.rating}:${feedback?.comment}"
                                        },
                                    ) { appointment ->
                                        val canConfirm = appointment.status ==
                                            AppointmentStatus.Pending &&
                                            (
                                                !appointment.isFollowUp ||
                                                    appointment.lastEvent == "rescheduled_by_student"
                                                )
                                        val canReschedule = appointment.status ==
                                            AppointmentStatus.Confirmed
                                        val swipeWidth = when {
                                            canConfirm && appointment.status ==
                                                AppointmentStatus.Pending -> 184.dp
                                            canReschedule -> 184.dp
                                            appointment.status == AppointmentStatus.Pending -> 96.dp
                                            else -> 0.dp
                                        }

                                        if (swipeWidth > 0.dp) {
                                            SwipeRevealRow(
                                                revealWidth = swipeWidth,
                                                actions = { close ->
                                                    when (appointment.status) {
                                                        AppointmentStatus.Pending -> {
                                                            if (canConfirm) {
                                                                SwipeAction(
                                                                    label = "Confirm",
                                                                    icon = Icons.Outlined.Check,
                                                                    onClick = {
                                                                        close()
                                                                        onConfirm(appointment.id)
                                                                    },
                                                                )
                                                            }
                                                            SwipeAction(
                                                                label = "Decline",
                                                                icon = Icons.Outlined.Close,
                                                                destructive = true,
                                                                onClick = {
                                                                    close()
                                                                    pendingDecline = appointment
                                                                },
                                                            )
                                                        }
                                                        else -> {
                                                            SwipeAction(
                                                                label = "Reschedule",
                                                                icon = Icons.Outlined.CalendarMonth,
                                                                onClick = {
                                                                    close()
                                                                    onReschedule(appointment.id)
                                                                },
                                                            )
                                                            SwipeAction(
                                                                label = "Cancel",
                                                                icon = Icons.Outlined.Close,
                                                                destructive = true,
                                                                onClick = {
                                                                    close()
                                                                    pendingCancel = appointment
                                                                },
                                                            )
                                                        }
                                                    }
                                                },
                                            ) {
                                                AppointmentRow(
                                                    appointment = appointment,
                                                    viewer = viewer,
                                                    onClick = { selectedAppointment = appointment },
                                                )
                                            }
                                        } else {
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
        }

        AppSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }

    selectedAppointment?.let { appointment ->
        val canConfirm = appointment.status == AppointmentStatus.Pending &&
            (
                !appointment.isFollowUp ||
                    appointment.lastEvent == "rescheduled_by_student"
                )
        val isConfirmed = appointment.status == AppointmentStatus.Confirmed
        val feedback = state.feedback[appointment.id]
        AppointmentDetailSheet(
            appointment = appointment,
            viewer = viewer,
            onDismiss = { selectedAppointment = null },
            isBusy = state.isMutating,
            onConfirm = if (canConfirm) {
                {
                    selectedAppointment = null
                    onConfirm(appointment.id)
                }
            } else {
                null
            },
            onDecline = if (appointment.status == AppointmentStatus.Pending) {
                {
                    selectedAppointment = null
                    pendingDecline = appointment
                }
            } else {
                null
            },
            onReschedule = if (isConfirmed) {
                {
                    selectedAppointment = null
                    onReschedule(appointment.id)
                }
            } else {
                null
            },
            onComplete = if (isConfirmed) {
                {
                    selectedAppointment = null
                    onComplete(appointment.id)
                }
            } else {
                null
            },
            onNoShow = if (isConfirmed) {
                {
                    selectedAppointment = null
                    pendingNoShow = appointment
                }
            } else {
                null
            },
            onCancel = if (isConfirmed) {
                {
                    selectedAppointment = null
                    pendingCancel = appointment
                }
            } else {
                null
            },
            cancelLabel = "Cancel session",
            feedbackRating = feedback?.rating,
            feedbackComment = feedback?.comment,
            notice = appointment.noticeFor(viewer),
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

private fun String.toApiStatus(): AppointmentStatus? = when (this) {
    "Pending" -> AppointmentStatus.Pending
    "Confirmed" -> AppointmentStatus.Confirmed
    "Completed" -> AppointmentStatus.Completed
    "No show" -> AppointmentStatus.NoShow
    "Cancelled" -> AppointmentStatus.Cancelled
    else -> null
}
