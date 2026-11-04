// Module 1 | Owner: Beh
// Profile reads and updates. Row-level security decides whether the caller is
// reading their own profile, a staff-visible student, or updating their own.
package com.example.counselling.feature.student.data

import com.example.counselling.core.ErrorMapper
import com.example.counselling.feature.student.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class ProfileRepository(private val supabase: SupabaseClient) {

    suspend fun getProfile(userId: String): Result<Profile?> = try {
        val profile = supabase
            .from("profiles")
            .select {
                filter { eq("id", userId) }
                limit(1)
            }
            .decodeSingleOrNull<Profile>()
        Result.success(profile)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not load the profile.")))
    }

    /** Staff-only in practice: RLS returns only student profiles to counsellors/admins. */
    suspend fun getStudents(): Result<List<Profile>> = try {
        val students = supabase
            .from("profiles")
            .select {
                filter { eq("role", "student") }
                order("full_name", Order.ASCENDING)
            }
            .decodeList<Profile>()
        Result.success(students)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not load the student list.")))
    }

    suspend fun updateStudentProfile(
        userId: String,
        fullName: String,
        programme: String,
        phone: String,
        preferredMode: String,
    ): Result<Profile> = try {
        val updated = supabase
            .from("profiles")
            .update(
                {
                    set("full_name", fullName.trim())
                    set("programme", programme.trim())
                    set("phone", phone.trim())
                    set("preferred_session_mode", preferredMode)
                },
            ) {
                select()
                filter { eq("id", userId) }
            }
            .decodeSingle<Profile>()
        Result.success(updated)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not update your profile.")))
    }

    suspend fun deactivateAccount(): Result<Unit> = try {
        supabase.postgrest.rpc("deactivate_account")
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not deactivate your account.")))
    }
}
