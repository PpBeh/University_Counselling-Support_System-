// Core domain tests | Owner: shared
package com.example.counselling.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UserFactoryTest {

    @Test
    fun studentRecordCreatesStudentSubclass() {
        val user = UserFactory.fromRecord(
            UserRecord(
                id = "student-1",
                email = "aina@example.com",
                fullName = "Aina Zulaikha",
                phone = "012-3456789",
                userType = "student",
                studentId = "DAMCS2401",
                programme = "Diploma in Computer Science",
                preferredSessionMode = "in_person",
            ),
        )

        assertTrue(user is Student)
        user as Student
        assertEquals(StudentId.of("DAMCS2401"), user.studentId)
        assertEquals(SessionMode.IN_PERSON, user.preferredMode)
        assertEquals(5, user.homeTabs().size)
    }

    @Test
    fun counsellorRecordResolvesAssociations() {
        val user = UserFactory.fromRecord(
            UserRecord(
                id = "counsellor-1",
                email = "aisyah@example.com",
                fullName = "Dr. Aisyah Rahman",
                userType = "counsellor",
                specialisations = listOf("Anxiety", "academic_pressure"),
                languages = listOf("English", "Bahasa Melayu"),
                bio = "Campus counsellor",
            ),
        )

        assertTrue(user is Counsellor)
        user as Counsellor
        assertTrue(user.supports(ConcernCategory.ANXIETY))
        assertTrue(user.supports(ConcernCategory.ACADEMIC_PRESSURE))
        assertTrue(user.speaks("bahasa melayu"))
    }

    @Test
    fun administratorRecordCreatesAdminSubclass() {
        val user = UserFactory.fromRecord(
            UserRecord(
                id = "admin-1",
                email = "admin@example.com",
                fullName = "System Administrator",
                userType = "administrator",
            ),
        )

        assertTrue(user is Administrator)
        assertTrue(user.permissions.canViewAllAppointments())
    }

    @Test
    fun invalidOrIncompleteRecordsAreRejected() {
        assertThrows(DomainException::class.java) {
            UserFactory.fromRecord(
                UserRecord(
                    id = "unknown-1",
                    email = "unknown@example.com",
                    fullName = "Unknown User",
                    userType = "superuser",
                ),
            )
        }
        assertThrows(DomainException::class.java) {
            UserFactory.fromRecord(
                UserRecord(
                    id = "student-2",
                    email = "student@example.com",
                    fullName = "Incomplete Student",
                    userType = "student",
                    programme = "Diploma",
                    preferredSessionMode = "in_person",
                ),
            )
        }
        assertThrows(DomainException::class.java) {
            UserFactory.fromRecord(
                UserRecord(
                    id = "counsellor-2",
                    email = "counsellor@example.com",
                    fullName = "Unknown Specialisation",
                    userType = "counsellor",
                    specialisations = listOf("Astrology"),
                    languages = listOf("English"),
                ),
            )
        }
    }
}
