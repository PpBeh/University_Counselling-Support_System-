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
        return copy(slot = newSlot, slotId = newSlotId)
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
        if (nextStatus == AppointmentStatus.Confirmed && !slot.start.isAfter(now)) {
            throw DomainException("That booking time has already passed.")
        }
        if (nextStatus == AppointmentStatus.Cancelled && !slot.start.isAfter(now)) {
            throw DomainException("This session has already started.")
        }
        return copy(status = nextStatus)
    }

    private fun copy(
        slot: TimeSlot = this.slot,
        slotId: String = this.slotId,
        status: AppointmentStatus = this.status,
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
    )

    override fun equals(other: Any?): Boolean =
        this === other || (other is Appointment && id == other.id)

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Appointment($id, ${status.wireValue})"
}
