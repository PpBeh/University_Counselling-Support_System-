// Module 3 | Owner: Jeremy
// Counsellor directory as availability-grouped rows. Search expands from the
// title, and the admin create action is a FAB.
package com.example.counselling.feature.counsellor.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import com.example.counselling.core.AppConstants
import com.example.counselling.core.domain.Counsellor
import com.example.counselling.feature.counsellor.domain.CounsellorRepository
import com.example.counselling.ui.components.AppListRow
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.FilterChipRow
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.StickyListHeader
import com.example.counselling.ui.components.UserAvatar

@Composable
fun CounsellorListRoute(
    repository: CounsellorRepository,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val viewModel: CounsellorListViewModel = viewModel(
        factory = CounsellorListViewModel.factory(repository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    CounsellorListScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onSpecialisationChange = viewModel::onSpecialisationChange,
        onAvailableThisWeekChange = viewModel::onAvailableThisWeekChange,
        onAdd = onAdd,
        onOpen = onOpen,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CounsellorListScreen(
    state: CounsellorListUiState,
    onQueryChange: (String) -> Unit,
    onSpecialisationChange: (String) -> Unit,
    onAvailableThisWeekChange: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    var showSearch by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val showExtendedFab by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 24
        }
    }
    val filtered = remember(
        state.counsellors,
        state.query,
        state.specialisation,
        state.availableThisWeekOnly,
    ) {
        state.counsellors.filter { counsellor ->
            val query = state.query.trim()
            val matchesQuery = query.isBlank() ||
                counsellor.fullName.contains(query, ignoreCase = true) ||
                counsellor.email.value.contains(query, ignoreCase = true) ||
                counsellor.specialisationLabels.any { it.contains(query, ignoreCase = true) }
            val matchesSpecialisation = state.specialisation == "All" ||
                counsellor.specialisationLabels.contains(state.specialisation)
            val matchesWeek = !state.availableThisWeekOnly || counsellor.availableThisWeek()
            matchesQuery && matchesSpecialisation && matchesWeek
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Counsellor directory",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "Add the entry first; the counsellor gets the role when their email signs up.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = {
                        showSearch = !showSearch
                        if (!showSearch) onQueryChange("")
                    },
                ) {
                    Icon(
                        imageVector = if (showSearch) Icons.Outlined.Close else Icons.Outlined.Search,
                        contentDescription = if (showSearch) {
                            "Close search"
                        } else {
                            "Search counsellors"
                        },
                    )
                }
            }

            if (showSearch) {
                AppTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    label = "Search name, email or specialisation",
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            FilterChipRow(
                options = listOf("All") + AppConstants.concernCategories,
                selected = state.specialisation,
                onSelect = onSpecialisationChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Available this week",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.availableThisWeekOnly,
                    onCheckedChange = onAvailableThisWeekChange,
                )
            }

            when {
                state.isLoading -> LoadingShimmer(
                    modifier = Modifier.padding(20.dp),
                    rows = 4,
                )

                state.error != null && state.counsellors.isEmpty() -> ErrorState(
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
                                icon = Icons.Outlined.Groups,
                                title = "No counsellors match",
                                message = "Try another search or add a counsellor to the directory.",
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 96.dp),
                        ) {
                            counsellorGroups(filtered).forEach { (label, rows) ->
                                if (rows.isEmpty()) return@forEach
                                stickyHeader(key = "header-$label") {
                                    StickyListHeader(text = label)
                                }
                                items(rows, key = { it.id }) { counsellor ->
                                    CounsellorRow(
                                        counsellor = counsellor,
                                        onClick = { onOpen(counsellor.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedContent(
            targetState = showExtendedFab,
            label = "counsellor-fab",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
        ) { extended ->
            if (extended) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Add counsellor") },
                )
            } else {
                FloatingActionButton(onClick = onAdd) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add counsellor")
                }
            }
        }
    }
}

private fun counsellorGroups(
    counsellors: List<Counsellor>,
): List<Pair<String, List<Counsellor>>> {
    fun groupOf(counsellor: Counsellor): String = when {
        !counsellor.isActive -> "Inactive"
        counsellor.availableThisWeek() -> "Available this week"
        else -> "Active counsellors"
    }
    return listOf("Available this week", "Active counsellors", "Inactive").map { label ->
        label to counsellors
            .filter { groupOf(it) == label }
            .sortedBy { it.fullName.lowercase() }
    }
}

@Composable
private fun CounsellorRow(counsellor: Counsellor, onClick: () -> Unit) {
    val supporting = buildString {
        append(counsellor.specialisationLabels.joinToString(" · "))
        if (!counsellor.isActive) {
            append(" · Inactive")
        } else if (counsellor.availableThisWeek()) {
            append(" · Free this week")
        }
        if (counsellor.authUserId == null) {
            append(" · Not linked to an auth account")
        }
    }
    AppListRow(
        title = counsellor.fullName,
        supportingText = supporting,
        leadingContent = {
            UserAvatar(
                initials = counsellor.initials,
                size = 40.dp,
                contentDescription = "${counsellor.fullName} avatar",
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = onClick,
    )
}
