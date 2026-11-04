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
import androidx.compose.material.icons.outlined.Forum
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.feature.enquiry.data.EnquiryRepository
import com.example.counselling.feature.enquiry.model.Enquiry
import com.example.counselling.feature.resource.data.ResourceRepository
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.DropdownField
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.FilterChipRow
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.SecondaryButton
import com.example.counselling.ui.components.StatusChip
import com.example.counselling.ui.components.showAppMessage

@Composable
fun CounsellorInboxRoute(
    enquiryRepository: EnquiryRepository,
    resourceRepository: ResourceRepository,
    onOpenFeedback: () -> Unit,
) {
    val viewModel: CounsellorInboxViewModel = viewModel(
        factory = CounsellorInboxViewModel.factory(enquiryRepository, resourceRepository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }

    CounsellorInboxScreen(
        state = state,
        snackbarHostState = snackbarState,
        onStatusFilterChange = viewModel::onStatusFilterChange,
        onReply = viewModel::reply,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onOpenFeedback = onOpenFeedback,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CounsellorInboxScreen(
    state: CounsellorInboxUiState,
    snackbarHostState: SnackbarHostState,
    onStatusFilterChange: (String) -> Unit,
    onReply: (Enquiry, String, String, String?) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onOpenFeedback: () -> Unit,
) {
    var replyTarget by remember { mutableStateOf<Enquiry?>(null) }
    val filtered = remember(state.enquiries, state.statusFilter) {
        if (state.statusFilter == "All") {
            state.enquiries
        } else {
            state.enquiries.filter { it.status == state.statusFilter.lowercase() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = "Counselling inbox")
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Student questions",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Anonymous enquiries never expose an identity.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                )
                SecondaryButton(
                    text = "View session feedback",
                    onClick = onOpenFeedback,
                )
                Spacer(Modifier.height(8.dp))
            }
            FilterChipRow(
                options = listOf("All", "Open", "Answered", "Closed"),
                selected = state.statusFilter,
                onSelect = onStatusFilterChange,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )

            when {
                state.isLoading -> LoadingShimmer(
                    modifier = Modifier.padding(20.dp),
                    rows = 4,
                )

                state.error != null && state.enquiries.isEmpty() -> ErrorState(
                    message = state.error,
                    onRetry = onRetry,
                )

                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (filtered.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            EmptyState(
                                icon = Icons.Outlined.Forum,
                                title = "No enquiries here",
                                message = "New student questions will appear in this inbox.",
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
                            items(filtered, key = { it.id }) { enquiry ->
                                InboxCard(
                                    enquiry = enquiry,
                                    isBusy = state.busyEnquiryId == enquiry.id,
                                    onReply = { replyTarget = enquiry },
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

    replyTarget?.let { enquiry ->
        ReplyDialog(
            enquiry = enquiry,
            resources = state.publishedResources.map { it.id to it.title },
            onDismiss = { replyTarget = null },
            onSave = { reply, status, resourceId ->
                onReply(enquiry, reply, status, resourceId)
                replyTarget = null
            },
        )
    }
}

@Composable
private fun InboxCard(
    enquiry: Enquiry,
    isBusy: Boolean,
    onReply: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (enquiry.isAnonymous) "Anonymous student" else {
                        enquiry.studentName ?: "Student"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(enquiry.status)
            }
            Text(
                text = "${enquiry.referenceCode} · ${enquiry.category} · " +
                    DateTimeUtils.formatDate(enquiry.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                text = enquiry.message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
            if (!enquiry.reply.isNullOrBlank()) {
                Text(
                    text = "Your reply",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    text = enquiry.reply,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            enquiry.recommendedResourceTitle?.let {
                Text(
                    text = "Attached: $it",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onReply, enabled = !isBusy) {
                    Text(if (enquiry.reply.isNullOrBlank()) "Reply" else "Update reply")
                }
            }
        }
    }
}

@Composable
private fun ReplyDialog(
    enquiry: Enquiry,
    resources: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit,
) {
    var reply by remember(enquiry.id) { mutableStateOf(enquiry.reply.orEmpty()) }
    var status by remember(enquiry.id) {
        mutableStateOf(if (enquiry.status == "open") "answered" else enquiry.status)
    }
    var resourceLabel by remember(enquiry.id) {
        mutableStateOf(
            resources.firstOrNull { it.first == enquiry.recommendedResourceId }?.second
                ?: "No attached resource",
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (enquiry.isAnonymous) "Anonymous enquiry" else "Student enquiry") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = enquiry.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = reply,
                    onValueChange = {
                        reply = it
                        error = null
                    },
                    label = "Reply",
                    singleLine = false,
                    maxLines = 6,
                    supportingText = "${reply.length}/2000",
                    error = error,
                )
                Spacer(Modifier.height(10.dp))
                DropdownField(
                    label = "Status",
                    options = listOf("Open", "Answered", "Closed"),
                    selected = status.replaceFirstChar { it.uppercase() },
                    onSelect = { status = it.lowercase() },
                )
                Spacer(Modifier.height(10.dp))
                DropdownField(
                    label = "Attach a published resource (optional)",
                    options = listOf("No attached resource") + resources.map { it.second },
                    selected = resourceLabel,
                    onSelect = { resourceLabel = it },
                )
                if (enquiry.isAnonymous) {
                    Spacer(Modifier.height(10.dp))
                    InfoCard(
                        title = "Anonymous",
                        body = "You cannot identify this student. Reply through the app; they can check it with their reference code.",
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    error = when {
                        status == "answered" && reply.isBlank() ->
                            "Write a reply before marking this as answered."
                        reply.length > 2000 -> "Reply must be 2,000 characters or fewer."
                        else -> null
                    }
                    if (error == null) {
                        val resourceId = resources
                            .firstOrNull { it.second == resourceLabel }
                            ?.first
                        onSave(reply, status, resourceId)
                    }
                },
            ) {
                Text("Save reply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = MaterialTheme.shapes.large,
    )
}
