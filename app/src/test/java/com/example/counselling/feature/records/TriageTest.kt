// Module 4 / Unique Feature 2 | Owner: Jeremy
package com.example.counselling.feature.records

import com.example.counselling.core.domain.AppointmentStatus
import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.Counsellor
import com.example.counselling.core.domain.Email
import com.example.counselling.core.domain.RiskLevel
import com.example.counselling.core.domain.SessionMode
import com.example.counselling.core.domain.Student
import com.example.counselling.core.domain.StudentId
import com.example.counselling.core.domain.TimeSlot
import com.example.counselling.feature.appointment.domain.Appointment
import com.example.counselling.feature.records.domain.FollowUp
import com.example.counselling.feature.records.domain.SessionRecord
import com.example.counselling.feature.records.domain.TriageBoard
import com.example.counselling.feature.records.domain.TriageBucket
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TriageTest {

    private val now = Instant.parse("2026-11-20T09:00:00Z")
    private val student = Student(
        id = "student-1",
        email = Email.of("aina@example.com"),
        fullName = "Aina",
        phone = null,
        isActive = true,
        studentId = StudentId.of("DAMCS2401"),
        programme = "Diploma in Computer Science",
        preferredMode = SessionMode.IN_PERSON,
    )
    private val counsellor = Counsellor(
        id = "counsellor-1",
        email = Email.of("counsellor@example.com"),
        fullName = "Counsellor",
        phone = null,
        isActive = true,
        specialisations = listOf(ConcernCategory.ANXIETY),
        languages = listOf("English"),
        bio = "",
    )
    private val appointment = Appointment(
        id = "appointment-1",
        student = student,
        counsellor = counsellor,
        slotId = "slot-1",
        slot = TimeSlot.fromStored(
            now.plusSeconds(86400),
            now.plusSeconds(90000),
        ),
        mode = SessionMode.IN_PERSON,
        concern = ConcernCategory.ANXIETY,
        note = null,
        status = AppointmentStatus.Completed,
    )

    private fun record(
        id: String,
        risk: RiskLevel,
        due: Instant?,
        archived: Boolean = false,
    ) = SessionRecord(
        id = id,
        appointment = appointment,
        privateNotes = "Private notes for $id are long enough.",
        publicSummary = "Summary $id",
        risk = risk,
        followUp = due?.let(::FollowUp),
        isArchived = archived,
        createdAt = now,
        updatedAt = null,
    )

    @Test
    fun triage_groupsOverdueSoonLaterAndExcludesIrrelevant() {
        val board = TriageBoard(now)
        val triage = board.group(
            listOf(
                record("overdue", RiskLevel.LOW, now.minusSeconds(2 * 86400)),
                record("soon", RiskLevel.HIGH, null),
                record("later", RiskLevel.MEDIUM, now.plusSeconds(10 * 86400)),
                record("no-follow-up", RiskLevel.LOW, null),
                record("archived", RiskLevel.HIGH, now.minusSeconds(86400), archived = true),
            ),
        )

        assertEquals(listOf("overdue"), triage[TriageBucket.OVERDUE]?.map { it.id })
        assertEquals(listOf("soon"), triage[TriageBucket.DUE_SOON]?.map { it.id })
        assertEquals(listOf("later"), triage[TriageBucket.LATER]?.map { it.id })
        assertTrue(triage.values.flatten().none { it.id == "no-follow-up" })
        assertTrue(triage.values.flatten().none { it.id == "archived" })
    }

    @Test
    fun dueSoon_includesFollowUpWithinSevenDays() {
        val triage = TriageBoard(now).group(
            listOf(
                record("three-days", RiskLevel.LOW, now.plusSeconds(3 * 86400)),
                record("eight-days", RiskLevel.LOW, now.plusSeconds(8 * 86400)),
            ),
        )

        assertEquals(listOf("three-days"), triage[TriageBucket.DUE_SOON]?.map { it.id })
        assertEquals(listOf("eight-days"), triage[TriageBucket.LATER]?.map { it.id })
    }
}
