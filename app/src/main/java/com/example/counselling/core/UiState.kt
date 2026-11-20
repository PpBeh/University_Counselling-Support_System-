// Core | Owner: shared (Beh, Jeremy, Michael)
// OOP: Sum type - sealed UiState makes loading/success/error exhaustive, and
// UiMessage gives every ViewModel one typed snackbar contract.
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
