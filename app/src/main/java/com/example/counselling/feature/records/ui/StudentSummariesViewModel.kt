// Module 4 | Owner: Jeremy
// Students read only the safe student_session_summaries view.
package com.example.counselling.feature.records.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.feature.records.data.SessionRecordRepository
import com.example.counselling.feature.records.model.StudentSessionSummaryRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentSummariesUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val summaries: List<StudentSessionSummaryRow> = emptyList(),
)

class StudentSummariesViewModel(
    private val repository: SessionRecordRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentSummariesUiState())
    val uiState: StateFlow<StudentSummariesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() = load(refresh = true)
    fun retry() = load()

    private fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.summaries.isEmpty(),
                    isRefreshing = refresh,
                    error = null,
                )
            }
            repository.getStudentSummaries()
                .onSuccess { summaries ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            summaries = summaries,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error.message ?: "Could not load session summaries.",
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(repository: SessionRecordRepository) = viewModelFactory {
            initializer { StudentSummariesViewModel(repository) }
        }
    }
}
