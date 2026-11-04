// Module 5 / Unique Feature 3 | Owner: Michael
// The 10 answers are held only in memory. Nothing is written to Supabase.
package com.example.counselling.feature.resource.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.feature.resource.data.ResourceRepository
import com.example.counselling.feature.resource.model.ResourceRow
import com.example.counselling.feature.resource.model.SelfCheckContent
import com.example.counselling.feature.resource.model.SelfCheckResult
import com.example.counselling.feature.resource.model.recommendResources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SelfCheckUiState(
    val started: Boolean = false,
    val currentIndex: Int = 0,
    val answers: List<Int?> = List(SelfCheckContent.questions.size) { null },
    val result: SelfCheckResult? = null,
    val recommendations: List<ResourceRow> = emptyList(),
    val isLoadingRecommendations: Boolean = false,
    val recommendationError: String? = null,
)

class SelfCheckViewModel(
    private val repository: ResourceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelfCheckUiState())
    val uiState: StateFlow<SelfCheckUiState> = _uiState.asStateFlow()

    val questions = SelfCheckContent.questions
    val options = SelfCheckContent.answerOptions

    fun start() {
        _uiState.update { it.copy(started = true) }
    }

    fun selectAnswer(value: Int) {
        _uiState.update { state ->
            val updated = state.answers.toMutableList()
            updated[state.currentIndex] = value
            state.copy(answers = updated)
        }
    }

    fun next() {
        val state = _uiState.value
        if (state.answers.getOrNull(state.currentIndex) == null) return
        if (state.currentIndex < questions.lastIndex) {
            _uiState.update { it.copy(currentIndex = it.currentIndex + 1) }
        } else {
            submit()
        }
    }

    fun back() {
        _uiState.update {
            if (it.currentIndex == 0) it.copy(started = false)
            else it.copy(currentIndex = it.currentIndex - 1)
        }
    }

    fun retake() {
        _uiState.value = SelfCheckUiState(started = true)
    }

    fun submit() {
        val state = _uiState.value
        if (state.answers.any { it == null }) return
        val result = SelfCheckContent.resultFor(state.answers.map { it ?: 0 })
        _uiState.update {
            it.copy(
                result = result,
                recommendations = emptyList(),
                recommendationError = null,
            )
        }
        loadRecommendations(result)
    }

    fun retryRecommendations() {
        _uiState.value.result?.let { loadRecommendations(it) }
    }

    private fun loadRecommendations(result: SelfCheckResult) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRecommendations = true, recommendationError = null) }
            repository.getResources()
                .onSuccess { resources ->
                    val matches = recommendResources(resources, result)
                    _uiState.update {
                        it.copy(
                            isLoadingRecommendations = false,
                            recommendations = matches,
                            recommendationError = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingRecommendations = false,
                            recommendationError = error.message
                                ?: "Could not load recommendations.",
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(repository: ResourceRepository) = viewModelFactory {
            initializer { SelfCheckViewModel(repository) }
        }
    }
}
