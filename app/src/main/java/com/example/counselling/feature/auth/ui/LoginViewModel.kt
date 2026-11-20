// Module 1 / Team auth | Owner: Beh
// Login restores the polymorphic User through SessionManager; the screen never
// asks the user to choose a role.
package com.example.counselling.feature.auth.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.SessionManager
import com.example.counselling.core.Validators
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.Email
import com.example.counselling.core.domain.Password
import com.example.counselling.core.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
)

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class LoginViewModel(
    private val sessionManager: SessionManager,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

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
            val result = try {
                sessionManager.signIn(
                    Email.of(current.email),
                    Password.of(current.password),
                )
            } catch (error: DomainException) {
                Result.failure(error)
            }
            _uiState.update { it.copy(isLoading = false) }
            result.onFailure { error ->
                postError(error.message ?: "Sign in failed. Please try again.")
            }
        }
    }

    companion object {
        fun factory(sessionManager: SessionManager) = viewModelFactory {
            initializer { LoginViewModel(sessionManager) }
        }
    }
}
