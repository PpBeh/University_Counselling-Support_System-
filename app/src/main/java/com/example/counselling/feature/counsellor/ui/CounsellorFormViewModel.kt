// Module 3 | Owner: Jeremy
package com.example.counselling.feature.counsellor.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.Validators
import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.Counsellor
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.Email
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.counsellor.domain.CounsellorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CounsellorFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val fullName: String = "",
    val email: String = "",
    val specialisations: List<String> = emptyList(),
    val languagesText: String = "",
    val bio: String = "",
    val fullNameError: String? = null,
    val emailError: String? = null,
    val specialisationsError: String? = null,
    val languagesError: String? = null,
    val bioError: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
)

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class CounsellorFormViewModel(
    private val counsellorId: String?,
    private val repository: CounsellorRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        CounsellorFormUiState(isEdit = counsellorId != null, isLoading = counsellorId != null),
    )
    val uiState: StateFlow<CounsellorFormUiState> = _uiState.asStateFlow()

    private var currentIsActive = true

    init {
        if (counsellorId != null) load()
    }

    fun retry() = load()

    fun onFullNameChange(value: String) =
        _uiState.update { it.copy(fullName = value, fullNameError = null) }

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, emailError = null) }

    fun onLanguagesChange(value: String) =
        _uiState.update { it.copy(languagesText = value, languagesError = null) }

    fun onBioChange(value: String) =
        _uiState.update { it.copy(bio = value, bioError = null) }

    fun toggleSpecialisation(value: String) {
        _uiState.update { state ->
            val next = if (state.specialisations.contains(value)) {
                state.specialisations - value
            } else {
                state.specialisations + value
            }
            state.copy(specialisations = next, specialisationsError = null)
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        val nameError = Validators.fullName(state.fullName)
        val emailError = Validators.email(state.email)
        val specialisationsError = when {
            state.specialisations.isEmpty() -> "Choose at least one specialisation."
            state.specialisations.size > 8 -> "Choose no more than 8 specialisations."
            else -> null
        }
        val languages = parseLanguages(state.languagesText)
        val languagesError = when {
            languages.isEmpty() -> "Add at least one language."
            languages.size > 10 -> "Add no more than 10 languages."
            languages.any { it.length > 40 } -> "Each language must be 40 characters or fewer."
            else -> null
        }
        val bioError = if (state.bio.length > 1000) {
            "Bio must be 1,000 characters or fewer."
        } else {
            null
        }
        _uiState.update {
            it.copy(
                fullNameError = nameError,
                emailError = emailError,
                specialisationsError = specialisationsError,
                languagesError = languagesError,
                bioError = bioError,
            )
        }
        if (listOf(nameError, emailError, specialisationsError, languagesError, bioError)
                .any { it != null }
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = try {
                val email = Email.of(state.email)
                val categories = state.specialisations.map { value ->
                    ConcernCategory.fromWire(value)
                        ?: throw DomainException("Unknown specialisation: $value")
                }
                if (counsellorId == null) {
                    repository.createCounsellor(
                        fullName = state.fullName,
                        email = email,
                        specialisations = categories,
                        languages = languages,
                        bio = state.bio,
                    )
                } else {
                    repository.updateCounsellor(
                        counsellorId = counsellorId,
                        fullName = state.fullName,
                        email = email,
                        specialisations = categories,
                        languages = languages,
                        bio = state.bio,
                        isActive = currentIsActive,
                    )
                }
            } catch (error: DomainException) {
                Result.failure<Counsellor>(error)
            }
            _uiState.update { it.copy(isSaving = false) }
            result.onSuccess {
                _uiState.update { it.copy(saveSuccess = true) }
                postSuccess(
                    if (counsellorId == null) {
                        "Counsellor added. Their Auth account can be created using this email."
                    } else {
                        "Counsellor details updated."
                    },
                )
            }.onFailure {
                postError(it.message ?: "Could not save the counsellor.")
            }
        }
    }

    private fun load() {
        val id = counsellorId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            repository.getCounsellor(id)
                .onSuccess { counsellor ->
                    if (counsellor == null) {
                        _uiState.update {
                            it.copy(isLoading = false, loadError = "That counsellor could not be found.")
                        }
                    } else {
                        currentIsActive = counsellor.isActive
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                fullName = counsellor.fullName,
                                email = counsellor.email.value,
                                specialisations = counsellor.specialisations.map { category -> category.label },
                                languagesText = counsellor.languages.joinToString(", "),
                                bio = counsellor.bio,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            loadError = error.message ?: "Could not load the counsellor.",
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(counsellorId: String?, repository: CounsellorRepository) =
            viewModelFactory {
                initializer { CounsellorFormViewModel(counsellorId, repository) }
            }
    }
}

private fun parseLanguages(raw: String): List<String> = raw
    .split(',')
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .distinct()
