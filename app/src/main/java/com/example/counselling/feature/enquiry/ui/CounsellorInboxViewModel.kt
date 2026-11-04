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
import com.example.counselling.feature.resource.data.ResourceRepository
import com.example.counselling.feature.resource.model.ResourceRow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CounsellorInboxUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val enquiries: List<Enquiry> = emptyList(),
    val publishedResources: List<ResourceRow> = emptyList(),
    val statusFilter: String = "All",
    val busyEnquiryId: String? = null,
)

class CounsellorInboxViewModel(
    private val enquiryRepository: EnquiryRepository,
    private val resourceRepository: ResourceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CounsellorInboxUiState())
    val uiState: StateFlow<CounsellorInboxUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

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
            _messages.trySend(UiMessage("Choose a valid enquiry status.", MessageKind.ERROR))
            return
        }
        if (status == "answered" && reply.isBlank()) {
            _messages.trySend(
                UiMessage("Write a reply before marking the enquiry as answered.", MessageKind.ERROR),
            )
            return
        }
        if (reply.length > 2000) {
            _messages.trySend(UiMessage("Reply must be 2,000 characters or fewer.", MessageKind.ERROR))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busyEnquiryId = enquiry.id) }
            enquiryRepository.reply(enquiry.id, reply, status, resourceId)
                .onSuccess {
                    _messages.send(UiMessage("Enquiry reply saved.", MessageKind.SUCCESS))
                    load()
                }
                .onFailure {
                    _messages.send(
                        UiMessage(it.message ?: "Could not save the reply.", MessageKind.ERROR),
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
