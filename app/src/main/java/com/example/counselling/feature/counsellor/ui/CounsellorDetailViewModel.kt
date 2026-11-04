// Module 3 | Owner: Jeremy
package com.example.counselling.feature.counsellor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.MessageKind
import com.example.counselling.core.UiMessage
import com.example.counselling.feature.counsellor.data.CounsellorRepository
import com.example.counselling.feature.counsellor.model.Counsellor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CounsellorDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val counsellor: Counsellor? = null,
    val isRemoving: Boolean = false,
    val removed: Boolean = false,
)

class CounsellorDetailViewModel(
    private val counsellorId: String,
    private val repository: CounsellorRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CounsellorDetailUiState())
    val uiState: StateFlow<CounsellorDetailUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        load()
    }

    fun retry() = load()

    fun remove() {
        if (_uiState.value.isRemoving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRemoving = true) }
            val result = repository.removeCounsellor(counsellorId)
            _uiState.update { it.copy(isRemoving = false) }
            result.onSuccess { outcome ->
                _uiState.update { it.copy(removed = true) }
                _messages.send(
                    UiMessage(
                        if (outcome == "deactivated") {
                            "Counsellor deactivated; historical sessions are retained."
                        } else {
                            "Counsellor removed from the directory."
                        },
                        MessageKind.SUCCESS,
                    ),
                )
            }.onFailure {
                _messages.send(
                    UiMessage(
                        it.message ?: "Could not remove the counsellor.",
                        MessageKind.ERROR,
                    ),
                )
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getCounsellor(counsellorId)
                .onSuccess { counsellor ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = if (counsellor == null) {
                                "That counsellor could not be found."
                            } else {
                                null
                            },
                            counsellor = counsellor,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = error.message ?: "Could not load the counsellor.",
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(counsellorId: String, repository: CounsellorRepository) =
            viewModelFactory {
                initializer { CounsellorDetailViewModel(counsellorId, repository) }
            }
    }
}
