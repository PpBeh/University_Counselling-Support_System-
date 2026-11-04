// Team auth (Login) + Module 1 context | Owner: Beh
package com.example.counselling.feature.auth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.counselling.core.SessionManager
import com.example.counselling.core.UiMessage
import com.example.counselling.ui.components.AppSnackbarHost
import com.example.counselling.ui.components.AppTextField
import com.example.counselling.ui.components.PrimaryButton
import com.example.counselling.ui.components.showAppMessage

@Composable
fun LoginRoute(
    sessionManager: SessionManager,
    notice: UiMessage?,
    onNoticeConsumed: () -> Unit,
    onRegister: () -> Unit,
) {
    val viewModel: LoginViewModel = viewModel(
        factory = LoginViewModel.factory(sessionManager),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarState.showAppMessage(it) }
    }

    LaunchedEffect(notice) {
        if (notice != null) {
            onNoticeConsumed()
            snackbarState.showAppMessage(notice)
        }
    }

    LoginScreen(
        state = state,
        snackbarHostState = snackbarState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onEmailBlur = viewModel::onEmailBlur,
        onPasswordBlur = viewModel::onPasswordBlur,
        onSignIn = viewModel::signIn,
        onRegister = onRegister,
    )
}

@Composable
private fun LoginScreen(
    state: LoginUiState,
    snackbarHostState: SnackbarHostState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onEmailBlur: () -> Unit,
    onPasswordBlur: () -> Unit,
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(76.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            Text(
                text = "A calmer place to start.",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = "Book support, find practical resources and talk privately when you are ready.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Welcome back",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Sign in with your student or staff email.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                    )

                    AppTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = "Email",
                        enabled = !state.isLoading,
                        error = state.emailError,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        onFocusLost = onEmailBlur,
                    )
                    Spacer(Modifier.height(12.dp))
                    AppTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        label = "Password",
                        enabled = !state.isLoading,
                        isPassword = true,
                        error = state.passwordError,
                        supportingText = "8+ characters with upper, lower and a number",
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onSignIn()
                            },
                        ),
                        onFocusLost = onPasswordBlur,
                    )
                    Spacer(Modifier.height(20.dp))
                    PrimaryButton(
                        text = "Sign in",
                        onClick = {
                            focusManager.clearFocus()
                            onSignIn()
                        },
                        loading = state.isLoading,
                    )
                }
            }

            TextButton(
                onClick = onRegister,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("New here? Create a student account")
            }
            Text(
                text = "Private by design. Your session is protected by the university counselling service.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
        }

        AppSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}
