// Core presentation | Owner: shared (Beh, Jeremy, Michael)
// Shared base for all ViewModels: one-off UiMessage channel plus small
// message/error helpers.
package com.example.counselling.core.ui

import androidx.lifecycle.ViewModel
import com.example.counselling.core.MessageKind
import com.example.counselling.core.UiMessage
import com.example.counselling.core.data.ErrorTranslator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

// OOP: Inheritance - every feature ViewModel extends BaseViewModel instead of
// re-declaring its own Channel, message flow and Snackbar boilerplate.
abstract class BaseViewModel : ViewModel() {

    private val messageChannel = Channel<UiMessage>(Channel.BUFFERED)
    private val errorTranslator = ErrorTranslator()

    val messages: Flow<UiMessage> = messageChannel.receiveAsFlow()

    protected fun postMessage(
        text: String,
        kind: MessageKind = MessageKind.INFO,
    ) {
        messageChannel.trySend(UiMessage(text, kind))
    }

    protected fun postSuccess(text: String) {
        postMessage(text, MessageKind.SUCCESS)
    }

    protected fun postInfo(text: String) {
        postMessage(text, MessageKind.INFO)
    }

    protected fun postError(text: String) {
        postMessage(text, MessageKind.ERROR)
    }

    // OOP: Encapsulation - raw exceptions are translated in one place.
    protected fun postFailure(
        error: Throwable,
        fallback: String = "Something went wrong. Please try again.",
    ) {
        postError(errorTranslator.translate(error, fallback))
    }

    override fun onCleared() {
        messageChannel.close()
        super.onCleared()
    }
}
