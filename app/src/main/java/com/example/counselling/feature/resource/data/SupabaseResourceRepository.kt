// Module 5 | Owner: Michael
package com.example.counselling.feature.resource.data

import com.example.counselling.core.data.BaseSupabaseRepository
import com.example.counselling.core.data.ErrorTranslator
import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.StressBracket
import com.example.counselling.feature.resource.domain.ArticleResource
import com.example.counselling.feature.resource.domain.AudioResource
import com.example.counselling.feature.resource.domain.HotlineResource
import com.example.counselling.feature.resource.domain.PdfResource
import com.example.counselling.feature.resource.domain.Resource
import com.example.counselling.feature.resource.domain.ResourceDraft
import com.example.counselling.feature.resource.domain.ResourceRepository
import com.example.counselling.feature.resource.domain.ResourceType
import com.example.counselling.feature.resource.domain.VideoResource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class ResourceViewRow(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("category_code") val categoryCode: String,
    val category: String,
    @SerialName("resource_type") val resourceType: String,
    @SerialName("media_type") val mediaType: String,
    @SerialName("content_url") val contentUrl: String,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("operating_hours") val operatingHours: String? = null,
    @SerialName("website_url") val websiteUrl: String? = null,
    @SerialName("target_stress_brackets") val targetStressBrackets: List<String> = emptyList(),
    @SerialName("is_published") val isPublished: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

// OOP: Repository implementation - maps the v_resources projection back into
// the correct Resource subtype (polymorphic mapping), never a generic row.
class SupabaseResourceRepository(
    supabase: SupabaseClient,
    errorTranslator: ErrorTranslator = ErrorTranslator(),
) : BaseSupabaseRepository(supabase, errorTranslator), ResourceRepository {

    override suspend fun getResources(): Result<List<Resource>> =
        runQuery("Could not load resources.") {
            supabase
                .from("v_resources")
                .select { order("title", Order.ASCENDING) }
                .decodeList<ResourceViewRow>()
                .map(::toDomain)
        }

    override suspend fun getResource(resourceId: String): Result<Resource?> =
        runQuery("Could not load the resource.") {
            supabase
                .from("v_resources")
                .select {
                    filter { eq("id", resourceId) }
                    limit(1)
                }
                .decodeSingleOrNull<ResourceViewRow>()
                ?.let(::toDomain)
        }

    override suspend fun createResource(draft: ResourceDraft): Result<Resource> =
        runQuery("Could not create the resource.") {
            draft.validate()
            supabase.postgrest.rpc(
                "create_resource",
                resourceParams(draft),
            ).decodeAs<String>()
                .let { id -> getResource(id).getOrElse { throw it } }
                ?: throw DomainException("The new resource could not be reloaded.")
        }

    override suspend fun updateResource(
        resourceId: String,
        draft: ResourceDraft,
    ): Result<Resource> = runQuery("Could not update the resource.") {
        draft.validate()
        supabase.postgrest.rpc("update_resource", resourceParams(draft, resourceId))
        getResource(resourceId).getOrElse { throw it }
            ?: throw DomainException("The updated resource could not be reloaded.")
    }

    override suspend fun setPublished(resourceId: String, published: Boolean): Result<Unit> =
        runQuery("Could not change the publish state.") {
            val resource = getResource(resourceId).getOrElse { throw it }
                ?: throw DomainException("That resource could not be found.")
            val draft = ResourceDraft(
                title = resource.title,
                description = resource.description,
                category = resource.category,
                type = resource.type,
                contentUrl = resource.contentUrl,
                targetBrackets = resource.targetBrackets,
                isPublished = published,
                phoneNumber = (resource as? HotlineResource)?.phoneNumber,
                operatingHours = (resource as? HotlineResource)?.operatingHours,
            )
            supabase.postgrest.rpc("update_resource", resourceParams(draft, resourceId))
            Unit
        }

    override suspend fun deleteResource(resourceId: String): Result<Unit> =
        runQuery("Could not delete the resource.") {
            supabase.postgrest.rpc(
                "delete_resource",
                buildJsonObject { put("p_resource_id", resourceId) },
            )
            Unit
        }

    private fun resourceParams(draft: ResourceDraft, resourceId: String? = null) = buildJsonObject {
        if (resourceId != null) put("p_resource_id", resourceId)
        put("p_title", draft.title.trim())
        put("p_description", draft.description.trim())
        put("p_category", draft.category.label)
        put("p_media_type", draft.type.label)
        put("p_content_url", draft.contentUrl.trim())
        put(
            "p_target_stress_brackets",
            buildJsonArray { draft.targetBrackets.forEach { add(it.code) } },
        )
        put("p_is_published", draft.isPublished)
        put("p_phone_number", draft.phoneNumber)
        put("p_operating_hours", draft.operatingHours)
    }

    private fun toDomain(row: ResourceViewRow): Resource {
        val category = ConcernCategory.fromWire(row.categoryCode)
            ?: ConcernCategory.fromWire(row.category)
            ?: throw DomainException("Unknown resource category: ${row.category}")
        val brackets = row.targetStressBrackets.mapNotNull(StressBracket::fromWire).toSet()
        val type = ResourceType.fromWire(row.resourceType)
        val created = row.createdAt?.let(::parseInstant)
        val updated = row.updatedAt?.let(::parseInstant)
        return when (type) {
            ResourceType.ARTICLE -> ArticleResource(
                row.id, row.title, row.description, category, brackets,
                row.isPublished, created, updated, row.contentUrl,
            )
            ResourceType.VIDEO -> VideoResource(
                row.id, row.title, row.description, category, brackets,
                row.isPublished, created, updated, row.contentUrl,
            )
            ResourceType.AUDIO -> AudioResource(
                row.id, row.title, row.description, category, brackets,
                row.isPublished, created, updated, row.contentUrl,
            )
            ResourceType.PDF -> PdfResource(
                row.id, row.title, row.description, category, brackets,
                row.isPublished, created, updated, row.contentUrl,
            )
            ResourceType.HOTLINE -> HotlineResource(
                row.id, row.title, row.description, category, brackets,
                row.isPublished, created, updated, row.contentUrl,
                row.phoneNumber, row.operatingHours,
            )
        }
    }

    private fun parseInstant(value: String): Instant =
        runCatching { Instant.parse(value) }
            .recoverCatching { OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { ZonedDateTime.parse(value).toInstant() }
            .getOrElse { throw DomainException("A date value could not be read: $value") }
}
