// Module 1 / Team auth | Owner: Beh
// Value object carrying the validated student registration fields that the
// database sign-up trigger reads from Auth metadata.
package com.example.counselling.feature.auth.domain

import com.example.counselling.core.domain.Email
import com.example.counselling.core.domain.Password
import com.example.counselling.core.domain.PhoneNumber
import com.example.counselling.core.domain.SessionMode
import com.example.counselling.core.domain.StudentId

// OOP: Encapsulation - registration fields are already valid value objects.
data class RegistrationDetails(
    val email: Email,
    val password: Password,
    val fullName: String,
    val studentId: StudentId,
    val programme: String,
    val phone: PhoneNumber,
    val preferredMode: SessionMode,
) {
    init {
        if (fullName.trim().length !in 2..100) {
            throw com.example.counselling.core.domain.DomainException(
                "Full name must be between 2 and 100 characters.",
            )
        }
        if (programme.trim().length !in 2..120) {
            throw com.example.counselling.core.domain.DomainException(
                "Programme must be between 2 and 120 characters.",
            )
        }
    }
}
