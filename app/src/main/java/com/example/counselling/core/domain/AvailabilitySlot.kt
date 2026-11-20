// Module 3 | Owner: Jeremy
// AvailabilitySlot is an entity owned by a Counsellor. It HAS-A TimeSlot.
package com.example.counselling.core.domain

import java.time.Instant

// OOP: Entity - a slot knows whether it is bookable, in the past, or overlaps
// another slot; callers do not recompute those rules ad hoc.
class AvailabilitySlot(
    val id: String,
    val counsellorId: String,
    val timeSlot: TimeSlot,
    val isBooked: Boolean = false,
) {
    init {
        if (id.isBlank() || counsellorId.isBlank()) {
            throw DomainException("An availability slot needs its id and counsellor id.")
        }
    }

    val startAt: String get() = timeSlot.start.toString()
    val endAt: String get() = timeSlot.end.toString()

    fun isPast(now: Instant = Instant.now()): Boolean = !timeSlot.start.isAfter(now)

    fun isEditable(now: Instant = Instant.now()): Boolean = !isBooked && !isPast(now)

    fun overlaps(other: AvailabilitySlot): Boolean = timeSlot.overlaps(other.timeSlot)
}
