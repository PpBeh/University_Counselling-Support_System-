// Module 6 | Owner: Michael
package com.example.counselling.feature.enquiry.data

import com.example.counselling.core.data.BaseSupabaseRepository
import com.example.counselling.core.data.ErrorTranslator
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.Rating
import com.example.counselling.feature.enquiry.domain.Feedback
import com.example.counselling.feature.enquiry.domain.FeedbackRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class FeedbackViewRow(
    val id: String,
    @SerialName("appointment_id") val appointmentId: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("student_name") val studentName: String = "Student",
    @SerialName("counsellor_id") val counsellorId: String,
    @SerialName("counsellor_name") val counsellorName: String = "Counsellor",
    val rating: Int,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("session_start") val sessionStart: String? = null,
    @SerialName("concern_category") val concernCategory: String = "Counselling session",
)

@Serializable
private data class FeedbackInsert(
    @SerialName("appointment_id") val appointmentId: String,
    val rating: Int,
    val comment: String? = null,
)

// OOP: Repository implementation - feedback is a read model over v_feedback.
class SupabaseFeedbackRepository(
    supabase: SupabaseClient,
    errorTranslator: ErrorTranslator = ErrorTranslator(),
) : BaseSupabaseRepository(supabase, errorTranslator), FeedbackRepository {

    override suspend fun getFeedback(): Result<List<Feedback>> =
        runQuery("Could not load feedback.") {
            supabase
                .from("v_feedback")
                .select { order("created_at", Order.DESCENDING) }
                .decodeList<FeedbackViewRow>()
                .map(::toDomain)
        }

    override suspend fun submitFeedback(
        appointmentId: String,
        rating: Rating,
        comment: String?,
    ): Result<Unit> = runQuery("Could not submit feedback.") {
        supabase.from("feedback").insert(
            FeedbackInsert(
                appointmentId = appointmentId,
                rating = rating.value,
                comment = comment?.trim()?.takeIf { it.isNotBlank() },
            ),
        )
        Unit
    }

    override suspend fun deleteFeedback(feedbackId: String): Result<Unit> =
        runQuery("Could not remove feedback.") {
            supabase.from("feedback").delete {
                filter { eq("id", feedbackId) }
            }
            Unit
        }

    private fun toDomain(row: FeedbackViewRow): Feedback = Feedback(
        id = row.id,
        appointmentId = row.appointmentId,
        studentName = row.studentName,
        counsellorName = row.counsellorName,
        rating = Rating.of(row.rating),
        comment = row.comment,
        createdAt = row.createdAt?.let(::parseInstant),
        sessionStart = row.sessionStart?.let(::parseInstant),
        concernCategory = row.concernCategory,
    )

    private fun parseInstant(value: String): Instant =
        runCatching { Instant.parse(value) }
            .recoverCatching { OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { ZonedDateTime.parse(value).toInstant() }
            .getOrElse { throw DomainException("A date value could not be read: $value") }
}
