// Module 3 | Owner: Jeremy
package com.example.counselling.feature.counsellor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.feature.counsellor.data.CounsellorRepository
import com.example.counselling.feature.counsellor.model.Counsellor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CounsellorListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val counsellors: List<Counsellor> = emptyList(),
    val query: String = "",
    val specialisation: String = "All",
    val availableThisWeekOnly: Boolean = false,
)

class CounsellorListViewModel(
    private val repository: CounsellorRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CounsellorListUiState())
    val uiState: StateFlow<CounsellorListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun onSpecialisationChange(value: String) =
        _uiState.update { it.copy(specialisation = value) }

    fun onAvailableThisWeekChange(value: Boolean) =
        _uiState.update { it.copy(availableThisWeekOnly = value) }

    fun refresh() = load(refresh = true)
    fun retry() = load()

    private fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.counsellors.isEmpty(),
                    isRefreshing = refresh,
                    error = null,
                )
            }
            repository.getCounsellors()
                .onSuccess { counsellors ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            counsellors = counsellors,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error.message ?: "Could not load counsellors.",
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(repository: CounsellorRepository) = viewModelFactory {
            initializer { CounsellorListViewModel(repository) }
        }
    }
}
