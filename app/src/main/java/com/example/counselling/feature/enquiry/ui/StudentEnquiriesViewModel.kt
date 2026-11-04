// Module 6 | Owner: Michael
package com.example.counselling.feature.enquiry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.AppConstants
import com.example.counselling.core.MessageKind
import com.example.counselling.core.UiMessage
import com.example.counselling.feature.enquiry.data.EnquiryRepository
import com.example.counselling.feature.enquiry.model.Enquiry
import com.example.counselling.feature.enquiry.model.EnquiryLookupRow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentEnquiriesUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val enquiries: List<Enquiry> = emptyList(),
    val lastReferenceCode: String? = null,
    val lookupResult: EnquiryLookupRow? = null,
    val isLookingUp: Boolean = false,
    val lookupError: String? = null,
    val isSubmitting: Boolean = false,
    val busyEnquiryId: String? = null,
)

class StudentEnquiriesViewModel(
    private val repository: EnquiryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentEnquiriesUiState())
    val uiState: StateFlow<StudentEnquiriesUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        load()
    }

    fun refresh() = load(refresh = true)
    fun retry() = load()

    fun submit(category: String, message: String, isAnonymous: Boolean) {
        if (_uiState.value.isSubmitting) return
        if (category !in AppConstants.concernCategories) {
            _messages.trySend(UiMessage("Choose a concern category.", MessageKind.ERROR))
            return
        }
        if (message.trim().length !in 10..1000) {
            _messages.trySend(
                UiMessage("Your message must be between 10 and 1,000 characters.", MessageKind.ERROR),
            )
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            repository.submitEnquiry(category, message, isAnonymous)
                .onSuccess { code ->
                    _uiState.update { it.copy(lastReferenceCode = code) }
                    _messages.send(
                        UiMessage("Enquiry submitted. Keep your reference code.", MessageKind.SUCCESS),
                    )
                    if (!isAnonymous) load(refresh = true)
                }
                .onFailure {
                    _messages.send(
                        UiMessage(it.message ?: "Could not submit the enquiry.", MessageKind.ERROR),
                    )
                }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun dismissReferenceCode() {
        _uiState.update { it.copy(lastReferenceCode = null) }
    }

    fun lookup(code: String) {
        val normalized = code.trim().uppercase()
        if (!Regex("^ENQ-[A-Z0-9]{6}$").matches(normalized)) {
            _uiState.update {
                it.copy(
                    lookupError = "Enter a valid reference code, for example ENQ-7K3P9Q.",
                    lookupResult = null,
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLookingUp = true, lookupError = null) }
            repository.lookupByCode(normalized)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isLookingUp = false,
                            lookupResult = result,
                            lookupError = if (result == null) {
                                "No enquiry was found for that reference code."
                            } else {
                                null
                            },
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(
                            isLookingUp = false,
                            lookupResult = null,
                            lookupError = it.message ?: "Could not look up that code.",
                        )
                    }
                }
        }
    }

    fun clearLookup() {
        _uiState.update { it.copy(lookupResult = null, lookupError = null) }
    }

    fun withdrawByCode(code: String) {
        viewModelScope.launch {
            repository.withdrawByCode(code)
                .onSuccess {
                    _messages.send(UiMessage("Enquiry withdrawn.", MessageKind.SUCCESS))
                    _uiState.update { it.copy(lookupResult = null) }
                    load(refresh = true)
                }
                .onFailure {
                    _messages.send(
                        UiMessage(it.message ?: "Could not withdraw the enquiry.", MessageKind.ERROR),
                    )
                }
        }
    }

    fun withdrawLinked(enquiry: Enquiry) {
        if (_uiState.value.busyEnquiryId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(busyEnquiryId = enquiry.id) }
            repository.deleteOwnLinkedEnquiry(enquiry.id)
                .onSuccess {
                    _messages.send(UiMessage("Enquiry withdrawn.", MessageKind.SUCCESS))
                    load(refresh = true)
                }
                .onFailure {
                    _messages.send(
                        UiMessage(it.message ?: "Could not withdraw the enquiry.", MessageKind.ERROR),
                    )
                }
            _uiState.update { it.copy(busyEnquiryId = null) }
        }
    }

    private fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.enquiries.isEmpty(),
                    isRefreshing = refresh,
                    error = null,
                )
            }
            repository.getMyEnquiries()
                .onSuccess { enquiries ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            enquiries = enquiries,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error.message ?: "Could not load your enquiries.",
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(repository: EnquiryRepository) = viewModelFactory {
            initializer { StudentEnquiriesViewModel(repository) }
        }
    }
}
