// Module 1 | Owner: Beh
// Read, update, password change and account deactivation for the signed-in user.
package com.example.counselling.feature.student.ui

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.auth.data.AuthRepository
import com.example.counselling.feature.student.data.ProfileRepository
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.AppTopBar
import com.example.counselling.ui.components.ConfirmDialog
import com.example.counselling.ui.components.DropdownField
import com.example.counselling.ui.components.InfoCard
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.SecondaryButton
import com.example.counselling.ui.components.SectionHeader
import com.example.counselling.ui.components.StatusChip
import com.example.counselling.ui.components.UserAvatar
import com.example.counselling.ui.components.showAppMessage
import com.example.counselling.ui.theme.SoftError

@Composable
fun ProfileRoute(
    sessionManager: SessionManager,
    profileRepository: ProfileRepository,
    authRepository: AuthRepository,
    appointmentRepository: AppointmentRepository,
    onSignOut: () -> Unit,
) {
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.factory(
            sessionManager = sessionManager,
            profileRepository = profileRepository,
            authRepository = authRepository,
            appointmentRepository = appointmentRepository,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }

    ProfileScreen(
        state = state,
        snackbarHostState = snackbarState,
        onFullNameChange = viewModel::onFullNameChange,
        onProgrammeChange = viewModel::onProgrammeChange,
        onPhoneChange = viewModel::onPhoneChange,
        onPreferredModeChange = viewModel::onPreferredModeChange,
        onFullNameBlur = viewModel::onFullNameBlur,
        onProgrammeBlur = viewModel::onProgrammeBlur,
        onPhoneBlur = viewModel::onPhoneBlur,
        onSaveProfile = viewModel::saveProfile,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onNewPasswordBlur = viewModel::onNewPasswordBlur,
        onConfirmPasswordBlur = viewModel::onConfirmPasswordBlur,
        onChangePassword = viewModel::changePassword,
        onRequestDeactivation = viewModel::requestDeactivation,
        onDismissDeactivation = viewModel::dismissDeactivationDialog,
        onDeactivateAccount = viewModel::deactivateAccount,
        onSignOut = onSignOut,
    )
}

@Composable
private fun ProfileScreen(
    state: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    onFullNameChange: (String) -> Unit,
    onProgrammeChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPreferredModeChange: (String) -> Unit,
    onFullNameBlur: () -> Unit,
    onProgrammeBlur: () -> Unit,
    onPhoneBlur: () -> Unit,
    onSaveProfile: () -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onNewPasswordBlur: () -> Unit,
    onConfirmPasswordBlur: () -> Unit,
    onChangePassword: () -> Unit,
    onRequestDeactivation: () -> Unit,
    onDismissDeactivation: () -> Unit,
    onDeactivateAccount: () -> Unit,
    onSignOut: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = "Profile")

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        UserAvatar(
                            initials = initialsFor(state.fullName),
                            size = 64.dp,
                            contentDescription = "Profile initials",
                        )
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(
                                text = state.fullName.ifBlank { "Your profile" },
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = state.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(modifier = Modifier.padding(top = 8.dp)) {
                                StatusChip(
                                    status = state.role.replaceFirstChar { it.uppercase() },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                SectionHeader(title = "Your details")
                Spacer(Modifier.height(10.dp))

                if (state.isStudent) {
                    InfoCard(
                        title = "Student information",
                        icon = Icons.Outlined.School,
                    ) {
                        ReadOnlyLine("Student ID", state.studentId)
                        ReadOnlyLine("Email", state.email)
                    }
                    Spacer(Modifier.height(14.dp))

                    AppTextField(
                        value = state.fullName,
                        onValueChange = onFullNameChange,
                        label = "Full name",
                        error = state.fullNameError,
                        enabled = !state.isSavingProfile,
                        onFocusLost = onFullNameBlur,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = state.programme,
                        onValueChange = onProgrammeChange,
                        label = "Programme",
                        error = state.programmeError,
                        enabled = !state.isSavingProfile,
                        onFocusLost = onProgrammeBlur,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = state.phone,
                        onValueChange = onPhoneChange,
                        label = "Malaysian phone number",
                        error = state.phoneError,
                        supportingText = "For example 012-3456789 or +60123456789",
                        keyboardType = KeyboardType.Phone,
                        enabled = !state.isSavingProfile,
                        onFocusLost = onPhoneBlur,
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
                        enabled = !state.isSavingProfile,
                        error = state.preferredModeError,
                    )
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(
                        text = "Save profile",
                        onClick = {
                            focusManager.clearFocus()
                            onSaveProfile()
                        },
                        loading = state.isSavingProfile,
                    )

                    Spacer(Modifier.height(24.dp))
                    SectionHeader(
                        title = "Change password",
                        subtitle = "Use at least 8 characters with upper, lower and a number.",
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = state.newPassword,
                        onValueChange = onNewPasswordChange,
                        label = "New password",
                        isPassword = true,
                        error = state.newPasswordError,
                        enabled = !state.isChangingPassword,
                        onFocusLost = onNewPasswordBlur,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = state.confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = "Confirm new password",
                        isPassword = true,
                        error = state.confirmPasswordError,
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onChangePassword()
                            },
                        ),
                        enabled = !state.isChangingPassword,
                        onFocusLost = onConfirmPasswordBlur,
                    )
                    Spacer(Modifier.height(16.dp))
                    SecondaryButton(
                        text = "Change password",
                        onClick = {
                            focusManager.clearFocus()
                            onChangePassword()
                        },
                        loading = state.isChangingPassword,
                    )

                    Spacer(Modifier.height(24.dp))
                    SectionHeader(title = "Account")
                    Spacer(Modifier.height(10.dp))
                    InfoCard(
                        title = "Deactivate my account",
                        body = "Your account will be signed out and you will not be able to sign in again. Upcoming appointments must be cancelled first.",
                        icon = Icons.Outlined.Lock,
                        actionLabel = "Deactivate account",
                        onAction = onRequestDeactivation,
                        actionEnabled = !state.isDeactivating,
                        accentColor = SoftError,
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    )
                } else {
                    InfoCard(
                        title = "Staff account",
                        body = "Your counsellor or administrator profile is managed by the service. Contact the administrator if your details need updating.",
                        icon = Icons.Outlined.Email,
                    )
                }

                Spacer(Modifier.height(24.dp))
                PrimaryButton(
                    text = "Sign out",
                    onClick = onSignOut,
                    icon = Icons.Outlined.Logout,
                )
                Spacer(Modifier.height(28.dp))
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

    if (state.showDeactivationDialog) {
        ConfirmDialog(
            title = "Deactivate account?",
            message = "You will be signed out immediately. This cannot be undone in the app; ask the counselling office to reactivate you.",
            confirmLabel = "Deactivate",
            onConfirm = onDeactivateAccount,
            onDismiss = onDismissDeactivation,
        )
    }
}

@Composable
private fun ReadOnlyLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1.4f),
        )
    }
}

private fun initialsFor(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts.first().take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}
