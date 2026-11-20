// Core domain tests | Owner: Beh
package com.example.counselling.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentStatusTest {

    @Test
    fun pending_canConfirmOrCancelAndCanBeChangedByStudent() {
        assertTrue(AppointmentStatus.Pending.canTransitionTo(AppointmentStatus.Confirmed))
        assertTrue(AppointmentStatus.Pending.canTransitionTo(AppointmentStatus.Cancelled))
        assertFalse(AppointmentStatus.Pending.canTransitionTo(AppointmentStatus.Completed))
        assertTrue(AppointmentStatus.Pending.canReschedule())
        assertTrue(AppointmentStatus.Pending.canCancel())
    }

    @Test
    fun confirmed_canCompleteNoShowOrCancel() {
        assertEquals(
            setOf(
                AppointmentStatus.Completed,
                AppointmentStatus.NoShow,
                AppointmentStatus.Cancelled,
            ),
            AppointmentStatus.Confirmed.allowedNextStatuses(),
        )
        assertTrue(AppointmentStatus.Confirmed.canReschedule())
        assertTrue(AppointmentStatus.Confirmed.canCancel())
    }

    @Test
    fun terminalStatuses_haveNoNextStateAndNoActions() {
        listOf(
            AppointmentStatus.Completed,
            AppointmentStatus.NoShow,
            AppointmentStatus.Cancelled,
        ).forEach { status ->
            assertTrue(status.isTerminal)
            assertTrue(status.allowedNextStatuses().isEmpty())
            assertFalse(status.canReschedule())
            assertFalse(status.canCancel())
        }
    }

    @Test
    fun fromWire_mapsDatabaseCodesAndRejectsUnknown() {
        assertEquals(AppointmentStatus.Pending, AppointmentStatus.fromWire("pending"))
        assertEquals(AppointmentStatus.NoShow, AppointmentStatus.fromWire(" NO_SHOW "))
        assertEquals("No show", AppointmentStatus.NoShow.label)
        assertThrows(DomainException::class.java) { AppointmentStatus.fromWire("unknown") }
    }
}
