// Module 6 | Owner: Michael
// Linked enquiries + anonymous reference-code lookup/withdrawal.
package com.example.counselling.feature.enquiry.ui

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.AppConstants
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.core.MessageKind
import com.example.counselling.core.UiMessage
import com.example.counselling.feature.enquiry.data.EnquiryRepository
import com.example.counselling.feature.enquiry.model.Enquiry
import com.example.counselling.feature.enquiry.model.EnquiryLookupRow
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.ConfirmDialog
import com.example.counselling.ui.components.DropdownField
import com.example.counselling.ui.components.EmptyState
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.SecondaryButton
import com.example.counselling.ui.components.StatusChip
import com.example.counselling.ui.components.showAppMessage
import kotlinx.coroutines.launch

@Composable
fun StudentEnquiriesRoute(
    repository: EnquiryRepository,
    onOpenFeedback: () -> Unit,
) {
    val viewModel: StudentEnquiriesViewModel = viewModel(
        factory = StudentEnquiriesViewModel.factory(repository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }

    StudentEnquiriesScreen(
        state = state,
        snackbarHostState = snackbarState,
        onSubmit = viewModel::submit,
        onLookup = viewModel::lookup,
        onClearLookup = viewModel::clearLookup,
        onWithdrawByCode = viewModel::withdrawByCode,
        onWithdrawLinked = viewModel::withdrawLinked,
        onDismissReference = viewModel::dismissReferenceCode,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onOpenFeedback = onOpenFeedback,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentEnquiriesScreen(
    state: StudentEnquiriesUiState,
    snackbarHostState: SnackbarHostState,
    onSubmit: (String, String, Boolean) -> Unit,
    onLookup: (String) -> Unit,
    onClearLookup: () -> Unit,
    onWithdrawByCode: (String) -> Unit,
    onWithdrawLinked: (Enquiry) -> Unit,
    onDismissReference: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onOpenFeedback: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showForm by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var anonymous by remember { mutableStateOf(false) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var messageError by remember { mutableStateOf<String?>(null) }
    var lookupCode by remember { mutableStateOf("") }
    var linkedToWithdraw by remember { mutableStateOf<Enquiry?>(null) }
    var codeToWithdraw by remember { mutableStateOf<String?>(null) }

    fun copyCode(code: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Enquiry reference", code))
        scope.launch {
            snackbarHostState.showAppMessage(
                UiMessage("Reference code copied.", MessageKind.SUCCESS),
            )
        }
    }

    LaunchedEffect(state.lastReferenceCode) {
        if (state.lastReferenceCode != null) {
            showForm = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = "Enquiries")
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            text = "A private space to ask",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            text = "Ask anonymously and keep the code, or send a linked question that appears below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        )
                        PrimaryButton(
                            text = "Ask a question",
                            onClick = {
                                category = ""
                                message = ""
                                anonymous = false
                                categoryError = null
                                messageError = null
                                showForm = true
                            },
                            icon = Icons.Outlined.Forum,
                        )
                        Spacer(Modifier.height(8.dp))
                        SecondaryButton(
                            text = "Rate a completed session",
                            onClick = onOpenFeedback,
                        )
                        Spacer(Modifier.height(14.dp))
                    }

                    item {
                        LookupCard(
                            code = lookupCode,
                            onCodeChange = { lookupCode = it.uppercase() },
                            onLookup = { onLookup(lookupCode) },
                            onClear = {
                                lookupCode = ""
                                onClearLookup()
                            },
                            isLoading = state.isLookingUp,
                            error = state.lookupError,
                            result = state.lookupResult,
                            onWithdraw = { codeToWithdraw = it },
                            onCopy = ::copyCode,
                        )
                    }

                    item {
                        Text(
                            text = "My linked enquiries",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    when {
                        state.isLoading -> item {
                            LoadingShimmer(rows = 3)
                        }

                        state.error != null && state.enquiries.isEmpty() -> item {
                            InfoCard(
                                title = "Could not load enquiries",
                                body = state.error,
                                actionLabel = "Try again",
                                onAction = onRetry,
                            )
                        }

                        state.enquiries.isEmpty() -> item {
                            EmptyState(
                                icon = Icons.Outlined.Forum,
                                title = "No linked enquiries",
                                message = "Linked questions appear here. Anonymous questions can only be checked with their code.",
                            )
                        }

                        else -> items(state.enquiries, key = { it.id }) { enquiry ->
                            LinkedEnquiryCard(
                                enquiry = enquiry,
                                isBusy = state.busyEnquiryId == enquiry.id,
                                onCopy = { copyCode(enquiry.referenceCode) },
                                onWithdraw = { linkedToWithdraw = enquiry },
                            )
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

    if (showForm) {
        AlertDialog(
            onDismissRequest = { if (!state.isSubmitting) showForm = false },
            title = { Text("Ask a private question") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DropdownField(
                        label = "Concern category",
                        options = AppConstants.concernCategories,
                        selected = category,
                        onSelect = {
                            category = it
                            categoryError = null
                        },
                        enabled = !state.isSubmitting,
                        error = categoryError,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = message,
                        onValueChange = {
                            message = it
                            messageError = null
                        },
                        label = "Your question",
                        singleLine = false,
                        maxLines = 6,
                        error = messageError,
                        supportingText = "${message.length}/1000 · minimum 10 characters",
                        enabled = !state.isSubmitting,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ask anonymously", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "No name or student ID is stored. Keep the reference code.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = anonymous,
                            onCheckedChange = { anonymous = it },
                            enabled = !state.isSubmitting,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        categoryError = if (category in AppConstants.concernCategories) {
                            null
                        } else {
                            "Choose a category."
                        }
                        messageError = when {
                            message.trim().length < 10 -> "Write at least 10 characters."
                            message.trim().length > 1000 -> "Use 1,000 characters or fewer."
                            else -> null
                        }
                        if (categoryError == null && messageError == null) {
                            onSubmit(category, message, anonymous)
                        }
                    },
                    enabled = !state.isSubmitting,
                ) {
                    Text(if (state.isSubmitting) "Sending…" else "Send question")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForm = false }, enabled = !state.isSubmitting) {
                    Text("Cancel")
                }
            },
            shape = MaterialTheme.shapes.large,
        )
    }

    state.lastReferenceCode?.let { code ->
        AlertDialog(
            onDismissRequest = onDismissReference,
            title = { Text("Question submitted") },
            text = {
                Column {
                    Text("Your reference code is:")
                    Text(
                        text = code,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                    Text(
                        text = "Save this code to check the reply. Anonymous enquiries cannot be recovered without it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissReference) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = { copyCode(code) }) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                    )
                    Text("Copy code")
                }
            },
            shape = MaterialTheme.shapes.large,
        )
    }

    linkedToWithdraw?.let { enquiry ->
        ConfirmDialog(
            title = "Withdraw this enquiry?",
            message = "Your linked question and its reply will be deleted. This cannot be undone.",
            confirmLabel = "Withdraw",
            onConfirm = { onWithdrawLinked(enquiry) },
            onDismiss = { linkedToWithdraw = null },
        )
    }

    codeToWithdraw?.let { code ->
        ConfirmDialog(
            title = "Withdraw anonymous enquiry?",
            message = "$code will be permanently deleted if it is still open.",
            confirmLabel = "Withdraw",
            onConfirm = { onWithdrawByCode(code) },
            onDismiss = { codeToWithdraw = null },
        )
    }
}

@Composable
private fun LookupCard(
    code: String,
    onCodeChange: (String) -> Unit,
    onLookup: () -> Unit,
    onClear: () -> Unit,
    isLoading: Boolean,
    error: String?,
    result: EnquiryLookupRow?,
    onWithdraw: (String) -> Unit,
    onCopy: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Check a reply by code", style = MaterialTheme.typography.titleMedium)
            Text(
                "No sign-in identity is shown for anonymous enquiries.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
            AppTextField(
                value = code,
                onValueChange = onCodeChange,
                label = "Reference code",
                supportingText = "For example ENQ-7K3P9Q",
            )
            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(
                    text = "Clear",
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                )
                PrimaryButton(
                    text = if (isLoading) "Checking…" else "Check reply",
                    onClick = onLookup,
                    modifier = Modifier.weight(1f),
                    enabled = code.isNotBlank() && !isLoading,
                )
            }
            result?.let {
                Spacer(Modifier.height(12.dp))
                LookupResultCard(
                    result = it,
                    onWithdraw = { onWithdraw(it.referenceCode) },
                    onCopy = { onCopy(it.referenceCode) },
                )
            }
        }
    }
}

@Composable
private fun LookupResultCard(
    result: EnquiryLookupRow,
    onWithdraw: () -> Unit,
    onCopy: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = result.referenceCode,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(result.status)
            }
            Text(
                text = "${result.category} · ${DateTimeUtils.formatDate(result.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                text = result.message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = result.reply ?: "No reply yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (result.reply == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(top = 8.dp),
            )
            result.recommendedResourceTitle?.let {
                Text(
                    text = "Attached resource: $it",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onCopy) { Text("Copy code") }
                if (result.status == "open") {
                    TextButton(onClick = onWithdraw) {
                        Text("Withdraw", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkedEnquiryCard(
    enquiry: Enquiry,
    isBusy: Boolean,
    onCopy: () -> Unit,
    onWithdraw: () -> Unit,
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
                    text = enquiry.referenceCode,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(enquiry.status)
            }
            Text(
                text = "${enquiry.category} · ${DateTimeUtils.formatDate(enquiry.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                text = enquiry.message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (!enquiry.reply.isNullOrBlank()) {
                Text(
                    text = "Reply",
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
                    text = "Attached resource: $it",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onCopy) { Text("Copy code") }
                if (enquiry.status == "open") {
                    TextButton(onClick = onWithdraw, enabled = !isBusy) {
                        Text("Withdraw", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
