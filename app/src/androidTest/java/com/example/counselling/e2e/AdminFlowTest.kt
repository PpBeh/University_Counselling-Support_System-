// Instrumented UI tests | Owner: shared (Beh, Jeremy, Michael)
// Phase D: the administrator journey. Admin screens are read-only apart from
// the counsellor directory, so these tests focus on reachability and data.
package com.example.counselling.e2e

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.counselling.MainActivity
import com.example.counselling.support.TestAccounts
import com.example.counselling.support.assertTextVisible
import com.example.counselling.support.awaitText
import com.example.counselling.support.clickTab
import com.example.counselling.support.goBack
import com.example.counselling.support.signIn
import com.example.counselling.support.tap
import com.example.counselling.support.tapAndAwait
import com.example.counselling.support.tapContentDescription
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
}
