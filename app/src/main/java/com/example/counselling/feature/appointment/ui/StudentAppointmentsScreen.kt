// Module 2 | Owner: Beh
// Student tabs: Upcoming / Past / Cancelled. Rows are grouped by date, the
// primary booking action is a FAB and every row opens an action sheet.
package com.example.counselling.feature.appointment.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import com.example.counselling.core.domain.AppointmentStatus
import com.example.counselling.core.domain.User
import com.example.counselling.feature.appointment.domain.Appointment
import com.example.counselling.feature.appointment.domain.AppointmentRepository
import com.example.counselling.feature.enquiry.domain.FeedbackRepository
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.ConfirmDialog
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.FilterChipRow
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.ScreenTitle
import com.example.counselling.ui.components.StarRating
import com.example.counselling.ui.components.StickyListHeader
import com.example.counselling.ui.components.SwipeAction
import com.example.counselling.ui.components.SwipeRevealRow
import com.example.counselling.ui.components.showAppMessage

private enum class StudentAppointmentTab(val label: String) {
    UPCOMING("Upcoming"),
    PAST("Past"),
    CANCELLED("Cancelled"),
}

@Composable
fun StudentAppointmentsRoute(
    repository: AppointmentRepository,
    feedbackRepository: FeedbackRepository,
    viewer: User,
    onBook: () -> Unit,
    onReschedule: (String) -> Unit,
    onOpenSummaries: () -> Unit,
) {
    val viewModel: AppointmentsViewModel = viewModel(
        factory = AppointmentsViewModel.factory(repository, feedbackRepository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }

    // Returning from booking/reschedule should show the new state immediately,
    // even if the previous destination was kept in the back stack.
    LaunchedEffect(Unit) {
        viewModel.load()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.load()
    }

    StudentAppointmentsScreen(
        state = state,
        viewer = viewer,
        snackbarHostState = snackbarState,
        onBook = onBook,
        onReschedule = onReschedule,
        onOpenSummaries = onOpenSummaries,
        onConfirmFollowUp = viewModel::confirmFollowUp,
        onSubmitFeedback = viewModel::submitFeedback,
        onDeleteFeedback = viewModel::deleteFeedback,
        onCancel = viewModel::cancelAppointment,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun StudentAppointmentsScreen(
    state: AppointmentsUiState,
    viewer: User,
    snackbarHostState: SnackbarHostState,
    onBook: () -> Unit,
    onReschedule: (String) -> Unit,
    onOpenSummaries: () -> Unit,
    onConfirmFollowUp: (String) -> Unit,
    onSubmitFeedback: (String, Int, String) -> Unit,
    onDeleteFeedback: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedStatusLabel by rememberSaveable { mutableStateOf("All") }
    var appointmentToCancel by remember { mutableStateOf<Appointment?>(null) }
    var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }
    var feedbackTarget by remember { mutableStateOf<Appointment?>(null) }
    var feedbackToDelete by remember { mutableStateOf<AppointmentFeedbackUi?>(null) }
    val listState = rememberLazyListState()
    val showExtendedFab by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 24
        }
    }

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

    // Do not remember this by domain-list equality: Appointment equality is
    // identity by id, so a status-only refresh would keep the old list.
    val appointments = run {
        val tabRows = when (selectedTab) {
            StudentAppointmentTab.UPCOMING -> state.appointments.filter {
                it.status in setOf(AppointmentStatus.Pending, AppointmentStatus.Confirmed)
            }.sortedBy { it.slotStart }

            StudentAppointmentTab.PAST -> state.appointments.filter {
                it.status in setOf(AppointmentStatus.Completed, AppointmentStatus.NoShow)
            }.sortedByDescending { it.slotStart }

            StudentAppointmentTab.CANCELLED -> state.appointments.filter {
                it.status == AppointmentStatus.Cancelled
            }.sortedByDescending { it.slotStart }
        }
        val status = selectedStatusLabel.toApiStatus()
        if (status == null) tabRows else tabRows.filter { it.status == status }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenTitle(
                title = "Your sessions",
                subtitle = "Private, calm and at your pace.",
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh appointments")
                    }
                },
            )

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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )

            if (selectedTab == StudentAppointmentTab.PAST) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onOpenSummaries) {
                        Text("Session summaries")
                    }
                }
            }

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
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 96.dp),
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
                                        val canChange = appointment.status in setOf(
                                            AppointmentStatus.Pending,
                                            AppointmentStatus.Confirmed,
                                        )
                                        val canReschedule = canChange &&
                                            DateTimeUtils.canReschedule(appointment.slotStart)
                                        val existingFeedback = state.feedback[appointment.id]

                                        if (canChange) {
                                            SwipeRevealRow(
                                                revealWidth = if (canReschedule) 184.dp else 96.dp,
                                                actions = { close ->
                                                    if (canReschedule) {
                                                        SwipeAction(
                                                            label = "Reschedule",
                                                            icon = Icons.Outlined.CalendarMonth,
                                                            onClick = {
                                                                close()
                                                                onReschedule(appointment.id)
                                                            },
                                                        )
                                                    }
                                                    SwipeAction(
                                                        label = "Cancel",
                                                        icon = Icons.Outlined.Close,
                                                        destructive = true,
                                                        onClick = {
                                                            close()
                                                            appointmentToCancel = appointment
                                                        },
                                                    )
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

        AnimatedContent(
            targetState = showExtendedFab,
            label = "book-fab",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
        ) { extended ->
            if (extended) {
                ExtendedFloatingActionButton(
                    onClick = onBook,
                    icon = {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    },
                    text = { Text("Book session") },
                )
            } else {
                FloatingActionButton(onClick = onBook) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = "Book session")
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
        val canChange = appointment.status in setOf(
            AppointmentStatus.Pending,
            AppointmentStatus.Confirmed,
        )
        val existingFeedback = state.feedback[appointment.id]
        AppointmentDetailSheet(
            appointment = appointment,
            viewer = viewer,
            onDismiss = { selectedAppointment = null },
            isBusy = state.isMutating ||
                state.isSubmittingFeedback ||
                state.deletingFeedbackId != null,
            onReschedule = if (
                canChange && DateTimeUtils.canReschedule(appointment.slotStart)
            ) {
                {
                    selectedAppointment = null
                    onReschedule(appointment.id)
                }
            } else {
                null
            },
            onCancel = if (canChange) {
                {
                    selectedAppointment = null
                    appointmentToCancel = appointment
                }
            } else {
                null
            },
            cancelLabel = "Cancel appointment",
            onConfirm = if (appointment.canConfirmFollowUp(viewer)) {
                {
                    selectedAppointment = null
                    onConfirmFollowUp(appointment.id)
                }
            } else {
                null
            },
            onFeedback = if (appointment.status == AppointmentStatus.Completed) {
                {
                    selectedAppointment = null
                    feedbackTarget = appointment
                }
            } else {
                null
            },
            feedbackRating = existingFeedback?.rating,
            feedbackComment = existingFeedback?.comment,
            onRemoveFeedback = existingFeedback?.let { feedback ->
                {
                    selectedAppointment = null
                    feedbackToDelete = feedback
                }
            },
            notice = appointment.noticeFor(viewer),
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

    feedbackTarget?.let { appointment ->
        SessionRatingDialog(
            appointment = appointment,
            existing = state.feedback[appointment.id],
            isSubmitting = state.isSubmittingFeedback,
            onDismiss = { feedbackTarget = null },
            onSubmit = { rating, comment ->
                onSubmitFeedback(appointment.id, rating, comment)
                feedbackTarget = null
            },
        )
    }

    feedbackToDelete?.let { feedback ->
        ConfirmDialog(
            title = "Remove your rating?",
            message = "You can rate this session again afterwards.",
            confirmLabel = "Remove rating",
            onConfirm = { onDeleteFeedback(feedback.id) },
            onDismiss = { feedbackToDelete = null },
        )
    }
}

@Composable
private fun SessionRatingDialog(
    appointment: Appointment,
    existing: AppointmentFeedbackUi?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit,
) {
    var rating by remember(appointment.id, existing?.rating) {
        mutableStateOf(existing?.rating ?: 0)
    }
    var comment by remember(appointment.id, existing?.comment) {
        mutableStateOf(existing?.comment.orEmpty())
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "Rate your session" else "Update your rating")
        },
        text = {
            Column {
                Text(
                    text = "${appointment.counsellorName} · " +
                        DateTimeUtils.formatDate(appointment.slotStart),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "How was this session?",
                    style = MaterialTheme.typography.bodyMedium,
                )
                StarRating(
                    rating = rating,
                    onRatingChange = {
                        rating = it
                        error = null
                    },
                    starSize = 36.dp,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                AppTextField(
                    value = comment,
                    onValueChange = {
                        comment = it
                        error = null
                    },
                    label = "Comment (optional)",
                    singleLine = false,
                    maxLines = 3,
                    error = error,
                    supportingText = "Maximum 500 characters.",
                    enabled = !isSubmitting,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        rating !in 1..5 -> error = "Choose a rating from 1 to 5 stars."
                        comment.length > 500 -> error = "Comment must be 500 characters or fewer."
                        else -> onSubmit(rating, comment)
                    }
                },
                enabled = !isSubmitting,
            ) {
                Text(if (existing == null) "Submit" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        },
    )
}

private fun String.toApiStatus(): AppointmentStatus? = when (this) {
    "Pending" -> AppointmentStatus.Pending
    "Confirmed" -> AppointmentStatus.Confirmed
    "Completed" -> AppointmentStatus.Completed
    "No show" -> AppointmentStatus.NoShow
    "Cancelled" -> AppointmentStatus.Cancelled
    else -> null
}
