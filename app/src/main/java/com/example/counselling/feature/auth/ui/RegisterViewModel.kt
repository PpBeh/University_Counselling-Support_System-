// Module 1 | Owner: Beh
// Self-registration always becomes a student unless the database trigger finds
// the email in the counsellor directory or the admin whitelist.
package com.example.counselling.feature.auth.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.AppConstants
import com.example.counselling.core.SessionManager
import com.example.counselling.core.Validators
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.Email
import com.example.counselling.core.domain.Password
import com.example.counselling.core.domain.PhoneNumber
import com.example.counselling.core.domain.SessionMode
import com.example.counselling.core.domain.StudentId
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.auth.domain.RegistrationDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val fullName: String = "",
    val studentId: String = "",
    val programme: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val preferredMode: String = AppConstants.sessionModes.first(),
    val fullNameError: String? = null,
    val studentIdError: String? = null,
    val programmeError: String? = null,
    val phoneError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val preferredModeError: String? = null,
    val isLoading: Boolean = false,
    val requiresEmailConfirmation: Boolean = false,
)

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class RegisterViewModel(
    private val sessionManager: SessionManager,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value, fullNameError = null) }
    }

    fun onStudentIdChange(value: String) {
        _uiState.update { it.copy(studentId = value, studentIdError = null) }
    }

    fun onProgrammeChange(value: String) {
        _uiState.update { it.copy(programme = value, programmeError = null) }
    }

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phone = value, phoneError = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null) }
    }

    fun onPreferredModeChange(value: String) {
        _uiState.update { it.copy(preferredMode = value, preferredModeError = null) }
    }

    fun onFullNameBlur() = _uiState.update {
        it.copy(fullNameError = Validators.fullName(it.fullName))
    }

    fun onStudentIdBlur() = _uiState.update {
        it.copy(studentIdError = Validators.studentId(it.studentId))
    }

    fun onProgrammeBlur() = _uiState.update {
        it.copy(programmeError = Validators.programme(it.programme))
    }

    fun onPhoneBlur() = _uiState.update {
        it.copy(phoneError = Validators.phone(it.phone))
    }

    fun onEmailBlur() = _uiState.update {
        it.copy(emailError = Validators.email(it.email))
    }

    fun onPasswordBlur() = _uiState.update {
        it.copy(passwordError = Validators.password(it.password))
    }

    fun onConfirmPasswordBlur() = _uiState.update {
        it.copy(
            confirmPasswordError = Validators.confirmPassword(
                it.password,
                it.confirmPassword,
            ),
        )
    }

    fun submit() {
        val state = _uiState.value
        val errors = RegistrationErrors(
            fullName = Validators.fullName(state.fullName),
            studentId = Validators.studentId(state.studentId),
            programme = Validators.programme(state.programme),
            phone = Validators.phone(state.phone),
            email = Validators.email(state.email),
            password = Validators.password(state.password),
            confirmPassword = Validators.confirmPassword(state.password, state.confirmPassword),
            preferredMode = Validators.preferredMode(state.preferredMode),
        )

        _uiState.update {
            it.copy(
                fullNameError = errors.fullName,
                studentIdError = errors.studentId,
                programmeError = errors.programme,
                phoneError = errors.phone,
                emailError = errors.email,
                passwordError = errors.password,
                confirmPasswordError = errors.confirmPassword,
                preferredModeError = errors.preferredMode,
            )
        }

        if (!errors.isValid || state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = try {
                sessionManager.signUp(
                    RegistrationDetails(
                        email = Email.of(state.email),
                        password = Password.of(state.password),
                        fullName = state.fullName.trim(),
                        studentId = StudentId.of(state.studentId),
                        programme = state.programme.trim(),
                        phone = PhoneNumber.of(state.phone),
                        preferredMode = SessionMode.fromWire(state.preferredMode)
                            ?: throw DomainException("Choose a preferred session mode."),
                    ),
                )
            } catch (error: DomainException) {
                Result.failure<Boolean>(error)
            }
            _uiState.update { it.copy(isLoading = false) }

            result.onSuccess { signedIn ->
                if (signedIn) {
                    postSuccess("Account created. Welcome to Counselling Support.")
                } else {
                    _uiState.update { it.copy(requiresEmailConfirmation = true) }
                    postSuccess("Account created. Confirm your email, then sign in.")
                }
            }.onFailure { error ->
                postError(error.message ?: "Registration failed. Please try again.")
            }
        }
    }

    companion object {
        fun factory(sessionManager: SessionManager) = viewModelFactory {
            initializer { RegisterViewModel(sessionManager) }
        }
    }
}

private data class RegistrationErrors(
    val fullName: String?,
    val studentId: String?,
    val programme: String?,
    val phone: String?,
    val email: String?,
    val password: String?,
    val confirmPassword: String?,
    val preferredMode: String?,
) {
    val isValid: Boolean
        get() = listOf(
            fullName,
            studentId,
            programme,
            phone,
            email,
            password,
            confirmPassword,
            preferredMode,
        ).all { it == null }
}
