// Module 4 / Unique Feature 2 | Owner: Jeremy
// Private records are visible only to the owning counsellor. Student summaries
// come from the safe owner-rights view; admins only call the aggregate RPC.
package com.example.counselling.feature.records.data

import com.example.counselling.core.ErrorMapper
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.appointment.model.AvailableSlotRow
import com.example.counselling.feature.records.model.AdminDashboardStats
import com.example.counselling.feature.records.model.SessionRecord
import com.example.counselling.feature.records.model.SessionRecordInsert
import com.example.counselling.feature.records.model.SessionRecordRow
import com.example.counselling.feature.records.model.StudentSessionSummaryRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SessionRecordRepository(
    private val supabase: SupabaseClient,
    private val appointmentRepository: AppointmentRepository,
) {

    suspend fun getRecords(): Result<List<SessionRecord>> = try {
        val rows = supabase
            .from("session_records")
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<SessionRecordRow>()
        val appointments = appointmentRepository.getAppointments()
            .getOrElse { throw it }
            .associateBy { it.id }
        Result.success(
            rows.map { row ->
                SessionRecord(
                    id = row.id,
                    appointmentId = row.appointmentId,
                    counsellorId = row.counsellorId,
                    studentId = row.studentId,
                    privateNotes = row.privateNotes,
                    publicSummary = row.publicSummary,
                    riskLevel = row.riskLevel,
                    followUpNeeded = row.followUpNeeded,
                    followUpDue = row.followUpDue,
                    isArchived = row.isArchived,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt,
                    appointment = appointments[row.appointmentId],
                )
            },
        )
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not load session records.")))
    }

    suspend fun createRecord(
        appointmentId: String,
        privateNotes: String,
        publicSummary: String,
        riskLevel: String,
        followUpNeeded: Boolean,
        followUpDue: String?,
    ): Result<Unit> = try {
        supabase.from("session_records").insert(
            SessionRecordInsert(
                appointmentId = appointmentId,
                privateNotes = privateNotes.trim(),
                publicSummary = publicSummary.trim(),
                riskLevel = riskLevel,
                followUpNeeded = followUpNeeded,
                followUpDue = if (followUpNeeded) followUpDue else null,
            ),
        )
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not save the session record.")))
    }

    suspend fun updateRecord(
        recordId: String,
        privateNotes: String,
        publicSummary: String,
        riskLevel: String,
        followUpNeeded: Boolean,
        followUpDue: String?,
    ): Result<Unit> = try {
        supabase.from("session_records").update(
            {
                set("private_notes", privateNotes.trim())
                set("public_summary", publicSummary.trim())
                set("risk_level", riskLevel)
                set("follow_up_needed", followUpNeeded)
                set("follow_up_due", if (followUpNeeded) followUpDue else null)
            },
        ) {
            filter { eq("id", recordId) }
        }
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not update the session record.")))
    }

    suspend fun setArchived(recordId: String, archived: Boolean): Result<Unit> = try {
        supabase.from("session_records").update(
            { set("is_archived", archived) },
        ) {
            filter { eq("id", recordId) }
        }
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not update the archive state.")))
    }

    suspend fun getFreeSlotsForCounsellor(counsellorId: String): Result<List<AvailableSlotRow>> = try {
        val slots = supabase
            .from("available_slots_view")
            .select { order("start_at", Order.ASCENDING) }
            .decodeList<AvailableSlotRow>()
            .filter { it.counsellorId == counsellorId }
        Result.success(slots)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not load free slots.")))
    }

    suspend fun scheduleFollowUp(recordId: String, slotId: String): Result<Unit> = try {
        supabase.postgrest.rpc(
            "create_followup_appointment",
            buildJsonObject {
                put("p_record_id", recordId)
                put("p_slot_id", slotId)
            },
        )
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not schedule the follow-up.")))
    }

    suspend fun getStudentSummaries(): Result<List<StudentSessionSummaryRow>> = try {
        val summaries = supabase
            .from("student_session_summaries")
            .select { order("session_start", Order.DESCENDING) }
            .decodeList<StudentSessionSummaryRow>()
        Result.success(summaries)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not load your session summaries.")))
    }

    suspend fun getAdminStats(): Result<AdminDashboardStats> = try {
        val stats = supabase.postgrest
            .rpc("admin_dashboard_stats")
            .decodeAs<AdminDashboardStats>()
        Result.success(stats)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not load dashboard statistics.")))
    }
}
