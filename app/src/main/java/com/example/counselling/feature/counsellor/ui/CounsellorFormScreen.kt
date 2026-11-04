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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import com.example.counselling.feature.counsellor.data.CounsellorRepository
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.ErrorState
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.LoadingShimmer
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.SectionHeader
import com.example.counselling.ui.components.showAppMessage
import kotlinx.coroutines.delay

@Composable
fun CounsellorFormRoute(
    counsellorId: String?,
    repository: CounsellorRepository,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val viewModel: CounsellorFormViewModel = viewModel(
        factory = CounsellorFormViewModel.factory(counsellorId, repository),
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

    CounsellorFormScreen(
        state = state,
        snackbarHostState = snackbarState,
        onBack = onBack,
        onRetry = viewModel::retry,
        onFullNameChange = viewModel::onFullNameChange,
        onEmailChange = viewModel::onEmailChange,
        onLanguagesChange = viewModel::onLanguagesChange,
        onBioChange = viewModel::onBioChange,
        onToggleSpecialisation = viewModel::toggleSpecialisation,
        onSave = viewModel::save,
    )
}

@Composable
private fun CounsellorFormScreen(
    state: CounsellorFormUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onLanguagesChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onToggleSpecialisation: (String) -> Unit,
    onSave: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = if (state.isEdit) "Edit counsellor" else "Add counsellor",
                onBack = onBack,
            )

            when {
                state.isLoading -> LoadingShimmer(
                    modifier = Modifier.padding(20.dp),
                    rows = 4,
                )

                state.loadError != null -> ErrorState(
                    message = state.loadError,
                    onRetry = onRetry,
                )

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = if (state.isEdit) {
                            "Keep the directory details current."
                        } else {
                            "Create the directory entry first."
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        text = "The counsellor role is assigned automatically when a matching Auth account signs up.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                    )

                    AppTextField(
                        value = state.fullName,
                        onValueChange = onFullNameChange,
                        label = "Full name",
                        error = state.fullNameError,
                        enabled = !state.isSaving,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = "University email",
                        error = state.emailError,
                        keyboardType = KeyboardType.Email,
                        enabled = !state.isSaving,
                    )
                    Spacer(Modifier.height(16.dp))
                    SectionHeader(
                        title = "Specialisations",
                        subtitle = "Used by Smart Counsellor Match.",
                    )
                    Spacer(Modifier.height(6.dp))
                    AppConstants.concernCategories.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            row.forEach { specialisation ->
                                FilterChip(
                                    selected = state.specialisations.contains(specialisation),
                                    onClick = { onToggleSpecialisation(specialisation) },
                                    label = { Text(specialisation) },
                                    modifier = Modifier.weight(1f),
                                    enabled = !state.isSaving,
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    state.specialisationsError?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    AppTextField(
                        value = state.languagesText,
                        onValueChange = onLanguagesChange,
                        label = "Languages",
                        error = state.languagesError,
                        supportingText = "Comma-separated, for example English, Bahasa Malaysia",
                        enabled = !state.isSaving,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = state.bio,
                        onValueChange = onBioChange,
                        label = "Short bio (optional)",
                        error = state.bioError,
                        supportingText = "Up to 1,000 characters.",
                        singleLine = false,
                        maxLines = 4,
                        enabled = !state.isSaving,
                    )
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(
                        text = if (state.isEdit) "Save changes" else "Add counsellor",
                        onClick = {
                            focusManager.clearFocus()
                            onSave()
                        },
                        loading = state.isSaving,
                    )
                    Spacer(Modifier.height(16.dp))
                    InfoCard(
                        title = "Auth account",
                        body = "Changing this directory email does not change an already-linked sign-in email. The counsellor creates their own Auth account with this address.",
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
