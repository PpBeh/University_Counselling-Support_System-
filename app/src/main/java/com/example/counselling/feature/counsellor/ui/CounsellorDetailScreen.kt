// Module 3 | Owner: Jeremy
package com.example.counselling.feature.counsellor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.feature.counsellor.data.CounsellorRepository
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.ConfirmDialog
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.SecondaryButton
import com.example.counselling.ui.components.SectionHeader
import com.example.counselling.ui.components.StatusChip
import com.example.counselling.ui.components.showAppMessage
import com.example.counselling.ui.theme.SoftError
import kotlinx.coroutines.delay

@Composable
fun CounsellorDetailRoute(
    counsellorId: String,
    repository: CounsellorRepository,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onManageAvailability: (String) -> Unit,
    onRemoved: () -> Unit,
) {
    val viewModel: CounsellorDetailViewModel = viewModel(
        factory = CounsellorDetailViewModel.factory(counsellorId, repository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }
    LaunchedEffect(state.removed) {
        if (state.removed) {
            delay(900)
            onRemoved()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.retry()
    }

    CounsellorDetailScreen(
        state = state,
        snackbarHostState = snackbarState,
        onBack = onBack,
        onRetry = viewModel::retry,
        onEdit = { onEdit(counsellorId) },
        onManageAvailability = { onManageAvailability(counsellorId) },
        onRemove = viewModel::remove,
    )
}

@Composable
private fun CounsellorDetailScreen(
    state: CounsellorDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
    onManageAvailability: () -> Unit,
    onRemove: () -> Unit,
) {
    var showRemoveDialog by remember { mutableStateOf(false) }
    val counsellor = state.counsellor

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = "Counsellor", onBack = onBack)

            when {
                state.isLoading -> LoadingShimmer(
                    modifier = Modifier.padding(20.dp),
                    rows = 4,
                )

                state.error != null || counsellor == null -> ErrorState(
                    message = state.error ?: "That counsellor could not be found.",
                    onRetry = onRetry,
                )

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 14.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = counsellor.fullName,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(
                                text = counsellor.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                StatusChip(if (counsellor.isActive) "active" else "inactive")
                                StatusChip(if (counsellor.userId == null) "unlinked" else "linked")
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    InfoCard(title = "Specialisations", icon = Icons.Outlined.Groups) {
                        Text(
                            text = counsellor.specialisations.joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    InfoCard(title = "Languages", icon = Icons.Outlined.Language) {
                        Text(
                            text = counsellor.languages.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (counsellor.bio.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        InfoCard(title = "Bio", icon = Icons.Outlined.Email) {
                            Text(counsellor.bio, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    SectionHeader(title = "Upcoming free slots")
                    Spacer(Modifier.height(8.dp))
                    if (counsellor.upcomingFreeSlots.isEmpty()) {
                        InfoCard(
                            title = "No free upcoming slots",
                            body = "Add availability so this counsellor can appear in Smart Match.",
                            icon = Icons.Outlined.CalendarMonth,
                            actionLabel = "Manage availability",
                            onAction = onManageAvailability,
                        )
                    } else {
                        counsellor.upcomingFreeSlots.take(8).forEach { slot ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            ) {
                                Text(
                                    text = "${DateTimeUtils.formatDate(slot.startAt)} · " +
                                        "${DateTimeUtils.formatTime(slot.startAt)} – " +
                                        DateTimeUtils.formatTime(slot.endAt),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }
                        if (counsellor.upcomingFreeSlots.size > 8) {
                            Text(
                                text = "+${counsellor.upcomingFreeSlots.size - 8} more slots",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    PrimaryButton(text = "Edit counsellor details", onClick = onEdit)
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton(
                        text = "Manage availability",
                        onClick = onManageAvailability,
                    )
                    Spacer(Modifier.height(20.dp))
                    InfoCard(
                        title = "Remove from directory",
                        body = "Removal is blocked while the counsellor has upcoming sessions. If they have historical sessions, their directory entry is deactivated instead.",
                        icon = Icons.Outlined.Groups,
                        actionLabel = "Remove counsellor",
                        onAction = { showRemoveDialog = true },
                        actionEnabled = !state.isRemoving,
                        accentColor = SoftError,
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    )
                    Spacer(Modifier.height(24.dp))
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

    if (showRemoveDialog && counsellor != null) {
        ConfirmDialog(
            title = "Remove ${counsellor.fullName}?",
            message = "If there is no session history the directory entry is deleted. If there is history it is deactivated and retained. Upcoming appointments block removal.",
            confirmLabel = "Remove",
            onConfirm = onRemove,
            onDismiss = { showRemoveDialog = false },
        )
    }
}
