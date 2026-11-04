// Core | Owner: shared (Beh, Jeremy, Michael)
// Small, shared presentation types: loading/error state and Snackbar messages.
package com.example.counselling.core

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MessageKind {
    SUCCESS,
    ERROR,
    INFO,
}

data class UiMessage(
    val text: String,
    val kind: MessageKind = MessageKind.INFO,
)
