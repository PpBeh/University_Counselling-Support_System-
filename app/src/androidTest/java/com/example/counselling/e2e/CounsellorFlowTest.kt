// Instrumented UI tests | Owner: shared (Beh, Jeremy, Michael)
// Phase C: the counsellor journey, signed in with the seeded Daniel account.
package com.example.counselling.e2e

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.counselling.MainActivity
import com.example.counselling.support.TestAccounts
import com.example.counselling.support.UI_TIMEOUT
import com.example.counselling.support.assertTextVisible
import com.example.counselling.support.awaitContentDescription
import com.example.counselling.support.awaitGone
import com.example.counselling.support.awaitText
import com.example.counselling.support.clickFirstClickableMatching
import com.example.counselling.support.clickTab
import com.example.counselling.support.goBack
import com.example.counselling.support.signIn
import com.example.counselling.support.tap
import com.example.counselling.support.tapAndAwait
import com.example.counselling.support.tapContentDescription
import com.example.counselling.support.tapContentDescriptionNear
import com.example.counselling.support.tapLast
import com.example.counselling.support.textExists
import com.example.counselling.support.turnSwitchOn
import com.example.counselling.support.visibleTexts
import com.example.counselling.support.waitFor
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CounsellorFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun signInAsCounsellor() {
        rule.signIn(TestAccounts.COUNSELLOR_EMAIL, TestAccounts.PASSWORD)
    }

    @Test
    fun home_showsTodayAndManagementRows() {
        rule.awaitText("Today")
        rule.assertTextVisible("Follow-ups")
        rule.assertTextVisible("My availability")
        rule.assertTextVisible("Student directory")
        rule.assertTextVisible("Session feedback")
    }

    @Test
    fun bottomNavigation_opensEveryCounsellorTab() {
        rule.clickTab("Schedule")
        rule.awaitText("Manage my availability")

        rule.clickTab("Records")
        rule.awaitText("Session records")

        rule.clickTab("Enquiries")
        rule.awaitText("Student questions")

        rule.clickTab("Profile")
        rule.awaitText("Sign out")

        rule.clickTab("Home")
        rule.awaitText("Today")
    }

    @Test
    fun availability_opensFromHome() {
        rule.tapAndAwait("My availability", "Add availability")
        rule.goBack()
        rule.awaitText("Today")
    }

    @Test
    fun studentDirectory_opensAndSearches() {
        rule.tap("Student directory")
        rule.awaitContentDescription("Search students")
        rule.tapContentDescription("Search students")
        rule.awaitText("Search by name or student ID")
        rule.goBack()
        rule.awaitText("Today")
    }

    @Test
    fun records_triageTabOpens() {
        rule.clickTab("Records")
        rule.awaitText("Session records")
        rule.assertTextVisible("Show archived")

        rule.tap("Triage board", substring = true)
        rule.awaitGone("Show archived")
    }

    @Test
    fun feedbackScreen_opensFromHome() {
        rule.tapAndAwait("Session feedback", "Student ratings")
        rule.goBack()
        rule.awaitText("Today")
    }

    /**
     * Creates a session record for the seeded completed session, then archives
     * and permanently deletes it so the next run starts clean.
     */
    @Test
    fun sessionRecord_canBeCreatedArchivedAndDeleted() {
        rule.clickTab("Records")
        rule.awaitText("Session records")
        rule.tapAndAwait("Add", "Private notes")

        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextReplacement("Private note created by the automated UI test.")
        fields[1].performTextReplacement("Automated summary for the session record.")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        rule.tap("Medium")
        rule.tapAndAwait("Create record", "Session record created.")
        rule.awaitText("Active records")

        rule.tapAndAwait("Michael Goh", "Archive record")
        rule.tap("Archive record")
        rule.awaitText("Session record archived.")

        // The list must refresh in place, without leaving the screen.
        rule.turnSwitchOn()
        if (!rule.waitFor(UI_TIMEOUT) { rule.textExists("Archived") }) {
            throw AssertionError("Archived header missing. Visible: ${rule.visibleTexts(40)}")
        }
        rule.tapAndAwait("Michael Goh", "Delete forever")
        rule.tap("Delete forever")
        rule.awaitText("Delete archived record permanently?")
        rule.tapLast("Delete forever")
        rule.awaitText("Archived session record permanently deleted.")
    }

    /** Creates a free slot, then deletes it so availability stays unchanged. */
    @Test
    fun availability_canBeCreatedAndDeleted() {
        rule.tapAndAwait("My availability", "Add availability")
        rule.tapAndAwait(
            "Add availability",
            "Set a day, start time and duration.",
            substring = true,
        )

        rule.clickFirstClickableMatching(Regex("""^[A-Z][a-z]{2} \d{1,2} [A-Z][a-z]{2}$"""))
        rule.tap("6:30 PM")
        rule.tapAndAwait("Save availability", "Availability added.")

        // The list must jump to the top and show the new earlier slot in place.
        rule.awaitText("29 Nov 2026")
        rule.tapContentDescriptionNear("Delete slot", "29 Nov 2026")
        rule.awaitText("Delete this free slot?")
        rule.tapLast("Delete slot")
        rule.awaitText("Availability slot deleted.")
    }
}
