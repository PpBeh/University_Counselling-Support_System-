// Module 6 | Owner: Michael
// Counsellor enquiry inbox as status-grouped rows. Swipe reveals Reply and the
// row sheet keeps the same action visible without a gesture.
package com.example.counselling.feature.enquiry.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.example.counselling.feature.enquiry.domain.Enquiry
import com.example.counselling.feature.enquiry.domain.EnquiryRepository
import com.example.counselling.feature.enquiry.domain.EnquiryStatus
import com.example.counselling.feature.resource.domain.ResourceRepository
import com.example.counselling.ui.components.AppBottomSheet
import com.example.counselling.ui.components.AppListRow
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.DropdownField
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.FilterChipRow
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.SecondaryButton
import com.example.counselling.ui.components.SegmentedChoiceField
import com.example.counselling.ui.components.StatusChip
import com.example.counselling.ui.components.StickyBottomAction
import com.example.counselling.ui.components.StickyListHeader
import com.example.counselling.ui.components.SwipeAction
import com.example.counselling.ui.components.SwipeRevealRow
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var selectedEnquiry by remember { mutableStateOf<Enquiry?>(null) }
    val filtered = remember(state.enquiries, state.statusFilter) {
        if (state.statusFilter == "All") {
            state.enquiries
        } else {
            state.enquiries.filter { it.status.code == state.statusFilter.lowercase() }
        }
    }

    BackHandler(enabled = replyTarget != null) {
        replyTarget = null
    }

    replyTarget?.let { enquiry ->
        ReplyFormScreen(
            enquiry = enquiry,
            resources = state.publishedResources.map { it.id to it.title },
            snackbarHostState = snackbarHostState,
            onClose = { replyTarget = null },
            onSave = { reply, status, resourceId ->
                onReply(enquiry, reply, status, resourceId)
                replyTarget = null
            },
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
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
                            contentPadding = PaddingValues(bottom = 24.dp),
                        ) {
                            EnquiryStatus.entries.forEach { status ->
                                val rows = filtered.filter { it.status == status }
                                if (rows.isNotEmpty()) {
                                    stickyHeader(key = "header-${status.code}") {
                                        StickyListHeader(text = status.label)
                                    }
                                    items(rows, key = { it.id }) { enquiry ->
                                        if (enquiry.canReply()) {
                                            SwipeRevealRow(
                                                actions = { close ->
                                                    SwipeAction(
                                                        label = if (enquiry.reply.isNullOrBlank()) {
                                                            "Reply"
                                                        } else {
                                                            "Update"
                                                        },
                                                        icon = Icons.Outlined.Forum,
                                                        onClick = {
                                                            close()
                                                            replyTarget = enquiry
                                                        },
                                                    )
                                                },
                                            ) {
                                                InboxRow(
                                                    enquiry = enquiry,
                                                    onClick = { selectedEnquiry = enquiry },
                                                )
                                            }
                                        } else {
                                            InboxRow(
                                                enquiry = enquiry,
                                                onClick = { selectedEnquiry = enquiry },
                                            )
                                        }
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

    selectedEnquiry?.let { enquiry ->
        InboxDetailSheet(
            enquiry = enquiry,
            isBusy = state.busyEnquiryId == enquiry.id,
            onDismiss = { selectedEnquiry = null },
            onReply = if (enquiry.canReply()) {
                {
                    selectedEnquiry = null
                    replyTarget = enquiry
                }
            } else {
                null
            },
        )
    }
}

@Composable
private fun InboxRow(
    enquiry: Enquiry,
    onClick: () -> Unit,
) {
    AppListRow(
        title = if (enquiry.isAnonymous) {
            "Anonymous student"
        } else {
            enquiry.studentName ?: "Student"
        },
        supportingText = "${enquiry.referenceCode} · ${enquiry.category.label} · " +
            DateTimeUtils.formatDate(enquiry.createdAt),
        leadingIcon = Icons.Outlined.Forum,
        trailingContent = {
            StatusChip(status = enquiry.status.code)
        },
        onClick = onClick,
    )
}

@Composable
private fun InboxDetailSheet(
    enquiry: Enquiry,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onReply: (() -> Unit)?,
) {
    AppBottomSheet(
        onDismissRequest = onDismiss,
        title = if (enquiry.isAnonymous) "Anonymous enquiry" else "Student enquiry",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${enquiry.referenceCode} · ${enquiry.category.label} · " +
                    DateTimeUtils.formatDate(enquiry.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            StatusChip(status = enquiry.status.code)
        }
        Text(
            text = enquiry.message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (!enquiry.reply.isNullOrBlank()) {
            Text(
                text = "Your reply",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                text = enquiry.reply,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        enquiry.recommendedResourceTitle?.let {
            Text(
                text = "Attached: $it",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        if (enquiry.isAnonymous) {
            Text(
                text = "You cannot identify this student. Reply through the app; they can check it with their reference code.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        if (onReply != null) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onReply,
                enabled = !isBusy,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(if (enquiry.reply.isNullOrBlank()) "Reply" else "Update reply")
            }
        }
    }
}

@Composable
private fun ReplyFormScreen(
    enquiry: Enquiry,
    resources: List<Pair<String, String>>,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onSave: (String, String, String?) -> Unit,
) {
    var reply by remember(enquiry.id) { mutableStateOf(enquiry.reply.orEmpty()) }
    var status by remember(enquiry.id) {
        mutableStateOf(if (enquiry.status == EnquiryStatus.OPEN) "answered" else enquiry.status.code)
    }
    var resourceLabel by remember(enquiry.id) {
        mutableStateOf(
            resources.firstOrNull { it.first == enquiry.recommendedResourceId }?.second
                ?: "No attached resource",
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = if (enquiry.isAnonymous) "Reply anonymously" else "Reply to student",
                onBack = onClose,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Text(
                    text = "${enquiry.referenceCode} · ${enquiry.category.label}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = enquiry.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
                )
                AppTextField(
                    value = reply,
                    onValueChange = {
                        reply = it
                        error = null
                    },
                    label = "Reply",
                    singleLine = false,
                    maxLines = 8,
                    supportingText = "${reply.length}/2000",
                    error = error,
                )
                Spacer(Modifier.height(16.dp))
                SegmentedChoiceField(
                    label = "Status",
                    options = listOf("Open", "Answered", "Closed"),
                    selected = status.replaceFirstChar { it.uppercase() },
                    onSelect = { status = it.lowercase() },
                )
                Spacer(Modifier.height(14.dp))
                DropdownField(
                    label = "Attach a published resource (optional)",
                    options = listOf("No attached resource") + resources.map { it.second },
                    selected = resourceLabel,
                    onSelect = { resourceLabel = it },
                )
                if (enquiry.isAnonymous) {
                    Spacer(Modifier.height(14.dp))
                    InfoCard(
                        title = "Anonymous",
                        body = "You cannot identify this student. Reply through the app; they can check it with their reference code.",
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            StickyBottomAction {
                PrimaryButton(
                    text = "Save reply",
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
                )
            }
        }

        AppSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
        )
    }
}
