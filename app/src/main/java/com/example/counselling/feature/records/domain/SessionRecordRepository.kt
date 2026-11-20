// Module 4 | Owner: Jeremy
package com.example.counselling.feature.records.domain

import com.example.counselling.core.domain.RiskLevel
import com.example.counselling.feature.appointment.domain.AvailableSlot
import java.time.Instant

// OOP: Abstraction - records/student-summary ViewModels depend on this contract.
interface SessionRecordRepository {

    suspend fun getRecords(): Result<List<SessionRecord>>

    suspend fun createRecord(
        appointmentId: String,
        privateNotes: String,
        publicSummary: String,
        risk: RiskLevel,
        followUp: FollowUp?,
    ): Result<Unit>

    suspend fun updateRecord(
        recordId: String,
        privateNotes: String,
        publicSummary: String,
        risk: RiskLevel,
        followUp: FollowUp?,
    ): Result<Unit>

    suspend fun setArchived(recordId: String, archived: Boolean): Result<Unit>

    suspend fun deleteRecord(recordId: String): Result<Unit>

    suspend fun getFreeSlotsForCounsellor(counsellorId: String): Result<List<AvailableSlot>>

    suspend fun scheduleFollowUp(recordId: String, slotId: String): Result<Unit>

    suspend fun getStudentSummaries(): Result<List<StudentSessionSummary>>

    suspend fun getAdminStats(): Result<AdminDashboardStats>
}

// OOP: Value object - safe projection shown to students.
class StudentSessionSummary(
    val recordId: String,
    val appointmentId: String,
    val sessionStart: Instant,
    val concernCategory: String,
    val counsellorName: String,
    val publicSummary: String,
    val summaryCreatedAt: Instant,
) {
    val sessionStartText: String get() = sessionStart.toString()
    val summaryCreatedAtText: String get() = summaryCreatedAt.toString()
}

// OOP: Value object - aggregate-only admin dashboard data.
class AdminDashboardStats(
    val totalStudents: Int,
    val activeStudents: Int,
    val totalCounsellors: Int,
    val activeCounsellors: Int,
    val totalAppointments: Int,
    val upcomingAppointments: Int,
    val completedAppointments: Int,
    val sessionRecords: Int,
    val totalResources: Int,
    val publishedResources: Int,
    val feedbackCount: Int,
    val averageRating: Double,
)
