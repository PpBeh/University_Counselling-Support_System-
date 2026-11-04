// Module 5 | Owner: Michael
package com.example.counselling.feature.resource.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.AppConstants
import com.example.counselling.core.MessageKind
import com.example.counselling.core.UiMessage
import com.example.counselling.feature.resource.data.ResourceRepository
import com.example.counselling.feature.resource.model.ResourceInsert
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResourceFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val mediaType: String = "",
    val contentUrl: String = "",
    val targetBrackets: List<String> = emptyList(),
    val isPublished: Boolean = false,
    val titleError: String? = null,
    val descriptionError: String? = null,
    val categoryError: String? = null,
    val mediaTypeError: String? = null,
    val urlError: String? = null,
    val bracketsError: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
)

class ResourceFormViewModel(
    private val resourceId: String?,
    private val repository: ResourceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ResourceFormUiState(isEdit = resourceId != null, isLoading = resourceId != null),
    )
    val uiState: StateFlow<ResourceFormUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        if (resourceId != null) load()
    }

    fun retry() = load()
    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, titleError = null) }
    fun onDescriptionChange(value: String) =
        _uiState.update { it.copy(description = value, descriptionError = null) }
    fun onCategoryChange(value: String) =
        _uiState.update { it.copy(category = value, categoryError = null) }
    fun onMediaTypeChange(value: String) =
        _uiState.update { it.copy(mediaType = value, mediaTypeError = null) }
    fun onUrlChange(value: String) =
        _uiState.update { it.copy(contentUrl = value, urlError = null) }
    fun onPublishedChange(value: Boolean) = _uiState.update { it.copy(isPublished = value) }

    fun toggleBracket(value: String) = _uiState.update { state ->
        val next = if (state.targetBrackets.contains(value)) {
            state.targetBrackets - value
        } else {
            state.targetBrackets + value
        }
        state.copy(targetBrackets = next, bracketsError = null)
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        val titleError = when {
            state.title.trim().length < 3 -> "Title must be at least 3 characters."
            state.title.trim().length > 120 -> "Title must be 120 characters or fewer."
            else -> null
        }
        val descriptionError = when {
            state.description.trim().length < 10 -> "Description must be at least 10 characters."
            state.description.trim().length > 2000 -> "Description must be 2,000 characters or fewer."
            else -> null
        }
        val categoryError = if (state.category !in AppConstants.concernCategories) {
            "Choose a category."
        } else {
            null
        }
        val mediaTypeError = if (state.mediaType !in AppConstants.resourceMediaTypes) {
            "Choose a media type."
        } else {
            null
        }
        val urlError = when {
            state.contentUrl.isBlank() -> "A resource link is required."
            state.contentUrl.length > 2048 -> "The link is too long."
            !Regex("""^https?://\S+$""").matches(state.contentUrl.trim()) ->
                "Use a valid http:// or https:// link."
            else -> null
        }
        val bracketsError = when {
            state.targetBrackets.isEmpty() -> "Choose at least one wellbeing bracket."
            state.targetBrackets.size > 3 -> "Choose no more than three brackets."
            state.targetBrackets.any { it !in AppConstants.stressBrackets } ->
                "Choose a valid wellbeing bracket."
            else -> null
        }
        _uiState.update {
            it.copy(
                titleError = titleError,
                descriptionError = descriptionError,
                categoryError = categoryError,
                mediaTypeError = mediaTypeError,
                urlError = urlError,
                bracketsError = bracketsError,
            )
        }
        if (listOf(
                titleError,
                descriptionError,
                categoryError,
                mediaTypeError,
                urlError,
                bracketsError,
            ).any { it != null }
        ) {
            return
        }

        val input = ResourceInsert(
            title = state.title.trim(),
            description = state.description.trim(),
            category = state.category,
            mediaType = state.mediaType,
            contentUrl = state.contentUrl.trim(),
            targetStressBrackets = state.targetBrackets,
            isPublished = state.isPublished,
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = if (resourceId == null) {
                repository.createResource(input)
            } else {
                repository.updateResource(resourceId, input)
            }
            _uiState.update { it.copy(isSaving = false) }
            result.onSuccess {
                _uiState.update { it.copy(saveSuccess = true) }
                _messages.send(
                    UiMessage(
                        if (resourceId == null) "Resource added." else "Resource updated.",
                        MessageKind.SUCCESS,
                    ),
                )
            }.onFailure {
                _messages.send(
                    UiMessage(it.message ?: "Could not save the resource.", MessageKind.ERROR),
                )
            }
        }
    }

    private fun load() {
        val id = resourceId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            repository.getResources()
                .onSuccess { resources ->
                    val resource = resources.firstOrNull { it.id == id }
                    if (resource == null) {
                        _uiState.update {
                            it.copy(isLoading = false, loadError = "That resource could not be found.")
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                title = resource.title,
                                description = resource.description,
                                category = resource.category,
                                mediaType = resource.mediaType,
                                contentUrl = resource.contentUrl,
                                targetBrackets = resource.targetStressBrackets,
                                isPublished = resource.isPublished,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            loadError = error.message ?: "Could not load the resource.",
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(resourceId: String?, repository: ResourceRepository) =
            viewModelFactory {
                initializer { ResourceFormViewModel(resourceId, repository) }
            }
    }
}
