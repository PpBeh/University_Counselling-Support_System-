// Module 2 / Unique Feature 1 tests | Owner: Beh
package com.example.counselling.feature.appointment.domain

import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.TimeSlot
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartMatchStrategyTest {

    private val now = Instant.parse("2026-11-20T09:00:00Z")

    private fun slot(
        slotId: String,
        counsellorId: String,
        name: String,
        specialisations: List<ConcernCategory>,
        startHour: Long,
        isBooked: Boolean = false,
    ) = AvailableSlot(
        slotId = slotId,
        counsellorId = counsellorId,
        counsellorName = name,
        specialisations = specialisations,
        languages = listOf("English"),
        bio = "",
        timeSlot = TimeSlot.fromStored(
            now.plusSeconds(startHour * 3600),
            now.plusSeconds((startHour + 1) * 3600),
        ),
        isBooked = isBooked,
    )

    @Test
    fun filtersBySpecialisationAndRanksByEarliestFreeSlot() {
        val slots = listOf(
            slot("s1", "c1", "Counsellor One", listOf(ConcernCategory.ANXIETY), 20),
            slot("s2", "c2", "Counsellor Two", listOf(ConcernCategory.ANXIETY), 10),
            slot("s3", "c3", "Counsellor Three", listOf(ConcernCategory.SLEEP), 5),
            slot("s4", "c1", "Counsellor One", listOf(ConcernCategory.ANXIETY), 30),
        )

        val matches = SpecialisationThenEarliestSlotStrategy()
            .match(slots, ConcernCategory.ANXIETY)

        assertEquals(listOf("c2", "c1"), matches.map { it.counsellorId })
        assertEquals("Counsellor Two", matches[0].counsellorName)
        assertEquals(2, matches[1].slots.size)
        assertTrue(matches[1].nextSlot.timeSlot.start.isBefore(matches[1].slots[1].timeSlot.start))
    }

    @Test
    fun bookedSlotsAreNotRankedAsFreeMatches() {
        val slots = listOf(
            slot(
                "s1",
                "c1",
                "Counsellor One",
                listOf(ConcernCategory.ANXIETY),
                10,
                isBooked = true,
            ),
            slot("s2", "c2", "Counsellor Two", listOf(ConcernCategory.ANXIETY), 20),
        )

        val matches = SpecialisationThenEarliestSlotStrategy()
            .match(slots, ConcernCategory.ANXIETY)

        assertEquals(listOf("c2"), matches.map { it.counsellorId })
    }

    @Test
    fun noSpecialisationMatchReturnsEmptyList() {
        val slots = listOf(
            slot("s1", "c1", "Counsellor One", listOf(ConcernCategory.SLEEP), 10),
        )
        assertTrue(
            SpecialisationThenEarliestSlotStrategy()
                .match(slots, ConcernCategory.FINANCIAL)
                .isEmpty(),
        )
    }
}
