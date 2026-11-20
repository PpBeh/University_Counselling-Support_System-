// Core | Owner: shared (Beh, Jeremy, Michael)
// OOP: Encapsulation/state holder - the signed-in User and auth lifecycle are
// private state exposed through a read-only StateFlow. UI code asks this object
// to sign in/out instead of calling Supabase Auth directly.
package com.example.counselling.core

import com.example.counselling.core.domain.Email
import com.example.counselling.core.domain.Password
import com.example.counselling.core.domain.User
import com.example.counselling.core.domain.UserRepository
import com.example.counselling.feature.auth.domain.AuthRepository
import com.example.counselling.feature.auth.domain.RegistrationDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SessionState {
    data object Loading : SessionState

    /** notice is used for a deactivated account or a one-off login message. */
    data class SignedOut(val notice: UiMessage? = null) : SessionState

    data class SignedIn(val user: User) : SessionState
}

class SessionManager(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var initialized = false

    val currentUser: User?
        get() = (_state.value as? SessionState.SignedIn)?.user

    suspend fun initialize() {
        if (initialized) return
        initialized = true
        _state.value = SessionState.Loading
        runCatching { authRepository.awaitReady() }
        restoreUserOrSignOut()
    }

    /**
     * Returns success(false) when Supabase requires email confirmation and no
     * session was created. Returns success(true) when the user is signed in.
     */
    suspend fun signUp(details: RegistrationDetails): Result<Boolean> {
        val authResult = authRepository.signUp(details)
        if (authResult.isFailure) {
            return Result.failure(authResult.exceptionOrNull() ?: Exception("Registration failed."))
        }

        if (authRepository.currentUserId() == null) {
            _state.value = SessionState.SignedOut()
            return Result.success(false)
        }

        return loadSignedInUser().fold(
            onSuccess = { Result.success(true) },
            onFailure = { Result.failure(it) },
        )
    }

    suspend fun signIn(email: Email, password: Password): Result<Unit> {
        val authResult = authRepository.signIn(email, password)
        if (authResult.isFailure) {
            return Result.failure(authResult.exceptionOrNull() ?: Exception("Sign in failed."))
        }
        return loadSignedInUser()
    }

    suspend fun signOut() {
        authRepository.signOut()
        _state.value = SessionState.SignedOut()
    }

    /** Re-reads the User after a profile edit. */
    suspend fun refreshProfile(): Result<Unit> = loadSignedInUser()

    fun clearNotice() {
        val current = _state.value
        if (current is SessionState.SignedOut && current.notice != null) {
            _state.value = SessionState.SignedOut()
        }
    }

    private suspend fun restoreUserOrSignOut() {
        val userId = authRepository.currentUserId()
        if (userId == null) {
            _state.value = SessionState.SignedOut()
            return
        }
        loadUser(userId)
    }

    private suspend fun loadSignedInUser(): Result<Unit> {
        val userId = authRepository.currentUserId()
        if (userId == null) {
            _state.value = SessionState.SignedOut()
            return Result.failure(Exception("Your session could not be created. Please sign in again."))
        }
        return loadUser(userId)
    }

    private suspend fun loadUser(authUserId: String): Result<Unit> {
        val result = userRepository.getCurrentUser(authUserId)
        val user = result.getOrElse { error ->
            authRepository.signOut()
            _state.value = SessionState.SignedOut()
            return Result.failure(Exception(ErrorMapper.message(error, "Could not load your profile.")))
        }

        if (!user.isActive) {
            authRepository.signOut()
            val message = "Your account has been deactivated. Please contact the counselling office."
            _state.value = SessionState.SignedOut(UiMessage(message, MessageKind.ERROR))
            return Result.failure(Exception(message))
        }

        _state.value = SessionState.SignedIn(user)
        return Result.success(Unit)
    }
}
