// Core domain tests | Owner: shared
package com.example.counselling.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyTest {

    @Test
    fun concernCategories_resolveCodesAndLabels() {
        assertEquals(ConcernCategory.ACADEMIC_PRESSURE, ConcernCategory.fromWire("Academic Pressure"))
        assertEquals(ConcernCategory.ACADEMIC_PRESSURE, ConcernCategory.fromWire("academic_pressure"))
        assertNull(ConcernCategory.fromWire("Not a category"))
        assertEquals(8, ConcernCategory.entries.size)
    }

    @Test
    fun sessionModes_resolveDatabaseCodes() {
        assertEquals(SessionMode.IN_PERSON, SessionMode.fromWire("in_person"))
        assertEquals(SessionMode.ONLINE, SessionMode.fromWire("Online"))
        assertNull(SessionMode.fromWire("hybrid"))
    }

    @Test
    fun riskLevel_ownsSeverityBehaviour() {
        assertEquals(RiskLevel.HIGH, RiskLevel.fromWire("high"))
        assertTrue(RiskLevel.HIGH.isHigh())
        assertFalse(RiskLevel.MEDIUM.isHigh())
        assertEquals(3, RiskLevel.HIGH.rank)
    }

    @Test
    fun stressBracket_ownsScoreWindows() {
        assertEquals(StressBracket.LOW, StressBracket.forScore(0))
        assertEquals(StressBracket.LOW, StressBracket.forScore(13))
        assertEquals(StressBracket.MODERATE, StressBracket.forScore(14))
        assertEquals(StressBracket.MODERATE, StressBracket.forScore(26))
        assertEquals(StressBracket.HIGH, StressBracket.forScore(27))
        assertEquals(StressBracket.HIGH, StressBracket.forScore(40))
        assertTrue(StressBracket.HIGH.contains(35))
        assertFalse(StressBracket.HIGH.contains(20))
        assertThrows(DomainException::class.java) { StressBracket.forScore(41) }
        assertThrows(DomainException::class.java) { StressBracket.forScore(-1) }
    }
}
