// Module 1 | Owner: Beh
// Own-profile editing, password change and account deactivation.
package com.example.counselling.feature.student.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.AppConstants
import com.example.counselling.core.MessageKind
import com.example.counselling.core.DateTimeUtils
import com.example.counselling.core.SessionManager
import com.example.counselling.core.UiMessage
import com.example.counselling.core.Validators
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.auth.data.AuthRepository
import com.example.counselling.feature.student.data.ProfileRepository
import com.example.counselling.feature.student.model.Profile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val fullName: String = "",
    val email: String = "",
    val role: String = "student",
    val studentId: String = "",
    val programme: String = "",
    val phone: String = "",
    val preferredMode: String = AppConstants.sessionModes.first(),
    val fullNameError: String? = null,
    val programmeError: String? = null,
    val phoneError: String? = null,
    val preferredModeError: String? = null,
    val newPassword: String = "",
    val confirmPassword: String = "",
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val isSavingProfile: Boolean = false,
    val isChangingPassword: Boolean = false,
    val isDeactivating: Boolean = false,
    val showDeactivationDialog: Boolean = false,
) {
    val isStudent: Boolean get() = role == "student"
}

class ProfileViewModel(
    private val sessionManager: SessionManager,
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val profile: Profile? = sessionManager.currentProfile
    private val _uiState = MutableStateFlow(profile.toUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value, fullNameError = null) }
    }

    fun onProgrammeChange(value: String) {
        _uiState.update { it.copy(programme = value, programmeError = null) }
    }

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phone = value, phoneError = null) }
    }

    fun onPreferredModeChange(value: String) {
        _uiState.update { it.copy(preferredMode = value, preferredModeError = null) }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, newPasswordError = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null) }
    }

    fun onNewPasswordBlur() = _uiState.update {
        it.copy(newPasswordError = Validators.password(it.newPassword))
    }

    fun onConfirmPasswordBlur() = _uiState.update {
        it.copy(
            confirmPasswordError = Validators.confirmPassword(
                it.newPassword,
                it.confirmPassword,
            ),
        )
    }

    fun onFullNameBlur() = _uiState.update {
        it.copy(fullNameError = Validators.fullName(it.fullName))
    }

    fun onProgrammeBlur() = _uiState.update {
        it.copy(programmeError = Validators.programme(it.programme))
    }

    fun onPhoneBlur() = _uiState.update {
        it.copy(phoneError = Validators.phone(it.phone))
    }

    fun saveProfile() {
        val current = _uiState.value
        val nameError = Validators.fullName(current.fullName)
        val programmeError = Validators.programme(current.programme)
        val phoneError = Validators.phone(current.phone)
        val modeError = Validators.preferredMode(current.preferredMode)
        _uiState.update {
            it.copy(
                fullNameError = nameError,
                programmeError = programmeError,
                phoneError = phoneError,
                preferredModeError = modeError,
            )
        }
        if (listOf(nameError, programmeError, phoneError, modeError).any { it != null }) return
        if (!current.isStudent || current.isSavingProfile) return
        val profileId = profile?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true) }
            val result = profileRepository.updateStudentProfile(
                userId = profileId,
                fullName = current.fullName,
                programme = current.programme,
                phone = current.phone,
                preferredMode = current.preferredMode,
            )
            _uiState.update { it.copy(isSavingProfile = false) }
            result.onSuccess { updated ->
                sessionManager.refreshProfile()
                _uiState.update {
                    it.copy(
                        fullName = updated.fullName,
                        programme = updated.programme.orEmpty(),
                        phone = updated.phone.orEmpty(),
                        preferredMode = updated.preferredSessionMode
                            ?: AppConstants.sessionModes.first(),
                    )
                }
                _messages.send(UiMessage("Profile updated.", MessageKind.SUCCESS))
            }.onFailure {
                _messages.send(
                    UiMessage(
                        it.message ?: "Could not update your profile.",
                        MessageKind.ERROR,
                    ),
                )
            }
        }
    }

    fun changePassword() {
        val current = _uiState.value
        val passwordError = Validators.password(current.newPassword)
        val confirmError = Validators.confirmPassword(
            current.newPassword,
            current.confirmPassword,
        )
        _uiState.update {
            it.copy(
                newPasswordError = passwordError,
                confirmPasswordError = confirmError,
            )
        }
        if (passwordError != null || confirmError != null || current.isChangingPassword) return

        viewModelScope.launch {
            _uiState.update { it.copy(isChangingPassword = true) }
            val result = authRepository.updatePassword(current.newPassword)
            _uiState.update {
                it.copy(
                    isChangingPassword = false,
                    newPassword = "",
                    confirmPassword = "",
                )
            }
            result.onSuccess {
                _messages.send(UiMessage("Password changed successfully.", MessageKind.SUCCESS))
            }.onFailure {
                _messages.send(
                    UiMessage(
                        it.message ?: "Could not change your password.",
                        MessageKind.ERROR,
                    ),
                )
            }
        }
    }

    fun requestDeactivation() {
        if (_uiState.value.isDeactivating) return
        viewModelScope.launch {
            val appointments = appointmentRepository.getAppointments().getOrElse { error ->
                _messages.send(
                    UiMessage(
                        error.message ?: "Could not check your upcoming appointments.",
                        MessageKind.ERROR,
                    ),
                )
                return@launch
            }
            val upcoming = appointments.count {
                it.status in listOf("pending", "confirmed") &&
                    DateTimeUtils.isFuture(it.slotStart)
            }
            if (upcoming > 0) {
                _messages.send(
                    UiMessage(
                        "Cancel your $upcoming upcoming appointment" +
                            (if (upcoming == 1) "" else "s") +
                            " before deactivating your account.",
                        MessageKind.ERROR,
                    ),
                )
            } else {
                _uiState.update { it.copy(showDeactivationDialog = true) }
            }
        }
    }

    fun dismissDeactivationDialog() {
        _uiState.update { it.copy(showDeactivationDialog = false) }
    }

    fun deactivateAccount() {
        if (_uiState.value.isDeactivating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeactivating = true) }
            val result = profileRepository.deactivateAccount()
            _uiState.update { it.copy(isDeactivating = false) }
            result.onSuccess {
                // Refreshing detects is_active = false, signs out and shows the
                // deactivated message on the login screen.
                sessionManager.refreshProfile()
            }.onFailure {
                _messages.send(
                    UiMessage(
                        it.message ?: "Could not deactivate your account.",
                        MessageKind.ERROR,
                    ),
                )
            }
        }
    }

    companion object {
        fun factory(
            sessionManager: SessionManager,
            profileRepository: ProfileRepository,
            authRepository: AuthRepository,
            appointmentRepository: AppointmentRepository,
        ) = viewModelFactory {
            initializer {
                ProfileViewModel(
                    sessionManager,
                    profileRepository,
                    authRepository,
                    appointmentRepository,
                )
            }
        }
    }
}

private fun Profile?.toUiState(): ProfileUiState {
    if (this == null) {
        return ProfileUiState()
    }
    return ProfileUiState(
        fullName = fullName,
        email = email,
        role = role,
        studentId = studentId.orEmpty(),
        programme = programme.orEmpty(),
        phone = phone.orEmpty(),
        preferredMode = preferredSessionMode ?: AppConstants.sessionModes.first(),
    )
}
