// Module 6 | Owner: Michael
package com.example.counselling.feature.enquiry.data

import com.example.counselling.core.data.BaseSupabaseRepository
import com.example.counselling.core.data.ErrorTranslator
import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.ReferenceCode
import com.example.counselling.feature.enquiry.domain.AnonymousEnquiry
import com.example.counselling.feature.enquiry.domain.Enquiry
import com.example.counselling.feature.enquiry.domain.EnquiryLookup
import com.example.counselling.feature.enquiry.domain.EnquiryRepository
import com.example.counselling.feature.enquiry.domain.EnquiryStatus
import com.example.counselling.feature.enquiry.domain.IdentifiedEnquiry
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class EnquiryViewRow(
    val id: String,
    @SerialName("reference_code") val referenceCode: String,
    @SerialName("category_code") val categoryCode: String,
    val category: String,
    val message: String,
    val status: String,
    val reply: String? = null,
    @SerialName("recommended_resource_id") val recommendedResourceId: String? = null,
    @SerialName("recommended_resource_title") val recommendedResourceTitle: String? = null,
    @SerialName("replied_at") val repliedAt: String? = null,
    @SerialName("enquiry_type") val enquiryType: String,
    @SerialName("is_anonymous") val isAnonymous: Boolean,
    @SerialName("student_id") val studentId: String? = null,
    @SerialName("student_name") val studentName: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class EnquiryLookupRow(
    @SerialName("reference_code") val referenceCode: String,
    val category: String,
    val message: String,
    val reply: String? = null,
    val status: String,
    @SerialName("recommended_resource_id") val recommendedResourceId: String? = null,
    @SerialName("recommended_resource_title") val recommendedResourceTitle: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("replied_at") val repliedAt: String? = null,
)

// OOP: Repository implementation - the view shape is mapped into the sealed
// Identified/Anonymous enquiry hierarchy; anonymous rows never receive an owner.
class SupabaseEnquiryRepository(
    supabase: SupabaseClient,
    errorTranslator: ErrorTranslator = ErrorTranslator(),
) : BaseSupabaseRepository(supabase, errorTranslator), EnquiryRepository {

    override suspend fun getMyEnquiries(): Result<List<Enquiry>> =
        runQuery("Could not load your enquiries.") { loadEnquiries() }

    override suspend fun getInbox(): Result<List<Enquiry>> =
        runQuery("Could not load the enquiry inbox.") { loadEnquiries() }

    override suspend fun submitEnquiry(
        category: ConcernCategory,
        message: String,
        isAnonymous: Boolean,
    ): Result<String> = runQuery("Could not submit the enquiry.") {
        supabase.postgrest.rpc(
            "submit_enquiry",
            buildJsonObject {
                put("p_category", category.label)
                put("p_message", message.trim())
                put("p_is_anonymous", isAnonymous)
            },
        ).decodeAs<String>()
    }

    override suspend fun lookupByCode(code: ReferenceCode): Result<EnquiryLookup?> =
        runQuery("Could not look up that reference code.") {
            supabase.postgrest.rpc(
                "get_enquiry_by_code",
                buildJsonObject { put("p_code", code.value) },
            ).decodeList<EnquiryLookupRow>()
                .firstOrNull()
                ?.let(::toDomain)
        }

    override suspend fun withdrawByCode(code: ReferenceCode): Result<Unit> =
        runQuery("Could not withdraw the enquiry.") {
            supabase.postgrest.rpc(
                "withdraw_enquiry_by_code",
                buildJsonObject { put("p_code", code.value) },
            )
            Unit
        }

    override suspend fun deleteOwnLinkedEnquiry(enquiryId: String): Result<Unit> =
        runQuery("Could not withdraw the enquiry.") {
            supabase.from("enquiries").delete {
                filter { eq("id", enquiryId) }
            }
            Unit
        }

    override suspend fun reply(
        enquiryId: String,
        reply: String,
        status: EnquiryStatus,
        resourceId: String?,
    ): Result<Unit> = runQuery("Could not send the reply.") {
        supabase.from("enquiries").update(
            {
                set("reply", reply.trim())
                set("status", status.code)
                set("recommended_resource_id", resourceId)
            },
        ) {
            filter { eq("id", enquiryId) }
        }
        Unit
    }

    private suspend fun loadEnquiries(): List<Enquiry> =
        supabase
            .from("v_enquiries")
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<EnquiryViewRow>()
            .map(::toDomain)

    private fun toDomain(row: EnquiryViewRow): Enquiry {
        val category = ConcernCategory.fromWire(row.categoryCode)
            ?: ConcernCategory.fromWire(row.category)
            ?: throw DomainException("Unknown enquiry category: ${row.category}")
        val status = EnquiryStatus.fromWire(row.status)
            ?: throw DomainException("Unknown enquiry status: ${row.status}")
        val repliedAt = row.repliedAt?.let(::parseInstant)
        val createdAt = row.createdAt?.let(::parseInstant)

        if (row.isAnonymous || row.enquiryType == "anonymous") {
            return AnonymousEnquiry(
                id = row.id,
                referenceCode = row.referenceCode,
                category = category,
                message = row.message,
                status = status,
                reply = row.reply,
                recommendedResourceId = row.recommendedResourceId,
                recommendedResourceTitle = row.recommendedResourceTitle,
                repliedAt = repliedAt,
                createdAt = createdAt,
            )
        }
        return IdentifiedEnquiry(
            id = row.id,
            referenceCode = row.referenceCode,
            category = category,
            message = row.message,
            status = status,
            reply = row.reply,
            recommendedResourceId = row.recommendedResourceId,
            recommendedResourceTitle = row.recommendedResourceTitle,
            repliedAt = repliedAt,
            createdAt = createdAt,
            ownerId = row.studentId
                ?: throw DomainException("An identified enquiry has no student id."),
            ownerName = row.studentName,
        )
    }

    private fun toDomain(row: EnquiryLookupRow): EnquiryLookup = EnquiryLookup(
        referenceCode = row.referenceCode,
        category = ConcernCategory.fromWire(row.category)
            ?: throw DomainException("Unknown enquiry category: ${row.category}"),
        message = row.message,
        reply = row.reply,
        status = EnquiryStatus.fromWire(row.status)
            ?: throw DomainException("Unknown enquiry status: ${row.status}"),
        recommendedResourceTitle = row.recommendedResourceTitle,
        createdAt = row.createdAt?.let(::parseInstant),
        repliedAt = row.repliedAt?.let(::parseInstant),
    )

    private fun parseInstant(value: String): Instant =
        runCatching { Instant.parse(value) }
            .recoverCatching { OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { ZonedDateTime.parse(value).toInstant() }
            .getOrElse { throw DomainException("A date value could not be read: $value") }
}
