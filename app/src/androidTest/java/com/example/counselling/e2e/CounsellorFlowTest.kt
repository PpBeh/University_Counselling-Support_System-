// Instrumented UI tests | Owner: shared (Beh, Jeremy, Michael)
// Phase C: the counsellor journey, signed in with the seeded Daniel account.
package com.example.counselling.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.counselling.MainActivity
import com.example.counselling.support.TestAccounts
import com.example.counselling.support.assertTextVisible
import com.example.counselling.support.awaitContentDescription
import com.example.counselling.support.awaitGone
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
}
