// Module 6 | Owner: Michael
// Feedback is insert/delete only. Students rate once per completed session and
// may delete their rating before rating again.
package com.example.counselling.feature.enquiry.data

import com.example.counselling.core.ErrorMapper
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.enquiry.model.FeedbackInsert
import com.example.counselling.feature.enquiry.model.FeedbackRow
import com.example.counselling.feature.enquiry.model.FeedbackWithAppointment
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class FeedbackRepository(
    private val supabase: SupabaseClient,
    private val appointmentRepository: AppointmentRepository,
) {

    suspend fun getFeedbackWithAppointments(): Result<List<FeedbackWithAppointment>> = try {
        val rows = supabase
            .from("feedback")
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<FeedbackRow>()
        val appointments = appointmentRepository.getAppointments()
            .getOrElse { throw it }
            .associateBy { it.id }
        Result.success(
            rows.map {
                FeedbackWithAppointment(
                    feedback = it,
                    appointment = appointments[it.appointmentId],
                )
            },
        )
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not load feedback.")))
    }

    suspend fun submitFeedback(
        appointmentId: String,
        rating: Int,
        comment: String?,
    ): Result<Unit> = try {
        supabase.from("feedback").insert(
            FeedbackInsert(
                appointmentId = appointmentId,
                rating = rating,
                comment = comment?.trim()?.takeIf { it.isNotBlank() },
            ),
        )
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not submit feedback.")))
    }

    suspend fun deleteFeedback(feedbackId: String): Result<Unit> = try {
        supabase.from("feedback").delete {
            filter { eq("id", feedbackId) }
        }
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not remove feedback.")))
    }
}
