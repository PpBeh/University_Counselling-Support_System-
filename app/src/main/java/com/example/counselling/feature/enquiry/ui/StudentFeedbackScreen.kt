// Module 6 | Owner: Michael
package com.example.counselling.feature.enquiry.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.appointment.model.Appointment
import com.example.counselling.feature.enquiry.data.FeedbackRepository
import com.example.counselling.feature.enquiry.model.FeedbackRow
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.ConfirmDialog
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.StarRating
import com.example.counselling.ui.components.showAppMessage

@Composable
fun StudentFeedbackRoute(
    feedbackRepository: FeedbackRepository,
    appointmentRepository: AppointmentRepository,
    onBack: () -> Unit,
) {
    val viewModel: StudentFeedbackViewModel = viewModel(
        factory = StudentFeedbackViewModel.factory(
            feedbackRepository,
            appointmentRepository,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    StudentFeedbackScreen(
        state = state,
        snackbarHostState = snackbarState,
        onBack = onBack,
        onSubmit = viewModel::submitRating,
        onDelete = viewModel::deleteRating,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentFeedbackScreen(
    state: StudentFeedbackUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSubmit: (String, Int, String) -> Unit,
    onDelete: (FeedbackRow) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    var ratingTarget by remember { mutableStateOf<Appointment?>(null) }
    var deleteTarget by remember { mutableStateOf<FeedbackRow?>(null) }
    val feedbackByAppointment = remember(state.feedback) {
        state.feedback.associateBy { it.appointmentId }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = "Session feedback", onBack = onBack)
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Help the service improve",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Rate a completed session. You can remove your rating and rate again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                )
            }
            when {
                state.isLoading -> LoadingShimmer(
                    modifier = Modifier.padding(20.dp),
                    rows = 4,
                )

                state.error != null && state.completedAppointments.isEmpty() -> ErrorState(
                    message = state.error,
                    onRetry = onRetry,
                )

                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.completedAppointments.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            EmptyState(
                                icon = Icons.Outlined.StarBorder,
                                title = "No completed sessions",
                                message = "After a session is marked completed you can rate it here.",
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
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(state.completedAppointments, key = { it.id }) { appointment ->
                                val feedback = feedbackByAppointment[appointment.id]
                                CompletedSessionCard(
                                    appointment = appointment,
                                    feedback = feedback,
                                    isBusy = feedback != null && state.busyFeedbackId == feedback.id,
                                    onRate = {
                                        ratingTarget = appointment
                                    },
                                    onDelete = { feedback?.let { deleteTarget = it } },
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

    ratingTarget?.let { appointment ->
        RatingDialog(
            appointment = appointment,
            onDismiss = { ratingTarget = null },
            onSubmit = { rating, comment ->
                onSubmit(appointment.id, rating, comment)
                ratingTarget = null
            },
        )
    }

    deleteTarget?.let { feedback ->
        ConfirmDialog(
            title = "Remove this rating?",
            message = "Your feedback will be deleted. You can rate the session again afterwards.",
            confirmLabel = "Remove rating",
            onConfirm = { onDelete(feedback) },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun CompletedSessionCard(
    appointment: Appointment,
    feedback: FeedbackRow?,
    isBusy: Boolean,
    onRate: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = appointment.concernCategory,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "with ${appointment.counsellorName} · " +
                    DateTimeUtils.formatDate(appointment.slotStart),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (feedback == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "How was this session?",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onRate, enabled = !isBusy) {
                        Text("Rate this session")
                    }
                }
            } else {
                Spacer(Modifier.height(6.dp))
                StarRating(rating = feedback.rating, starSize = 24.dp)
                if (!feedback.comment.isNullOrBlank()) {
                    Text(
                        text = feedback.comment,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDelete, enabled = !isBusy) {
                        Text("Remove feedback", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingDialog(
    appointment: Appointment,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit,
) {
    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate your session") },
        text = {
            Column {
                Text(
                    text = "${appointment.counsellorName} · " +
                        DateTimeUtils.formatDate(appointment.slotStart),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                StarRating(
                    rating = rating,
                    onRatingChange = {
                        rating = it
                        error = null
                    },
                    starSize = 36.dp,
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = comment,
                    onValueChange = {
                        comment = it
                        error = null
                    },
                    label = "Comment (optional)",
                    singleLine = false,
                    maxLines = 4,
                    supportingText = "${comment.length}/500",
                    error = error,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    error = when {
                        rating !in 1..5 -> "Choose a rating from 1 to 5 stars."
                        comment.length > 500 -> "Comment must be 500 characters or fewer."
                        else -> null
                    }
                    if (error == null) onSubmit(rating, comment)
                },
            ) {
                Text("Submit feedback")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = MaterialTheme.shapes.large,
    )
}
