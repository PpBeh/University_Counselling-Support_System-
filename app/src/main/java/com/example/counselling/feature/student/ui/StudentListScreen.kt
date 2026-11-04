// Module 1 | Owner: Beh
// Counsellor/admin student search. Only staff-permitted profile fields are shown.
package com.example.counselling.feature.student.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.feature.student.data.ProfileRepository
import com.example.counselling.feature.student.model.Profile
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.UserAvatar

@Composable
fun StudentListRoute(
    profileRepository: ProfileRepository,
    onBack: (() -> Unit)?,
    onOpenStudent: (String) -> Unit,
) {
    val viewModel: StudentListViewModel = viewModel(
        factory = StudentListViewModel.factory(profileRepository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StudentListScreen(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onOpenStudent = onOpenStudent,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentListScreen(
    state: StudentListUiState,
    onBack: (() -> Unit)?,
    onQueryChange: (String) -> Unit,
    onOpenStudent: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    val filtered = remember(state.students, state.query) {
        val search = state.query.trim()
        if (search.isBlank()) {
            state.students
        } else {
            state.students.filter {
                it.fullName.contains(search, ignoreCase = true) ||
                    it.studentId?.contains(search, ignoreCase = true) == true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Student directory", onBack = onBack)

        AppTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = "Search by name or student ID",
            modifier = Modifier.padding(horizontal = 20.dp),
            supportingText = "Only staff can use this directory.",
        )
        Spacer(Modifier.height(12.dp))

        when {
            state.isLoading -> LoadingShimmer(
                modifier = Modifier.padding(horizontal = 20.dp),
                rows = 5,
            )

            state.error != null && state.students.isEmpty() -> ErrorState(
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
                            icon = Icons.Outlined.Search,
                            title = if (state.students.isEmpty()) {
                                "No student profiles yet"
                            } else {
                                "No matching students"
                            },
                            message = if (state.students.isEmpty()) {
                                "Student profiles appear here after students register."
                            } else {
                                "Try a different name or student ID."
                            },
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filtered, key = { it.id }) { student ->
                            StudentRow(student = student, onClick = { onOpenStudent(student.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentRow(student: Profile, onClick: () -> Unit) {
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(
                initials = student.initials,
                size = 46.dp,
                contentDescription = "${student.fullName} initials",
            )
            Column(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .weight(1f),
            ) {
                Text(
                    text = student.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = student.studentId ?: "No student ID",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = student.programme ?: "Programme not set",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Open ${student.fullName}",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}
