// Module 1 | Owner: Beh
package com.example.counselling.feature.student.data

import com.example.counselling.core.data.BaseSupabaseRepository
import com.example.counselling.core.data.ErrorTranslator
import com.example.counselling.core.data.UserViewRow
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.PhoneNumber
import com.example.counselling.core.domain.SessionMode
import com.example.counselling.core.domain.Student
import com.example.counselling.core.domain.UserFactory
import com.example.counselling.feature.student.domain.StudentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// OOP: Repository implementation - the Supabase queries live here; ViewModels
// only see the StudentRepository interface and domain Student objects.
class SupabaseStudentRepository(
    supabase: SupabaseClient,
    errorTranslator: ErrorTranslator = ErrorTranslator(),
) : BaseSupabaseRepository(supabase, errorTranslator), StudentRepository {

    override suspend fun getStudents(): Result<List<Student>> =
        runQuery("Could not load the student list.") {
            supabase
                .from("v_students")
                .select { order("full_name", Order.ASCENDING) }
                .decodeList<UserViewRow>()
                .map(::toDomain)
        }

    override suspend fun getStudent(userId: String): Result<Student?> =
        runQuery("Could not load the student profile.") {
            supabase
                .from("v_students")
                .select {
                    filter { eq("id", userId) }
                    limit(1)
                }
                .decodeSingleOrNull<UserViewRow>()
                ?.let(::toDomain)
        }

    override suspend fun updateProfile(
        userId: String,
        fullName: String,
        programme: String,
        phone: PhoneNumber,
        preferredMode: SessionMode,
    ): Result<Student> = runQuery("Could not update your profile.") {
        val params = buildJsonObject {
            put("p_full_name", fullName.trim())
            put("p_programme", programme.trim())
            put("p_phone", phone.value)
            put("p_preferred_session_mode", preferredMode.code)
        }
        supabase.postgrest.rpc("update_student_profile", params)

        supabase
            .from("v_students")
            .select {
                filter { eq("id", userId) }
                limit(1)
            }
            .decodeSingleOrNull<UserViewRow>()
            ?.let(::toDomain)
            ?: throw DomainException("Your updated profile could not be reloaded.")
    }

    override suspend fun deactivateAccount(): Result<Unit> =
        runQuery("Could not deactivate your account.") {
            supabase.postgrest.rpc("deactivate_account")
            Unit
        }

    private fun toDomain(row: UserViewRow): Student {
        val user = UserFactory.fromRecord(row.toRecord())
        return user as? Student
            ?: throw DomainException("That user is not a student.")
    }
}
