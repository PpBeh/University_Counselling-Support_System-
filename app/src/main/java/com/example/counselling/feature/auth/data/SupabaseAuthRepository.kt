// Module 1 / Team auth | Owner: Beh
package com.example.counselling.feature.auth.data

import com.example.counselling.core.data.BaseSupabaseRepository
import com.example.counselling.core.data.ErrorTranslator
import com.example.counselling.core.domain.Email
import com.example.counselling.core.domain.Password
import com.example.counselling.feature.auth.domain.AuthRepository
import com.example.counselling.feature.auth.domain.RegistrationDetails
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email as SupabaseEmail
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// OOP: Repository implementation - hides Supabase Auth behind AuthRepository.
class SupabaseAuthRepository(
    supabase: SupabaseClient,
    errorTranslator: ErrorTranslator = ErrorTranslator(),
) : BaseSupabaseRepository(supabase, errorTranslator), AuthRepository {

    override suspend fun awaitReady() {
        supabase.auth.awaitInitialization()
    }

    override fun currentUserId(): String? =
        supabase.auth.currentUserOrNull()?.id

    override suspend fun signIn(
        email: Email,
        password: Password,
    ): Result<Unit> = runQuery("Sign in failed. Please try again.") {
        supabase.auth.signInWith(SupabaseEmail) {
            this.email = email.value
            this.password = password.reveal()
        }
        Unit
    }

    override suspend fun signUp(details: RegistrationDetails): Result<Unit> =
        runQuery("Registration failed. Please try again.") {
            val metadata = buildJsonObject {
                put("full_name", details.fullName.trim())
                put("student_id", details.studentId.value)
                put("programme", details.programme.trim())
                put("phone", details.phone.value)
                put("preferred_session_mode", details.preferredMode.code)
            }
            supabase.auth.signUpWith(SupabaseEmail) {
                email = details.email.value
                password = details.password.reveal()
                data = metadata
            }
            Unit
        }

    override suspend fun updatePassword(password: Password): Result<Unit> =
        runQuery("Could not update your password.") {
            val rawPassword = password.reveal()
            supabase.auth.updateUser { this.password = rawPassword }
            Unit
        }

    override suspend fun signOut(): Result<Unit> =
        runQuery("Could not sign out. Please try again.") {
            supabase.auth.signOut()
            Unit
        }
}
