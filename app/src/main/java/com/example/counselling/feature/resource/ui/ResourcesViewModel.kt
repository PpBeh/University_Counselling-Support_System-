// Module 5 | Owner: Michael
package com.example.counselling.feature.resource.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.AppConstants
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.resource.domain.Resource
import com.example.counselling.feature.resource.domain.ResourceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResourcesUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val resources: List<Resource> = emptyList(),
    val query: String = "",
    val category: String = "All",
    val mediaType: String = "All",
    val busyResourceId: String? = null,
)

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class ResourcesViewModel(
    val canManage: Boolean,
    private val repository: ResourceRepository,
    private val hotlineOnly: Boolean,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        ResourcesUiState(mediaType = if (hotlineOnly) "Hotline" else "All"),
    )
    val uiState: StateFlow<ResourcesUiState> = _uiState.asStateFlow()

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

    fun setPublished(resource: Resource, published: Boolean) {
        if (_uiState.value.busyResourceId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(busyResourceId = resource.id) }
            repository.setPublished(resource.id, published)
                .onSuccess {
                    postSuccess(if (published) "Resource published." else "Resource moved to drafts.")
                    load()
                }
                .onFailure {
                    postError(it.message ?: "Could not change publish state.")
                }
            _uiState.update { it.copy(busyResourceId = null) }
        }
    }

    fun delete(resource: Resource) {
        if (_uiState.value.busyResourceId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(busyResourceId = resource.id) }
            repository.deleteResource(resource.id)
                .onSuccess {
                    postSuccess("Resource deleted.")
                    load()
                }
                .onFailure {
                    postError(it.message ?: "Could not delete the resource.")
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
                    val visible = if (canManage) resources else resources.filter { it.isPublished }
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
            canManage: Boolean,
            repository: ResourceRepository,
            hotlineOnly: Boolean,
        ) = viewModelFactory {
            initializer { ResourcesViewModel(canManage, repository, hotlineOnly) }
        }
    }
}
