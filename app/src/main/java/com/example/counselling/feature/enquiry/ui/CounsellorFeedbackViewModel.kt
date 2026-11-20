// Module 6 | Owner: Michael
package com.example.counselling.feature.enquiry.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.enquiry.domain.Feedback
import com.example.counselling.feature.enquiry.domain.FeedbackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CounsellorFeedbackUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val feedback: List<Feedback> = emptyList(),
)

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class CounsellorFeedbackViewModel(
    private val repository: FeedbackRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CounsellorFeedbackUiState())
    val uiState: StateFlow<CounsellorFeedbackUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() = load(refresh = true)
    fun retry() = load()

    private fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.feedback.isEmpty(),
                    isRefreshing = refresh,
                    error = null,
                )
            }
            repository.getFeedback()
                .onSuccess { feedback ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            feedback = feedback,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error.message ?: "Could not load feedback.",
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(repository: FeedbackRepository) = viewModelFactory {
            initializer { CounsellorFeedbackViewModel(repository) }
        }
    }
}
