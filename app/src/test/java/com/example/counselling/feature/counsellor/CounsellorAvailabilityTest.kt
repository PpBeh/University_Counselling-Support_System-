// Module 3 domain tests | Owner: Jeremy
package com.example.counselling.feature.counsellor

import com.example.counselling.core.domain.AvailabilitySlot
import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.Counsellor
import com.example.counselling.core.domain.Email
import com.example.counselling.core.domain.TimeSlot
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CounsellorAvailabilityTest {

    private val now = Instant.parse("2026-11-20T09:00:00Z")

    private fun slot(id: String, startHours: Long, booked: Boolean = false) = AvailabilitySlot(
        id = id,
        counsellorId = "counsellor-1",
        timeSlot = TimeSlot.fromStored(
            now.plusSeconds(startHours * 3600),
            now.plusSeconds(startHours * 3600 + 3600),
        ),
        isBooked = booked,
    )

    private fun counsellor(vararg slots: AvailabilitySlot) = Counsellor(
        id = "counsellor-1",
        email = Email.of("counsellor@example.com"),
        fullName = "Counsellor",
        phone = null,
        isActive = true,
        specialisations = listOf(ConcernCategory.ANXIETY),
        languages = listOf("English"),
        bio = "",
        availability = slots.toList(),
    )

    @Test
    fun slotRules_bookedAndPastSlotsAreNotEditable() {
        assertFalse(slot("past", -2).isEditable(now))
        assertFalse(slot("booked", 2, booked = true).isEditable(now))
        assertTrue(slot("free", 2).isEditable(now))
    }

    @Test
    fun counsellorHidesBookedAndPastSlotsFromUpcomingFreeSlots() {
        val counsellor = counsellor(
            slot("free", 2),
            slot("booked", 3, booked = true),
            slot("past", -1),
        )

        assertTrue(counsellor.availableThisWeek(now))
        assertEquals(listOf("free"), counsellor.upcomingFreeSlots(now).map { it.id })
        assertFalse(counsellor.withAvailability(emptyList()).availableThisWeek(now))
    }
}
