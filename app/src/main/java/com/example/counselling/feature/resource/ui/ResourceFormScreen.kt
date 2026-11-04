// Module 5 | Owner: Michael
package com.example.counselling.feature.resource.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.AppConstants
import com.example.counselling.feature.resource.data.ResourceRepository
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.DropdownField
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.SectionHeader
import com.example.counselling.ui.components.showAppMessage
import kotlinx.coroutines.delay

@Composable
fun ResourceFormRoute(
    resourceId: String?,
    repository: ResourceRepository,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val viewModel: ResourceFormViewModel = viewModel(
        factory = ResourceFormViewModel.factory(resourceId, repository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            delay(900)
            onSaved()
        }
    }

    ResourceFormScreen(
        state = state,
        snackbarHostState = snackbarState,
        onBack = onBack,
        onRetry = viewModel::retry,
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onCategoryChange = viewModel::onCategoryChange,
        onMediaTypeChange = viewModel::onMediaTypeChange,
        onUrlChange = viewModel::onUrlChange,
        onPublishedChange = viewModel::onPublishedChange,
        onToggleBracket = viewModel::toggleBracket,
        onSave = viewModel::save,
    )
}

@Composable
private fun ResourceFormScreen(
    state: ResourceFormUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onMediaTypeChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onPublishedChange: (Boolean) -> Unit,
    onToggleBracket: (String) -> Unit,
    onSave: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = if (state.isEdit) "Edit resource" else "Add resource",
                onBack = onBack,
            )
            when {
                state.isLoading -> LoadingShimmer(
                    modifier = Modifier.padding(20.dp),
                    rows = 4,
                )
                state.loadError != null -> ErrorState(message = state.loadError, onRetry = onRetry)
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = if (state.isEdit) "Keep this resource current." else "Share something useful.",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        text = "Published resources are visible to students. Drafts stay with staff.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                    )
                    AppTextField(
                        value = state.title,
                        onValueChange = onTitleChange,
                        label = "Title",
                        error = state.titleError,
                        enabled = !state.isSaving,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = state.description,
                        onValueChange = onDescriptionChange,
                        label = "Description",
                        error = state.descriptionError,
                        supportingText = "10–2,000 characters.",
                        singleLine = false,
                        maxLines = 5,
                        enabled = !state.isSaving,
                    )
                    Spacer(Modifier.height(10.dp))
                    DropdownField(
                        label = "Category",
                        options = AppConstants.concernCategories,
                        selected = state.category,
                        onSelect = onCategoryChange,
                        enabled = !state.isSaving,
                        error = state.categoryError,
                    )
                    Spacer(Modifier.height(10.dp))
                    DropdownField(
                        label = "Media type",
                        options = AppConstants.resourceMediaTypes,
                        selected = state.mediaType,
                        onSelect = onMediaTypeChange,
                        enabled = !state.isSaving,
                        error = state.mediaTypeError,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = state.contentUrl,
                        onValueChange = onUrlChange,
                        label = "Resource link",
                        error = state.urlError,
                        supportingText = "Must start with https:// or http://",
                        keyboardType = KeyboardType.Uri,
                        enabled = !state.isSaving,
                    )
                    Spacer(Modifier.height(16.dp))
                    SectionHeader(
                        title = "Wellbeing brackets",
                        subtitle = "Used by the private self-check recommender.",
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppConstants.stressBrackets.forEach { bracket ->
                            FilterChip(
                                selected = state.targetBrackets.contains(bracket),
                                onClick = { onToggleBracket(bracket) },
                                label = {
                                    Text(AppConstants.stressBracketLabels[bracket] ?: bracket)
                                },
                                enabled = !state.isSaving,
                            )
                        }
                    }
                    state.bracketsError?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Published", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Students can see this resource.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.isPublished,
                            onCheckedChange = onPublishedChange,
                            enabled = !state.isSaving,
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    PrimaryButton(
                        text = if (state.isEdit) "Save resource" else "Add resource",
                        onClick = {
                            focusManager.clearFocus()
                            onSave()
                        },
                        loading = state.isSaving,
                    )
                    Spacer(Modifier.height(16.dp))
                    InfoCard(
                        title = "Safe sharing",
                        body = "Use an https:// link to a trusted article, video, audio or PDF. Hotline resources are recommended only for the High self-check bracket.",
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
                .imePadding()
                .padding(16.dp),
        )
    }
}
