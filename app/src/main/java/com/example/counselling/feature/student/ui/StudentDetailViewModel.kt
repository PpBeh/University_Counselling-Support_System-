// Module 1 | Owner: Beh
// Staff-only detail screen for the permitted parts of a student profile.
package com.example.counselling.feature.student.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.UiState
import com.example.counselling.feature.student.data.ProfileRepository
import com.example.counselling.feature.student.model.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudentDetailViewModel(
    private val studentId: String,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Profile>>(UiState.Loading)
    val uiState: StateFlow<UiState<Profile>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            profileRepository.getProfile(studentId)
                .onSuccess { profile ->
                    _uiState.value = if (profile == null) {
                        UiState.Error("That student profile could not be found.")
                    } else {
                        UiState.Success(profile)
                    }
                }
                .onFailure {
                    _uiState.value = UiState.Error(
                        it.message ?: "Could not load the student profile.",
                    )
                }
        }
    }

    companion object {
        fun factory(studentId: String, profileRepository: ProfileRepository) = viewModelFactory {
            initializer { StudentDetailViewModel(studentId, profileRepository) }
        }
    }
}
