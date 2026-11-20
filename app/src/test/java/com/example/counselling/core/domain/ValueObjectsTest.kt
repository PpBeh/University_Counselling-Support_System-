// Core domain tests | Owner: shared
package com.example.counselling.core.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ValueObjectsTest {

    @Test
    fun email_normalisesAndRejectsInvalidValues() {
        assertEquals("aina@example.com", Email.of(" AINA@Example.com ").value)
        assertThrows(DomainException::class.java) { Email.of("") }
        assertThrows(DomainException::class.java) { Email.of("not-an-email") }
    }

    @Test
    fun phone_acceptsMalaysianFormatsOnly() {
        assertEquals("012-3456789", PhoneNumber.of("012-3456789").value)
        assertEquals("+60123456789", PhoneNumber.of(" +60123456789 ").value)
        assertThrows(DomainException::class.java) { PhoneNumber.of("12345") }
        assertThrows(DomainException::class.java) { PhoneNumber.of("") }
    }

    @Test
    fun password_enforcesStrengthAndDoesNotLeakInToString() {
        val password = Password.of("Demo@12345")
        assertTrue(password.matches("Demo@12345"))
        assertFalse(password.matches("demo@12345"))
        assertEquals("Password(***)", password.toString())

        assertThrows(DomainException::class.java) { Password.of("Ab1") }
        assertThrows(DomainException::class.java) { Password.of("abcdefg1") }
        assertThrows(DomainException::class.java) { Password.of("ABCDEFG1") }
        assertThrows(DomainException::class.java) { Password.of("Abcdefgh") }
    }

    @Test
    fun studentId_trimsAndEnforcesLength() {
        assertEquals("DAMCS2401", StudentId.of(" DAMCS2401 ").value)
        assertThrows(DomainException::class.java) { StudentId.of("") }
        assertThrows(DomainException::class.java) { StudentId.of("AB") }
        assertThrows(DomainException::class.java) { StudentId.of("A".repeat(31)) }
    }

    @Test
    fun timeSlot_acceptsFutureOrderedRangeAndRejectsPastForNewBookings() {
        val now = Instant.parse("2026-11-20T09:00:00Z")
        val slot = TimeSlot.of(
            Instant.parse("2026-11-20T10:00:00Z"),
            Instant.parse("2026-11-20T11:00:00Z"),
            now,
        )
        assertEquals(60, slot.durationMinutes())
        assertTrue(slot.isFuture(now))

        assertThrows(DomainException::class.java) {
            TimeSlot.of(
                Instant.parse("2026-11-20T11:00:00Z"),
                Instant.parse("2026-11-20T10:00:00Z"),
                now,
            )
        }
        assertThrows(DomainException::class.java) {
            TimeSlot.of(now, Instant.parse("2026-11-20T11:00:00Z"), now)
        }
    }

    @Test
    fun timeSlot_historicalStorageStillAllowsPastSessions() {
        val stored = TimeSlot.fromStored(
            Instant.parse("2026-01-10T10:00:00Z"),
            Instant.parse("2026-01-10T11:00:00Z"),
        )
        assertFalse(stored.isFuture(Instant.parse("2026-11-20T09:00:00Z")))
    }

    @Test
    fun timeSlot_overlapUsesHalfOpenRanges() {
        val first = TimeSlot.fromStored(
            Instant.parse("2026-11-20T10:00:00Z"),
            Instant.parse("2026-11-20T11:00:00Z"),
        )
        val overlapping = TimeSlot.fromStored(
            Instant.parse("2026-11-20T10:30:00Z"),
            Instant.parse("2026-11-20T11:30:00Z"),
        )
        val touching = TimeSlot.fromStored(
            Instant.parse("2026-11-20T11:00:00Z"),
            Instant.parse("2026-11-20T12:00:00Z"),
        )
        assertTrue(first.overlaps(overlapping))
        assertFalse(first.overlaps(touching))
    }

    @Test
    fun rating_onlyAcceptsOneToFive() {
        assertEquals(5, Rating.of(5).value)
        assertThrows(DomainException::class.java) { Rating.of(0) }
        assertThrows(DomainException::class.java) { Rating.of(6) }
    }

    @Test
    fun referenceCode_isNormalisedAndFormatChecked() {
        assertEquals("ENQ-ABC123", ReferenceCode.of(" enq-abc123 ").value)
        assertThrows(DomainException::class.java) { ReferenceCode.of("ABC123") }
        assertThrows(DomainException::class.java) { ReferenceCode.of("ENQ-123") }
    }
}
