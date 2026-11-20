// Module 1 | Owner: Beh
package com.example.counselling.feature.student.domain

import com.example.counselling.core.domain.PhoneNumber
import com.example.counselling.core.domain.SessionMode
import com.example.counselling.core.domain.Student

// OOP: Abstraction - profile/directory ViewModels depend on this contract.
interface StudentRepository {

    suspend fun getStudents(): Result<List<Student>>

    suspend fun getStudent(userId: String): Result<Student?>

    suspend fun updateProfile(
        userId: String,
        fullName: String,
        programme: String,
        phone: PhoneNumber,
        preferredMode: SessionMode,
    ): Result<Student>

    suspend fun deactivateAccount(): Result<Unit>
}
