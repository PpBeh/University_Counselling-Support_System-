// Module 5 | Owner: Michael
package com.example.counselling.feature.resource.domain

import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.StressBracket
import java.time.Instant

// OOP: Inheritance - Resource is the abstract supertype for the five media
// subtypes; subclasses supply their own content link/contact detail.
sealed class Resource(
    val id: String,
    val title: String,
    val description: String,
    val category: ConcernCategory,
    val targetBrackets: Set<StressBracket>,
    val isPublished: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
) {
    abstract val type: ResourceType
    abstract val contentUrl: String

    val mediaType: String get() = type.label
    val targetStressBrackets: List<String> get() = targetBrackets.map { it.code }
    val isHotline: Boolean get() = this is HotlineResource
    val bracketLabels: List<String> get() = targetBrackets.map { it.label }.sorted()

    init {
        if (id.isBlank()) throw DomainException("Resource id cannot be blank.")
        if (title.trim().length !in 3..120) {
            throw DomainException("Title must be between 3 and 120 characters.")
        }
        if (description.trim().length !in 10..2000) {
            throw DomainException("Description must be between 10 and 2,000 characters.")
        }
        if (targetBrackets.isEmpty()) {
            throw DomainException("Choose at least one wellbeing bracket.")
        }
    }

    fun targets(bracket: StressBracket): Boolean = bracket in targetBrackets
}

class ArticleResource(
    id: String,
    title: String,
    description: String,
    category: ConcernCategory,
    targetBrackets: Set<StressBracket>,
    isPublished: Boolean,
    createdAt: Instant?,
    updatedAt: Instant?,
    override val contentUrl: String,
) : Resource(id, title, description, category, targetBrackets, isPublished, createdAt, updatedAt) {
    override val type: ResourceType get() = ResourceType.ARTICLE
}

class VideoResource(
    id: String,
    title: String,
    description: String,
    category: ConcernCategory,
    targetBrackets: Set<StressBracket>,
    isPublished: Boolean,
    createdAt: Instant?,
    updatedAt: Instant?,
    override val contentUrl: String,
) : Resource(id, title, description, category, targetBrackets, isPublished, createdAt, updatedAt) {
    override val type: ResourceType get() = ResourceType.VIDEO
}

class AudioResource(
    id: String,
    title: String,
    description: String,
    category: ConcernCategory,
    targetBrackets: Set<StressBracket>,
    isPublished: Boolean,
    createdAt: Instant?,
    updatedAt: Instant?,
    override val contentUrl: String,
) : Resource(id, title, description, category, targetBrackets, isPublished, createdAt, updatedAt) {
    override val type: ResourceType get() = ResourceType.AUDIO
}

class PdfResource(
    id: String,
    title: String,
    description: String,
    category: ConcernCategory,
    targetBrackets: Set<StressBracket>,
    isPublished: Boolean,
    createdAt: Instant?,
    updatedAt: Instant?,
    override val contentUrl: String,
) : Resource(id, title, description, category, targetBrackets, isPublished, createdAt, updatedAt) {
    override val type: ResourceType get() = ResourceType.PDF
}

class HotlineResource(
    id: String,
    title: String,
    description: String,
    category: ConcernCategory,
    targetBrackets: Set<StressBracket>,
    isPublished: Boolean,
    createdAt: Instant?,
    updatedAt: Instant?,
    override val contentUrl: String,
    val phoneNumber: String?,
    val operatingHours: String?,
) : Resource(id, title, description, category, targetBrackets, isPublished, createdAt, updatedAt) {
    override val type: ResourceType get() = ResourceType.HOTLINE
}

// OOP: Value object - the form submits a draft, not a partially built entity.
data class ResourceDraft(
    val title: String,
    val description: String,
    val category: ConcernCategory,
    val type: ResourceType,
    val contentUrl: String,
    val targetBrackets: Set<StressBracket>,
    val isPublished: Boolean,
    val phoneNumber: String? = null,
    val operatingHours: String? = null,
) {
    fun validate(): ResourceDraft {
        if (title.trim().length !in 3..120) {
            throw DomainException("Title must be between 3 and 120 characters.")
        }
        if (description.trim().length !in 10..2000) {
            throw DomainException("Description must be between 10 and 2,000 characters.")
        }
        if (targetBrackets.isEmpty()) {
            throw DomainException("Choose at least one wellbeing bracket.")
        }
        if (!Regex("""^https?://\S+$""").matches(contentUrl.trim())) {
            throw DomainException("Use a valid http:// or https:// link.")
        }
        return this
    }
}
