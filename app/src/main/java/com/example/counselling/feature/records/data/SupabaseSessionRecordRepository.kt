// Module 4 / Unique Feature 2 | Owner: Jeremy
package com.example.counselling.feature.records.data

import com.example.counselling.core.data.BaseSupabaseRepository
import com.example.counselling.core.data.ErrorTranslator
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.RiskLevel
import com.example.counselling.feature.appointment.domain.AppointmentRepository
import com.example.counselling.feature.appointment.domain.AvailableSlot
import com.example.counselling.feature.records.domain.AdminDashboardStats
import com.example.counselling.feature.records.domain.FollowUp
import com.example.counselling.feature.records.domain.SessionRecord
import com.example.counselling.feature.records.domain.SessionRecordRepository
import com.example.counselling.feature.records.domain.StudentSessionSummary
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
private data class SessionRecordTableRow(
    val id: String,
    @SerialName("appointment_id") val appointmentId: String,
    @SerialName("counsellor_id") val counsellorId: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("private_notes") val privateNotes: String,
    @SerialName("public_summary") val publicSummary: String,
    @SerialName("risk_code") val riskCode: String,
    @SerialName("follow_up_needed") val followUpNeeded: Boolean,
    @SerialName("follow_up_due") val followUpDue: String? = null,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class SessionRecordInsert(
    @SerialName("appointment_id") val appointmentId: String,
    @SerialName("private_notes") val privateNotes: String,
    @SerialName("public_summary") val publicSummary: String,
    @SerialName("risk_code") val riskCode: String,
    @SerialName("follow_up_needed") val followUpNeeded: Boolean,
    @SerialName("follow_up_due") val followUpDue: String? = null,
)

@Serializable
private data class StudentSummaryRow(
    @SerialName("record_id") val recordId: String,
    @SerialName("appointment_id") val appointmentId: String,
    @SerialName("session_start") val sessionStart: String,
    @SerialName("concern_category") val concernCategory: String,
    @SerialName("counsellor_name") val counsellorName: String,
    @SerialName("public_summary") val publicSummary: String,
    @SerialName("summary_created_at") val summaryCreatedAt: String,
)

@Serializable
private data class AdminStatsRow(
    @SerialName("total_students") val totalStudents: Int = 0,
    @SerialName("active_students") val activeStudents: Int = 0,
    @SerialName("total_counsellors") val totalCounsellors: Int = 0,
    @SerialName("active_counsellors") val activeCounsellors: Int = 0,
    @SerialName("total_appointments") val totalAppointments: Int = 0,
    @SerialName("upcoming_appointments") val upcomingAppointments: Int = 0,
    @SerialName("completed_appointments") val completedAppointments: Int = 0,
    @SerialName("session_records") val sessionRecords: Int = 0,
    @SerialName("total_resources") val totalResources: Int = 0,
    @SerialName("published_resources") val publishedResources: Int = 0,
    @SerialName("feedback_count") val feedbackCount: Int = 0,
    @SerialName("average_rating") val averageRating: Double = 0.0,
)

// OOP: Repository implementation - records are domain objects; appointment
// composition is loaded through the AppointmentRepository interface.
class SupabaseSessionRecordRepository(
    supabase: SupabaseClient,
    private val appointmentRepository: AppointmentRepository,
    errorTranslator: ErrorTranslator = ErrorTranslator(),
) : BaseSupabaseRepository(supabase, errorTranslator), SessionRecordRepository {

    override suspend fun getRecords(): Result<List<SessionRecord>> =
        runQuery("Could not load session records.") {
            val rows = supabase
                .from("session_records")
                .select { order("created_at", Order.DESCENDING) }
                .decodeList<SessionRecordTableRow>()
            val appointments = appointmentRepository.getAppointments()
                .getOrElse { throw it }
                .associateBy { it.id }

            rows.map { row ->
                val appointment = appointments[row.appointmentId]
                    ?: throw DomainException("A session record has no matching appointment.")
                SessionRecord(
                    id = row.id,
                    appointment = appointment,
                    privateNotes = row.privateNotes,
                    publicSummary = row.publicSummary,
                    risk = RiskLevel.fromWire(row.riskCode)
                        ?: throw DomainException("Unknown risk level: ${row.riskCode}"),
                    followUp = row.followUpDue?.let { FollowUp(parseInstant(it)) },
                    isArchived = row.isArchived,
                    createdAt = parseInstant(row.createdAt),
                    updatedAt = row.updatedAt?.let(::parseInstant),
                )
            }
        }

    override suspend fun createRecord(
        appointmentId: String,
        privateNotes: String,
        publicSummary: String,
        risk: RiskLevel,
        followUp: FollowUp?,
    ): Result<Unit> = runQuery("Could not save the session record.") {
        supabase.from("session_records").insert(
            SessionRecordInsert(
                appointmentId = appointmentId,
                privateNotes = privateNotes.trim(),
                publicSummary = publicSummary.trim(),
                riskCode = risk.code,
                followUpNeeded = followUp != null,
                followUpDue = followUp?.dueAt?.toString(),
            ),
        )
        Unit
    }

    override suspend fun updateRecord(
        recordId: String,
        privateNotes: String,
        publicSummary: String,
        risk: RiskLevel,
        followUp: FollowUp?,
    ): Result<Unit> = runQuery("Could not update the session record.") {
        supabase.from("session_records").update(
            {
                set("private_notes", privateNotes.trim())
                set("public_summary", publicSummary.trim())
                set("risk_code", risk.code)
                set("follow_up_needed", followUp != null)
                set("follow_up_due", followUp?.dueAt?.toString())
            },
        ) {
            filter { eq("id", recordId) }
        }
        Unit
    }

    override suspend fun setArchived(recordId: String, archived: Boolean): Result<Unit> =
        runQuery("Could not update the archive state.") {
            supabase.from("session_records").update(
                { set("is_archived", archived) },
            ) {
                filter { eq("id", recordId) }
            }
            Unit
        }

    override suspend fun deleteRecord(recordId: String): Result<Unit> =
        runQuery("Could not delete the session record.") {
            supabase.from("session_records").delete {
                filter { eq("id", recordId) }
            }
            Unit
        }

    override suspend fun getFreeSlotsForCounsellor(
        counsellorId: String,
    ): Result<List<AvailableSlot>> =
        appointmentRepository.getAvailableSlots().map { slots ->
            slots.filter { it.counsellorId == counsellorId }
        }

    override suspend fun scheduleFollowUp(recordId: String, slotId: String): Result<Unit> =
        runQuery("Could not schedule the follow-up.") {
            supabase.postgrest.rpc(
                "create_followup_appointment",
                buildJsonObject {
                    put("p_record_id", recordId)
                    put("p_slot_id", slotId)
                },
            )
            Unit
        }

    override suspend fun getStudentSummaries(): Result<List<StudentSessionSummary>> =
        runQuery("Could not load your session summaries.") {
            supabase
                .from("student_session_summaries")
                .select { order("session_start", Order.DESCENDING) }
                .decodeList<StudentSummaryRow>()
                .map {
                    StudentSessionSummary(
                        recordId = it.recordId,
                        appointmentId = it.appointmentId,
                        sessionStart = parseInstant(it.sessionStart),
                        concernCategory = it.concernCategory,
                        counsellorName = it.counsellorName,
                        publicSummary = it.publicSummary,
                        summaryCreatedAt = parseInstant(it.summaryCreatedAt),
                    )
                }
        }

    override suspend fun getAdminStats(): Result<AdminDashboardStats> =
        runQuery("Could not load dashboard statistics.") {
            val row = supabase.postgrest
                .rpc("admin_dashboard_stats")
                .decodeAs<AdminStatsRow>()
            AdminDashboardStats(
                totalStudents = row.totalStudents,
                activeStudents = row.activeStudents,
                totalCounsellors = row.totalCounsellors,
                activeCounsellors = row.activeCounsellors,
                totalAppointments = row.totalAppointments,
                upcomingAppointments = row.upcomingAppointments,
                completedAppointments = row.completedAppointments,
                sessionRecords = row.sessionRecords,
                totalResources = row.totalResources,
                publishedResources = row.publishedResources,
                feedbackCount = row.feedbackCount,
                averageRating = row.averageRating,
            )
        }

    private fun parseInstant(value: String): Instant =
        runCatching { Instant.parse(value) }
            .recoverCatching { OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { ZonedDateTime.parse(value).toInstant() }
            .getOrElse { throw DomainException("A date value could not be read: $value") }
}
