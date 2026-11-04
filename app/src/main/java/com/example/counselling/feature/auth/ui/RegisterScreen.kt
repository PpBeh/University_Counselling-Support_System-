// Module 1 | Owner: Beh
package com.example.counselling.feature.auth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.AppConstants
import com.example.counselling.core.SessionManager
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.DropdownField
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.SecondaryButton
import com.example.counselling.ui.components.showAppMessage

@Composable
fun RegisterRoute(
    sessionManager: SessionManager,
    onBackToLogin: () -> Unit,
) {
    val viewModel: RegisterViewModel = viewModel(
        factory = RegisterViewModel.factory(sessionManager),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }

    RegisterScreen(
        state = state,
        snackbarHostState = snackbarState,
        onBack = onBackToLogin,
        onFullNameChange = viewModel::onFullNameChange,
        onStudentIdChange = viewModel::onStudentIdChange,
        onProgrammeChange = viewModel::onProgrammeChange,
        onPhoneChange = viewModel::onPhoneChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onPreferredModeChange = viewModel::onPreferredModeChange,
        onFullNameBlur = viewModel::onFullNameBlur,
        onStudentIdBlur = viewModel::onStudentIdBlur,
        onProgrammeBlur = viewModel::onProgrammeBlur,
        onPhoneBlur = viewModel::onPhoneBlur,
        onEmailBlur = viewModel::onEmailBlur,
        onPasswordBlur = viewModel::onPasswordBlur,
        onConfirmPasswordBlur = viewModel::onConfirmPasswordBlur,
        onSubmit = viewModel::submit,
        onGoToLogin = onBackToLogin,
    )
}

@Composable
private fun RegisterScreen(
    state: RegisterUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onStudentIdChange: (String) -> Unit,
    onProgrammeChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPreferredModeChange: (String) -> Unit,
    onFullNameBlur: () -> Unit,
    onStudentIdBlur: () -> Unit,
    onProgrammeBlur: () -> Unit,
    onPhoneBlur: () -> Unit,
    onEmailBlur: () -> Unit,
    onPasswordBlur: () -> Unit,
    onConfirmPasswordBlur: () -> Unit,
    onSubmit: () -> Unit,
    onGoToLogin: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.statusBarsPadding()) {
                AppTopBar(title = "Create account", onBack = onBack)
            }

            if (state.requiresEmailConfirmation) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(24.dp),
                ) {
                    InfoCard(
                        title = "Check your email",
                        body = "Your account has been created. Confirm your email address, then sign in with your new password.",
                        icon = Icons.Outlined.Email,
                    )
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(
                        text = "Back to sign in",
                        onClick = onGoToLogin,
                    )
                }
                return@Column
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Your details stay private.",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Create a student account to book counselling and use the self-help library.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
                )

                AppTextField(
                    value = state.fullName,
                    onValueChange = onFullNameChange,
                    label = "Full name",
                    enabled = !state.isLoading,
                    error = state.fullNameError,
                    onFocusLost = onFullNameBlur,
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.studentId,
                    onValueChange = onStudentIdChange,
                    label = "Student ID",
                    enabled = !state.isLoading,
                    error = state.studentIdError,
                    onFocusLost = onStudentIdBlur,
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.programme,
                    onValueChange = onProgrammeChange,
                    label = "Programme",
                    enabled = !state.isLoading,
                    error = state.programmeError,
                    onFocusLost = onProgrammeBlur,
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.phone,
                    onValueChange = onPhoneChange,
                    label = "Malaysian phone number",
                    enabled = !state.isLoading,
                    error = state.phoneError,
                    supportingText = "For example 012-3456789 or +60123456789",
                    keyboardType = KeyboardType.Phone,
                    onFocusLost = onPhoneBlur,
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = "Email",
                    enabled = !state.isLoading,
                    error = state.emailError,
                    keyboardType = KeyboardType.Email,
                    onFocusLost = onEmailBlur,
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    enabled = !state.isLoading,
                    isPassword = true,
                    error = state.passwordError,
                    supportingText = "8+ characters with uppercase, lowercase and a number",
                    onFocusLost = onPasswordBlur,
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = "Confirm password",
                    enabled = !state.isLoading,
                    isPassword = true,
                    error = state.confirmPasswordError,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onSubmit()
                        },
                    ),
                    onFocusLost = onConfirmPasswordBlur,
                )
                Spacer(Modifier.height(10.dp))
                DropdownField(
                    label = "Preferred session mode",
                    options = AppConstants.sessionModes.map {
                        AppConstants.sessionModeLabels[it] ?: it
                    },
                    selected = AppConstants.sessionModeLabels[state.preferredMode]
                        ?: state.preferredMode,
                    onSelect = { label ->
                        val wireValue = AppConstants.sessionModeLabels.entries
                            .firstOrNull { it.value == label }
                            ?.key
                            ?: AppConstants.sessionModes.first()
                        onPreferredModeChange(wireValue)
                    },
                    enabled = !state.isLoading,
                    error = state.preferredModeError,
                )

                Spacer(Modifier.height(20.dp))
                PrimaryButton(
                    text = "Create my account",
                    onClick = {
                        focusManager.clearFocus()
                        onSubmit()
                    },
                    loading = state.isLoading,
                )
                Spacer(Modifier.height(8.dp))
                SecondaryButton(
                    text = "I already have an account",
                    onClick = onBack,
                    enabled = !state.isLoading,
                )
                Spacer(Modifier.height(24.dp))
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
