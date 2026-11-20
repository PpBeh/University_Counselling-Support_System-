// Module 1 | Owner: Beh
// Staff-only detail screen for the permitted parts of a student profile.
package com.example.counselling.feature.student.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.UiState
import com.example.counselling.core.domain.Student
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.student.domain.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class StudentDetailViewModel(
    private val studentId: String,
    private val studentRepository: StudentRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<UiState<Student>>(UiState.Loading)
    val uiState: StateFlow<UiState<Student>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            studentRepository.getStudent(studentId)
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
        fun factory(studentId: String, studentRepository: StudentRepository) = viewModelFactory {
            initializer { StudentDetailViewModel(studentId, studentRepository) }
        }
    }
}
