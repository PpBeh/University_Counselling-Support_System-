// Core infrastructure | Owner: shared (Beh, Jeremy, Michael)
package com.example.counselling.core.data

import com.example.counselling.core.domain.User
import com.example.counselling.core.domain.UserFactory
import com.example.counselling.core.domain.UserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

// OOP: Repository implementation - queries the three safe user views and uses
// UserFactory to create the right subclass. The rest of the app sees User.
class SupabaseUserRepository(
    supabase: SupabaseClient,
    errorTranslator: ErrorTranslator = ErrorTranslator(),
) : BaseSupabaseRepository(supabase, errorTranslator), UserRepository {

    override suspend fun getCurrentUser(authUserId: String): Result<User> =
        runQuery("Could not load your profile.") {
            val row = findIn("v_students", authUserId)
                ?: findIn("v_counsellors", authUserId)
                ?: findIn("v_administrators", authUserId)
                ?: throw IllegalStateException("Your profile could not be found. Please contact the administrator.")

            UserFactory.fromRecord(row.toRecord())
        }

    private suspend fun findIn(view: String, authUserId: String): UserViewRow? =
        supabase
            .from(view)
            .select {
                filter { eq("auth_user_id", authUserId) }
                limit(1)
            }
            .decodeSingleOrNull<UserViewRow>()
}
