// Instrumented UI tests | Owner: shared (Beh, Jeremy, Michael)
// Cross-role flows: a student acts, staff respond, the student sees the result.
// Each test signs out and back in as the other role and cleans up after itself.
package com.example.counselling.e2e

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.counselling.MainActivity
import com.example.counselling.support.TestAccounts
import com.example.counselling.support.UI_TIMEOUT
import com.example.counselling.support.assertTextVisible
import com.example.counselling.support.awaitEnabled
import com.example.counselling.support.awaitNewTextMatching
import com.example.counselling.support.awaitText
import com.example.counselling.support.clickTab
import com.example.counselling.support.invokeClick
import com.example.counselling.support.pickAnyFreeTimeSlot
import com.example.counselling.support.scrollToLabel
import com.example.counselling.support.selectSheetOption
import com.example.counselling.support.signIn
import com.example.counselling.support.tap
import com.example.counselling.support.tapAndAwait
import com.example.counselling.support.tapBottomEndFab
import com.example.counselling.support.tapEnquiryFab
import com.example.counselling.support.tapLast
import com.example.counselling.support.tapTextNear
import com.example.counselling.support.textExists
import com.example.counselling.support.textsMatching
import com.example.counselling.support.turnSwitchOn
import com.example.counselling.support.visibleTexts
import com.example.counselling.support.waitFor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrossRoleFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    /**
     * Student books "Financial" (Daniel-only specialisation). The booking is
     * auto-confirmed, Daniel sees it in his schedule, then the student cancels.
     */
    @Test
    fun booking_isVisibleToCounsellorAndCancelledByStudent() {
        rule.signIn(TestAccounts.STUDENT_EMAIL, TestAccounts.PASSWORD)
        rule.clickTab("Sessions")
        rule.awaitText("Your sessions")
        rule.tapBottomEndFab()
        rule.awaitText("What would you like support with?")

        rule.tap("Financial")
        rule.tapAndAwait("Continue", "Best matches", substring = true)
        rule.tap("Best match")
        rule.tapAndAwait("Continue", "Available times")

        val time = rule.pickAnyFreeTimeSlot()
        if (time == null) {
            rule.assertTextVisible("Available times")
            return
        }
        rule.awaitEnabled("Confirm appointment")
        rule.tapAndAwait("Confirm appointment", "Appointment confirmed")
        rule.tap("View my appointments")

        rule.signIn(TestAccounts.COUNSELLOR_EMAIL, TestAccounts.PASSWORD)
        rule.clickTab("Schedule")
        rule.awaitText("Your sessions")
        rule.tapAndAwait(time, "Student: Michael Goh", substring = true)
        androidx.test.espresso.Espresso.pressBack()

        rule.signIn(TestAccounts.STUDENT_EMAIL, TestAccounts.PASSWORD)
        rule.clickTab("Sessions")
        rule.awaitText("Your sessions")
        rule.invokeClick("Cancel")
        rule.awaitText("Cancel appointment?")
        rule.tapLast("Cancel appointment")
        rule.awaitText("Appointment cancelled.")
    }

    /** Student asks, Daniel replies in the inbox, student reads it by code. */
    @Test
    fun enquiryReply_isReadableByStudentByCode() {
        val codePattern = Regex("""^ENQ-[A-Z0-9]{6}$""")

        rule.signIn(TestAccounts.STUDENT_EMAIL, TestAccounts.PASSWORD)
        rule.clickTab("Enquiries")
        rule.awaitText("Your enquiries")
        val existingCodes = rule.textsMatching(codePattern)

        rule.tapEnquiryFab()
        rule.awaitText("New enquiry")
        rule.tap("Concern category")
        rule.selectSheetOption("Sleep")
        rule.onAllNodes(hasSetTextAction() and hasText("Your question"))[0]
            .performTextReplacement("Automated cross-role reply question.")
        rule.tapAndAwait("Send question", "Question submitted")
        val code = rule.awaitNewTextMatching(codePattern, existingCodes)
        rule.tap("Done")

        rule.signIn(TestAccounts.COUNSELLOR_EMAIL, TestAccounts.PASSWORD)
        rule.clickTab("Enquiries")
        rule.awaitText("Student questions")
        rule.tapAndAwait(code, "Reply", substring = true)
        rule.tapLast("Reply")
        rule.awaitText("Save reply")
        rule.onAllNodes(hasSetTextAction() and hasText("Reply"))[0]
            .performTextReplacement("Automated counsellor reply.")
        // Keep the enquiry open so the student can still withdraw it afterwards.
        rule.tap("Status")
        rule.selectSheetOption("Open")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        rule.tapAndAwait("Save reply", "Enquiry reply saved.")

        rule.signIn(TestAccounts.STUDENT_EMAIL, TestAccounts.PASSWORD)
        rule.clickTab("Enquiries")
        rule.awaitText("Your enquiries")
        rule.scrollToLabel("Check a reply by code")
        rule.onAllNodes(hasSetTextAction())[0].performTextReplacement(code)
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        rule.tapAndAwait("Check reply", "Automated counsellor reply.")

        // Withdraw the replied enquiry so the demo inbox is unchanged. Anchor on
        // the reply text so this can never hit another row's swipe action.
        rule.tapTextNear("Withdraw", "Automated counsellor reply.")
        val dialog = rule.waitFor(UI_TIMEOUT) {
            rule.textExists("Withdraw anonymous enquiry?") ||
                rule.textExists("Withdraw this enquiry?")
        }
        if (!dialog) throw AssertionError("No withdraw dialog. Visible: ${rule.visibleTexts(40)}")
        rule.tapLast("Withdraw")
        rule.awaitText("Enquiry withdrawn.")
    }

    /**
     * Daniel writes a session record, proposes a follow-up on a free slot, the
     * student confirms it, then cancels it. The record is archived and deleted
     * afterwards so the demo data is unchanged.
     */
    @Test
    fun followUp_isProposedByCounsellorAndConfirmedByStudent() {
        rule.signIn(TestAccounts.COUNSELLOR_EMAIL, TestAccounts.PASSWORD)
        rule.clickTab("Records")
        rule.awaitText("Session records")
        rule.tapAndAwait("Add", "Private notes")

        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextReplacement("Follow-up test private note for the session.")
        fields[1].performTextReplacement("Follow-up test public summary.")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        rule.tap("Low")
        rule.tapAndAwait("Create record", "Session record created.")
        rule.awaitText("Active records")

        rule.tapAndAwait("Michael Goh", "Schedule follow-up")
        rule.tap("Schedule follow-up")
        rule.awaitText("Choose one of your free slots")
        val slot = rule.pickAnyFreeTimeSlot()
        if (slot == null) {
            rule.assertTextVisible("No free slots available")
            return
        }
        rule.awaitEnabled("Send follow-up request")
        rule.tapAndAwait("Send follow-up request", "Follow-up proposed")
        rule.tap("Back to records")

        rule.signIn(TestAccounts.STUDENT_EMAIL, TestAccounts.PASSWORD)
        rule.clickTab("Sessions")
        rule.awaitText("Your sessions")
        rule.tapAndAwait("Follow-up", "Confirm your follow-up", substring = true)
        rule.tap("Confirm session")
        rule.awaitText("Follow-up confirmed.")

        rule.invokeClick("Cancel")
        rule.awaitText("Cancel appointment?")
        rule.tapLast("Cancel appointment")
        rule.awaitText("Appointment cancelled.")

        // Remove the session record so the next run starts from "Ready for notes".
        rule.signIn(TestAccounts.COUNSELLOR_EMAIL, TestAccounts.PASSWORD)
        rule.clickTab("Records")
        rule.awaitText("Session records")
        rule.tapAndAwait("Michael Goh", "Archive record")
        rule.tap("Archive record")
        rule.awaitText("Session record archived.")
        rule.clickTab("Home")
        rule.clickTab("Records")
        rule.awaitText("Show archived")
        rule.turnSwitchOn()
        rule.tapAndAwait("Michael Goh", "Delete forever")
        rule.tap("Delete forever")
        rule.awaitText("Delete archived record permanently?")
        rule.tapLast("Delete forever")
        rule.awaitText("Archived session record permanently deleted.")
    }
}
