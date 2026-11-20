// Module 4 / Unique Feature 2 | Owner: Jeremy
package com.example.counselling.feature.records.domain

import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.RiskLevel
import com.example.counselling.feature.appointment.domain.Appointment
import java.time.Duration
import java.time.Instant

// OOP: Value object - a follow-up owns its due date and urgency windows.
class FollowUp(val dueAt: Instant) {
    fun isOverdue(now: Instant = Instant.now()): Boolean = dueAt.isBefore(now)

    fun isDueSoon(now: Instant = Instant.now()): Boolean =
        !isOverdue(now) && dueAt.isBefore(now.plus(Duration.ofDays(7)))
}

// OOP: Entity - SessionRecord HAS-A Appointment, RiskLevel and optional FollowUp.
class SessionRecord(
    val id: String,
    val appointment: Appointment,
    val privateNotes: String,
    val publicSummary: String,
    val risk: RiskLevel,
    val followUp: FollowUp?,
    val isArchived: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant?,
) {
    init {
        if (id.isBlank()) throw DomainException("Session record id cannot be blank.")
        if (privateNotes.trim().length !in 20..5000) {
            throw DomainException("Private notes must be between 20 and 5,000 characters.")
        }
        if (publicSummary.trim().length !in 1..2000) {
            throw DomainException("Public summary must be between 1 and 2,000 characters.")
        }
    }

    val appointmentId: String get() = appointment.id
    val counsellorId: String get() = appointment.counsellor.id
    val studentId: String get() = appointment.student.id
    val studentName: String get() = appointment.studentName
    val concernCategory: String get() = appointment.concern.label
    val sessionStart: Instant get() = appointment.slot.start
    val sessionStartText: String get() = sessionStart.toString()
    val isHighRisk: Boolean get() = risk.isHigh()
    val followUpNeeded: Boolean get() = followUp != null
    val followUpDue: String? get() = followUp?.dueAt?.toString()
    val riskLevel: String get() = risk.code

    fun canBeEditedAt(now: Instant = Instant.now()): Boolean =
        Duration.between(createdAt, now) < Duration.ofDays(7)

    val canEdit: Boolean get() = canBeEditedAt()

    fun archive(): SessionRecord = copy(isArchived = true)

    fun restore(): SessionRecord = copy(isArchived = false)

    fun withContent(
        privateNotes: String,
        publicSummary: String,
        risk: RiskLevel,
        followUp: FollowUp?,
        now: Instant = Instant.now(),
    ): SessionRecord {
        if (!canBeEditedAt(now)) {
            throw DomainException("Session records can only be edited within 7 days of creation.")
        }
        return SessionRecord(
            id = id,
            appointment = appointment,
            privateNotes = privateNotes,
            publicSummary = publicSummary,
            risk = risk,
            followUp = followUp,
            isArchived = isArchived,
            createdAt = createdAt,
            updatedAt = now,
        )
    }

    private fun copy(isArchived: Boolean): SessionRecord = SessionRecord(
        id = id,
        appointment = appointment,
        privateNotes = privateNotes,
        publicSummary = publicSummary,
        risk = risk,
        followUp = followUp,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    override fun equals(other: Any?): Boolean =
        this === other || (other is SessionRecord && id == other.id)

    override fun hashCode(): Int = id.hashCode()
}
