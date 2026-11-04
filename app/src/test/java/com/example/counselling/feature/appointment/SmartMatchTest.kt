// Module 2 / Unique Feature 1 | Owner: Beh
package com.example.counselling.feature.appointment

import com.example.counselling.feature.appointment.model.AvailableSlotRow
import com.example.counselling.feature.appointment.model.buildSmartMatches
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartMatchTest {

    private fun slot(
        id: String,
        counsellorId: String,
        startAt: String,
        specialisations: List<String>,
    ) = AvailableSlotRow(
        slotId = id,
        counsellorId = counsellorId,
        counsellorName = "Counsellor $counsellorId",
        specialisations = specialisations,
        languages = listOf("English"),
        bio = "",
        startAt = startAt,
        endAt = startAt,
    )

    @Test
    fun match_keepsOnlyMatchingSpecialisations() {
        val slots = listOf(
            slot("a", "c1", "2026-11-10T01:00:00+00:00", listOf("Anxiety")),
            slot("b", "c2", "2026-11-09T01:00:00+00:00", listOf("Financial")),
        )

        val matches = buildSmartMatches(slots, "Anxiety")

        assertEquals(1, matches.size)
        assertEquals("c1", matches.first().counsellorId)
    }

    @Test
    fun match_ranksCounsellorsByEarliestFreeSlot() {
        val slots = listOf(
            slot("late", "c1", "2026-11-12T01:00:00+00:00", listOf("Stress")),
            slot("early", "c2", "2026-11-09T01:00:00+00:00", listOf("Stress")),
            slot("c1-early", "c1", "2026-11-10T01:00:00+00:00", listOf("Stress")),
        )

        val matches = buildSmartMatches(slots, "Stress")

        assertEquals(listOf("c2", "c1"), matches.map { it.counsellorId })
        assertEquals("2026-11-09T01:00:00+00:00", matches[0].nextSlotStart)
        assertEquals("2026-11-10T01:00:00+00:00", matches[1].nextSlotStart)
        assertEquals(2, matches[1].slots.size)
        assertTrue(matches[1].slots.first().slotId == "c1-early")
    }
}
