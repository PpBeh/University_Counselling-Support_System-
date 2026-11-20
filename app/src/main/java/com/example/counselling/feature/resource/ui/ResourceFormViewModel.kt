// Module 5 | Owner: Michael
package com.example.counselling.feature.resource.ui

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.core.AppConstants
import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.StressBracket
import com.example.counselling.core.ui.BaseViewModel
import com.example.counselling.feature.resource.domain.Resource
import com.example.counselling.feature.resource.domain.ResourceDraft
import com.example.counselling.feature.resource.domain.ResourceRepository
import com.example.counselling.feature.resource.domain.ResourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

// OOP: Inheritance - this ViewModel extends BaseViewModel and reuses its shared UiMessage channel.

class ResourceFormViewModel(
    private val resourceId: String?,
    private val repository: ResourceRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        ResourceFormUiState(isEdit = resourceId != null, isLoading = resourceId != null),
    )
    val uiState: StateFlow<ResourceFormUiState> = _uiState.asStateFlow()

    // Existing hotline contact fields are preserved by the aggregate update.
    private var phoneNumber: String? = null
    private var operatingHours: String? = null

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

        val category = ConcernCategory.fromWire(state.category)
        val type = runCatching { ResourceType.fromLabel(state.mediaType) }.getOrNull()
        val brackets = state.targetBrackets.mapNotNull(StressBracket::fromWire).toSet()
        if (category == null || type == null || brackets.isEmpty()) {
            postError("Choose a valid category, media type and wellbeing bracket.")
            return
        }
        val draft = ResourceDraft(
            title = state.title.trim(),
            description = state.description.trim(),
            category = category,
            type = type,
            contentUrl = state.contentUrl.trim(),
            targetBrackets = brackets,
            isPublished = state.isPublished,
            phoneNumber = phoneNumber,
            operatingHours = operatingHours,
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = try {
                draft.validate()
                if (resourceId == null) repository.createResource(draft)
                else repository.updateResource(resourceId, draft)
            } catch (error: DomainException) {
                Result.failure<Resource>(error)
            }
            _uiState.update { it.copy(isSaving = false) }
            result.onSuccess {
                _uiState.update { it.copy(saveSuccess = true) }
                postSuccess(if (resourceId == null) "Resource added." else "Resource updated.")
            }.onFailure {
                postError(it.message ?: "Could not save the resource.")
            }
        }
    }

    private fun load() {
        val id = resourceId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            repository.getResource(id)
                .onSuccess { resource ->
                    if (resource == null) {
                        _uiState.update {
                            it.copy(isLoading = false, loadError = "That resource could not be found.")
                        }
                    } else {
                        val hotline = resource as? com.example.counselling.feature.resource.domain.HotlineResource
                        phoneNumber = hotline?.phoneNumber
                        operatingHours = hotline?.operatingHours
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                title = resource.title,
                                description = resource.description,
                                category = resource.category.label,
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
