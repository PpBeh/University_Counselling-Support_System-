// Module 2 domain tests | Owner: Beh
package com.example.counselling.feature.appointment.domain

import com.example.counselling.core.domain.AppointmentStatus
import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.Counsellor
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.Email
import com.example.counselling.core.domain.PhoneNumber
import com.example.counselling.core.domain.SessionMode
import com.example.counselling.core.domain.Student
import com.example.counselling.core.domain.StudentId
import com.example.counselling.core.domain.TimeSlot
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentEntityTest {

    private val now = Instant.parse("2026-11-20T09:00:00Z")
    private val student = Student(
        id = "student-1",
        email = Email.of("aina@example.com"),
        fullName = "Aina Zulaikha",
        phone = PhoneNumber.of("012-3456789"),
        isActive = true,
        studentId = StudentId.of("DAMCS2401"),
        programme = "Diploma in Computer Science",
        preferredMode = SessionMode.IN_PERSON,
    )
    private val counsellor = Counsellor(
        id = "counsellor-1",
        email = Email.of("aisyah@example.com"),
        fullName = "Dr. Aisyah Rahman",
        phone = null,
        isActive = true,
        specialisations = listOf(ConcernCategory.ANXIETY),
        languages = listOf("English"),
        bio = "Campus counsellor",
    )

    private fun slot(startHoursFromNow: Long, endHoursFromNow: Long = startHoursFromNow + 1) =
        TimeSlot.fromStored(
            now.plusSeconds(startHoursFromNow * 3600),
            now.plusSeconds(endHoursFromNow * 3600),
        )

    private fun appointment(
        startHoursFromNow: Long = 48,
        status: AppointmentStatus = AppointmentStatus.Confirmed,
        followUpOf: String? = null,
        lastEvent: String = "booked",
    ) = Appointment(
        id = "appointment-1",
        student = student,
        counsellor = counsellor,
        slotId = "slot-1",
        slot = slot(startHoursFromNow),
        mode = SessionMode.IN_PERSON,
        concern = ConcernCategory.ANXIETY,
        note = "First session",
        status = status,
        followUpOf = followUpOf,
        lastEvent = lastEvent,
    )

    @Test
    fun studentReschedule_needsTwelveHoursNotice() {
        val tooSoon = appointment(startHoursFromNow = 6, status = AppointmentStatus.Pending)
        val later = appointment(startHoursFromNow = 13, status = AppointmentStatus.Pending)

        assertFalse(tooSoon.canBeRescheduledBy(student, now))
        assertTrue(later.canBeRescheduledBy(student, now))
        assertThrows(DomainException::class.java) {
            tooSoon.reschedule("slot-2", slot(24), now, student)
        }
    }

    @Test
    fun studentReschedule_returnsConfirmedAppointmentToPendingForCounsellorReconfirmation() {
        val confirmed = appointment(startHoursFromNow = 48, status = AppointmentStatus.Confirmed)
        val moved = confirmed.reschedule("slot-2", slot(72), now, student)
        assertEquals(AppointmentStatus.Pending, moved.status)
        assertEquals("slot-2", moved.slotId)
    }

    @Test
    fun counsellorReschedule_isAllowedWithoutTwelveHourRuleForConfirmedOnly() {
        val confirmed = appointment(startHoursFromNow = 2, status = AppointmentStatus.Confirmed)
        val pending = appointment(startHoursFromNow = 2, status = AppointmentStatus.Pending)

        assertTrue(confirmed.canBeRescheduledBy(counsellor, now))
        assertFalse(pending.canBeRescheduledBy(counsellor, now))
        val moved = confirmed.reschedule("slot-2", slot(5), now, counsellor)
        assertEquals("slot-2", moved.slotId)
        assertEquals(AppointmentStatus.Confirmed, moved.status)
        assertThrows(DomainException::class.java) {
            pending.reschedule("slot-2", slot(5), now, counsellor)
        }
    }

    @Test
    fun cancel_isBlockedAfterStartAndAllowedBefore() {
        val future = appointment(startHoursFromNow = 2)
        val started = appointment(startHoursFromNow = -1)
        assertEquals(AppointmentStatus.Cancelled, future.cancel(now, student).status)
        assertThrows(DomainException::class.java) { started.cancel(now, student) }
    }

    @Test
    fun followUp_isConfirmedByStudentNotCounsellorAndProducesNotices() {
        val proposed = appointment(
            startHoursFromNow = 48,
            status = AppointmentStatus.Pending,
            followUpOf = "appointment-0",
            lastEvent = "followup_requested",
        )

        assertTrue(proposed.canConfirmFollowUp(student, now))
        assertFalse(proposed.canConfirmFollowUp(counsellor, now))
        assertEquals("Confirm your follow-up", proposed.noticeFor(student))
        assertEquals(
            "Waiting for the student to confirm the follow-up",
            proposed.noticeFor(counsellor),
        )

        assertThrows(DomainException::class.java) {
            proposed.withStatus(AppointmentStatus.Confirmed, now, counsellor)
        }

        val confirmed = appointment(
            startHoursFromNow = 48,
            status = AppointmentStatus.Confirmed,
            followUpOf = "appointment-0",
            lastEvent = "followup_confirmed_by_student",
        )
        assertEquals("Student confirmed the follow-up", confirmed.noticeFor(counsellor))
    }

    @Test
    fun studentRescheduledFollowUpNeedsCounsellorReconfirmation() {
        val rescheduled = appointment(
            startHoursFromNow = 48,
            status = AppointmentStatus.Pending,
            followUpOf = "appointment-0",
            lastEvent = "rescheduled_by_student",
        )

        assertFalse(rescheduled.canConfirmFollowUp(student, now))
        assertEquals(
            "Student rescheduled the follow-up — reconfirm it",
            rescheduled.noticeFor(counsellor),
        )
        assertEquals(
            AppointmentStatus.Confirmed,
            rescheduled.withStatus(AppointmentStatus.Confirmed, now, counsellor).status,
        )
    }

    @Test
    fun notices_distinguishConfirmationRescheduleAndCancellation() {
        assertEquals(
            "Your booking is confirmed",
            appointment(status = AppointmentStatus.Confirmed, lastEvent = "confirmed")
                .noticeFor(student),
        )
        assertEquals(
            "The counsellor changed the time",
            appointment(status = AppointmentStatus.Confirmed, lastEvent = "rescheduled_by_counsellor")
                .noticeFor(student),
        )
        assertEquals(
            "The counsellor cancelled this session",
            appointment(status = AppointmentStatus.Cancelled, lastEvent = "cancelled_by_counsellor")
                .noticeFor(student),
        )
        assertEquals(
            "Student cancelled this session",
            appointment(status = AppointmentStatus.Cancelled, lastEvent = "cancelled_by_student")
                .noticeFor(counsellor),
        )
    }

    @Test
    fun statusChange_followsStateAndTimeRules() {
        val confirmed = appointment()
        assertEquals(
            AppointmentStatus.Completed,
            confirmed.withStatus(AppointmentStatus.Completed, now, counsellor).status,
        )
        val pending = appointment(status = AppointmentStatus.Pending)
        assertThrows(DomainException::class.java) {
            pending.withStatus(AppointmentStatus.Completed, now, counsellor)
        }
        assertThrows(DomainException::class.java) {
            confirmed.withStatus(AppointmentStatus.Completed, now, student)
        }
    }
}
