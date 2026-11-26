// Core settings tests | Owner: shared
package com.example.counselling.core.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun labelsRoundTripToTheStoredMode() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromLabel(mode.label))
        }
    }

    @Test
    fun unknownStoredLabelFallsBackToSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromLabel("AMOLED"))
    }
}
