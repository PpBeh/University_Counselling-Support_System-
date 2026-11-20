// Module 6 | Owner: Michael
package com.example.counselling.feature.enquiry.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.AppConstants
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.enquiry.domain.Enquiry
import com.example.counselling.feature.enquiry.domain.EnquiryRepository
import com.example.counselling.feature.enquiry.domain.EnquiryStatus
import com.example.counselling.feature.resource.domain.Resource
import com.example.counselling.feature.resource.domain.ResourceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CounsellorInboxUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val enquiries: List<Enquiry> = emptyList(),
    val publishedResources: List<Resource> = emptyList(),
    val statusFilter: String = "All",
    val busyEnquiryId: String? = null,
)

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class CounsellorInboxViewModel(
    private val enquiryRepository: EnquiryRepository,
    private val resourceRepository: ResourceRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CounsellorInboxUiState())
    val uiState: StateFlow<CounsellorInboxUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onStatusFilterChange(value: String) =
        _uiState.update { it.copy(statusFilter = value) }

    fun refresh() = load(refresh = true)
    fun retry() = load()

    fun reply(
        enquiry: Enquiry,
        reply: String,
        status: String,
        resourceId: String?,
    ) {
        if (_uiState.value.busyEnquiryId != null) return
        if (status !in AppConstants.enquiryStatuses) {
            postError("Choose a valid enquiry status.")
            return
        }
        val statusValue = EnquiryStatus.fromWire(status) ?: return
        if (statusValue == EnquiryStatus.ANSWERED && reply.isBlank()) {
            postError("Write a reply before marking the enquiry as answered.")
            return
        }
        if (reply.length > 2000) {
            postError("Reply must be 2,000 characters or fewer.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busyEnquiryId = enquiry.id) }
            enquiryRepository.reply(enquiry.id, reply, statusValue, resourceId)
                .onSuccess {
                    postSuccess("Enquiry reply saved.")
                    load()
                }
                .onFailure {
                    postError(it.message ?: "Could not save the reply.")
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
            val inbox = enquiryRepository.getInbox()
            val resources = resourceRepository.getResources()
            inbox.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message ?: "Could not load the enquiry inbox.",
                    )
                }
                return@launch
            }
            resources.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message ?: "Could not load resources for replies.",
                    )
                }
                return@launch
            }
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    enquiries = inbox.getOrThrow(),
                    publishedResources = resources.getOrThrow().filter { it.isPublished },
                )
            }
        }
    }

    companion object {
        fun factory(
            enquiryRepository: EnquiryRepository,
            resourceRepository: ResourceRepository,
        ) = viewModelFactory {
            initializer { CounsellorInboxViewModel(enquiryRepository, resourceRepository) }
        }
    }
}
