// Module 5 | Owner: Michael
// Resource library as category-grouped rows. Admins create from a FAB and act
// through the row sheet; students open the resource from the same sheet.
package com.example.counselling.feature.resource.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import com.example.counselling.feature.resource.domain.Resource
import com.example.counselling.feature.resource.domain.ResourceRepository
import com.example.counselling.ui.components.AppBottomSheet
import com.example.counselling.ui.components.AppListRow
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.ConfirmDialog
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.FilterChipRow
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.ScreenTitle
import com.example.counselling.ui.components.StatusChip
import com.example.counselling.ui.components.StickyListHeader
import com.example.counselling.ui.components.showAppMessage
import kotlinx.coroutines.launch

@Composable
fun ResourcesRoute(
    canManage: Boolean,
    repository: ResourceRepository,
    hotlineOnly: Boolean = false,
    onBack: (() -> Unit)? = null,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val viewModel: ResourcesViewModel = viewModel(
        factory = ResourcesViewModel.factory(canManage, repository, hotlineOnly),
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    onOpen: (Resource) -> Unit,
    onTogglePublish: (Resource, Boolean) -> Unit,
    onDelete: (Resource) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    var resourceToDelete by remember { mutableStateOf<Resource?>(null) }
    var selectedResource by remember { mutableStateOf<Resource?>(null) }
    val listState = rememberLazyListState()
    val showExtendedFab by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 24
        }
    }
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
                resource.category.label.contains(query, ignoreCase = true)
            val matchesCategory = state.category == "All" || resource.category.label == state.category
            val matchesMedia = state.mediaType == "All" || resource.mediaType == state.mediaType
            matchesQuery && matchesCategory && matchesMedia
        }
    }

    val subtitle = if (hotlineOnly) {
        "If you are in immediate danger, contact campus security or local emergency services."
    } else {
        "Practical self-help for common student concerns."
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (onBack != null) {
                AppTopBar(
                    title = if (hotlineOnly) "Urgent support" else "Resources",
                    onBack = onBack,
                )
            } else {
                ScreenTitle(
                    title = if (hotlineOnly) "Immediate support" else "Self-help library",
                    subtitle = subtitle,
                )
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
            ) {
                if (onBack != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    )
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            FilterChipRow(
                options = mediaOptions,
                selected = state.mediaType,
                onSelect = onMediaTypeChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
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
                                icon = Icons.AutoMirrored.Outlined.MenuBook,
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
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                bottom = if (canManage) 96.dp else 24.dp,
                            ),
                        ) {
                            filtered
                                .groupBy { it.category.label }
                                .forEach { (category, rows) ->
                                    stickyHeader(key = "header-$category") {
                                        StickyListHeader(text = category)
                                    }
                                    items(rows, key = { it.id }) { resource ->
                                        ResourceRow(
                                            resource = resource,
                                            canManage = canManage,
                                            onClick = { selectedResource = resource },
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }

        if (canManage) {
            AnimatedContent(
                targetState = showExtendedFab,
                label = "resource-fab",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) { extended ->
                if (extended) {
                    ExtendedFloatingActionButton(
                        onClick = onAdd,
                        icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                        text = { Text("New resource") },
                    )
                } else {
                    FloatingActionButton(onClick = onAdd) {
                        Icon(Icons.Outlined.Add, contentDescription = "New resource")
                    }
                }
            }
        }

        AppSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }

    selectedResource?.let { resource ->
        ResourceDetailSheet(
            resource = resource,
            canManage = canManage,
            isBusy = state.busyResourceId == resource.id,
            onDismiss = { selectedResource = null },
            onOpen = {
                selectedResource = null
                onOpen(resource)
            },
            onEdit = {
                selectedResource = null
                onEdit(resource.id)
            },
            onTogglePublish = {
                selectedResource = null
                onTogglePublish(resource, !resource.isPublished)
            },
            onDelete = {
                selectedResource = null
                resourceToDelete = resource
            },
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
private fun ResourceRow(
    resource: Resource,
    canManage: Boolean,
    onClick: () -> Unit,
) {
    AppListRow(
        title = resource.title,
        supportingText = "${resource.category.label} · ${resource.mediaType}",
        leadingIcon = Icons.AutoMirrored.Outlined.MenuBook,
        trailingContent = {
            if (canManage) {
                StatusChip(if (resource.isPublished) "published" else "draft")
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun ResourceDetailSheet(
    resource: Resource,
    canManage: Boolean,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onTogglePublish: () -> Unit,
    onDelete: () -> Unit,
) {
    AppBottomSheet(
        onDismissRequest = onDismiss,
        title = resource.title,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${resource.category.label} · ${resource.mediaType}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            if (canManage) {
                StatusChip(if (resource.isPublished) "published" else "draft")
            }
        }
        Text(
            text = resource.description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (resource.targetStressBrackets.isNotEmpty()) {
            Text(
                text = "For: " + resource.targetStressBrackets.joinToString(", ") {
                    AppConstants.stressBracketLabels[it] ?: it
                } + " wellbeing",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        Text(
            text = resource.contentUrl,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp),
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onOpen,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text("Open resource")
        }
        if (canManage) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onEdit,
                enabled = !isBusy,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("Edit")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onTogglePublish,
                enabled = !isBusy,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(if (resource.isPublished) "Unpublish" else "Publish")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDelete,
                enabled = !isBusy,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
