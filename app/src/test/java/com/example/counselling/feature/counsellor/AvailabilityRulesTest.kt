// Module 3 | Owner: Jeremy
package com.example.counselling.feature.counsellor

import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.TimeSlot
import com.example.counselling.feature.counsellor.domain.WeeklyAvailabilityPattern
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AvailabilityRulesTest {

    @Test
    fun threeWeekRepeat_createsThreeRowsSevenDaysApart() {
        val start = Instant.parse("2026-11-10T01:00:00Z")
        val end = Instant.parse("2026-11-10T02:00:00Z")

        val slots = WeeklyAvailabilityPattern(TimeSlot.fromStored(start, end), 3).generate()

        assertEquals(3, slots.size)
        assertEquals(7, ChronoUnit.DAYS.between(slots[0].start, slots[1].start))
        assertEquals(14, ChronoUnit.DAYS.between(slots[0].start, slots[2].start))
        assertTrue(slots.all { ChronoUnit.MINUTES.between(it.start, it.end) == 60L })
    }

    @Test
    fun endBeforeStart_throws() {
        assertThrows(DomainException::class.java) {
            TimeSlot.fromStored(
                Instant.parse("2026-11-10T02:00:00Z"),
                Instant.parse("2026-11-10T01:00:00Z"),
            )
        }
    }

    @Test
    fun zeroWeeks_throws() {
        assertThrows(DomainException::class.java) {
            WeeklyAvailabilityPattern(
                TimeSlot.fromStored(
                    Instant.parse("2026-11-10T01:00:00Z"),
                    Instant.parse("2026-11-10T02:00:00Z"),
                ),
                0,
            ).generate()
        }
    }
}
