// Module 6 | Owner: Michael
package com.example.counselling.feature.enquiry.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.AppConstants
import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.ReferenceCode
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.enquiry.domain.Enquiry
import com.example.counselling.feature.enquiry.domain.EnquiryLookup
import com.example.counselling.feature.enquiry.domain.EnquiryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentEnquiriesUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val enquiries: List<Enquiry> = emptyList(),
    val lastReferenceCode: String? = null,
    val lookupResult: EnquiryLookup? = null,
    val isLookingUp: Boolean = false,
    val lookupError: String? = null,
    val isSubmitting: Boolean = false,
    val busyEnquiryId: String? = null,
)

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class StudentEnquiriesViewModel(
    private val repository: EnquiryRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(StudentEnquiriesUiState())
    val uiState: StateFlow<StudentEnquiriesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() = load(refresh = true)
    fun retry() = load()

    fun submit(category: String, message: String, isAnonymous: Boolean) {
        if (_uiState.value.isSubmitting) return
        if (category !in AppConstants.concernCategories) {
            postError("Choose a concern category.")
            return
        }
        if (message.trim().length !in 10..1000) {
            postError("Your message must be between 10 and 1,000 characters.")
            return
        }
        val categoryValue = ConcernCategory.fromWire(category) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            repository.submitEnquiry(categoryValue, message, isAnonymous)
                .onSuccess { code ->
                    _uiState.update { it.copy(lastReferenceCode = code) }
                    postSuccess("Enquiry submitted. Keep your reference code.")
                    if (!isAnonymous) load(refresh = true)
                }
                .onFailure {
                    postError(it.message ?: "Could not submit the enquiry.")
                }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun dismissReferenceCode() {
        _uiState.update { it.copy(lastReferenceCode = null) }
    }

    fun lookup(code: String) {
        val reference = try {
            ReferenceCode.of(code)
        } catch (_: DomainException) {
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
            repository.lookupByCode(reference)
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
        val reference = runCatching { ReferenceCode.of(code) }.getOrNull() ?: return
        viewModelScope.launch {
            repository.withdrawByCode(reference)
                .onSuccess {
                    postSuccess("Enquiry withdrawn.")
                    _uiState.update { it.copy(lookupResult = null) }
                    load(refresh = true)
                }
                .onFailure {
                    postError(it.message ?: "Could not withdraw the enquiry.")
                }
        }
    }

    fun withdrawLinked(enquiry: Enquiry) {
        if (_uiState.value.busyEnquiryId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(busyEnquiryId = enquiry.id) }
            repository.deleteOwnLinkedEnquiry(enquiry.id)
                .onSuccess {
                    postSuccess("Enquiry withdrawn.")
                    load(refresh = true)
                }
                .onFailure {
                    postError(it.message ?: "Could not withdraw the enquiry.")
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
