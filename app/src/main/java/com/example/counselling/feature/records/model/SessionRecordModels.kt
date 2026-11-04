// Module 4 / Unique Feature 2 | Owner: Jeremy
// Private session records, student-safe summaries and admin aggregate stats.
package com.example.counselling.feature.records.model

import com.example.counselling.core.DateTimeUtils
import com.example.counselling.feature.appointment.model.Appointment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.temporal.ChronoUnit

@Serializable
data class SessionRecordRow(
    val id: String,
    @SerialName("appointment_id") val appointmentId: String,
    @SerialName("counsellor_id") val counsellorId: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("private_notes") val privateNotes: String,
    @SerialName("public_summary") val publicSummary: String,
    @SerialName("risk_level") val riskLevel: String,
    @SerialName("follow_up_needed") val followUpNeeded: Boolean,
    @SerialName("follow_up_due") val followUpDue: String? = null,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class SessionRecordInsert(
    @SerialName("appointment_id") val appointmentId: String,
    @SerialName("private_notes") val privateNotes: String,
    @SerialName("public_summary") val publicSummary: String,
    @SerialName("risk_level") val riskLevel: String,
    @SerialName("follow_up_needed") val followUpNeeded: Boolean,
    @SerialName("follow_up_due") val followUpDue: String? = null,
)

@Serializable
data class StudentSessionSummaryRow(
    @SerialName("record_id") val recordId: String,
    @SerialName("appointment_id") val appointmentId: String,
    @SerialName("session_start") val sessionStart: String,
    @SerialName("concern_category") val concernCategory: String,
    @SerialName("counsellor_name") val counsellorName: String,
    @SerialName("public_summary") val publicSummary: String,
    @SerialName("summary_created_at") val summaryCreatedAt: String,
)

@Serializable
data class AdminDashboardStats(
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

data class SessionRecord(
    val id: String,
    val appointmentId: String,
    val counsellorId: String,
    val studentId: String,
    val privateNotes: String,
    val publicSummary: String,
    val riskLevel: String,
    val followUpNeeded: Boolean,
    val followUpDue: String?,
    val isArchived: Boolean,
    val createdAt: String,
    val updatedAt: String?,
    val appointment: Appointment?,
) {
    val studentName: String get() = appointment?.studentName ?: "Student"
    val concernCategory: String get() = appointment?.concernCategory ?: "Counselling session"
    val sessionStart: String? get() = appointment?.slotStart
    val isHighRisk: Boolean get() = riskLevel == "high"

    val canEdit: Boolean
        get() {
            val created = DateTimeUtils.parse(createdAt) ?: return false
            return ChronoUnit.DAYS.between(created, DateTimeUtils.nowInKualaLumpur()) < 7
        }
}

enum class TriageBucket(val label: String) {
    OVERDUE("Overdue"),
    DUE_SOON("Due soon"),
    LATER("Later"),
}

fun SessionRecord.triageBucket(): TriageBucket {
    val due = DateTimeUtils.parse(followUpDue)
    val now = DateTimeUtils.nowInKualaLumpur()
    return when {
        due == null -> if (isHighRisk) TriageBucket.DUE_SOON else TriageBucket.LATER
        due.isBefore(now) -> TriageBucket.OVERDUE
        due.isBefore(now.plusDays(7)) -> TriageBucket.DUE_SOON
        else -> TriageBucket.LATER
    }
}

fun List<SessionRecord>.triageRecords(): Map<TriageBucket, List<SessionRecord>> {
    val filtered = filter { !it.isArchived && (it.isHighRisk || it.followUpNeeded) }
    return TriageBucket.entries.associateWith { bucket ->
        filtered.filter { it.triageBucket() == bucket }
            .sortedBy { it.followUpDue ?: "9999" }
    }
}
