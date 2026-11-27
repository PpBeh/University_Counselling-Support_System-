// Module 6 | Owner: Michael
// Linked enquiries + anonymous reference-code lookup/withdrawal. Rows are
// grouped by status, swipe reveals withdraw, and the sheet keeps it visible.
package com.example.counselling.feature.enquiry.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.AppConstants
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.core.MessageKind
import com.example.counselling.core.UiMessage
import com.example.counselling.feature.enquiry.domain.Enquiry
import com.example.counselling.feature.enquiry.domain.EnquiryLookup
import com.example.counselling.feature.enquiry.domain.EnquiryRepository
import com.example.counselling.feature.enquiry.domain.EnquiryStatus
import com.example.counselling.ui.components.AppBottomSheet
import com.example.counselling.ui.components.AppListRow
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
import com.example.counselling.ui.components.SectionLabel
import com.example.counselling.ui.components.StatusChip
import com.example.counselling.ui.components.StickyBottomAction
import com.example.counselling.ui.components.StickyListHeader
import com.example.counselling.ui.components.SwipeAction
import com.example.counselling.ui.components.SwipeRevealRow
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var selectedEnquiry by remember { mutableStateOf<Enquiry?>(null) }
    var linkedToWithdraw by remember { mutableStateOf<Enquiry?>(null) }
    var codeToWithdraw by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val showExtendedFab by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 24
        }
    }

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

    BackHandler(enabled = showForm) {
        if (!state.isSubmitting) showForm = false
    }

    if (showForm) {
        NewEnquiryForm(
            category = category,
            message = message,
            anonymous = anonymous,
            categoryError = categoryError,
            messageError = messageError,
            isSubmitting = state.isSubmitting,
            snackbarHostState = snackbarHostState,
            onCategoryChange = {
                category = it
                categoryError = null
            },
            onMessageChange = {
                message = it
                messageError = null
            },
            onAnonymousChange = { anonymous = it },
            onClose = { if (!state.isSubmitting) showForm = false },
            onSubmit = {
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
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                item(key = "header") {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
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
                        SecondaryButton(
                            text = "Rate a completed session",
                            onClick = onOpenFeedback,
                        )
                    }
                }

                item(key = "lookup") {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        SectionLabel(title = "Check a reply by code")
                        Text(
                            text = "No sign-in identity is shown for anonymous enquiries.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                        )
                        AppTextField(
                            value = lookupCode,
                            onValueChange = { lookupCode = it.uppercase() },
                            label = "Reference code",
                            supportingText = "For example ENQ-7K3P9Q",
                        )
                        if (state.lookupError != null) {
                            Text(
                                text = state.lookupError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    lookupCode = ""
                                    onClearLookup()
                                },
                                enabled = !state.isLookingUp,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                            ) {
                                Text("Clear")
                            }
                            Button(
                                onClick = { onLookup(lookupCode) },
                                enabled = lookupCode.isNotBlank() && !state.isLookingUp,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                            ) {
                                Text(if (state.isLookingUp) "Checking…" else "Check reply")
                            }
                        }
                        state.lookupResult?.let { result ->
                            LookupResultSection(
                                result = result,
                                onWithdraw = { codeToWithdraw = it },
                                onCopy = ::copyCode,
                            )
                        }
                    }
                }

                item(key = "linked-title") {
                    SectionLabel(
                        title = "My linked enquiries",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }

                when {
                    state.isLoading -> item(key = "loading") {
                        LoadingShimmer(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            rows = 3,
                        )
                    }

                    state.error != null && state.enquiries.isEmpty() -> item(key = "error") {
                        InfoCard(
                            title = "Could not load enquiries",
                            body = state.error,
                            actionLabel = "Try again",
                            onAction = onRetry,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }

                    state.enquiries.isEmpty() -> item(key = "empty") {
                        EmptyState(
                            icon = Icons.Outlined.Forum,
                            title = "No linked enquiries",
                            message = "Linked questions appear here. Anonymous questions can only be checked with their code.",
                        )
                    }

                    else -> {
                        EnquiryStatus.entries.forEach { status ->
                            val rows = state.enquiries.filter { it.status == status }
                            if (rows.isNotEmpty()) {
                                stickyHeader(key = "header-${status.code}") {
                                    StickyListHeader(text = status.label)
                                }
                                items(rows, key = { it.id }) { enquiry ->
                                    if (enquiry.status == EnquiryStatus.OPEN) {
                                        SwipeRevealRow(
                                            actions = { close ->
                                                SwipeAction(
                                                    label = "Withdraw",
                                                    icon = Icons.Outlined.Delete,
                                                    destructive = true,
                                                    onClick = {
                                                        close()
                                                        linkedToWithdraw = enquiry
                                                    },
                                                )
                                            },
                                        ) {
                                            EnquiryRow(
                                                enquiry = enquiry,
                                                onClick = { selectedEnquiry = enquiry },
                                            )
                                        }
                                    } else {
                                        EnquiryRow(
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

        AnimatedContent(
            targetState = showExtendedFab,
            label = "enquiry-fab",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
        ) { extended ->
            if (extended) {
                ExtendedFloatingActionButton(
                    onClick = {
                        category = ""
                        message = ""
                        anonymous = false
                        categoryError = null
                        messageError = null
                        showForm = true
                    },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("New enquiry") },
                )
            } else {
                FloatingActionButton(
                    onClick = {
                        category = ""
                        message = ""
                        anonymous = false
                        categoryError = null
                        messageError = null
                        showForm = true
                    },
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "New enquiry")
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
        EnquiryDetailSheet(
            enquiry = enquiry,
            isBusy = state.busyEnquiryId == enquiry.id,
            onDismiss = { selectedEnquiry = null },
            onCopy = {
                selectedEnquiry = null
                copyCode(enquiry.referenceCode)
            },
            onWithdraw = if (enquiry.status == EnquiryStatus.OPEN) {
                {
                    selectedEnquiry = null
                    linkedToWithdraw = enquiry
                }
            } else {
                null
            },
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
                    Icon(
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
private fun EnquiryRow(
    enquiry: Enquiry,
    onClick: () -> Unit,
) {
    AppListRow(
        title = enquiry.referenceCode,
        supportingText = "${enquiry.category.label} · " +
            DateTimeUtils.formatDate(enquiry.createdAt),
        leadingIcon = Icons.Outlined.Forum,
        trailingContent = {
            StatusChip(status = enquiry.status.code)
        },
        onClick = onClick,
    )
}

@Composable
private fun EnquiryDetailSheet(
    enquiry: Enquiry,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onWithdraw: (() -> Unit)?,
) {
    AppBottomSheet(
        onDismissRequest = onDismiss,
        title = enquiry.referenceCode,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${enquiry.category.label} · " +
                    DateTimeUtils.formatDate(enquiry.createdAt),
                style = MaterialTheme.typography.bodyMedium,
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
                text = "Reply",
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
                text = "Attached resource: $it",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onCopy,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text("Copy code")
        }
        if (onWithdraw != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onWithdraw,
                enabled = !isBusy,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("Withdraw enquiry", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun LookupResultSection(
    result: EnquiryLookup,
    onWithdraw: (String) -> Unit,
    onCopy: (String) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = result.referenceCode,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(result.status.code)
            }
            Text(
                text = "${result.category.label} · ${DateTimeUtils.formatDate(result.createdAt)}",
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
                TextButton(onClick = { onCopy(result.referenceCode) }) { Text("Copy code") }
                if (result.status == EnquiryStatus.OPEN) {
                    TextButton(onClick = { onWithdraw(result.referenceCode) }) {
                        Text("Withdraw", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun NewEnquiryForm(
    category: String,
    message: String,
    anonymous: Boolean,
    categoryError: String?,
    messageError: String?,
    isSubmitting: Boolean,
    snackbarHostState: SnackbarHostState,
    onCategoryChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onAnonymousChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onSubmit: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = "New enquiry", onBack = onClose)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Text(
                    text = "Ask a private question",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = "Choose whether your name is attached. Anonymous enquiries can only be checked with the reference code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
                )

                DropdownField(
                    label = "Concern category",
                    options = AppConstants.concernCategories,
                    selected = category,
                    onSelect = onCategoryChange,
                    enabled = !isSubmitting,
                    error = categoryError,
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    label = "Your question",
                    singleLine = false,
                    maxLines = 6,
                    error = messageError,
                    supportingText = "${message.length}/1000 · minimum 10 characters",
                    enabled = !isSubmitting,
                )
                Spacer(Modifier.height(16.dp))
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
                        onCheckedChange = onAnonymousChange,
                        enabled = !isSubmitting,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            StickyBottomAction {
                PrimaryButton(
                    text = if (isSubmitting) "Sending…" else "Send question",
                    onClick = onSubmit,
                    loading = isSubmitting,
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
