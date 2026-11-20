// Core infrastructure tests | Owner: shared
package com.example.counselling.core.data

import com.example.counselling.core.ErrorMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorTranslatorTest {

    private val translator = ErrorTranslator()

    @Test
    fun translatesKnownSupabaseAndNetworkErrors() {
        assertEquals(
            "Email or password is incorrect.",
            translator.translate(Exception("Invalid login credentials")),
        )
        assertEquals(
            "You do not have permission to do that.",
            translator.translate(Exception("new row violates row-level security policy")),
        )
        assertEquals(
            "Cannot reach the server. Check your internet connection and try again.",
            translator.translate(Exception("Failed to fetch")),
        )
        assertEquals(
            "That time slot was just booked. Please choose another slot.",
            translator.translate(Exception("That time slot was just booked")),
        )
    }

    @Test
    fun unknownErrorsUseTheCallerFallback() {
        assertEquals(
            "Could not load the profile.",
            translator.translate(Exception("strange backend text"), "Could not load the profile."),
        )
        assertEquals(
            "Something went wrong. Please try again.",
            translator.translate(Exception("strange backend text")),
        )
    }

    @Test
    fun temporaryErrorMapperFacadeDelegatesToSameRules() {
        assertEquals(
            "Email or password is incorrect.",
            ErrorMapper.message(Exception("invalid login credentials")),
        )
    }
}
