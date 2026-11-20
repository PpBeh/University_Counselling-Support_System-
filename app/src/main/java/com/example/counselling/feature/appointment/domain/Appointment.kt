// Module 2 | Owner: Beh
// Appointment entity. It HAS-A Student, Counsellor, TimeSlot, SessionMode,
// ConcernCategory and AppointmentStatus, and owns the reschedule/cancel rules.
package com.example.counselling.feature.appointment.domain

import com.example.counselling.core.domain.AppointmentStatus
import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.Counsellor
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.SessionMode
import com.example.counselling.core.domain.Student
import com.example.counselling.core.domain.TimeSlot
import com.example.counselling.core.domain.User
import java.time.Duration
import java.time.Instant

// OOP: Entity - behaviour lives here; callers cannot simply assign a new status
// or slot through public mutable fields.
class Appointment(
    val id: String,
    val student: Student,
    val counsellor: Counsellor,
    val slotId: String,
    val slot: TimeSlot,
    val mode: SessionMode,
    val concern: ConcernCategory,
    val note: String?,
    val status: AppointmentStatus,
    val followUpOf: String? = null,
    val createdAt: Instant? = null,
    val lastEvent: String = "booked",
    val updatedAt: Instant? = null,
) {

    init {
        if (id.isBlank()) throw DomainException("Appointment id cannot be blank.")
        if (slotId.isBlank()) throw DomainException("Appointment slot id cannot be blank.")
        if ((note?.trim()?.length ?: 0) > 1000) {
            throw DomainException("Your note must be 1,000 characters or fewer.")
        }
        if (followUpOf == id) {
            throw DomainException("An appointment cannot follow itself.")
        }
    }

    val isFollowUp: Boolean get() = followUpOf != null

    // Presentation-friendly immutable projections. They do not weaken the
    // entity: the underlying relationships are still Student/Counsellor/slot.
    val studentName: String get() = student.fullName
    val counsellorName: String get() = counsellor.fullName
    val concernCategory: String get() = concern.label
    val studentCode: String get() = student.studentId.value
    val studentEmail: String get() = student.email.value
    val studentPhone: String? get() = student.phone?.value
    val counsellorSpecialisations: List<String>
        get() = counsellor.specialisations.map { it.label }
    val slotStart: String get() = slot.start.toString()
    val slotEnd: String get() = slot.end.toString()

    fun canBeRescheduledBy(actor: User, now: Instant = Instant.now()): Boolean {
        if (actor is Student) {
            return actor.id == student.id &&
                status in setOf(AppointmentStatus.Pending, AppointmentStatus.Confirmed) &&
                !slot.start.isBefore(now.plus(Duration.ofHours(12)))
        }
        if (actor is Counsellor) {
            return actor.id == counsellor.id &&
                status == AppointmentStatus.Confirmed &&
                slot.start.isAfter(now)
        }
        return false
    }

    fun canConfirmFollowUp(actor: User, now: Instant = Instant.now()): Boolean =
        actor is Student &&
            actor.id == student.id &&
            isFollowUp &&
            status == AppointmentStatus.Pending &&
            lastEvent == "followup_requested" &&
            slot.start.isAfter(now)

    // OOP: Polymorphism - the card asks the appointment what the signed-in
    // participant should be alerted about; no screen needs a role string.
    fun noticeFor(actor: User): String? {
        if (actor is Student) {
            if (canConfirmFollowUp(actor)) return "Confirm your follow-up"
            return when (lastEvent) {
                "confirmed" -> "Your booking is confirmed"
                "rescheduled_by_counsellor" -> "The counsellor changed the time"
                "cancelled_by_counsellor" -> "The counsellor cancelled this session"
                "completed" -> "Session completed — rate it"
                "no_show" -> "This session was marked no-show"
                "followup_requested" -> "Your counsellor proposed a follow-up"
                else -> null
            }
        }
        if (actor is Counsellor) {
            if (isFollowUp && status == AppointmentStatus.Pending) {
                return if (lastEvent == "rescheduled_by_student") {
                    "Student rescheduled the follow-up — reconfirm it"
                } else {
                    "Waiting for the student to confirm the follow-up"
                }
            }
            if (status == AppointmentStatus.Pending && !isFollowUp) {
                return if (lastEvent == "rescheduled_by_student") {
                    "Student rescheduled — review the request"
                } else {
                    "New booking request — review it"
                }
            }
            return when (lastEvent) {
                "followup_confirmed_by_student" -> "Student confirmed the follow-up"
                "cancelled_by_student" -> "Student cancelled this session"
                "rescheduled_by_student" -> "Student changed the time"
                else -> null
            }
        }
        return null
    }

    fun canBeCancelledBy(actor: User, now: Instant = Instant.now()): Boolean {
        if (actor is Student) {
            return actor.id == student.id &&
                status in setOf(AppointmentStatus.Pending, AppointmentStatus.Confirmed) &&
                slot.start.isAfter(now)
        }
        if (actor is Counsellor) {
            return actor.id == counsellor.id &&
                status in setOf(AppointmentStatus.Pending, AppointmentStatus.Confirmed) &&
                slot.start.isAfter(now)
        }
        return false
    }

    // OOP: Encapsulation - the 12-hour student notice rule and the counsellor
    // emergency exception are both methods on the entity.
    fun reschedule(
        newSlotId: String,
        newSlot: TimeSlot,
        now: Instant = Instant.now(),
        actor: User,
    ): Appointment {
        if (newSlotId.isBlank() || newSlotId == slotId) {
            throw DomainException("Please choose a different time slot.")
        }
        if (!newSlot.start.isAfter(now)) {
            throw DomainException("That new time slot has already passed.")
        }
        if (!canBeRescheduledBy(actor, now)) {
            when {
                actor is Student -> throw DomainException(
                    "Appointments can only be rescheduled at least 12 hours in advance.",
                )
                actor is Counsellor -> throw DomainException(
                    "Only confirmed sessions can be rescheduled by the counsellor.",
                )
                else -> throw DomainException("You cannot reschedule this appointment.")
            }
        }
        // A student move returns to Pending so the counsellor must reconfirm
        // the new time. A counsellor emergency move stays Confirmed.
        return copy(
            slot = newSlot,
            slotId = newSlotId,
            status = if (actor is Student) AppointmentStatus.Pending else status,
        )
    }

    // OOP: Encapsulation - cancellation is only valid before the session starts.
    fun cancel(
        now: Instant = Instant.now(),
        actor: User,
    ): Appointment {
        if (!canBeCancelledBy(actor, now)) {
            if (!slot.start.isAfter(now)) {
                throw DomainException("This appointment has already started.")
            }
            throw DomainException("You cannot cancel this appointment.")
        }
        return copy(status = AppointmentStatus.Cancelled)
    }

    fun withStatus(
        nextStatus: AppointmentStatus,
        now: Instant = Instant.now(),
        actor: User,
    ): Appointment {
        if (actor !is Counsellor || actor.id != counsellor.id) {
            throw DomainException("Only the assigned counsellor can change this status.")
        }
        if (!status.canTransitionTo(nextStatus)) {
            throw DomainException("That appointment status change is not allowed.")
        }
        if (
            nextStatus == AppointmentStatus.Confirmed &&
            isFollowUp &&
            lastEvent != "rescheduled_by_student"
        ) {
            throw DomainException("A proposed follow-up must be confirmed by the student.")
        }
        if (nextStatus == AppointmentStatus.Confirmed && !slot.start.isAfter(now)) {
            throw DomainException("That booking time has already passed.")
        }
        if (nextStatus == AppointmentStatus.Cancelled && !slot.start.isAfter(now)) {
            throw DomainException("This session has already started.")
        }
        return copy(status = nextStatus)
    }

    // Used only after a successful server action to reflect the accepted state
    // immediately while the fresh list loads.
    fun withServerStatus(
        nextStatus: AppointmentStatus,
        event: String = lastEvent,
    ): Appointment = copy(status = nextStatus, lastEvent = event)

    private fun copy(
        slot: TimeSlot = this.slot,
        slotId: String = this.slotId,
        status: AppointmentStatus = this.status,
        lastEvent: String = this.lastEvent,
    ): Appointment = Appointment(
        id = id,
        student = student,
        counsellor = counsellor,
        slotId = slotId,
        slot = slot,
        mode = mode,
        concern = concern,
        note = note,
        status = status,
        followUpOf = followUpOf,
        createdAt = createdAt,
        lastEvent = lastEvent,
        updatedAt = updatedAt,
    )

    override fun equals(other: Any?): Boolean =
        this === other || (other is Appointment && id == other.id)

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Appointment($id, ${status.wireValue})"
}
