// Module 6 | Owner: Michael
package com.example.counselling.feature.enquiry.domain

import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.Rating
import java.time.Instant

// OOP: Entity/value object - feedback no longer carries a mutable appointment
// DTO; it owns the safe session projection returned by v_feedback.
class Feedback(
    val id: String,
    val appointmentId: String,
    val studentName: String,
    val counsellorName: String,
    val rating: Rating,
    val comment: String?,
    val createdAt: Instant?,
    val sessionStart: Instant?,
    val concernCategory: String,
) {
    init {
        if (id.isBlank()) throw DomainException("Feedback id cannot be blank.")
        if ((comment?.length ?: 0) > 500) {
            throw DomainException("Comment must be 500 characters or fewer.")
        }
    }
}

interface FeedbackRepository {

    suspend fun getFeedback(): Result<List<Feedback>>

    suspend fun submitFeedback(
        appointmentId: String,
        rating: Rating,
        comment: String?,
    ): Result<Unit>

    suspend fun deleteFeedback(feedbackId: String): Result<Unit>
}
