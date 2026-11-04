// Module 1 | Owner: Beh
package com.example.counselling.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatorsTest {

    @Test
    fun malaysianPhone_formatsAccepted() {
        assertNull(Validators.phone("012-3456789"))
        assertNull(Validators.phone("+60123456789"))
    }

    @Test
    fun malaysianPhone_invalidRejected() {
        assertEquals(
            "Use a Malaysian number such as 012-3456789 or +60123456789.",
            Validators.phone("12345"),
        )
    }

    @Test
    fun password_requiresUpperLowerAndDigit() {
        assertEquals("Password must be at least 8 characters.", Validators.password("Ab1"))
        assertEquals(
            "Password needs at least one uppercase letter.",
            Validators.password("abcdefg1"),
        )
        assertNull(Validators.password("Demo@12345"))
    }

    @Test
    fun confirmPassword_mustMatch() {
        assertEquals("Passwords do not match.", Validators.confirmPassword("Secret123", "Secret124"))
        assertNull(Validators.confirmPassword("Secret123", "Secret123"))
    }
}
