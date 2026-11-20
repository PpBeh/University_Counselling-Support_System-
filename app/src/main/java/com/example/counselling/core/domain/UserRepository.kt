// Core domain | Owner: shared (Beh, Jeremy, Michael)
package com.example.counselling.core.domain

// OOP: Abstraction - SessionManager depends on this user-loading contract.
interface UserRepository {

    suspend fun getCurrentUser(authUserId: String): Result<User>
}
