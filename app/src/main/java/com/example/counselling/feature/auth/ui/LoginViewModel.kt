// Module 1 / Team auth | Owner: Beh
// Login uses the role returned by the database profile; the screen never asks
// the user to choose a role.
package com.example.counselling.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.MessageKind
import com.example.counselling.core.SessionManager
import com.example.counselling.core.UiMessage
import com.example.counselling.core.Validators
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
)

class LoginViewModel(
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null) }
    }

    fun onEmailBlur() {
        _uiState.update { it.copy(emailError = Validators.email(it.email)) }
    }

    fun onPasswordBlur() {
        _uiState.update { it.copy(passwordError = Validators.password(it.password)) }
    }

    fun signIn() {
        val current = _uiState.value
        val emailError = Validators.email(current.email)
        val passwordError = Validators.password(current.password)
        _uiState.update {
            it.copy(emailError = emailError, passwordError = passwordError)
        }
        if (emailError != null || passwordError != null) return
        if (current.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = sessionManager.signIn(current.email, current.password)
            _uiState.update { it.copy(isLoading = false) }
            result.onFailure { error ->
                _messages.send(
                    UiMessage(
                        text = error.message ?: "Sign in failed. Please try again.",
                        kind = MessageKind.ERROR,
                    ),
                )
            }
        }
    }

    companion object {
        fun factory(sessionManager: SessionManager) = viewModelFactory {
            initializer { LoginViewModel(sessionManager) }
        }
    }
}
