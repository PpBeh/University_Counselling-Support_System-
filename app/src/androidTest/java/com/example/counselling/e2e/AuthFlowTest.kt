// Instrumented UI tests | Owner: shared (Beh, Jeremy, Michael)
// Phase A: the signed-out surface. Validation runs entirely on-device, but the
// wrong-password case goes through Supabase Auth so it covers real errors.
package com.example.counselling.e2e

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.counselling.MainActivity
import com.example.counselling.support.TestAccounts
import com.example.counselling.support.assertTextVisible
import com.example.counselling.support.awaitText
import com.example.counselling.support.ensureSignedOut
import com.example.counselling.support.tap
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun startSignedOut() {
        rule.ensureSignedOut()
    }

    @Test
    fun loginScreen_showsBrandingAndActions() {
        rule.awaitText("Counselling support")
        rule.assertTextVisible("Sign in")
        rule.assertTextVisible("New here? Create a student account")
        rule.assertTextVisible("Email")
        rule.assertTextVisible("Password")
    }

    @Test
    fun login_emptySubmit_showsBothFieldErrors() {
        rule.tap("Sign in")
        rule.awaitText("Email is required.")
        rule.awaitText("Password is required.")
    }

    @Test
    fun login_invalidEmailFormat_showsFormatError() {
        rule.onAllNodes(hasSetTextAction())[0].performTextReplacement("not-an-email")
        rule.onAllNodes(hasSetTextAction())[1].performTextReplacement("Demo@12345")
        rule.tap("Sign in")
        rule.awaitText("Enter a valid email address.")
    }

    @Test
    fun login_weakPasswordFormat_showsRuleError() {
        rule.onAllNodes(hasSetTextAction())[0]
            .performTextReplacement(TestAccounts.STUDENT_EMAIL)
        rule.onAllNodes(hasSetTextAction())[1].performTextReplacement("alllowercase")
        rule.tap("Sign in")
        rule.awaitText("Password needs at least one uppercase letter.")
    }

    @Test
    fun login_wrongPassword_showsServerErrorAndStaysSignedOut() {
        rule.onAllNodes(hasSetTextAction())[0]
            .performTextReplacement(TestAccounts.STUDENT_EMAIL)
        rule.onAllNodes(hasSetTextAction())[1].performTextReplacement("WrongPass123")
        rule.tap("Sign in")
        rule.awaitText("Email or password is incorrect.")
        rule.assertTextVisible("Sign in")
    }

    @Test
    fun register_opensFromLoginAndValidatesRequiredFields() {
        rule.tap("New here? Create a student account")
        rule.awaitText("Create account")
        rule.tap("Create my account")
        rule.awaitText("Full name is required.")
        rule.assertTextVisible("Student ID is required.")
        rule.assertTextVisible("Email is required.")
    }

    @Test
    fun register_backReturnsToLogin() {
        rule.tap("New here? Create a student account")
        rule.awaitText("Create account")
        tapContentDescriptionBack()
        rule.awaitText("Counselling support")
        rule.assertTextVisible("Sign in")
    }

    private fun tapContentDescriptionBack() {
        rule.onAllNodes(
            androidx.compose.ui.test.hasContentDescription("Go back") and hasClickAction(),
        )[0].performClick()
    }
}
