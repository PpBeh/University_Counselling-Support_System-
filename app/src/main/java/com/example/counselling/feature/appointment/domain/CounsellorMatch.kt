// Module 2 / Unique Feature 1 | Owner: Beh
// Pure Smart Counsellor Match domain: a free slot and a ranked match.
package com.example.counselling.feature.appointment.domain

import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.TimeSlot

// OOP: Value object - one free availability slot exposed by the booking view.
class AvailableSlot(
    val slotId: String,
    val counsellorId: String,
    val counsellorName: String,
    val specialisations: List<ConcernCategory>,
    val languages: List<String>,
    val bio: String,
    val timeSlot: TimeSlot,
    val isBooked: Boolean = false,
) {
    val startAt: String get() = timeSlot.start.toString()
    val endAt: String get() = timeSlot.end.toString()
    val specialisationLabels: List<String> get() = specialisations.map { it.label }

    init {
        if (slotId.isBlank() || counsellorId.isBlank()) {
            throw com.example.counselling.core.domain.DomainException(
                "A free slot needs its slot and counsellor ids.",
            )
        }
    }
}

// OOP: Entity/read model - CounsellorMatch HAS-A list of AvailableSlot objects.
class CounsellorMatch(
    val counsellorId: String,
    val counsellorName: String,
    val specialisations: List<ConcernCategory>,
    val languages: List<String>,
    val bio: String,
    val nextSlot: AvailableSlot,
    val slots: List<AvailableSlot>,
) {
    val nextSlotStart: String get() = nextSlot.timeSlot.start.toString()
    val specialisationLabels: List<String> get() = specialisations.map { it.label }

    init {
        if (slots.isEmpty()) {
            throw com.example.counselling.core.domain.DomainException(
                "A counsellor match needs at least one free slot.",
            )
        }
    }
}

// OOP: Strategy - Smart Match can be swapped without changing ViewModels.
interface CounsellorMatchStrategy {
    fun match(
        slots: List<AvailableSlot>,
        concern: ConcernCategory,
    ): List<CounsellorMatch>
}

// OOP: Strategy implementation - filter by specialisation, then rank by the
// earliest free slot. This is the original Smart Match rule, now polymorphic.
class SpecialisationThenEarliestSlotStrategy : CounsellorMatchStrategy {

    override fun match(
        slots: List<AvailableSlot>,
        concern: ConcernCategory,
    ): List<CounsellorMatch> = slots
        .filter { concern in it.specialisations && !it.isBooked }
        .groupBy { it.counsellorId }
        .mapNotNull { (counsellorId, counsellorSlots) ->
            val ordered = counsellorSlots.sortedBy { it.timeSlot.start }
            val first = ordered.firstOrNull() ?: return@mapNotNull null
            CounsellorMatch(
                counsellorId = counsellorId,
                counsellorName = first.counsellorName,
                specialisations = first.specialisations,
                languages = first.languages,
                bio = first.bio,
                nextSlot = first,
                slots = ordered,
            )
        }
        .sortedBy { it.nextSlot.timeSlot.start }
}
