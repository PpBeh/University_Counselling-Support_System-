// Module 3 | Owner: Jeremy
package com.example.counselling.feature.counsellor

import com.example.counselling.feature.counsellor.model.buildWeeklySlots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class AvailabilityRulesTest {

    @Test
    fun threeWeekRepeat_createsThreeRowsSevenDaysApart() {
        val start = Instant.parse("2026-11-10T01:00:00Z")
        val end = Instant.parse("2026-11-10T02:00:00Z")

        val slots = buildWeeklySlots(start, end, 3)

        assertEquals(3, slots.size)
        assertEquals(7, ChronoUnit.DAYS.between(slots[0].first, slots[1].first))
        assertEquals(14, ChronoUnit.DAYS.between(slots[0].first, slots[2].first))
        assertTrue(slots.all { ChronoUnit.MINUTES.between(it.first, it.second) == 60L })
    }

    @Test
    fun endBeforeStart_throws() {
        try {
            buildWeeklySlots(
                Instant.parse("2026-11-10T02:00:00Z"),
                Instant.parse("2026-11-10T01:00:00Z"),
                1,
            )
            fail("Expected an IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun zeroWeeks_throws() {
        try {
            buildWeeklySlots(
                Instant.parse("2026-11-10T01:00:00Z"),
                Instant.parse("2026-11-10T02:00:00Z"),
                0,
            )
            fail("Expected an IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }
}
