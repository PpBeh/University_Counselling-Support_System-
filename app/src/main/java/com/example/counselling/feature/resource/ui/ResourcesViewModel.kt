// Module 5 | Owner: Michael
package com.example.counselling.feature.resource.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.AppConstants
import com.example.counselling.core.MessageKind
import com.example.counselling.core.UiMessage
import com.example.counselling.core.UserRole
import com.example.counselling.feature.resource.data.ResourceRepository
import com.example.counselling.feature.resource.model.ResourceRow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResourcesUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val resources: List<ResourceRow> = emptyList(),
    val query: String = "",
    val category: String = "All",
    val mediaType: String = "All",
    val busyResourceId: String? = null,
)

class ResourcesViewModel(
    val role: UserRole,
    private val repository: ResourceRepository,
    private val hotlineOnly: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ResourcesUiState(mediaType = if (hotlineOnly) "Hotline" else "All"),
    )
    val uiState: StateFlow<ResourcesUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val canManage: Boolean = role == UserRole.COUNSELLOR || role == UserRole.ADMIN
    val mediaOptions: List<String> = if (hotlineOnly) {
        listOf("Hotline")
    } else {
        listOf("All") + AppConstants.resourceMediaTypes
    }

    init {
        load()
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }
    fun onCategoryChange(value: String) = _uiState.update { it.copy(category = value) }
    fun onMediaTypeChange(value: String) = _uiState.update { it.copy(mediaType = value) }

    fun refresh() = load(refresh = true)
    fun retry() = load()

    fun setPublished(resource: ResourceRow, published: Boolean) {
        if (_uiState.value.busyResourceId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(busyResourceId = resource.id) }
            repository.setPublished(resource.id, published)
                .onSuccess {
                    _messages.send(
                        UiMessage(
                            if (published) "Resource published." else "Resource moved to drafts.",
                            MessageKind.SUCCESS,
                        ),
                    )
                    load()
                }
                .onFailure {
                    _messages.send(
                        UiMessage(it.message ?: "Could not change publish state.", MessageKind.ERROR),
                    )
                }
            _uiState.update { it.copy(busyResourceId = null) }
        }
    }

    fun delete(resource: ResourceRow) {
        if (_uiState.value.busyResourceId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(busyResourceId = resource.id) }
            repository.deleteResource(resource.id)
                .onSuccess {
                    _messages.send(UiMessage("Resource deleted.", MessageKind.SUCCESS))
                    load()
                }
                .onFailure {
                    _messages.send(
                        UiMessage(it.message ?: "Could not delete the resource.", MessageKind.ERROR),
                    )
                }
            _uiState.update { it.copy(busyResourceId = null) }
        }
    }

    private fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.resources.isEmpty(),
                    isRefreshing = refresh,
                    error = null,
                )
            }
            repository.getResources()
                .onSuccess { resources ->
                    val visible = if (role == UserRole.STUDENT) {
                        resources.filter { it.isPublished }
                    } else {
                        resources
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            resources = visible,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error.message ?: "Could not load resources.",
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(
            role: UserRole,
            repository: ResourceRepository,
            hotlineOnly: Boolean,
        ) = viewModelFactory {
            initializer { ResourcesViewModel(role, repository, hotlineOnly) }
        }
    }
}
