// Instrumented UI tests | Owner: shared (Beh, Jeremy, Michael)
// Phase B2: the student write paths. These tests create real records and clean
// up after themselves so the demo project stays usable.
package com.example.counselling.e2e

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.counselling.MainActivity
import com.example.counselling.support.TestAccounts
import com.example.counselling.support.assertTextVisible
import com.example.counselling.support.awaitAnyText
import com.example.counselling.support.awaitEnabled
import com.example.counselling.support.awaitNewTextMatching
import com.example.counselling.support.awaitText
import com.example.counselling.support.clickTab
import com.example.counselling.support.goBack
import com.example.counselling.support.invokeClick
import com.example.counselling.support.pickAnyFreeTimeSlot
import com.example.counselling.support.scrollToLabel
import com.example.counselling.support.selectSheetOption
import com.example.counselling.support.signIn
import com.example.counselling.support.tap
import com.example.counselling.support.tapAndAwait
import com.example.counselling.support.tapBottomEndFab
import com.example.counselling.support.tapContentDescription
import com.example.counselling.support.tapEnquiryFab
import com.example.counselling.support.tapLast
import com.example.counselling.support.tapTextNear
import com.example.counselling.support.textExists
import com.example.counselling.support.textsMatching
import com.example.counselling.support.turnSwitchOn
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudentWriteFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun signInAsStudent() {
        rule.signIn(TestAccounts.STUDENT_EMAIL, TestAccounts.PASSWORD)
    }

    /**
     * Books a session, reschedules it, then cancels it. Uses the slot the app
     * offers so it also covers the date-strip and time-chip interaction.
     */
    @Test
    fun appointmentLifecycle_booksReschedulesAndCancels() {
        rule.clickTab("Sessions")
        rule.awaitText("Your sessions")
        rule.tapBottomEndFab()
        rule.awaitText("What would you like support with?")

        rule.tap("Stress")
        rule.tapAndAwait("Continue", "Best matches", substring = true)
        rule.tap("Best match")
        rule.tapAndAwait("Continue", "Available times")

        val bookedTime = rule.pickAnyFreeTimeSlot()
        if (bookedTime == null) {
            // Every offered date was fully booked; the slot step handled it.
            rule.assertTextVisible("Available times")
            return
        }
        rule.awaitEnabled("Confirm appointment")
        rule.tapAndAwait("Confirm appointment", "Appointment confirmed")
        rule.tapAndAwait("View my appointments", "Your sessions")

        // The booked session is the only upcoming one; its swipe actions are
        // always composed, so invoke them directly instead of hunting the card.
        rule.invokeClick("Reschedule")
        rule.awaitText("Choose a new time")

        val movedTime = rule.pickAnyFreeTimeSlot()
        if (movedTime == null) {
            rule.assertTextVisible("No free slots right now")
            rule.goBack()
            rule.awaitText("Your sessions")
        } else {
            rule.awaitEnabled("Confirm new time")
            rule.tapAndAwait("Confirm new time", "Appointment rescheduled")
            rule.awaitText("Your sessions")
        }

        rule.invokeClick("Cancel")
        rule.awaitText("Cancel appointment?")
        rule.tapLast("Cancel appointment")
        rule.awaitText("Appointment cancelled.")
    }

    @Test
    fun enquiry_submitsLinkedQuestionAndWithdrawsIt() {
        rule.clickTab("Enquiries")
        rule.awaitText("Your enquiries")
        val existingCodes = rule.textsMatching(Regex("""^ENQ-[A-Z0-9]{6}$"""))
        rule.tapEnquiryFab()
        rule.awaitText("New enquiry")

        rule.tap("Concern category")
        rule.selectSheetOption("Stress")

        rule.onAllNodes(hasSetTextAction() and hasText("Your question"))[0]
            .performTextReplacement("Automated UI test question about exam stress.")
        rule.tapAndAwait("Send question", "Question submitted")
        rule.tap("Done")

        // Withdraw only the enquiry this test just created.
        val reference = rule.awaitNewTextMatching(
            Regex("""^ENQ-[A-Z0-9]{6}$"""),
            existingCodes,
        )
        rule.tapAndAwait(reference, "Withdraw enquiry", substring = true)
        rule.tap("Withdraw enquiry")
        rule.awaitText("Withdraw this enquiry?")
        rule.tapLast("Withdraw")
        rule.awaitText("Enquiry withdrawn.")
    }

    @Test
    fun booking_allowsChoosingANonMatchedCounsellor() {
        rule.clickTab("Sessions")
        rule.awaitText("Your sessions")
        rule.tapBottomEndFab()
        rule.awaitText("What would you like support with?")

        rule.tap("Stress")
        rule.tapAndAwait("Continue", "Best matches", substring = true)

        // Daniel does not specialise in Stress, so Smart Match lists him under
        // Other counsellors. Choosing him must still move on to his timetable.
        rule.awaitText("Other counsellors")
        rule.tap("Mr. Daniel Lim")
        rule.awaitEnabled("Continue")
        rule.tapAndAwait("Continue", "Choose a time with Mr. Daniel Lim")
    }

    @Test
    fun profile_passwordValidation_rejectsWeakAndMismatchedPasswords() {
        rule.clickTab("Profile")
        rule.awaitText("Change password")

        val fields = rule.onAllNodes(hasSetTextAction())
        fields[3].performTextReplacement("short")
        fields[4].performTextReplacement("different")

        rule.invokeClick("Change password")
        rule.awaitText("Password must be at least 8 characters.")
        rule.assertTextVisible("Passwords do not match.")
    }

    /** Saves the profile unchanged, then opens and cancels the deactivate dialog. */
    @Test
    fun profile_savesDetailsAndDeactivationCanBeCancelled() {
        rule.clickTab("Profile")
        rule.awaitText("Save profile")

        rule.tapAndAwait("Save profile", "Profile updated.")

        rule.tap("Deactivate account")
        rule.awaitText("Deactivate account?")
        rule.tapLast("Cancel")

        // Still signed in and on the profile screen.
        rule.assertTextVisible("Sign out")
        rule.assertTextVisible("Profile")
    }

    /** Rates a completed session, then removes the rating again. */
    @Test
    fun feedback_canBeSubmittedAndRemoved() {
        rule.tapAndAwait("Your feedback", "Rate a completed session")
        rule.awaitAnyText("Remove rating", "Rate this session")

        if (rule.textExists("Rate this session")) {
            rule.tapAndAwait("Rate this session", "Rate your session")
            rule.tapContentDescription("4 stars")
            rule.onAllNodes(hasSetTextAction() and hasText("Comment (optional)"))[0]
                .performTextReplacement("Automated feedback comment.")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            rule.tapAndAwait("Submit feedback", "Thank you for your feedback.")
            rule.awaitText("Remove rating")

            rule.tapAndAwait("Remove rating", "Remove this rating?")
            rule.tapLast("Remove rating")
            rule.awaitText("Feedback removed.")
        } else {
            // The demo already has ratings; verify the delete path without
            // destroying the seeded feedback.
            rule.tapAndAwait("Remove rating", "Remove this rating?")
            rule.tapLast("Cancel")
            rule.assertTextVisible("Remove rating")
        }
    }

    /** Creates an anonymous enquiry, checks it by code, then withdraws it. */
    @Test
    fun anonymousEnquiry_canBeLookedUpByCodeAndWithdrawn() {
        rule.clickTab("Enquiries")
        rule.awaitText("Your enquiries")
        val existingCodes = rule.textsMatching(Regex("""^ENQ-[A-Z0-9]{6}$"""))

        rule.tapEnquiryFab()
        rule.awaitText("New enquiry")
        rule.tap("Concern category")
        rule.selectSheetOption("Anxiety")
        rule.onAllNodes(hasSetTextAction() and hasText("Your question"))[0]
            .performTextReplacement("Anonymous automated question about anxiety.")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        rule.turnSwitchOn()
        rule.tapAndAwait("Send question", "Question submitted")
        val code = rule.awaitNewTextMatching(Regex("""^ENQ-[A-Z0-9]{6}$"""), existingCodes)
        rule.tap("Done")

        rule.scrollToLabel("Check a reply by code")
        rule.onAllNodes(hasSetTextAction())[0].performTextReplacement(code)
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        rule.tapAndAwait("Check reply", "No reply yet.")

        rule.tapTextNear("Withdraw", "No reply yet.")
        rule.awaitText("Withdraw anonymous enquiry?")
        rule.tapLast("Withdraw")
        rule.awaitText("Enquiry withdrawn.")
    }
}
