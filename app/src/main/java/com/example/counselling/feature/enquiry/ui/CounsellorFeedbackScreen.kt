// Module 6 | Owner: Michael
package com.example.counselling.feature.enquiry.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.feature.enquiry.data.FeedbackRepository
import com.example.counselling.feature.enquiry.model.FeedbackWithAppointment
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.StarRating

@Composable
fun CounsellorFeedbackRoute(
    repository: FeedbackRepository,
    onBack: () -> Unit,
) {
    val viewModel: CounsellorFeedbackViewModel = viewModel(
        factory = CounsellorFeedbackViewModel.factory(repository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CounsellorFeedbackScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CounsellorFeedbackScreen(
    state: CounsellorFeedbackUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Session feedback", onBack = onBack)
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "What students shared",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Only feedback for your own sessions. Admin sees aggregate counts only.",
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

            state.error != null && state.feedback.isEmpty() -> ErrorState(
                message = state.error,
                onRetry = onRetry,
            )

            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.feedback.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        EmptyState(
                            icon = Icons.Outlined.StarBorder,
                            title = "No feedback yet",
                            message = "Students can rate a completed session. Their ratings will appear here.",
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
                        items(state.feedback, key = { it.feedback.id }) { item ->
                            CounsellorFeedbackCard(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CounsellorFeedbackCard(item: FeedbackWithAppointment) {
    val appointment = item.appointment
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = appointment?.studentName ?: "Student",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${appointment?.concernCategory ?: "Completed session"} · " +
                    DateTimeUtils.formatDate(appointment?.slotStart),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StarRating(rating = item.feedback.rating, starSize = 22.dp)
            if (!item.feedback.comment.isNullOrBlank()) {
                Text(
                    text = item.feedback.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
