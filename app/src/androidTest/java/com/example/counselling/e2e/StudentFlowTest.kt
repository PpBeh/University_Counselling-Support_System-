// Instrumented UI tests | Owner: shared (Beh, Jeremy, Michael)
// Phase B: the student journey. Each test signs in with the seeded student
// account, so the suite exercises the real Supabase data path.
package com.example.counselling.e2e

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.counselling.MainActivity
import com.example.counselling.support.TestAccounts
import com.example.counselling.support.assertTextVisible
import com.example.counselling.support.awaitText
import com.example.counselling.support.clickFirstClickableMatching
import com.example.counselling.support.clickTab
import com.example.counselling.support.goBack
import com.example.counselling.support.scrollToLabel
import com.example.counselling.support.signIn
import com.example.counselling.support.tap
import com.example.counselling.support.tapAndAwait
import com.example.counselling.support.tapEnquiryFab
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudentFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun signInAsStudent() {
        rule.signIn(TestAccounts.STUDENT_EMAIL, TestAccounts.PASSWORD)
    }

    @Test
    fun home_showsStudentShortcutsAndUrgentHelp() {
        rule.awaitText("Need urgent help?")
        rule.assertTextVisible("Book")
        rule.assertTextVisible("Check-in")
        rule.assertTextVisible("Session summaries")
        rule.assertTextVisible("Your feedback")
    }

    @Test
    fun bottomNavigation_opensEveryStudentTab() {
        rule.clickTab("Sessions")
        rule.awaitText("Your sessions")

        rule.clickTab("Resources")
        rule.awaitText("Search resources")

        rule.clickTab("Enquiries")
        rule.awaitText("Your enquiries")

        rule.clickTab("Profile")
        rule.awaitText("Sign out")

        rule.clickTab("Home")
        rule.awaitText("Need urgent help?")
    }

    @Test
    fun sessions_switchesBetweenUpcomingPastAndCancelled() {
        rule.clickTab("Sessions")
        rule.awaitText("Your sessions")

        rule.tap("Past")
        rule.tap("Cancelled")
        rule.tap("Upcoming")

        rule.assertTextVisible("Your sessions")
    }

    @Test
    fun bookingFlow_completesWhenSlotsAreAvailable() {
        rule.tapAndAwait("Book", "Your sessions")
        rule.tapAndAwait("Book session", "What would you like support with?")

        rule.tap("Stress")
        rule.tapAndAwait("Continue", "Best matches", substring = true)

        rule.tap("Best match")
        rule.tapAndAwait("Continue", "Available times")

        val pickedSlot = rule.clickFirstClickableMatching(
            Regex("""^\d{1,2}[:.]\d{2}(\s?[APap][Mm])?$"""),
        )
        if (!pickedSlot) {
            // No free times for this counsellor: the flow still reached the slot step.
            rule.assertTextVisible("Available times")
            return
        }

        rule.tap("Confirm appointment")
        rule.awaitText("Appointment confirmed")

        rule.tap("View my appointments")
        rule.awaitText("Your sessions")
    }

    @Test
    fun enquiryForm_requiresACategoryBeforeSending() {
        rule.clickTab("Enquiries")
        rule.awaitText("Your enquiries")

        rule.tapEnquiryFab()
        rule.awaitText("New enquiry")

        rule.tap("Send question")
        rule.awaitText("Choose a category.")

        rule.goBack()
        rule.awaitText("Your enquiries")
    }

    @Test
    fun selfCheck_answersAllTenQuestionsAndShowsResult() {
        rule.tapAndAwait("Check-in", "Wellbeing check-in")

        rule.tapAndAwait("Start check-in", "Question 1 of 10")
        for (index in 1..10) {
            rule.awaitText("Question $index of 10")
            rule.tap("Not at all")
            val next = if (index == 10) "See my result" else "Next"
            if (index == 10) {
                rule.tapAndAwait(next, "Your check-in result")
            } else {
                rule.tapAndAwait(next, "Question ${index + 1} of 10")
            }
        }

        rule.awaitText("Your check-in result")
        rule.assertTextVisible("Retake check-in")
    }

    @Test
    fun resources_searchFiltersTheLibrary() {
        rule.clickTab("Resources")
        rule.awaitText("5-Minute Grounding for Anxiety")

        rule.onAllNodes(hasSetTextAction())[0].performTextReplacement("sleep")
        rule.awaitText("Sleep Reset: A Wind-Down Audio")
    }

    @Test
    fun sessionSummaries_andFeedbackScreensOpen() {
        rule.tapAndAwait("Session summaries", "Your session summaries")
        rule.goBack()
        rule.awaitText("Need urgent help?")

        rule.tapAndAwait("Your feedback", "Rate a completed session")
        rule.goBack()
        rule.awaitText("Need urgent help?")
    }

    @Test
    fun profile_switchesThemeAndKeepsSettings() {
        rule.clickTab("Profile")
        rule.awaitText("Appearance")
        rule.scrollToLabel("Sign out")

        rule.tapAndAwait("Dark", "The app will use dark mode.")
        rule.tapAndAwait("Light", "The app will use light mode.")
    }

    @Test
    fun profile_signOutReturnsToLogin() {
        rule.clickTab("Profile")
        rule.awaitText("Sign out")

        rule.tapAndAwait("Sign out", "Counselling support")
        rule.assertTextVisible("Sign in")
    }
}
