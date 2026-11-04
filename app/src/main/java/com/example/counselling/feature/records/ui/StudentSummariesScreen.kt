// Module 4 | Owner: Jeremy
// Read-only, privacy-safe session summaries for the signed-in student.
package com.example.counselling.feature.records.ui

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
import androidx.compose.material.icons.outlined.Description
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
import com.example.counselling.feature.records.data.SessionRecordRepository
import com.example.counselling.feature.records.model.StudentSessionSummaryRow
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.LoadingShimmer

@Composable
fun StudentSummariesRoute(
    repository: SessionRecordRepository,
    onBack: () -> Unit,
) {
    val viewModel: StudentSummariesViewModel = viewModel(
        factory = StudentSummariesViewModel.factory(repository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    StudentSummariesScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentSummariesScreen(
    state: StudentSummariesUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Session summaries", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "Your session summaries",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "These are the public summaries your counsellor chose to share. Private notes and risk levels are never shown.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
        }

        when {
            state.isLoading -> LoadingShimmer(
                modifier = Modifier.padding(20.dp),
                rows = 4,
            )

            state.error != null && state.summaries.isEmpty() -> ErrorState(
                message = state.error,
                onRetry = onRetry,
            )

            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.summaries.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        EmptyState(
                            icon = Icons.Outlined.Description,
                            title = "No session summaries yet",
                            message = "After a completed session, your counsellor's public summary will appear here.",
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
                        items(state.summaries, key = { it.recordId }) { summary ->
                            SummaryCard(summary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: StudentSessionSummaryRow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = summary.concernCategory,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${DateTimeUtils.formatDate(summary.sessionStart)} · " +
                    summary.counsellorName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = summary.publicSummary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
