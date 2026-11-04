// Module 6 | Owner: Michael
// Anonymous rows deliberately carry no student_id. The code-lookup RPC returns
// only public enquiry fields and a published resource title.
package com.example.counselling.feature.enquiry.data

import com.example.counselling.core.ErrorMapper
import com.example.counselling.feature.enquiry.model.Enquiry
import com.example.counselling.feature.enquiry.model.EnquiryLookupRow
import com.example.counselling.feature.enquiry.model.EnquiryRow
import com.example.counselling.feature.resource.data.ResourceRepository
import com.example.counselling.feature.student.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class EnquiryRepository(
    private val supabase: SupabaseClient,
    private val resourceRepository: ResourceRepository,
) {

    suspend fun getMyEnquiries(): Result<List<Enquiry>> = try {
        val rows = supabase
            .from("enquiries")
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<EnquiryRow>()
        val resources = resourceRepository.getResources()
            .getOrElse { throw it }
            .associateBy { it.id }
        Result.success(
            rows.map { row ->
                Enquiry(
                    id = row.id,
                    referenceCode = row.referenceCode,
                    studentId = row.studentId,
                    isAnonymous = row.isAnonymous,
                    category = row.category,
                    message = row.message,
                    status = row.status,
                    reply = row.reply,
                    recommendedResourceId = row.recommendedResourceId,
                    recommendedResourceTitle = row.recommendedResourceId?.let {
                        resources[it]?.title
                    },
                    repliedAt = row.repliedAt,
                    createdAt = row.createdAt,
                    studentName = null,
                )
            },
        )
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not load your enquiries.")))
    }

    suspend fun getInbox(): Result<List<Enquiry>> = try {
        val rows = supabase
            .from("enquiries")
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<EnquiryRow>()

        val profiles = supabase
            .from("profiles")
            .select { filter { eq("role", "student") } }
            .decodeList<Profile>()
            .associateBy { it.id }

        val resources = resourceRepository.getResources()
            .getOrElse { throw it }
            .associateBy { it.id }

        Result.success(
            rows.map { row ->
                Enquiry(
                    id = row.id,
                    referenceCode = row.referenceCode,
                    studentId = row.studentId,
                    isAnonymous = row.isAnonymous,
                    category = row.category,
                    message = row.message,
                    status = row.status,
                    reply = row.reply,
                    recommendedResourceId = row.recommendedResourceId,
                    recommendedResourceTitle = row.recommendedResourceId?.let {
                        resources[it]?.title
                    },
                    repliedAt = row.repliedAt,
                    createdAt = row.createdAt,
                    studentName = row.studentId?.let { profiles[it]?.fullName },
                )
            },
        )
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not load the enquiry inbox.")))
    }

    suspend fun submitEnquiry(
        category: String,
        message: String,
        isAnonymous: Boolean,
    ): Result<String> = try {
        val result = supabase.postgrest.rpc(
            "submit_enquiry",
            buildJsonObject {
                put("p_category", category)
                put("p_message", message.trim())
                put("p_is_anonymous", isAnonymous)
            },
        )
        Result.success(result.decodeAs<String>())
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not submit the enquiry.")))
    }

    suspend fun lookupByCode(code: String): Result<EnquiryLookupRow?> = try {
        val rows = supabase.postgrest.rpc(
            "get_enquiry_by_code",
            buildJsonObject { put("p_code", code.trim().uppercase()) },
        ).decodeList<EnquiryLookupRow>()
        Result.success(rows.firstOrNull())
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not look up that reference code.")))
    }

    suspend fun withdrawByCode(code: String): Result<Unit> = try {
        supabase.postgrest.rpc(
            "withdraw_enquiry_by_code",
            buildJsonObject { put("p_code", code.trim().uppercase()) },
        )
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not withdraw the enquiry.")))
    }

    suspend fun deleteOwnLinkedEnquiry(enquiryId: String): Result<Unit> = try {
        supabase.from("enquiries").delete {
            filter { eq("id", enquiryId) }
        }
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not withdraw the enquiry.")))
    }

    suspend fun reply(
        enquiryId: String,
        reply: String,
        status: String,
        resourceId: String?,
    ): Result<Unit> = try {
        supabase.from("enquiries").update(
            {
                set("reply", reply.trim())
                set("status", status)
                set("recommended_resource_id", resourceId)
            },
        ) {
            filter { eq("id", enquiryId) }
        }
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not send the reply.")))
    }
}
