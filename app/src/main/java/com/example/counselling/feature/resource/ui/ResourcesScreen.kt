// Module 5 | Owner: Michael
package com.example.counselling.feature.resource.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.AppConstants
import com.example.counselling.core.MessageKind
import com.example.counselling.core.UiMessage
import com.example.counselling.core.UserRole
import com.example.counselling.feature.resource.data.ResourceRepository
import com.example.counselling.feature.resource.model.ResourceRow
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.ConfirmDialog
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.FilterChipRow
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.StatusChip
import com.example.counselling.ui.components.showAppMessage
import kotlinx.coroutines.launch

@Composable
fun ResourcesRoute(
    role: UserRole,
    repository: ResourceRepository,
    hotlineOnly: Boolean = false,
    onBack: (() -> Unit)? = null,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val viewModel: ResourcesViewModel = viewModel(
        factory = ResourcesViewModel.factory(role, repository, hotlineOnly),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    ResourcesScreen(
        state = state,
        canManage = viewModel.canManage,
        mediaOptions = viewModel.mediaOptions,
        hotlineOnly = hotlineOnly,
        snackbarHostState = snackbarState,
        onBack = onBack,
        onAdd = onAdd,
        onEdit = onEdit,
        onQueryChange = viewModel::onQueryChange,
        onCategoryChange = viewModel::onCategoryChange,
        onMediaTypeChange = viewModel::onMediaTypeChange,
        onOpen = { resource ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resource.contentUrl))
            runCatching { context.startActivity(intent) }
                .onFailure {
                    scope.launch {
                        snackbarState.showAppMessage(
                            UiMessage("No app can open this resource link.", MessageKind.ERROR),
                        )
                    }
                }
        },
        onTogglePublish = viewModel::setPublished,
        onDelete = viewModel::delete,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResourcesScreen(
    state: ResourcesUiState,
    canManage: Boolean,
    mediaOptions: List<String>,
    hotlineOnly: Boolean,
    snackbarHostState: SnackbarHostState,
    onBack: (() -> Unit)?,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onMediaTypeChange: (String) -> Unit,
    onOpen: (ResourceRow) -> Unit,
    onTogglePublish: (ResourceRow, Boolean) -> Unit,
    onDelete: (ResourceRow) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    var resourceToDelete by remember { mutableStateOf<ResourceRow?>(null) }
    val filtered = remember(
        state.resources,
        state.query,
        state.category,
        state.mediaType,
    ) {
        state.resources.filter { resource ->
            val query = state.query.trim()
            val matchesQuery = query.isBlank() ||
                resource.title.contains(query, ignoreCase = true) ||
                resource.description.contains(query, ignoreCase = true) ||
                resource.category.contains(query, ignoreCase = true)
            val matchesCategory = state.category == "All" || resource.category == state.category
            val matchesMedia = state.mediaType == "All" || resource.mediaType == state.mediaType
            matchesQuery && matchesCategory && matchesMedia
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = if (hotlineOnly) "Urgent support" else "Resources",
                onBack = onBack,
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = if (hotlineOnly) "Immediate support" else "Self-help library",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = if (hotlineOnly) {
                        "If you are in immediate danger, contact campus security or local emergency services."
                    } else {
                        "Calm, practical resources for the things students bring to counselling."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                if (canManage) {
                    PrimaryButton(
                        text = "Add resource",
                        onClick = onAdd,
                        icon = Icons.Outlined.Add,
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            AppTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = "Search resources",
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            if (!hotlineOnly) {
                FilterChipRow(
                    options = listOf("All") + AppConstants.concernCategories,
                    selected = state.category,
                    onSelect = onCategoryChange,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
            FilterChipRow(
                options = mediaOptions,
                selected = state.mediaType,
                onSelect = onMediaTypeChange,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )

            when {
                state.isLoading -> LoadingShimmer(
                    modifier = Modifier.padding(20.dp),
                    rows = 4,
                )

                state.error != null && state.resources.isEmpty() -> ErrorState(
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
                                icon = Icons.Outlined.MenuBook,
                                title = "No resources found",
                                message = if (state.resources.isEmpty()) {
                                    "Resources added by the counselling team will appear here."
                                } else {
                                    "Try another search or filter."
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
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(filtered, key = { it.id }) { resource ->
                                ResourceCard(
                                    resource = resource,
                                    canManage = canManage,
                                    isBusy = state.busyResourceId == resource.id,
                                    onOpen = { onOpen(resource) },
                                    onEdit = { onEdit(resource.id) },
                                    onTogglePublish = {
                                        onTogglePublish(resource, !resource.isPublished)
                                    },
                                    onDelete = { resourceToDelete = resource },
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

    resourceToDelete?.let { resource ->
        ConfirmDialog(
            title = "Delete this resource?",
            message = "\"${resource.title}\" will be permanently removed from the library.",
            confirmLabel = "Delete",
            onConfirm = { onDelete(resource) },
            onDismiss = { resourceToDelete = null },
        )
    }
}

@Composable
private fun ResourceCard(
    resource: ResourceRow,
    canManage: Boolean,
    isBusy: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onTogglePublish: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resource.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${resource.category} · ${resource.mediaType}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (canManage) {
                    StatusChip(if (resource.isPublished) "published" else "draft")
                }
            }
            Text(
                text = resource.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (resource.targetStressBrackets.isNotEmpty()) {
                Text(
                    text = "For: " + resource.targetStressBrackets.joinToString(", ") {
                        AppConstants.stressBracketLabels[it] ?: it
                    } + " wellbeing",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onOpen) { Text("Open resource") }
                if (canManage) {
                    TextButton(onClick = onEdit, enabled = !isBusy) { Text("Edit") }
                    TextButton(onClick = onTogglePublish, enabled = !isBusy) {
                        Text(if (resource.isPublished) "Unpublish" else "Publish")
                    }
                    TextButton(onClick = onDelete, enabled = !isBusy) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
