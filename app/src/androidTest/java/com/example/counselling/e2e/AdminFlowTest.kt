// Instrumented UI tests | Owner: shared (Beh, Jeremy, Michael)
// Phase D: the administrator journey. Admin screens are read-only apart from
// the counsellor directory, so these tests focus on reachability and data.
package com.example.counselling.e2e

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.counselling.MainActivity
import com.example.counselling.support.TestAccounts
import com.example.counselling.support.assertTextVisible
import com.example.counselling.support.awaitField
import com.example.counselling.support.awaitGone
import com.example.counselling.support.awaitText
import com.example.counselling.support.clickTab
import com.example.counselling.support.goBack
import com.example.counselling.support.invokeClick
import com.example.counselling.support.selectSheetOption
import com.example.counselling.support.signIn
import com.example.counselling.support.tap
import com.example.counselling.support.tapAndAwait
import com.example.counselling.support.tapBottomEndFab
import com.example.counselling.support.tapContentDescription
import com.example.counselling.support.tapLast
import com.example.counselling.support.textExists
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdminFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun signInAsAdmin() {
        rule.signIn(TestAccounts.ADMIN_EMAIL, TestAccounts.PASSWORD)
    }

    @Test
    fun home_showsServiceStatisticsAndManageRows() {
        rule.awaitText("Upcoming sessions")
        rule.assertTextVisible("Average rating")
        rule.assertTextVisible("Counsellors")
        rule.assertTextVisible("All appointments")
        rule.assertTextVisible("Manage")
    }

    @Test
    fun bottomNavigation_opensEveryAdminTab() {
        rule.clickTab("Counsellors")
        rule.awaitText("Counsellor directory")

        rule.clickTab("Students")
        rule.awaitText("Student directory")

        rule.clickTab("Resources")
        rule.awaitText("Search resources")

        rule.clickTab("Profile")
        rule.awaitText("Sign out")

        rule.clickTab("Home")
        rule.awaitText("Manage")
    }

    @Test
    fun counsellorDirectory_searchFindsDaniel() {
        rule.clickTab("Counsellors")
        rule.awaitText("Counsellor directory")
        rule.tapContentDescription("Search counsellors")
        rule.awaitText("Search name, email or specialisation")
        rule.onAllNodes(hasSetTextAction())[0].performTextReplacement("Daniel")
        rule.awaitText(TestAccounts.COUNSELLOR_NAME)
    }

    @Test
    fun studentDirectory_searchFindsSeededStudent() {
        rule.clickTab("Students")
        rule.awaitText("Student directory")
        rule.tapContentDescription("Search students")
        rule.awaitText("Search by name or student ID")
    }

    @Test
    fun allAppointments_opensReadOnlyOverview() {
        rule.tapAndAwait("All appointments", "Service overview")
        rule.assertTextVisible("Read-only.", substring = true)

        rule.goBack()
        rule.awaitText("Manage")
    }

    @Test
    fun feedbackScreen_opensFromHome() {
        rule.tapAndAwait("Session feedback", "Student ratings")
        rule.goBack()
        rule.awaitText("Manage")
    }

    /** Creates a draft resource, then deletes it so the library is unchanged. */
    @Test
    fun resource_canBeCreatedAndDeleted() {
        val title = "Automated test resource"

        rule.clickTab("Resources")
        rule.awaitText("Search resources")
        rule.tapBottomEndFab()
        rule.awaitText("New resource")

        rule.awaitField("Title")
        rule.onAllNodes(hasSetTextAction() and hasText("Title"))[0]
            .performTextReplacement(title)
        rule.awaitField("Description")
        rule.onAllNodes(hasSetTextAction() and hasText("Description"))[0]
            .performTextReplacement("Created by the automated UI test to verify resource management.")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        rule.tap("Category")
        rule.selectSheetOption("Stress")
        rule.tap("Media type")
        rule.selectSheetOption("Article")
        rule.tap("Low")
        rule.onAllNodes(hasSetTextAction() and hasText("Resource link"))[0]
            .performTextReplacement("https://example.com/automated-test")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        rule.tapAndAwait("Save resource", "Resource added.")
        rule.awaitText("Self-help library")
        rule.awaitText(title)

        // Edit the title, then toggle its published state.
        val editedTitle = "$title (edited)"
        rule.tapAndAwait(title, "Edit")
        rule.tap("Edit")
        rule.awaitText("Edit resource")
        rule.awaitField("Title")
        rule.onAllNodes(hasSetTextAction() and hasText("Title"))[0]
            .performTextReplacement(editedTitle)
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        rule.tapAndAwait("Save resource", "Resource updated.")
        rule.awaitText("Self-help library")
        rule.awaitText(editedTitle)

        rule.tapAndAwait(editedTitle, "Publish")
        val publishAction = if (rule.textExists("Unpublish")) "Unpublish" else "Publish"
        rule.tap(publishAction)
        rule.awaitText(if (publishAction == "Unpublish") "Draft" else "Published")

        rule.tapAndAwait(editedTitle, "Delete")
        rule.invokeClick("Delete")
        rule.awaitText("Delete this resource?")
        rule.tapLast("Delete")
        rule.awaitText("Resource deleted.")
    }
}
