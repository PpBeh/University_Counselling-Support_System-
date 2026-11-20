// Core domain | Owner: shared (Beh, Jeremy, Michael)
// Factory that turns a database/view read model into the correct User subclass.
package com.example.counselling.core.domain

// OOP: Data Transfer Object - a read-only record from v_students/v_counsellors/
// v_administrators. It is not a domain entity; UserFactory converts it.
data class UserRecord(
    val id: String,
    val email: String,
    val fullName: String,
    val phone: String? = null,
    val isActive: Boolean = true,
    val authUserId: String? = null,
    val userType: String,
    val studentId: String? = null,
    val programme: String? = null,
    val preferredSessionMode: String? = null,
    val specialisations: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val bio: String = "",
)

// OOP: Factory - the one place allowed to inspect the database discriminator
// and construct the correct subclass. UI code never switches on a role string.
object UserFactory {

    fun fromRecord(record: UserRecord): User {
        val email = Email.of(record.email)
        val phone = record.phone
            ?.takeIf { it.isNotBlank() }
            ?.let(PhoneNumber::of)

        return when (record.userType.trim().lowercase()) {
            "student" -> {
                val studentId = record.studentId
                    ?: throw DomainException("Student record is missing its student ID.")
                val programme = record.programme
                    ?: throw DomainException("Student record is missing its programme.")
                val mode = SessionMode.fromWire(record.preferredSessionMode)
                    ?: throw DomainException("Student record has an invalid preferred mode.")

                Student(
                    id = record.id,
                    email = email,
                    fullName = record.fullName,
                    phone = phone,
                    isActive = record.isActive,
                    studentId = StudentId.of(studentId),
                    programme = programme,
                    preferredMode = mode,
                    authUserId = record.authUserId,
                )
            }

            "counsellor" -> {
                val specialisations = record.specialisations.map { value ->
                    ConcernCategory.fromWire(value)
                        ?: throw DomainException("Unknown counsellor specialisation: $value")
                }
                Counsellor(
                    id = record.id,
                    email = email,
                    fullName = record.fullName,
                    phone = phone,
                    isActive = record.isActive,
                    specialisations = specialisations,
                    languages = record.languages,
                    bio = record.bio,
                    authUserId = record.authUserId,
                )
            }

            "administrator", "admin" -> Administrator(
                id = record.id,
                email = email,
                fullName = record.fullName,
                phone = phone,
                isActive = record.isActive,
                authUserId = record.authUserId,
            )

            else -> throw DomainException("Unknown user type: ${record.userType}")
        }
    }
}
