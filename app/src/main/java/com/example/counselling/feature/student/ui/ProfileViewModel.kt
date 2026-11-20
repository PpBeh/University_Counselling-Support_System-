// Module 1 | Owner: Beh
// Own-profile editing, password change and account deactivation.
package com.example.counselling.feature.student.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.AppConstants
import com.example.counselling.core.SessionManager
import com.example.counselling.core.Validators
import com.example.counselling.core.domain.Administrator
import com.example.counselling.core.domain.AppointmentStatus
import com.example.counselling.core.domain.Counsellor
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.Password
import com.example.counselling.core.domain.PhoneNumber
import com.example.counselling.core.domain.SessionMode
import com.example.counselling.core.domain.Student
import com.example.counselling.core.domain.User
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.appointment.domain.AppointmentRepository
import com.example.counselling.feature.auth.domain.AuthRepository
import com.example.counselling.feature.student.domain.StudentRepository
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val fullName: String = "",
    val email: String = "",
    val roleLabel: String = "Student",
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
    val isStudent: Boolean = false,
)

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class ProfileViewModel(
    private val sessionManager: SessionManager,
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository,
    private val appointmentRepository: AppointmentRepository,
) : BaseViewModel() {

    private val user: User? = sessionManager.currentUser
    private val student: Student? = user as? Student

    private val _uiState = MutableStateFlow(user.toUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

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
        if (student == null || current.isSavingProfile) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true) }
            val result = try {
                studentRepository.updateProfile(
                    userId = student.id,
                    fullName = current.fullName,
                    programme = current.programme,
                    phone = PhoneNumber.of(current.phone),
                    preferredMode = SessionMode.fromWire(current.preferredMode)
                        ?: throw DomainException("Choose a preferred session mode."),
                )
            } catch (error: DomainException) {
                Result.failure<Student>(error)
            }
            _uiState.update { it.copy(isSavingProfile = false) }
            result.onSuccess { updated ->
                sessionManager.refreshProfile()
                _uiState.update { it.fromStudent(updated) }
                postSuccess("Profile updated.")
            }.onFailure {
                postError(it.message ?: "Could not update your profile.")
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
            val result = try {
                authRepository.updatePassword(Password.of(current.newPassword))
            } catch (error: DomainException) {
                Result.failure<Unit>(error)
            }
            _uiState.update {
                it.copy(
                    isChangingPassword = false,
                    newPassword = "",
                    confirmPassword = "",
                )
            }
            result.onSuccess {
                postSuccess("Password changed successfully.")
            }.onFailure {
                postError(it.message ?: "Could not change your password.")
            }
        }
    }

    fun requestDeactivation() {
        if (_uiState.value.isDeactivating) return
        viewModelScope.launch {
            val appointments = appointmentRepository.getAppointments().getOrElse { error ->
                postError(error.message ?: "Could not check your upcoming appointments.")
                return@launch
            }
            val now = Instant.now()
            val upcoming = appointments.count {
                it.status in setOf(AppointmentStatus.Pending, AppointmentStatus.Confirmed) &&
                    it.slot.start.isAfter(now)
            }
            if (upcoming > 0) {
                postError(
                    "Cancel your $upcoming upcoming appointment" +
                        (if (upcoming == 1) "" else "s") +
                        " before deactivating your account.",
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
            val result = studentRepository.deactivateAccount()
            _uiState.update { it.copy(isDeactivating = false) }
            result.onSuccess {
                // Refreshing detects is_active = false, signs out and shows the
                // deactivated message on the login screen.
                sessionManager.refreshProfile()
            }.onFailure {
                postError(it.message ?: "Could not deactivate your account.")
            }
        }
    }

    companion object {
        fun factory(
            sessionManager: SessionManager,
            studentRepository: StudentRepository,
            authRepository: AuthRepository,
            appointmentRepository: AppointmentRepository,
        ) = viewModelFactory {
            initializer {
                ProfileViewModel(
                    sessionManager,
                    studentRepository,
                    authRepository,
                    appointmentRepository,
                )
            }
        }
    }
}

private fun User?.toUiState(): ProfileUiState {
    if (this == null) return ProfileUiState()
    return ProfileUiState(
        fullName = fullName,
        email = email.value,
        roleLabel = when (this) {
            is Student -> "Student"
            is Counsellor -> "Counsellor"
            is Administrator -> "Administrator"
        },
        studentId = (this as? Student)?.studentId?.value.orEmpty(),
        programme = (this as? Student)?.programme.orEmpty(),
        phone = phone?.value.orEmpty(),
        preferredMode = (this as? Student)?.preferredMode?.code
            ?: AppConstants.sessionModes.first(),
        isStudent = this is Student,
    )
}

private fun ProfileUiState.fromStudent(student: Student): ProfileUiState = copy(
    fullName = student.fullName,
    email = student.email.value,
    studentId = student.studentId.value,
    programme = student.programme,
    phone = student.phone?.value.orEmpty(),
    preferredMode = student.preferredMode.code,
    isStudent = true,
)
