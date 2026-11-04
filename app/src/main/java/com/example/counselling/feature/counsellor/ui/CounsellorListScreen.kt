// Module 3 | Owner: Jeremy
package com.example.counselling.feature.counsellor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.AppConstants
import com.example.counselling.feature.counsellor.data.CounsellorRepository
import com.example.counselling.feature.counsellor.model.Counsellor
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.FilterChipRow
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.StatusChip
import com.example.counselling.ui.theme.SuccessContainer
import com.example.counselling.ui.theme.SuccessGreen

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

@OptIn(ExperimentalMaterial3Api::class)
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
                counsellor.email.contains(query, ignoreCase = true) ||
                counsellor.specialisations.any { it.contains(query, ignoreCase = true) }
            val matchesSpecialisation = state.specialisation == "All" ||
                counsellor.specialisations.contains(state.specialisation)
            val matchesWeek = !state.availableThisWeekOnly || counsellor.availableThisWeek
            matchesQuery && matchesSpecialisation && matchesWeek
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Counsellors")
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = "Counsellor directory",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Add the directory entry first; the counsellor gets the role when their email signs up.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            PrimaryButton(
                text = "Add counsellor",
                onClick = onAdd,
                icon = Icons.Outlined.Add,
            )
            Spacer(Modifier.height(12.dp))
        }

        AppTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = "Search name, email or specialisation",
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        FilterChipRow(
            options = listOf("All") + AppConstants.concernCategories,
            selected = state.specialisation,
            onSelect = onSpecialisationChange,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
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
        Spacer(Modifier.height(8.dp))

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
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filtered, key = { it.id }) { counsellor ->
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

@Composable
private fun CounsellorRow(counsellor: Counsellor, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(46.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = counsellor.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!counsellor.isActive) {
                        StatusChip(status = "inactive")
                    } else if (counsellor.availableThisWeek) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = SuccessContainer,
                        ) {
                            Text(
                                text = "Free this week",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                Text(
                    text = counsellor.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = counsellor.specialisations.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
                if (counsellor.userId == null) {
                    Text(
                        text = "Not linked to an Auth account yet",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
        }
    }
}
