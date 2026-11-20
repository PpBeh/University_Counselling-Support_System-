// Core domain tests | Owner: shared
package com.example.counselling.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDomainTest {

    private fun student() = Student(
        id = "student-1",
        email = Email.of("aina@example.com"),
        fullName = "Aina Zulaikha",
        phone = PhoneNumber.of("012-3456789"),
        isActive = true,
        studentId = StudentId.of("DAMCS2401"),
        programme = "Diploma in Computer Science",
        preferredMode = SessionMode.IN_PERSON,
    )

    private fun counsellor() = Counsellor(
        id = "counsellor-1",
        email = Email.of("aisyah@example.com"),
        fullName = "Dr. Aisyah Rahman",
        phone = null,
        isActive = true,
        specialisations = listOf(ConcernCategory.ANXIETY, ConcernCategory.STRESS),
        languages = listOf("English", "Bahasa Melayu"),
        bio = "Campus counsellor",
    )

    private fun administrator() = Administrator(
        id = "admin-1",
        email = Email.of("admin@example.com"),
        fullName = "System Administrator",
        phone = null,
        isActive = true,
    )

    @Test
    fun student_hasStudentTabsPermissionsAndDeactivationRule() {
        val user = student()
        assertEquals(
            listOf(
                HomeTab.HOME,
                HomeTab.APPOINTMENTS,
                HomeTab.RESOURCES,
                HomeTab.ENQUIRIES,
                HomeTab.PROFILE,
            ),
            user.homeTabs(),
        )
        assertTrue(user.permissions.canBookAppointments())
        assertTrue(user.permissions.canGiveFeedback())
        assertTrue(user.permissions.canDeactivateOwnAccount())
        assertFalse(user.permissions.canViewPrivateSessionNotes())
        assertFalse(user.permissions.canManageResources())
        assertTrue(user.canDeactivate(0))
        assertFalse(user.canDeactivate(1))
        assertThrows(DomainException::class.java) { user.canDeactivate(-1) }
    }

    @Test
    fun counsellor_ownsSpecialisationsLanguagesAndSessionPermissions() {
        val user = counsellor()
        assertTrue(user.supports(ConcernCategory.ANXIETY))
        assertFalse(user.supports(ConcernCategory.FINANCIAL))
        assertTrue(user.speaks("english"))
        assertFalse(user.speaks("Mandarin"))
        assertTrue(user.permissions.canViewPrivateSessionNotes())
        assertTrue(user.permissions.canManageAvailability())
        assertTrue(user.permissions.canAccessEnquiryInbox())
        assertFalse(user.permissions.canViewAllAppointments())
        assertEquals(
            listOf(
                HomeTab.HOME,
                HomeTab.SCHEDULE,
                HomeTab.RECORDS,
                HomeTab.ENQUIRIES,
                HomeTab.PROFILE,
            ),
            user.homeTabs(),
        )
    }

    @Test
    fun administrator_canOverseeButCannotReadPrivateNotesOrEnquiries() {
        val user = administrator()
        assertTrue(user.permissions.canManageCounsellors())
        assertTrue(user.permissions.canViewStudentDirectory())
        assertTrue(user.permissions.canViewAllAppointments())
        assertFalse(user.permissions.canViewPrivateSessionNotes())
        assertFalse(user.permissions.canAccessEnquiryInbox())
        assertEquals(
            listOf(
                HomeTab.HOME,
                HomeTab.COUNSELLORS,
                HomeTab.STUDENTS,
                HomeTab.RESOURCES,
                HomeTab.PROFILE,
            ),
            user.homeTabs(),
        )
    }

    @Test
    fun homeGreeting_isPolymorphicAndMatchesTheUiCopy() {
        assertEquals(
            "You do not have to manage everything alone. What feels useful today?",
            student().homeGreeting(),
        )
        assertEquals(
            "Your space to support students with care and privacy.",
            counsellor().homeGreeting(),
        )
        assertEquals(
            "A calm overview of the counselling service.",
            administrator().homeGreeting(),
        )
    }

    @Test
    fun counsellor_requiresSpecialisationsAndLanguages() {
        assertThrows(DomainException::class.java) {
            Counsellor(
                id = "counsellor-2",
                email = Email.of("new@example.com"),
                fullName = "New Counsellor",
                phone = null,
                isActive = true,
                specialisations = emptyList(),
                languages = listOf("English"),
                bio = "",
            )
        }
        assertThrows(DomainException::class.java) {
            Counsellor(
                id = "counsellor-3",
                email = Email.of("new2@example.com"),
                fullName = "New Counsellor",
                phone = null,
                isActive = true,
                specialisations = listOf(ConcernCategory.STRESS),
                languages = emptyList(),
                bio = "",
            )
        }
    }
}
