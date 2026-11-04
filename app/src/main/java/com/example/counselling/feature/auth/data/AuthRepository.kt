// Module 1 | Owner: Beh
// Thin wrapper around Supabase Auth so ViewModels never handle raw exceptions.
package com.example.counselling.feature.auth.data

import com.example.counselling.core.ErrorMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.JsonObject

class AuthRepository(private val supabase: SupabaseClient) {

    suspend fun awaitReady() {
        supabase.auth.awaitInitialization()
    }

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun signIn(email: String, password: String): Result<Unit> = try {
        supabase.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error)))
    }

    /**
     * Creates the Auth account with the metadata read by the database trigger.
     * The trigger itself chooses student/counsellor/admin and builds profiles.
     */
    suspend fun signUp(
        email: String,
        password: String,
        metadata: JsonObject,
    ): Result<Unit> = try {
        supabase.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
            data = metadata
        }
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Registration failed. Please try again.")))
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> = try {
        supabase.auth.updateUser {
            password = newPassword
        }
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not update your password.")))
    }

    suspend fun signOut(): Result<Unit> = try {
        supabase.auth.signOut()
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not sign out. Please try again.")))
    }
}
