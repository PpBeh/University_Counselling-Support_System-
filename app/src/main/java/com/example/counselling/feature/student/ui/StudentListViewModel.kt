// Module 1 | Owner: Beh
// Counsellors and admins can search the student directory by name or ID.
package com.example.counselling.feature.student.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.domain.Student
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.student.domain.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val students: List<Student> = emptyList(),
)

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class StudentListViewModel(
    private val studentRepository: StudentRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(StudentListUiState())
    val uiState: StateFlow<StudentListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun refresh() = load(refresh = true)

    fun retry() = load()

    private fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.students.isEmpty(),
                    isRefreshing = refresh,
                    error = null,
                )
            }
            studentRepository.getStudents()
                .onSuccess { students ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            students = students,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error.message ?: "Could not load students.",
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(studentRepository: StudentRepository) = viewModelFactory {
            initializer { StudentListViewModel(studentRepository) }
        }
    }
}
