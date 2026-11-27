// Module 1 | Owner: Beh
// Counsellor/admin student search. Only staff-permitted profile fields are
// shown; rows are grouped by initial and search expands from the title.
package com.example.counselling.feature.student.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.domain.Student
import com.example.counselling.feature.student.domain.StudentRepository
import com.example.counselling.ui.components.AppListRow
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.ScreenTitle
import com.example.counselling.ui.components.StickyListHeader
import com.example.counselling.ui.components.UserAvatar

@Composable
fun StudentListRoute(
    studentRepository: StudentRepository,
    onBack: (() -> Unit)?,
    onOpenStudent: (String) -> Unit,
) {
    val viewModel: StudentListViewModel = viewModel(
        factory = StudentListViewModel.factory(studentRepository),
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun StudentListScreen(
    state: StudentListUiState,
    onBack: (() -> Unit)?,
    onQueryChange: (String) -> Unit,
    onOpenStudent: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    var showSearch by remember { mutableStateOf(false) }
    val filtered = remember(state.students, state.query) {
        val search = state.query.trim()
        if (search.isBlank()) {
            state.students
        } else {
            state.students.filter {
                it.fullName.contains(search, ignoreCase = true) ||
                    it.studentId.value.contains(search, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (onBack != null) {
            AppTopBar(
                title = "Student directory",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            showSearch = !showSearch
                            if (!showSearch) onQueryChange("")
                        },
                    ) {
                        Icon(
                            imageVector = if (showSearch) {
                                Icons.Outlined.Close
                            } else {
                                Icons.Outlined.Search
                            },
                            contentDescription = if (showSearch) {
                                "Close search"
                            } else {
                                "Search students"
                            },
                        )
                    }
                },
            )
        } else {
            ScreenTitle(
                title = "Student directory",
                actions = {
                    IconButton(
                        onClick = {
                            showSearch = !showSearch
                            if (!showSearch) onQueryChange("")
                        },
                    ) {
                        Icon(
                            imageVector = if (showSearch) {
                                Icons.Outlined.Close
                            } else {
                                Icons.Outlined.Search
                            },
                            contentDescription = if (showSearch) {
                                "Close search"
                            } else {
                                "Search students"
                            },
                        )
                    }
                },
            )
        }

        if (showSearch) {
            AppTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = "Search by name or student ID",
                modifier = Modifier.padding(horizontal = 20.dp),
                supportingText = "Only staff can use this directory.",
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        }

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
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        filtered
                            .sortedBy { it.fullName.lowercase() }
                            .groupBy { student ->
                                student.fullName.firstOrNull()?.uppercase() ?: "#"
                            }
                            .forEach { (initial, rows) ->
                                stickyHeader(key = "header-$initial") {
                                    StickyListHeader(text = initial)
                                }
                                items(rows, key = { it.id }) { student ->
                                    StudentRow(
                                        student = student,
                                        onClick = { onOpenStudent(student.id) },
                                    )
                                }
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentRow(student: Student, onClick: () -> Unit) {
    AppListRow(
        title = student.fullName,
        supportingText = "${student.studentId.value} · ${student.programme}",
        leadingContent = {
            UserAvatar(
                initials = student.initials,
                size = 40.dp,
                contentDescription = "${student.fullName} initials",
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
