// Module 2 / Unique Feature 1 | Owner: Beh
// Pure Smart Counsellor Match ranking so the rule is easy to test and explain:
// keep counsellors whose specialisations contain the concern, then rank by the
// earliest free slot.
package com.example.counselling.feature.appointment.model

internal fun buildSmartMatches(
    slots: List<AvailableSlotRow>,
    concernCategory: String,
): List<CounsellorMatch> = slots
    .filter { it.specialisations.contains(concernCategory) }
    .groupBy { it.counsellorId }
    .map { (counsellorId, counsellorSlots) ->
        val ordered = counsellorSlots.sortedBy { it.startAt }
        val first = ordered.first()
        CounsellorMatch(
            counsellorId = counsellorId,
            counsellorName = first.counsellorName,
            specialisations = first.specialisations,
            languages = first.languages,
            bio = first.bio,
            nextSlotStart = first.startAt,
            slots = ordered,
        )
    }
    .sortedBy { it.nextSlotStart }
