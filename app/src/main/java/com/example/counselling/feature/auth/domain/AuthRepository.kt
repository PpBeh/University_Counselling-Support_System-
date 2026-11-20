// Module 1 / Team auth | Owner: Beh
package com.example.counselling.feature.auth.domain

import com.example.counselling.core.domain.Email
import com.example.counselling.core.domain.Password

// OOP: Abstraction - SessionManager and auth ViewModels depend on this contract,
// not on the Supabase Auth implementation.
interface AuthRepository {

    suspend fun awaitReady()

    fun currentUserId(): String?

    suspend fun signIn(email: Email, password: Password): Result<Unit>

    suspend fun signUp(details: RegistrationDetails): Result<Unit>

    suspend fun updatePassword(password: Password): Result<Unit>

    suspend fun signOut(): Result<Unit>
}
